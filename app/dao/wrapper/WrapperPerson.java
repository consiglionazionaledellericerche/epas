package dao.wrapper;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.CompetenceDao;
import dao.ContractDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.CompetenceManager;
import manager.PersonManager;

import models.CertificatedData;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Person;
import models.PersonDay;
import models.VacationPeriod;
import models.WorkingTimeType;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import java.util.List;

/**
 * @author marco
 */
public class WrapperPerson implements IWrapperPerson {

  private final Person value;
  private final ContractDao contractDao;
  private final CompetenceManager competenceManager;
  private final PersonManager personManager;
  private final PersonDao personDao;
  private final PersonDayDao personDayDao;
  private final PersonMonthRecapDao personMonthRecapDao;
  private final IWrapperFactory wrapperFactory;
  private final CompetenceDao competenceDao;

  private Optional<Contract> currentContract = null;
  private Optional<WorkingTimeType> currentWorkingTimeType = null;
  private Optional<VacationPeriod> currentVacationPeriod = null;
  private Optional<ContractStampProfile> currentContractStampProfile = null;
  private Optional<ContractWorkingTimeType> currentContractWorkingTimeType = null;


  @Inject
  WrapperPerson(@Assisted Person person, ContractDao contractDao,
                CompetenceManager competenceManager, PersonManager personManager,
                PersonDao personDao, PersonMonthRecapDao personMonthRecapDao,
                PersonDayDao personDayDao, CompetenceDao competenceDao,
                IWrapperFactory wrapperFactory) {
    this.value = person;
    this.contractDao = contractDao;
    this.competenceManager = competenceManager;
    this.personManager = personManager;
    this.personDao = personDao;
    this.personMonthRecapDao = personMonthRecapDao;
    this.personDayDao = personDayDao;
    this.competenceDao = competenceDao;
    this.wrapperFactory = wrapperFactory;
  }

  @Override
  public Person getValue() {
    return value;
  }

  @Override
  public boolean isActiveInDay(LocalDate date) {
    return true;
  }

  @Override
  public boolean isActiveInMonth(YearMonth yearMonth) {
    return true;
  }

  /**
   * Calcola il contratto attualmente attivo.
   *
   * @return il contratto attualmente attivo per quella persona
   */
  @Override
  public Optional<Contract> getCurrentContract() {

    if (this.currentContract != null) {
      return this.currentContract;
    }

    if (this.currentContract == null) {
      this.currentContract = Optional.fromNullable(
              contractDao.getContract(LocalDate.now(), value));
    }
    return this.currentContract;
  }

  @Override
  public List<Contract> getMonthContracts(int year, int month) {

    List<Contract> contracts = Lists.newArrayList();

    LocalDate monthBegin = new LocalDate(year, month, 1);
    DateInterval monthInterval = new DateInterval(monthBegin,
            monthBegin.dayOfMonth().withMaximumValue());

    for (Contract contract : value.contracts) {
      if (DateUtility.intervalIntersection(monthInterval, wrapperFactory
              .create(contract).getContractDateInterval()) != null) {
        contracts.add(contract);
      }
    }
    return contracts;
  }

  @Override
  public List<Contract> getYearContracts(int year) {

    List<Contract> contracts = Lists.newArrayList();

    DateInterval yearInterval = new DateInterval(new LocalDate(year, 1, 1),
            new LocalDate(year, 12, 31));

    for (Contract contract : value.contracts) {
      if (DateUtility.intervalIntersection(yearInterval, wrapperFactory
              .create(contract).getContractDateInterval()) != null) {
        contracts.add(contract);
      }
    }
    return contracts;
  }

  /**
   * @return l'ultimo contratto attivo nel mese.
   */
  @Override
  public Optional<Contract> getLastContractInMonth(int year, int month) {

    List<Contract> contractInMonth = this.getMonthContracts(year, month);

    if (contractInMonth.size() == 0) {
      return Optional.absent();
    }

    return Optional.fromNullable(contractInMonth.get(contractInMonth.size() - 1));
  }

  /**
   * @return il primo contratto attivo nel mese.
   */
  public Optional<Contract> getFirstContractInMonth(int year, int month) {

    List<Contract> contractInMonth = this.getMonthContracts(year, month);

    if (contractInMonth.size() == 0) {
      return Optional.absent();
    }

    return Optional.fromNullable(contractInMonth.get(0));
  }

  /**
   * L'ultimo mese con contratto attivo.
   */
  public YearMonth getLastActiveMonth() {

    Optional<Contract> lastContract = personDao.getLastContract(value);

    // Importante per sinc con Perseo:
    // devo assumere che la persona abbia almeno un contratto
    // attivo in ePAS. Altrimenti non dovrebbe essere in ePAS.
    Preconditions.checkState(lastContract.isPresent());

    YearMonth current = YearMonth.now();
    YearMonth contractBegin = new YearMonth(lastContract.get().beginDate);

    if (contractBegin.isAfter(current)) {
      //vado in avanti
      while (true) {
        if (isActiveInMonth(current)) {
          return current;
        }
        current = current.plusMonths(1);
      }
    } else {
      //vado indietro
      while (true) {
        if (isActiveInMonth(current)) {
          return current;
        }
        current = current.minusMonths(1);
      }
    }
  }

  /**
   * True se la persona è passata da determinato a indeterminato durante l'anno.
   */
  @Override
  public boolean hasPassToIndefiniteInYear(int year) {

    List<Contract> orderedContractInYear = personDao.getContractList(this.value,
            new LocalDate(year, 1, 1), new LocalDate(year, 12, 31));


    boolean hasDefinite = false;
    boolean hasPassToIndefinite = false;

    for (Contract contract : orderedContractInYear) {
      if (contract.endDate != null) {
        hasDefinite = true;
      }

      if (hasDefinite && contract.endDate == null) {
        hasPassToIndefinite = true;
      }
    }

    return hasPassToIndefinite;
  }

