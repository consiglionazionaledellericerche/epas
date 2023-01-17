/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dao.wrapper;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.assistedinject.Assisted;
import dao.CompetenceDao;
import dao.ContractDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.SortedMap;
import javax.inject.Inject;
import manager.CompetenceManager;
import manager.PersonManager;
import models.CertificatedData;
import models.Certification;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Person;
import models.PersonDay;
import models.VacationPeriod;
import models.WorkingTimeType;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

/**
 * Wrapper per la person.
 *
 * @author Marco Andreini
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

  private List<Contract> sortedContracts;
  private Optional<Contract> currentContract;
  private Optional<Contract> previousContract;
  private Optional<WorkingTimeType> currentWorkingTimeType;
  private Optional<VacationPeriod> currentVacationPeriod;
  private Optional<ContractStampProfile> currentContractStampProfile;
  private Optional<ContractWorkingTimeType> currentContractWorkingTimeType;

  private Optional<Boolean> properSynchronized = Optional.absent();

  @Inject
  WrapperPerson(@Assisted Person person, ContractDao contractDao,
      CompetenceManager competenceManager, PersonManager personManager,
      PersonDao personDao, PersonMonthRecapDao personMonthRecapDao,
      PersonDayDao personDayDao, CompetenceDao competenceDao,
      IWrapperFactory wrapperFactory) {
    value = person;
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
    for (Contract contract : orderedMonthContracts(date.getYear(), date.getMonthOfYear())) {
      if (DateUtility.isDateIntoInterval(date, contract.periodInterval())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isActiveInMonth(YearMonth yearMonth) {
    return getFirstContractInMonth(yearMonth.getYear(), yearMonth.getMonthOfYear()) != null;
  }

  /**
   * Calcola il contratto attualmente attivo.
   *
   * @return il contratto attualmente attivo per quella persona
   */
  @Override
  public Optional<Contract> getCurrentContract() {

    if (currentContract != null) {
      return currentContract;
    }

    if (currentContract == null) {
      currentContract = Optional.fromNullable(
          contractDao.getContract(LocalDate.now(), value));
    }
    return currentContract;
  }

  @Override
  public List<Contract> orderedMonthContracts(int year, int month) {

    List<Contract> contracts = Lists.newArrayList();

    LocalDate monthBegin = new LocalDate(year, month, 1);
    DateInterval monthInterval = new DateInterval(monthBegin,
        monthBegin.dayOfMonth().withMaximumValue());

    for (Contract contract : orderedContracts()) {
      if (DateUtility.intervalIntersection(monthInterval, wrapperFactory
          .create(contract).getContractDateInterval()) != null) {
        contracts.add(contract);
      }
    }
    return contracts;
  }

  @Override
  public List<Contract> orderedYearContracts(int year) {

    List<Contract> contracts = Lists.newArrayList();
    DateInterval yearInterval = new DateInterval(new LocalDate(year, 1, 1),
        new LocalDate(year, 12, 31));

    for (Contract contract : orderedContracts()) {
      if (DateUtility.intervalIntersection(yearInterval, wrapperFactory
          .create(contract).getContractDateInterval()) != null) {
        contracts.add(contract);
      }
    }
    return contracts;
  }

  @Override
  public List<Contract> orderedContracts() {
    if (sortedContracts != null) {
      return sortedContracts;
    }
    SortedMap<LocalDate, Contract> contracts = Maps.newTreeMap();
    for (Contract contract : value.getContracts()) {
      contracts.put(contract.getBeginDate(), contract);
    }
    sortedContracts = Lists.newArrayList(contracts.values());
    return sortedContracts;
  }

  /**
   * L'ultimo contratto attivo nel mese, se esiste.
   *
   * @param year l'anno
   * @param month il mese
   * @return l'ultimo contratto attivo nel mese.
   */
  @Override
  public Optional<Contract> getLastContractInMonth(int year, int month) {

    List<Contract> contractInMonth = orderedMonthContracts(year, month);

    if (contractInMonth.isEmpty()) {
      return Optional.absent();
    }

    return Optional.fromNullable(contractInMonth.get(contractInMonth.size() - 1));
  }

  /**
   * Il primo contratto attivo nel mese se esiste.
   *
   * @param year l'anno
   * @param month il mese
   * @return il primo contratto attivo nel mese.
   */
  @Override
  public Optional<Contract> getFirstContractInMonth(int year, int month) {

    List<Contract> contractInMonth = orderedMonthContracts(year, month);

    if (contractInMonth.isEmpty()) {
      return Optional.absent();
    }

    return Optional.fromNullable(contractInMonth.get(0));
  }

  /**
   * L'ultimo mese con contratto attivo.
   */
  @Override
  public YearMonth getLastActiveMonth() {

    Optional<Contract> lastContract = personDao.getLastContract(value);

    // Importante per sinc con Perseo:
    // devo assumere che la persona abbia almeno un contratto
    // attivo in ePAS. Altrimenti non dovrebbe essere in ePAS.
    Preconditions.checkState(lastContract.isPresent());

    YearMonth current = YearMonth.now();
    YearMonth contractBegin = new YearMonth(lastContract.get().getBeginDate());

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

    List<Contract> orderedContractInYear = personDao.getContractList(value,
        new LocalDate(year, 1, 1), new LocalDate(year, 12, 31));


    boolean hasDefinite = false;
    boolean hasPassToIndefinite = false;

    for (Contract contract : orderedContractInYear) {
      if (contract.getEndDate() != null) {
        hasDefinite = true;
      }

      if (hasDefinite && contract.getEndDate() == null) {
        hasPassToIndefinite = true;
      }
    }

    return hasPassToIndefinite;
  }

  @Override
  public Optional<ContractStampProfile> getCurrentContractStampProfile() {

    if (currentContractStampProfile != null) {
      return currentContractStampProfile;
    }

    if (currentContract == null) {
      getCurrentContract();
    }

    if (!currentContract.isPresent()) {
      return Optional.absent();
    }

    currentContractStampProfile = currentContract.get()
        .getContractStampProfileFromDate(LocalDate.now());

    return currentContractStampProfile;
  }

  @Override
  public Optional<WorkingTimeType> getCurrentWorkingTimeType() {

    if (currentWorkingTimeType != null) {
      return currentWorkingTimeType;
    }

    if (currentContract == null) {
      getCurrentContract();
    }

    if (!currentContract.isPresent()) {
      return Optional.absent();
    }

    //ricerca
    for (ContractWorkingTimeType cwtt : currentContract.get().getContractWorkingTimeType()) {
      if (DateUtility
          .isDateIntoInterval(LocalDate.now(), new DateInterval(cwtt.getBeginDate(), cwtt.getEndDate()))) {
        currentWorkingTimeType = Optional.fromNullable(cwtt.getWorkingTimeType());
        return currentWorkingTimeType;
      }
    }
    return Optional.absent();

  }

  @Override
  public Optional<ContractWorkingTimeType> getCurrentContractWorkingTimeType() {

    if (currentContractWorkingTimeType != null) {
      return currentContractWorkingTimeType;
    }

    if (currentContract == null) {
      getCurrentContract();
    }

    if (!currentContract.isPresent()) {
      return Optional.absent();
    }

    //ricerca
    for (ContractWorkingTimeType cwtt : currentContract.get().getContractWorkingTimeType()) {
      if (DateUtility.isDateIntoInterval(
          LocalDate.now(), new DateInterval(cwtt.getBeginDate(), cwtt.getEndDate()))) {
        currentContractWorkingTimeType = Optional.fromNullable(cwtt);
        return currentContractWorkingTimeType;
      }
    }
    return Optional.absent();
  }

  @Override
  public Optional<VacationPeriod> getCurrentVacationPeriod() {

    if (currentVacationPeriod != null) {
      return currentVacationPeriod;
    }

    if (currentContract == null) {
      getCurrentContract();
    }
    if (!currentContract.isPresent()) {
      return Optional.absent();
    }

    //ricerca
    for (VacationPeriod vp : currentContract.get().getVacationPeriods()) {
      if (DateUtility.isDateIntoInterval(
          LocalDate.now(), new DateInterval(vp.getBeginDate(), vp.calculatedEnd()))) {
        currentVacationPeriod = Optional.fromNullable(vp);
        return currentVacationPeriod;
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
    Optional<Competence> optCompetence = competenceDao.getCompetence(value, year, month, code);
    if (optCompetence.isPresent()) {
      return optCompetence.get();
    } else {
      Competence competence = new Competence(value, code, year, month);
      competence.setValueApproved(0);
      competence.save();
      return competence;
    }
  }

  /**
   * Il residuo positivo del mese fatto dalla person.
   */
  @Override
  public Integer getPositiveResidualInMonth(int year, int month) {

    return competenceManager.positiveResidualInMonth(value, year, month) / 60;
  }

  /**
   * L'esito dell'invio attestati per la persona (null se non è ancora stato effettuato).
   */
  @Override
  public CertificatedData getCertificatedData(int year, int month) {

    CertificatedData cd = personMonthRecapDao.getPersonCertificatedData(value, month, year);
    return cd;
  }

  /**
   * Diagnostiche sui dati della persona.
   */
  @Override
  public boolean currentContractInitializationMissing() {
    getCurrentContract();
    if (currentContract.isPresent()) {
      return wrapperFactory.create(currentContract.get()).initializationMissing();
    }
    return false;
  }

  @Override
  public boolean currentContractMonthRecapMissing() {
    getCurrentContract();
    if (currentContract.isPresent()) {
      YearMonth now = new YearMonth(LocalDate.now());
      return wrapperFactory.create(currentContract.get())
          .monthRecapMissing(now);
    }
    return false;
  }

  /**
   * I tempo totale di ore lavorate dalla persona nei giorni festivi.
   */
  public int totalHolidayWorkingTime(Integer year) {
    return personManager.holidayWorkingTimeTotal(value,
        Optional.fromNullable(year), Optional.absent());
  }

  /**
   * I tempo totale di ore lavorate dalla persona nei giorni festivi e accettate.
   */
  public int totalHolidayWorkingTimeAccepted(Integer year) {
    return personManager.holidayWorkingTimeAccepted(value,
        Optional.fromNullable(year), Optional.absent());
  }

  /**
   * I giorni festivi con ore lavorate.
   */
  public List<PersonDay> holidyWorkingTimeDay(Integer year) {
    return personDayDao.getHolidayWorkingTime(value,
        Optional.fromNullable(year), Optional.absent());
  }

  /**
   * Diagnostiche sullo stato di sincronizzazione della persona.
   * Ha perseoId null oppure uno dei suoi contratti attivi o futuri ha perseoId null.
   */
  @Override
  public boolean isProperSynchronized() {

    if (properSynchronized.isPresent()) {
      return properSynchronized.get();
    }

    properSynchronized = Optional.of(false);

    if (value.getPerseoId() == null) {
      return properSynchronized.get();
    }

    for (Contract contract : value.getContracts()) {
      if (!contract.isProperSynchronized()) {
        return properSynchronized.get();
      }
    }

    properSynchronized = Optional.of(true);
    return properSynchronized.get();
  }

  /**
   * Il contratto della persona con quel perseoId.
   *
   * @param perseoId id di perseo della Persona
   * @return Contract.
   */
  @Override
  public Contract perseoContract(String perseoId) {
    if (perseoId == null) {
      return null;
    }
    for (Contract contract : value.getContracts()) {
      if (contract.getPerseoId() == null) {
        continue;
      }
      if (contract.getPerseoId().equals(perseoId)) {
        return contract;
      }
    }
    return null;
  }

  @Override
  public boolean isTechnician() {
    return value.getQualification().getQualification() > 3;
  }
  
  /**
   * L'ultimo invio attestati effettuato tramite ePAS.
   *
   * @return mese / anno
   */
  @Override
  public Optional<YearMonth> lastUpload() {
    if (value.getCertifications().isEmpty()) {
      return Optional.absent();
    }
    YearMonth last = null;
    for (Certification certification : value.getCertifications()) {
      if (last == null || last.isBefore(new YearMonth(certification.getYear(), certification.getMonth()))) {
        last = new YearMonth(certification.getYear(), certification.getMonth());
      }
    }
    return Optional.of(last);
  }

  @Override
  public Optional<Contract> getPreviousContract() {
    
    if (previousContract != null) {
      return previousContract;
    }
    
    if (previousContract == null) {
      previousContract = contractDao.getPreviousContract(getCurrentContract().get());
    }
    return previousContract;
  }

  public List<IWrapperContractMonthRecap> getWrapperContractMonthRecaps(YearMonth yearMonth) {
    // ******************************************************************************************
    // DATI MENSILI
    // ******************************************************************************************
    // I riepiloghi mensili (uno per ogni contratto attivo nel mese)
    List<IWrapperContractMonthRecap> contractMonths = Lists.newArrayList();
    
    List<Contract> monthContracts = wrapperFactory.create(value)
        .orderedMonthContracts(yearMonth.getYear(), yearMonth.getMonthOfYear());

    for (Contract contract : monthContracts) {
      Optional<ContractMonthRecap> cmr =
          wrapperFactory.create(contract).getContractMonthRecap(yearMonth);
      if (cmr.isPresent()) {
        contractMonths.add(wrapperFactory.create(cmr.get()));
      }
    }
    return contractMonths;
  }

  public int getNumberOfMealTicketsPreviousMonth(YearMonth yearMonth) {
    return getWrapperContractMonthRecaps(yearMonth).stream().mapToInt(
        cm -> cm.getValue().getBuoniPastoDalMesePrecedente()).reduce(0, Integer::sum);
  }
}
