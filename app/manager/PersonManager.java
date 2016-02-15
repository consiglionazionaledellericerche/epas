package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import dao.AbsenceDao;
import dao.ContractDao;
import dao.PersonChildrenDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import models.AbsenceType;
import models.Contract;
import models.ContractWorkingTimeType;
import models.Person;
import models.PersonDay;
import models.WorkingTimeTypeDay;
import models.enumerate.EpasParam;
import models.enumerate.EpasParam.EpasParamValueType.DayMonth;

import org.joda.time.LocalDate;
import org.joda.time.MonthDay;

import play.db.jpa.JPA;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.Query;

public class PersonManager {

  private final ContractDao contractDao;
  private final PersonDao personDao;
  private final PersonDayDao personDayDao;
  private final PersonDayManager personDayManager;
  private final IWrapperFactory wrapperFactory;
  private final AbsenceDao absenceDao;
  private final ConfigurationManager configurationManager;

  @Inject
  public PersonManager(ContractDao contractDao,
      PersonChildrenDao personChildrenDao, 
      PersonDao personDao,
      PersonDayDao personDayDao, 
      AbsenceDao absenceDao,
      PersonDayManager personDayManager,
      IWrapperFactory wrapperFactory, 
      ConfigurationManager configurationManager) {
    this.contractDao = contractDao;
    this.personDao = personDao;
    this.personDayDao = personDayDao;
    this.absenceDao = absenceDao;
    this.personDayManager = personDayManager;
    this.wrapperFactory = wrapperFactory;
    this.configurationManager = configurationManager;
  }

