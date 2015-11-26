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
import models.base.IPeriodModel;
import models.base.PeriodModel;

import org.joda.time.LocalDate;

import play.db.jpa.JPAPlugin;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;


/**
 * Manager per Contract.
 *
 * @author alessandro
 */
public class ContractManager {

  private final ConfGeneralManager confGeneralManager;
  private final ConsistencyManager consistencyManager;
  private final IWrapperFactory wrapperFactory;
  private final VacationCodeDao vacationCodeDao;

  /**
   * Constructor.
   *
   * @param confGeneralManager {@link ConfGeneralManager}
   * @param consistencyManager {@link ConsistencyManager}
   * @param vacationCodeDao    {@link VacationCodeDao}
   * @param wrapperFactory     {@link IWrapperFactory}
   */
  @Inject
  public ContractManager(final ConfGeneralManager confGeneralManager,
                         final ConsistencyManager consistencyManager, final VacationCodeDao vacationCodeDao,
                         final IWrapperFactory wrapperFactory) {

    this.confGeneralManager = confGeneralManager;
    this.consistencyManager = consistencyManager;
    this.vacationCodeDao = vacationCodeDao;
    this.wrapperFactory = wrapperFactory;
  }

  /**
   * Check di contratto valido con gli altri contratti della persona.
   *
   * @param contract contract
   * @return esito
   */
  public final boolean isProperContract(final Contract contract) {

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
  public final boolean contractCrossFieldValidation(final Contract contract) {

    if (contract.expireContract != null
            && contract.expireContract.isBefore(contract.beginContract)) {
      return false;
    }

    if (contract.endContract != null && contract.endContract.isBefore(contract.beginContract)) {
      return false;
    }

    if (contract.expireContract != null && contract.endContract != null
            && contract.expireContract.isBefore(contract.endContract)) {
      return false;
    }

    if (!isProperContract(contract)) {
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

    if (!contractCrossFieldValidation(contract)) {
      return false;
    }

    if (!isProperContract(contract)) {
      return false;
    }

    contract.save();

    buildVacationPeriods(contract);

    ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
    cwtt.beginDate = contract.beginContract;
    cwtt.endDate = contract.expireContract;
    cwtt.workingTimeType = wtt;
    cwtt.contract = contract;
    cwtt.save();
    contract.contractWorkingTimeType.add(cwtt);

    ContractStampProfile csp = new ContractStampProfile();
    csp.contract = contract;
    csp.startFrom = contract.beginContract;
    csp.endTo = contract.expireContract;
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
   * Costruisce il contratto in modo sicuro e effettua il calcolo dalla data from.
   *
   * @param contract   contract
   * @param from       da quando effettuare il ricalcolo.
   * @param onlyRecaps ricalcolare solo i riepiloghi mensili.
   */
  public final void properContractUpdate(final Contract contract, final LocalDate from,
                                         final boolean onlyRecaps) {

    buildVacationPeriods(contract);
    updateContractWorkingTimeType(contract);
    updateContractStampProfile(contract);

    recomputeContract(contract, Optional.fromNullable(from), true, onlyRecaps);
  }

  /**
   * Ricalcola completamente tutti i dati del contratto da dateFrom a dateTo.
   *
   * @param contract contratto su cui effettuare il ricalcolo.
   * @param dateFrom giorno a partire dal quale effettuare il ricalcolo. Se null ricalcola
   *        dall'inizio del contratto. newContract: indica se il ricalcolo è relativo ad un nuvo
   *        contratto ad uno già esistente
   */

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

    IWrapperContract wContract = wrapperFactory.create(contract);
    LocalDate startDate = wContract.getContractDatabaseInterval().getBegin();
    if (dateFrom.isPresent() && dateFrom.get().isAfter(startDate)) {
      startDate = dateFrom.get();
    }

    if (!newContract) {
      // Distruggere i riepiloghi
      // TODO: anche quelli sulle ferie quando ci saranno
      destroyContractMonthRecap(contract);

      JPAPlugin.closeTx(false);
      JPAPlugin.startTx(false);
    }

    consistencyManager.updateContractSituation(contract, startDate);
  }

  private void destroyContractMonthRecap(final Contract contract) {
    for (ContractMonthRecap cmr : contract.contractMonthRecaps) {
      cmr.delete();
    }
    contract.save();
  }

  private VacationPeriod buildVacationPeriod(final Contract contract,
                                             final VacationCode vacationCode, final LocalDate beginFrom, final LocalDate endTo) {

    VacationPeriod vacationPeriod = new VacationPeriod();
    vacationPeriod.contract = contract;
    vacationPeriod.beginFrom = beginFrom;
    vacationPeriod.endTo = endTo;
    vacationPeriod.vacationCode = vacationCode;
    vacationPeriod.save();
    return vacationPeriod;
  }

  /**
   * Costruisce la struttura dei periodi ferie associati al contratto applicando la normativa
   * vigente.
   */
  public void buildVacationPeriods(final Contract contract) {

    // TODO: Quando verrà implementata la crud per modificare manualmente
    // i piani ferie non sarà sufficiente cancellare la storia, ma dare
    // conflitto.

    for (VacationPeriod oldVacation : contract.vacationPeriods) {
      oldVacation.delete();
    }

    contract.save();
    contract.refresh();

    VacationCode v26 = vacationCodeDao.getVacationCodeByDescription("26+4");
    VacationCode v28 = vacationCodeDao.getVacationCodeByDescription("28+4");

    if (contract.expireContract == null) {

      // Tempo indeterminato, creo due vacation 3 anni più infinito

      contract.vacationPeriods.add(buildVacationPeriod(contract, v26, contract.beginContract,
              contract.beginContract.plusYears(3).minusDays(1)));
      contract.vacationPeriods
              .add(buildVacationPeriod(contract, v28, contract.beginContract.plusYears(3), null));

    } else {

      if (contract.expireContract.isAfter(contract.beginContract.plusYears(3).minusDays(1))) {

        // Tempo determinato più lungo di 3 anni

        contract.vacationPeriods.add(buildVacationPeriod(contract, v26, contract.beginContract,
                contract.beginContract.plusYears(3).minusDays(1)));

        contract.vacationPeriods.add(buildVacationPeriod(contract, v28,
                contract.beginContract.plusYears(3), contract.expireContract));

      } else {

        contract.vacationPeriods.add(
                buildVacationPeriod(contract, v26, contract.beginContract, contract.expireContract));
      }
    }
  }

  /**
   * Quando vengono modificate le date di inizio o fine del contratto occorre rivedere la struttura
   * dei periodi di tipo orario. (1)Eliminare i periodi non più appartenenti al contratto
   * (2)Modificare la data di inizio del primo periodo se è cambiata la data di inizio del contratto
   * (3)Modificare la data di fine dell'ultimo periodo se è cambiata la data di fine del contratto
   */
  private void updateContractWorkingTimeType(final Contract contract) {
    // Aggiornare i periodi workingTimeType
    // 1) Cancello quelli che non appartengono più a contract
    List<ContractWorkingTimeType> toDelete = Lists.newArrayList();
    IWrapperContract wrappedContract = wrapperFactory.create(contract);
    for (ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {
      DateInterval cwttInterval = new DateInterval(cwtt.beginDate, cwtt.endDate);
      if (DateUtility.intervalIntersection(wrappedContract.getContractDateInterval(),
              cwttInterval) == null) {
        toDelete.add(cwtt);
      }
    }
    for (ContractWorkingTimeType cwtt : toDelete) {
      cwtt.delete();
      contract.contractWorkingTimeType.remove(cwtt);
      contract.save();
    }

    // Conversione a List per avere il metodo get()
    final List<ContractWorkingTimeType> cwttList =
            Lists.newArrayList(contract.contractWorkingTimeType);

    // Sistemo il primo
    ContractWorkingTimeType first = cwttList.get(0);
    first.beginDate = wrappedContract.getContractDateInterval().getBegin();
    first.save();
    // Sistemo l'ultimo
    ContractWorkingTimeType last = cwttList.get(contract.contractWorkingTimeType.size() - 1);
    last.endDate = wrappedContract.getContractDateInterval().getEnd();
    if (DateUtility.isInfinity(last.endDate)) {
      last.endDate = null;
    }
    last.save();
    contract.save();
  }

  /**
   * Quando vengono modificate le date di inizio o fine del contratto occorre rivedere la struttura
   * dei periodi di stampProfile. (1)Eliminare i periodi non più appartenenti al contratto
   * (2)Modificare la data di inizio del primo periodo se è cambiata la data di inizio del contratto
   * (3)Modificare la data di fine dell'ultimo periodo se è cambiata la data di fine del contratto
   */
  private void updateContractStampProfile(final Contract contract) {
    // Aggiornare i periodi stampProfile
    // 1) Cancello quelli che non appartengono più a contract
    List<ContractStampProfile> toDelete = Lists.newArrayList();
    IWrapperContract wrappedContract = wrapperFactory.create(contract);
    for (ContractStampProfile csp : contract.contractStampProfile) {
      DateInterval cspInterval = new DateInterval(csp.startFrom, csp.endTo);
      if (DateUtility.intervalIntersection(wrappedContract.getContractDateInterval(),
              cspInterval) == null) {
        toDelete.add(csp);
      }
    }
    for (ContractStampProfile csp : toDelete) {
      csp.delete();
      contract.contractWorkingTimeType.remove(csp);
      contract.save();
    }

    // Conversione a List per avere il metodo get()
    List<ContractStampProfile> cspList = Lists.newArrayList(contract.contractStampProfile);

    // Sistemo il primo
    ContractStampProfile first = cspList.get(0);
    first.startFrom = wrappedContract.getContractDateInterval().getBegin();
    first.save();
    // Sistemo l'ultimo
    ContractStampProfile last = cspList.get(contract.contractStampProfile.size() - 1);
    last.endTo = wrappedContract.getContractDateInterval().getEnd();
    if (DateUtility.isInfinity(last.endTo)) {
      last.endTo = null;
    }
    last.save();
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
   * Conversione della lista dei contractStampProfile da Set a List.
   */
  public final List<ContractStampProfile> getContractStampProfileAsList(final Contract contract) {

    return Lists.newArrayList(contract.contractStampProfile);
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

  /**
   * L'impatto di un nuovo periodo tipo orario sul contratto.
   */

  public final List<PeriodModel> changedRecap(Contract contract, 
      IPeriodModel period, boolean persist) {
    boolean recomputeBeginSet = false;

    //copia di sicurezza

    List<PeriodModel> originals = period.orderedPeriods();
//    for (IPeriodModel periodsOriginal : period.orderedPeriods()) {
//      originals.add(periodsOriginal);
//    }
//    Collections.sort(originals);
    
    //riepilogo delle modifiche
    DateInterval periodInterval = new DateInterval(period.getBegin(), period.getEnd());
    List<PeriodModel> periodList = Lists.newArrayList();
    PeriodModel previous = null;
    
    List<PeriodModel> toRemove = Lists.newArrayList();
    
    for (PeriodModel oldPeriod : originals) {
      DateInterval oldInterval = new DateInterval(oldPeriod.getBegin(), oldPeriod.getEnd());

      //non cambia il tipo orario nessuna modifica su quel oldCwtt
      if (period.getValue().getPeriodValueId().equals(oldPeriod.getValue().getPeriodValueId())) {        
        previous = insertIntoList(previous, oldPeriod, periodList);
        if (previous.id == null || !previous.id.equals(oldPeriod.id)) {
          toRemove.add(oldPeriod);
        }
        continue;
      }
      DateInterval intersection = DateUtility.intervalIntersection(periodInterval, oldInterval);
      //non si intersecano nessuna modifica su quel oldCwtt
      if (intersection == null) {
        previous = insertIntoList(previous, oldPeriod, periodList);
        if (previous.id == null || !previous.id.equals(oldPeriod.id)) {
          toRemove.add(oldPeriod);
        }
        continue;
      }

      //si sovrappongono e sono diversi
      toRemove.add(oldPeriod);
      
      PeriodModel periodIntersect = period.newInstance();
      periodIntersect.setTarget(contract);
      periodIntersect.setBegin(intersection.getBegin());
      periodIntersect.setEnd(Optional.fromNullable(intersection.getEnd()));
      periodIntersect.setValue(period.getValue());
       
      //Parte iniziale old
      if (oldPeriod.getBegin().isBefore(periodIntersect.getBegin())) {
        PeriodModel periodOldBeginRemain = period.newInstance();
        periodOldBeginRemain.setTarget(contract);
        periodOldBeginRemain.setBegin(oldPeriod.getBegin());
        periodOldBeginRemain.setEnd(Optional.fromNullable(periodIntersect.getBegin().minusDays(1)));
        periodOldBeginRemain.setValue(oldPeriod.getValue());
        previous = insertIntoList(previous, periodOldBeginRemain, periodList); 
      }

      if (!recomputeBeginSet) {
        periodIntersect.recomputeFrom = periodIntersect.getBegin();
      }

      previous = insertIntoList(previous, periodIntersect, periodList);
      
      //Parte finale old
      if (periodIntersect.getEnd().isPresent()) {
        PeriodModel periodOldEndRemain = period.newInstance();
        periodOldEndRemain.setTarget(contract);
        periodOldEndRemain.setBegin(((LocalDate)periodIntersect.getEnd().get()).plusDays(1));
        periodOldEndRemain.setValue(oldPeriod.getValue());
        if (oldPeriod.getEnd().isPresent()) {
          if (((LocalDate)periodIntersect.getEnd().get())
              .isBefore((LocalDate)oldPeriod.getEnd().get())) {
            periodOldEndRemain.setEnd(oldPeriod.getEnd());
            previous = insertIntoList(previous, periodOldEndRemain, periodList); 
          }
        } else {
          periodOldEndRemain.setEnd(oldPeriod.getEnd());
          previous = insertIntoList(previous, periodOldEndRemain, periodList); 
        }
      }

    }

    if (persist) {
//      contract.refresh();
//      for (ContractWorkingTimeType cwttRemoved : toRemove) {
//        cwttRemoved.delete();
//        contract.contractWorkingTimeType.remove(cwttRemoved);
//        contract.save();
//      }
//      for (ContractWorkingTimeType cwttInsert : periodList) {
//        //if (cwttInsert.isPersistent()) {
//          cwttInsert.save();
//          contract.contractWorkingTimeType.add(cwttInsert);
//          contract.save();
//          
//        //}
//      }
//      contract.save();
//      JPAPlugin.closeTx(false);
//      JPAPlugin.startTx(false);
    }

    
    return periodList;

  }

  /**
   * Inserisce il periodo nella lista. Ritorna l'ultimo periodo inserito.
   */

  private PeriodModel insertIntoList(PeriodModel previous, 
      PeriodModel present, List<PeriodModel> periodList) {
    
    if (previous != null 
        && previous.getValue().getPeriodValueId() == present.getValue().getPeriodValueId())  {
      previous.setEnd(present.getEnd()); 
      if (present.recomputeFrom != null) {
        previous.recomputeFrom = present.recomputeFrom;
      }
      return previous;
    } else {
      periodList.add(present);
      return present;
    }
  }


}