  @Override
  public Optional<ContractStampProfile> getCurrentContractStampProfile() {

    if (this.currentContractStampProfile != null) {
      return this.currentContractStampProfile;
    }

    if (this.currentContract == null) {
      getCurrentContract();
    }

    if (!this.currentContract.isPresent()) {
      return Optional.absent();
    }

    this.currentContractStampProfile = this.currentContract.get()
            .getContractStampProfileFromDate(LocalDate.now());

    return this.currentContractStampProfile;
  }

  @Override
  public Optional<WorkingTimeType> getCurrentWorkingTimeType() {

    if (this.currentWorkingTimeType != null) {
      return this.currentWorkingTimeType;
    }

    if (this.currentContract == null) {
      getCurrentContract();
    }

    if (!this.currentContract.isPresent()) {
      return Optional.absent();
    }

    //ricerca
    for (ContractWorkingTimeType cwtt : this.currentContract.get().contractWorkingTimeType) {
      if (DateUtility
          .isDateIntoInterval(LocalDate.now(), new DateInterval(cwtt.beginDate, cwtt.endDate))) {
        this.currentWorkingTimeType = Optional.fromNullable(cwtt.workingTimeType);
        return this.currentWorkingTimeType;
      }
    }
    return Optional.absent();

  }

  @Override
  public Optional<ContractWorkingTimeType> getCurrentContractWorkingTimeType() {

    if (this.currentContractWorkingTimeType != null) {
      return this.currentContractWorkingTimeType;
    }

    if (this.currentContract == null) {
      getCurrentContract();
    }

    if (!this.currentContract.isPresent()) {
      return Optional.absent();
    }

    //ricerca
    for (ContractWorkingTimeType cwtt : this.currentContract.get().contractWorkingTimeType) {
      if (DateUtility.isDateIntoInterval(
          LocalDate.now(), new DateInterval(cwtt.beginDate, cwtt.endDate))) {
        this.currentContractWorkingTimeType = Optional.fromNullable(cwtt);
        return this.currentContractWorkingTimeType;
      }
    }
    return Optional.absent();
  }

  @Override
  public Optional<VacationPeriod> getCurrentVacationPeriod() {

    if (this.currentVacationPeriod != null) {
      return this.currentVacationPeriod;
    }

    if (this.currentContract == null) {
      getCurrentContract();
    }
    if (!this.currentContract.isPresent()) {
      return Optional.absent();
    }

    //ricerca
    for (VacationPeriod vp : this.currentContract.get().vacationPeriods) {
      if (DateUtility.isDateIntoInterval(
          LocalDate.now(), new DateInterval(vp.beginFrom, vp.endTo))) {
        this.currentVacationPeriod = Optional.fromNullable(vp);
        return this.currentVacationPeriod;
      }
    }
    return Optional.absent();
  }


  /**
   * Getter per la competenza della persona con CompetenceCode, year, month.
   */
  @Override
  public Competence competence(final CompetenceCode code,
                               final int year, final int month) {
    Optional<Competence> optCompetence = competenceDao.getCompetence(this.value, year, month, code);
    if (optCompetence.isPresent()) {
      return optCompetence.get();
    } else {
      Competence competence = new Competence(this.value, code, year, month);
      competence.valueApproved = 0;
      competence.save();
      return competence;
    }
  }

  /**
   * Il residuo positivo del mese fatto dalla person.
   */
  @Override
  public Integer getPositiveResidualInMonth(int year, int month) {

    return competenceManager.positiveResidualInMonth(this.value, year, month) / 60;
  }

  /**
   * L'esito dell'invio attestati per la persona (null se non è ancora stato effettuato).
   */
  @Override
  public CertificatedData getCertificatedData(int year, int month) {

    CertificatedData cd = personMonthRecapDao.getPersonCertificatedData(this.value, month, year);
    return cd;
  }

  /**
   * Diagnostiche sui dati della persona.
   */
  @Override
  public boolean currentContractInitializationMissing() {
    getCurrentContract();
    if (currentContract.isPresent()) {
      if (wrapperFactory.create(currentContract.get()).initializationMissing()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean currentContractMonthRecapMissing() {
    getCurrentContract();
    if (currentContract.isPresent()) {
      YearMonth now = new YearMonth(LocalDate.now());
      if (wrapperFactory.create(currentContract.get())
              .monthRecapMissing(now)) {
        return true;
      }
    }
    return false;
  }

  /**
   * I tempo totale di ore lavorate dalla persona nei giorni festivi.
   */
  public int totalHolidayWorkingTime(Integer year) {
    return personManager.holidayWorkingTimeTotal(value,
            Optional.fromNullable(year), Optional.<Integer>absent());
  }

  /**
   * I tempo totale di ore lavorate dalla persona nei giorni festivi e accettate.
   */
  public int totalHolidayWorkingTimeAccepted(Integer year) {
    return personManager.holidayWorkingTimeAccepted(value,
            Optional.fromNullable(year), Optional.<Integer>absent());
  }

  /**
   * I giorni festivi con ore lavorate.
   */
  public List<PersonDay> holidyWorkingTimeDay(Integer year) {
    return personDayDao.getHolidayWorkingTime(this.value,
            Optional.fromNullable(year), Optional.<Integer>absent());
  }
}