  /**
   * Se il giorno è festivo per la persona.
   */
  public boolean isHoliday(Person person, LocalDate date) {

    DayMonth patron = (DayMonth)configurationManager
        .configValue(person.office, EpasParam.DAY_OF_PATRON, date);
    
    if (DateUtility
        .isGeneralHoliday(Optional.fromNullable(new MonthDay(patron.month, patron.day)), date)) {
      return true;
    }

    Contract contract = contractDao.getContract(date, person);
    if (contract == null) {
      //persona fuori contratto
      return false;
    }

    for (ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {
      if (DateUtility.isDateIntoInterval(date,
          new DateInterval(cwtt.beginDate, cwtt.endDate))) {

        int dayOfWeekIndex = date.getDayOfWeek() - 1;
        WorkingTimeTypeDay wttd = cwtt.workingTimeType
            .workingTimeTypeDays.get(dayOfWeekIndex);
        Preconditions.checkState(wttd.dayOfWeek == date.getDayOfWeek());
        return wttd.holiday;

      }
    }

    throw new IllegalStateException();
    //se il db è consistente non si verifica mai
    //return false;

  }

  /**
   * @return false se l'id passato alla funzione non trova tra le persone presenti in anagrafica,
   * una che avesse nella vecchia applicazione un id uguale a quello che la sequence postgres genera
   * automaticamente all'inserimento di una nuova persona in anagrafica. In particolare viene
   * controllato il campo oldId presente per ciascuna persona e si verifica che non esista un valore
   * uguale a quello che la sequence postgres ha generato
   */
  public boolean isIdPresentInOldSoftware(Long id) {
    Person person = personDao.getPersonByOldID(id);
    //Person person = Person.find("Select p from Person p where p.oldId = ?", id).first();
    if (person == null) {
      return false;
    } else {
      return true;
    }

  }

  /**
   * @return true se in quel giorno quella persona non è in turno nè in reperibilità (metodo
   * chiamato dal controller di inserimento assenza).
   */
  public boolean canPersonTakeAbsenceInShiftOrReperibility(Person person, LocalDate date) {
    Query queryReperibility =
        JPA.em().createQuery(
            "Select count(*) from PersonReperibilityDay prd where prd.date = :date "
                + "and prd.personReperibility.person = :person");
    queryReperibility.setParameter("date", date).setParameter("person", person);
    int prdCount = queryReperibility.getFirstResult();
    if (prdCount != 0) {
      return false;
    }
    Query queryShift =
        JPA.em().createQuery(
            "Select count(*) from PersonShiftDay psd where psd.date = :date "
                + "and psd.personShift.person = :person");
    queryShift.setParameter("date", date).setParameter("person", person);
    int psdCount = queryShift.getFirstResult();
    if (psdCount != 0) {
      return false;
    }

    return true;
  }

  /**
   * //TODO utilizzare jpa per prendere direttamente i codici (e migrare ad una lista).
   *
   * @param personDays lista di PersonDay
   * @return la lista contenente le assenze fatte nell'arco di tempo dalla persona
   */
  public Map<AbsenceType, Integer> getAllAbsenceCodeInMonth(List<PersonDay> personDays) {
    int month = personDays.get(0).date.getMonthOfYear();
    int year = personDays.get(0).date.getYear();
    LocalDate beginMonth = new LocalDate(year, month, 1);
    LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
    Person person = personDays.get(0).person;

    List<AbsenceType> abtList =
        AbsenceType.find(
            "Select abt from AbsenceType abt, Absence ab, PersonDay pd where ab.personDay = pd "
                + "and ab.absenceType = abt and pd.person = ? and pd.date between ? and ?",
            person, beginMonth, endMonth).fetch();
    Map<AbsenceType, Integer> absenceCodeMap = new HashMap<AbsenceType, Integer>();
    int i = 0;
    for (AbsenceType abt : abtList) {
      boolean stato = absenceCodeMap.containsKey(abt);
      if (stato == false) {
        i = 1;
        absenceCodeMap.put(abt, i);
      } else {
        i = absenceCodeMap.get(abt);
        absenceCodeMap.remove(abt);
        absenceCodeMap.put(abt, i + 1);
      }
    }
    return absenceCodeMap;
  }


  /**
   * @return il numero di giorni lavorati in sede.
   */
  public int basedWorkingDays(List<PersonDay> personDays) {

    int basedDays = 0;
    for (PersonDay pd : personDays) {

      IWrapperPersonDay day = wrapperFactory.create(pd);
      boolean fixed = day.isFixedTimeAtWork();

      if (pd.isHoliday) {
        continue;
      }

      if (fixed && !personDayManager.isAllDayAbsences(pd)) {
        basedDays++;
      } else if (!fixed && pd.stampings.size() > 0
          && !personDayManager.isAllDayAbsences(pd)) {
        basedDays++;
      }
    }
    return basedDays;
  }

  /**
   * Il numero di riposi compensativi utilizzati nell'anno dalla persona.
   */
  public int numberOfCompensatoryRestUntilToday(Person person, int year, int month) {

    // TODO: andare a fare bound con sourceDate e considerare quelli da
    // inizializzazione

    LocalDate begin = new LocalDate(year, 1, 1);
    LocalDate end = new LocalDate(year, month, 1).dayOfMonth().withMaximumValue();
    return absenceDao.absenceInPeriod(person, begin, end, "91").size();
  }

  public int holidayWorkingTimeNotAccepted(
      Person person, Optional<Integer> year, Optional<Integer> month) {

    List<PersonDay> pdList = personDayDao
        .getHolidayWorkingTime(person, year, month);
    int value = 0;
    for (PersonDay pd : pdList) {
      if (!pd.acceptedHolidayWorkingTime) {
        value += pd.timeAtWork;
      }
    }
    return value;
  }

  public int holidayWorkingTimeAccepted(
      Person person, Optional<Integer> year, Optional<Integer> month) {

    List<PersonDay> pdList = personDayDao
        .getHolidayWorkingTime(person, year, month);
    int value = 0;
    for (PersonDay pd : pdList) {
      if (pd.acceptedHolidayWorkingTime) {
        value += pd.timeAtWork;
      }
    }
    return value;
  }

  public int holidayWorkingTimeTotal(
      Person person, Optional<Integer> year, Optional<Integer> month) {
    List<PersonDay> pdList = personDayDao
        .getHolidayWorkingTime(person, year, month);
    int value = 0;
    for (PersonDay pd : pdList) {
      value += pd.timeAtWork;
    }
    return value;
  }

}
