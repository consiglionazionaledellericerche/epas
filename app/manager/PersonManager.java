package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import dao.AbsenceDao;
import dao.ContractDao;
import dao.PersonDayDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;

import it.cnr.iit.epas.DateUtility;

import models.Contract;
import models.Person;
import models.PersonDay;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;

import play.db.jpa.JPA;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.Query;

public class PersonManager {

  private final ContractDao contractDao;
  private final PersonDayDao personDayDao;
  public final PersonDayManager personDayManager;
  private final IWrapperFactory wrapperFactory;
  private final AbsenceDao absenceDao;
  private final UsersRolesOfficesDao uroDao;

  /**
   * Costrutture.
   *
   * @param contractDao      contractDao
   * @param personDayDao     personDayDao
   * @param absenceDao       absenceDao
   * @param personDayManager personDayManager
   * @param wrapperFactory   wrapperFactory
   */
  @Inject
  public PersonManager(ContractDao contractDao,
      PersonDayDao personDayDao,
      AbsenceDao absenceDao,
      PersonDayManager personDayManager,
      IWrapperFactory wrapperFactory,
      UsersRolesOfficesDao uroDao) {
    this.contractDao = contractDao;
    this.personDayDao = personDayDao;
    this.absenceDao = absenceDao;
    this.personDayManager = personDayManager;
    this.wrapperFactory = wrapperFactory;
    this.uroDao = uroDao;
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
   * Conta i codici di assenza.
   * 
   * @param personDays lista di PersonDay
   * @return La mappa dei codici di assenza utilizzati nei persondays specificati
   */
  public Map<AbsenceType, Integer> countAbsenceCodes(List<PersonDay> personDays) {

    final Map<AbsenceType, Integer> absenceCodeMap = Maps.newHashMap();

    personDays.stream().flatMap(personDay -> personDay.absences.stream()
        .<AbsenceType>map(absence -> absence.absenceType)).forEach(absenceType -> {
          Integer count = absenceCodeMap.get(absenceType);
          absenceCodeMap.put(absenceType, (count == null) ? 1 : count + 1);
        });

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
   * Il numero di riposi compensativi utilizzati tra 2 date (in linea di massima ha senso
   * dall'inizio dell'anno a una certa data).
   */
  public int numberOfCompensatoryRestUntilToday(Person person, LocalDate begin, LocalDate end) {

    List<Contract> contractsInPeriod = contractDao
        .getActiveContractsInPeriod(person, begin, Optional.of(end));

    Contract newerContract = contractsInPeriod.stream().filter(contract ->
        contract.sourceDateResidual != null).max(Comparator
        .comparing(Contract::getSourceDateResidual)).orElse(null);

    if (newerContract != null && newerContract.sourceDateResidual != null 
        && !newerContract.sourceDateResidual.isBefore(begin)
        && !newerContract.sourceDateResidual.isAfter(end)) {
      return newerContract.sourceRecoveryDayUsed + absenceDao
          .absenceInPeriod(person, newerContract.sourceDateResidual, end, "91").size();
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

  /**
   * @param user l'utente
   * @return true se l'utente è amministratore del personale, false altrimenti.
   */
  @Deprecated
  public boolean isPersonnelAdmin(User user) {
    List<UsersRolesOffices> uros = uroDao.getUsersRolesOfficesByUser(user);
    long count = uros.stream().filter(uro -> uro.role.name.equals(Role.PERSONNEL_ADMIN)).count();
    if (count > 0) {
      return true;
    }
    return false;
  }

}
