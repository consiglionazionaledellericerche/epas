package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.VacationCodeDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import models.Contract;
import models.ContractMonthRecap;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.VacationCode;
import models.VacationPeriod;
import models.WorkingTimeType;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.db.jpa.JPA;

import java.util.List;

import javax.inject.Inject;


/**
 * Manager per Contract.
 *
 * @author alessandro
 */
public class ContractManager {

  private final ConsistencyManager consistencyManager;
  private final IWrapperFactory wrapperFactory;
  private final VacationCodeDao vacationCodeDao;
  private final PeriodManager periodManager;

  /**
   * Constructor.
   *
   * @param consistencyManager {@link ConsistencyManager}
   * @param vacationCodeDao    {@link VacationCodeDao}
   * @param wrapperFactory     {@link IWrapperFactory}
   */
  @Inject
  public ContractManager(
      final ConsistencyManager consistencyManager, final VacationCodeDao vacationCodeDao,
      final PeriodManager periodManager, final IWrapperFactory wrapperFactory) {

    this.consistencyManager = consistencyManager;
    this.vacationCodeDao = vacationCodeDao;
    this.periodManager = periodManager;
    this.wrapperFactory = wrapperFactory;
  }

  /**
   * Check di contratto valido con gli altri contratti della persona.
   *
   * @param contract contract
   * @return esito
   */
  public final boolean isContractNotOverlapping(final Contract contract) {

    DateInterval contractInterval = wrapperFactory.create(contract).getContractDateInterval();
    for (Contract c : contract.person.contracts) {

      if (contract.id != null && c.id.equals(contract.id)) {
        continue;
      }

      if (DateUtility.intervalIntersection(contractInterval,
              wrapperFactory.create(c).getContractDateInterval()) != null) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check di date valide singolo contratto.
   *
   * @param contract contract
   * @return esito
   */
  public final boolean isContractCrossFieldValidationPassed(final Contract contract) {

    if (contract.endDate != null
            && contract.endDate.isBefore(contract.beginDate)) {
      return false;
    }

    if (contract.endContract != null && contract.endContract.isBefore(contract.beginDate)) {
      return false;
    }

    if (contract.endDate != null && contract.endContract != null
            && contract.endDate.isBefore(contract.endContract)) {
      return false;
    }

    if (!isContractNotOverlapping(contract)) {
      return false;
    }

    return true;
  }

  /**
   * Costruisce il contratto in modo sicuro e effettua il calcolo.
   *
   * @param contract contract
   * @param wtt      il tipo orarii
   * @return esito costruzione
   */
  public final boolean properContractCreate(final Contract contract, final WorkingTimeType wtt) {

    if (!isContractCrossFieldValidationPassed(contract)) {
      return false;
    }

    if (!isContractNotOverlapping(contract)) {
      return false;
    }

    contract.save();

    setContractVacationPeriod(contract);

    ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
    cwtt.beginDate = contract.getBeginDate();
    cwtt.endDate = contract.calculatedEnd();
    cwtt.workingTimeType = wtt;
    cwtt.contract = contract;
    cwtt.save();
    contract.contractWorkingTimeType.add(cwtt);

    ContractStampProfile csp = new ContractStampProfile();
    csp.contract = contract;
    csp.beginDate = contract.getBeginDate();
    csp.endDate = contract.calculatedEnd();
    csp.fixedworkingtime = false;
    csp.save();
    contract.contractStampProfile.add(csp);

    contract.sourceDateResidual = null;
    contract.save();

    // FIXME: comando JPA per aggiornare la person
    contract.person.contracts.add(contract);

    recomputeContract(contract, Optional.<LocalDate>absent(), true, false);

    return true;

  }

  /**
   * Aggiorna il contratto in modo sicuro ed effettua il ricalcolo.
   *
   * @param contract   contract
   * @param from       da quando effettuare il ricalcolo.
   * @param onlyRecaps ricalcolare solo i riepiloghi mensili.
   */
  public final void properContractUpdate(final Contract contract, final LocalDate from,
                                         final boolean onlyRecaps) {

    setContractVacationPeriod(contract);
    
    periodManager.updatePropertiesInPeriodOwner(contract, ContractWorkingTimeType.class);
    periodManager.updatePropertiesInPeriodOwner(contract, ContractStampProfile.class);
    
    recomputeContract(contract, Optional.fromNullable(from), false, onlyRecaps);
  }

  /**
   * Ricalcolo del contratto a partire dalla data from.
   *
   * @param contract    contract
   * @param dateFrom    dateFrom
   * @param newContract se il contratto è nuovo (non ho strutture da ripulire)
   * @param onlyRecaps  ricalcolare solo i riepiloghi mensili.
   */
  public final void recomputeContract(final Contract contract, final Optional<LocalDate> dateFrom,
                                      final boolean newContract, final boolean onlyRecaps) {

    IWrapperContract wrContract = wrapperFactory.create(contract);
    LocalDate startDate = wrContract.getContractDatabaseInterval().getBegin();
    if (dateFrom.isPresent() && dateFrom.get().isAfter(startDate)) {
      startDate = dateFrom.get();
    }

    if (!newContract) {

      YearMonth yearMonthFrom = new YearMonth(startDate);
      
      // Distruggere i riepiloghi esistenti da yearMonthFrom.
      // TODO: anche quelli sulle ferie quando ci saranno
      for (ContractMonthRecap cmr : contract.contractMonthRecaps) {
        if (new YearMonth(cmr.year, cmr.month).isBefore(yearMonthFrom)) {
          continue;
        }
        cmr.delete();
      }
      
      JPA.em().flush();
      contract.refresh();   //per aggiornare la lista contract.contractMonthRecaps
    }

    consistencyManager.updateContractSituation(contract, startDate);
  }


  /**
   * Builder dell'oggetto vacationPeriod.
   * @param contract contratto
   * @param vacationCode vacationCode
   * @param beginFrom da
   * @param endTo a
   * @return il vacationPeriod
   */
  private static VacationPeriod buildVacationPeriod(final Contract contract, 
      final VacationCode vacationCode, final LocalDate beginFrom, final LocalDate endTo) {

    VacationPeriod vacationPeriod = new VacationPeriod();
    vacationPeriod.contract = contract;
    vacationPeriod.beginFrom = beginFrom;
    vacationPeriod.endTo = endTo;
    vacationPeriod.vacationCode = vacationCode;
    return vacationPeriod;
  }
  
  /**
   * Ritorna i vacation period di default per il contratto applicando la normativa
   * vigente.
   * 
   * @param contract contract
   * @return i vacation period
   */
  public List<VacationPeriod> contractVacationPeriods(Contract contract)  {

    List<VacationPeriod> vacationPeriods = Lists.newArrayList();

    VacationCode v26 = vacationCodeDao.getVacationCodeByDescription("26+4");
    VacationCode v28 = vacationCodeDao.getVacationCodeByDescription("28+4");

    if (contract.getEndDate() == null) {

      // Tempo indeterminato, creo due vacation 3 anni più infinito
      vacationPeriods.add(buildVacationPeriod(contract, v26, contract.getBeginDate(),
          contract.getBeginDate().plusYears(3).minusDays(1)));
      vacationPeriods.add(
          buildVacationPeriod(contract, v28, contract.getBeginDate().plusYears(3), null));

    } else {

      if (contract.getEndDate().isAfter(contract.getBeginDate().plusYears(3).minusDays(1))) {

        // Tempo determinato più lungo di 3 anni
        vacationPeriods.add(buildVacationPeriod(contract, v26, contract.getBeginDate(),
            contract.getBeginDate().plusYears(3).minusDays(1)));
        vacationPeriods.add(
            buildVacationPeriod(contract, v28, contract.getBeginDate().plusYears(3), 
                contract.getEndDate()));
      } else {
        vacationPeriods.add(
            buildVacationPeriod(contract, v26, contract.getBeginDate(), contract.getEndDate()));
      }
    }
    return vacationPeriods;
  }
  
  /**
   * Assegna i vacationPeriod di default al contratto, eliminando quelli precedentemente impostati.
   * 
   * @param contract contratto.
   */
  public void setContractVacationPeriod(final Contract contract) {

    // TODO: Quando verrà implementata la crud per modificare manualmente
    // i piani ferie non sarà sufficiente cancellare la storia, ma dare
    // conflitto.

    for (VacationPeriod oldVacation : contract.vacationPeriods) {
      oldVacation.delete();
    }

    contract.save();
    contract.refresh();

    contract.vacationPeriods = Lists.newArrayList();
    contract.vacationPeriods.addAll(contractVacationPeriods(contract));
    for (VacationPeriod vacationPeriod : contract.getVacationPeriods()) {
      vacationPeriod.save();
    }
  
    contract.save();
  }

  /**
   * Il ContractWorkingTimeType associato ad un contratto in una specifica data.
   *
   * @param contract il contratto di cui prelevare il ContractWorkingTimeType
   * @param date     la data in cui controllare il ContractWorkingTimeType
   * @return il ContractWorkingTimeType di un contratto ad una data specifica
   */
  public final ContractWorkingTimeType getContractWorkingTimeTypeFromDate(final Contract contract,
                                                                          final LocalDate date) {

    for (ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {

      if (DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate))) {
        return cwtt;
      }
    }
    // FIXME: invece del null utilizzare un Optional!
    return null;
  }


  /**
   * Sistema l'inizializzazione impostando i valori corretti se mancanti.
   *
   * @param contract contract
   */
  public final void setSourceContractProperly(final Contract contract) {

    if (contract.sourceVacationLastYearUsed == null) {
      contract.sourceVacationLastYearUsed = 0;
    }
    if (contract.sourceVacationCurrentYearUsed == null) {
      contract.sourceVacationCurrentYearUsed = 0;
    }
    if (contract.sourcePermissionUsed == null) {
      contract.sourcePermissionUsed = 0;
    }
    if (contract.sourceRemainingMinutesCurrentYear == null) {
      contract.sourceRemainingMinutesCurrentYear = 0;
    }
    if (contract.sourceRemainingMinutesLastYear == null) {
      contract.sourceRemainingMinutesLastYear = 0;
    }
    if (contract.sourceRecoveryDayUsed == null) {
      contract.sourceRecoveryDayUsed = 0;
    }
    if (contract.sourceRemainingMealTicket == null) {
      contract.sourceRemainingMealTicket = 0;
    }
    if (contract.sourceDateMealTicket == null && contract.sourceDateResidual != null) {
      contract.sourceDateMealTicket = contract.sourceDateResidual;
    }
    contract.save();
  }

  /**
   * Azzera l'inizializzazione del contratto.
   *
   * @param contract contract
   */
  public final void cleanResidualInitialization(final Contract contract) {
    contract.sourceVacationLastYearUsed = 0;
    contract.sourceVacationCurrentYearUsed = 0;
    contract.sourcePermissionUsed = 0;
    contract.sourceRemainingMinutesCurrentYear = 0;
    contract.sourceRemainingMinutesLastYear = 0;
    contract.sourceRecoveryDayUsed = 0;
    contract.sourceRemainingMealTicket = 0;
  }

  /**
   * Azzera l'inizializzazione del buono pasto.
   *
   * @param contract contract
   */
  public final void cleanMealTicketInitialization(final Contract contract) {
    contract.sourceDateMealTicket = contract.sourceDateResidual;
  }

}
