package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import dao.AbsenceDao;
import dao.ContractDao;
import dao.PersonChildrenDao;
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

import org.joda.time.LocalDate;
import org.joda.time.MonthDay;

import play.db.jpa.JPA;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.Query;

public class PersonManager {

  private final ContractDao contractDao;
  private final PersonDayDao personDayDao;
  private final PersonDayManager personDayManager;
  private final IWrapperFactory wrapperFactory;
  private final AbsenceDao absenceDao;
  private final ConfigurationManager configurationManager;

  /**
   * Costrutture.
   *
   * @param contractDao          contractDao
   * @param personChildrenDao    personChildrenDao
   * @param personDayDao         personDayDao
   * @param absenceDao           absenceDao
   * @param personDayManager     personDayManager
   * @param wrapperFactory       wrapperFactory
   * @param configurationManager configurationManager
   */
  @Inject
  public PersonManager(ContractDao contractDao,
      PersonChildrenDao personChildrenDao,
      PersonDayDao personDayDao,
      AbsenceDao absenceDao,
      PersonDayManager personDayManager,
      IWrapperFactory wrapperFactory,
      ConfigurationManager configurationManager) {
    this.contractDao = contractDao;
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

    MonthDay patron = (MonthDay) configurationManager
        .configValue(person.office, EpasParam.DAY_OF_PATRON, date);

    if (DateUtility.isGeneralHoliday(Optional.fromNullable(patron), date)) {
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
   * Calcola se la persona nel giorno non è nè in turno nè in reperibilità e quindi può prendere
   * l'assenza.
   *
   * @return esito
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
    int index = 0;
    for (AbsenceType abt : abtList) {
      boolean stato = absenceCodeMap.containsKey(abt);
      if (stato == false) {
        index = 1;
        absenceCodeMap.put(abt, index);
      } else {
        index = absenceCodeMap.get(abt);
        absenceCodeMap.remove(abt);
        absenceCodeMap.put(abt, index + 1);
      }
    }
    return absenceCodeMap;
  }


  /**
   * @return il numero di giorni lavorati in sede.
   */
  public int basedWorkingDays(List<PersonDay> personDays,
      List<Contract> contracts, LocalDate end) {

    int basedDays = 0;

    for (PersonDay pd : personDays) {

      if (pd.isHoliday) {
        continue;
      }
      boolean find = false;
      for (Contract contract : contracts) {
        if (DateUtility.isDateIntoInterval(pd.date, contract.periodInterval())) {
          find = true;
        }
      }

      if (!find) {
        continue;
      }
      IWrapperPersonDay day = wrapperFactory.create(pd);
      boolean fixed = day.isFixedTimeAtWork();

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
   * Il numero di riposi compensativi utilizzati tra 2 date
   * (in linea di massima ha senso dall'inizio dell'anno a una certa data)
   */
  public int numberOfCompensatoryRestUntilToday(Person person, LocalDate begin, LocalDate end) {

    List<Contract> contractsInPeriod = contractDao
        .getActiveContractsInPeriod(person, begin, Optional.of(end));

    Contract newerContract = contractsInPeriod.stream()
        .max(Comparator.comparing(Contract::getSourceDateResidual)).get();

    if (newerContract != null && newerContract.sourceDateResidual != null &&
        !newerContract.sourceDateResidual.isBefore(begin)
        && !newerContract.sourceDateResidual.isAfter(end)) {
      return newerContract.sourceRecoveryDayUsed +
          absenceDao.absenceInPeriod(person, newerContract.sourceDateResidual, end, "91").size();
    }

    return absenceDao.absenceInPeriod(person, begin, end, "91").size();
  }

  /**
   * Il numero di riposi compensativi utilizzati nell'anno dalla persona.
   */
  public int numberOfCompensatoryRestUntilToday(Person person, int year, int month) {

    LocalDate begin = new LocalDate(year, 1, 1);
    LocalDate end = new LocalDate(year, month, 1).dayOfMonth().withMaximumValue();
    return numberOfCompensatoryRestUntilToday(person, begin, end);
  }

  /**
   * Minuti di presenza festiva non accettata.
   *
   * @param person persona
   * @param year   anno
   * @param month  mese
   * @return minuti
   */
  public int holidayWorkingTimeNotAccepted(Person person, Optional<Integer> year,
      Optional<Integer> month) {

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

  /**
   * Minuti di presenza festiva accettata.
   *
   * @param person persona
   * @param year   anno
   * @param month  mese
   * @return minuti
   */
  public int holidayWorkingTimeAccepted(Person person, Optional<Integer> year,
      Optional<Integer> month) {

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

  /**
   * Minuti di presenza festiva totali.
   *
   * @param person persona
   * @param year   anno
   * @param month  mese
   * @return minuti
   */
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
