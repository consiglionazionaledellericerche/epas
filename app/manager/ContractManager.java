package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import dao.ContractDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.recaps.recomputation.RecomputeRecap;
import models.Contract;
import models.ContractMandatoryTimeSlot;
import models.ContractMonthRecap;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.VacationPeriod;
import models.WorkingTimeType;
import models.base.IPropertyInPeriod;
import models.enumerate.VacationCode;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.db.jpa.JPA;


/**
 * Manager per Contract.
 *
 * @author alessandro
 */
@Slf4j
public class ContractManager {

  private final ConsistencyManager consistencyManager;
  private final IWrapperFactory wrapperFactory;
  private final PeriodManager periodManager;
  private final PersonDayInTroubleManager personDayInTroubleManager;
  private final WorkingTimeTypeDao workingTimeTypeDao;
  private final ContractDao contractDao;

  /**
   * Constructor.
   *
   * @param consistencyManager          {@link manager.ConsistencyManager}
   * @param periodManager               {@link manager.PeriodManager}
   * @param personDayInTroubleManager   {@link manager.PersonDayInTroubleManager}
   * @param wrapperFactory              {@link IWrapperFactory}
   */
  @Inject
  public ContractManager(
      final ConsistencyManager consistencyManager,
      final PeriodManager periodManager, final PersonDayInTroubleManager personDayInTroubleManager, 
      final WorkingTimeTypeDao workingTimeTypeDao,
      final IWrapperFactory wrapperFactory, final ContractDao contractDao) {

    this.consistencyManager = consistencyManager;
    this.periodManager = periodManager;
    this.personDayInTroubleManager = personDayInTroubleManager;
    this.workingTimeTypeDao = workingTimeTypeDao;
    this.wrapperFactory = wrapperFactory;
    this.contractDao = contractDao;
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
        log.debug("Il contratto {} si sovrappone con il contratto {}", contract, c);
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
   * @param wtt      il tipo orari
   * @param recomputation      se effettuare subito il ricalcolo della persona.
   * @return esito costruzione
   */
  public final boolean properContractCreate(final Contract contract, 
      Optional<WorkingTimeType> wtt, 
      boolean recomputation) {

    if (contract.beginDate == null) {
      log.debug("Impossibile creare il contratto, beginDate è null");
      return false;
    }

    if (!isContractCrossFieldValidationPassed(contract)) {
      log.debug("Impossibile creare il contratto, crossData validation fallita");
      return false;
    }

    if (!isContractNotOverlapping(contract)) {
      log.debug("Impossibile creare il contratto, il contratto si sovrappone con altri contratti "
          + "preesistenti");
      return false;
    }

    contract.save();

    contract.vacationPeriods.addAll(contractVacationPeriods(contract));
    for (VacationPeriod vacationPeriod : contract.getVacationPeriods()) {
      vacationPeriod.save();
    }

    if (!wtt.isPresent()) {
      wtt = Optional.fromNullable(workingTimeTypeDao
          .workingTypeTypeByDescription("Normale", Optional.absent()));
      if (!wtt.isPresent()) {
        throw new IllegalStateException();
      }
    }

    ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
    cwtt.beginDate = contract.getBeginDate();
    cwtt.endDate = contract.calculatedEnd();
    cwtt.workingTimeType = wtt.get();
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

    if (recomputation) {
      recomputeContract(contract, Optional.<LocalDate>absent(), true, false);
    }

    return true;

  }

  /**
   * Aggiorna il contratto in modo sicuro ed effettua il ricalcolo.
   *
   * @param contract   contract
   * @param from       da quando effettuare il ricalcolo.
   * @param onlyRecaps ricalcolare solo i riepiloghi mensili.
   */
  public final boolean properContractUpdate(final Contract contract, final LocalDate from,
      final boolean onlyRecaps) {

    if (!isContractCrossFieldValidationPassed(contract)) {
      return false;
    }

    if (!isContractNotOverlapping(contract)) {
      return false;
    }

    contract.save();
    periodManager.updatePropertiesInPeriodOwner(contract);
    personDayInTroubleManager.cleanPersonDayInTrouble(contract.person);

    recomputeContract(contract, Optional.fromNullable(from), false, onlyRecaps);

    return true;
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

    LocalDate startDate = dateFrom
        .or(wrContract.getContractDatabaseInterval().getBegin());

    if (!newContract) {

      YearMonth yearMonthFrom = new YearMonth(startDate);

      // Distruggere i riepiloghi esistenti da yearMonthFrom.
      // TODO: anche quelli sulle ferie quando ci saranno
      for (ContractMonthRecap cmr : contract.contractMonthRecaps) {
        if (!yearMonthFrom.isAfter(new YearMonth(cmr.year, cmr.month))) {
          if (cmr.isPersistent()) {
            cmr.delete();
          }          
        }
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
    vacationPeriod.setBeginDate(beginFrom);
    vacationPeriod.setEndDate(endTo);
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

    VacationCode v26 = VacationCode.CODE_26_4;
    VacationCode v28 = VacationCode.CODE_28_4;

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
    contract.save();
  }

  /**
   * Azzera l'inizializzazione del contratto.
   *
   * @param contract contract
   */
  public final void cleanResidualInitialization(final Contract contract) {
    contract.sourceRemainingMinutesCurrentYear = 0;
    contract.sourceRemainingMinutesLastYear = 0;
  }

  /**
   * Azzera l'inizializzazione del contratto.
   *
   * @param contract contract
   */
  public final void cleanVacationInitialization(final Contract contract) {
    contract.sourceVacationLastYearUsed = 0;
    contract.sourceVacationCurrentYearUsed = 0;
    contract.sourcePermissionUsed = 0;
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
   * Il ContractMandatoryTimeSlot associato ad un contratto in una specifica data.
   *
   * @param contract il contratto di cui prelevare il ContractMandatoryTimeSlot
   * @param date     la data in cui controllare il ContractMandatoryTimeSlot
   * @return il ContractMandatoryTimeSlot di un contratto ad una data specifica
   */
  public final Optional<ContractMandatoryTimeSlot> getContractMandatoryTimeSlotFromDate(
      final Contract contract, final LocalDate date) {

    for (ContractMandatoryTimeSlot cmts : contract.contractMandatoryTimeSlots) {
      if (DateUtility.isDateIntoInterval(date, new DateInterval(cmts.beginDate, cmts.endDate))) {
        return Optional.of(cmts);
      }
    }

    return Optional.absent();
  }

  /**
   * Sistema le durate dei piani ferie in relazione alle date dei piani ferie del contratto 
   * precedente.
   * @param contract il contratto attuale
   * @param previousContract il contratto precedente
   */
  public void mergeVacationPeriods(Contract contract, Contract previousContract) {
    VacationPeriod twentysixplus4 = null;
    VacationPeriod twentyeightplus4 = null;
    VacationPeriod other = null;
    IWrapperContract wrappedContract = wrapperFactory.create(contract);
    List<VacationPeriod> vpList = previousContract.getVacationPeriods();    
    VacationPeriod vp = null;
    for (VacationPeriod vpPrevious : vpList) {
      if (vpPrevious.vacationCode.equals(VacationCode.CODE_26_4)) {
        twentysixplus4 = vpPrevious;
      } else if (vpPrevious.vacationCode.equals(VacationCode.CODE_28_4)) {
        twentyeightplus4 = vpPrevious;
      } else {
        other = vpPrevious;
      }
    }
    if (twentysixplus4 == null && other != null) {
      //non c'è piano ferie iniziale, si è cominiciato con un part time...che si fa?
    }
    if (twentyeightplus4 != null && twentysixplus4 != null) {
      //in questo caso il nuovo contratto partirà già col piano ferie 28+4 dal primo giorno
      vp = new VacationPeriod();
      vp.contract = contract;
      vp.beginDate = contract.beginDate;
      vp.vacationCode = VacationCode.CODE_28_4;
      vp.endDate = contract.endDate;
    }
    if (twentysixplus4 != null && twentyeightplus4 == null) {
      // si calcola la durata del piano ferie 26+4 e in base a quello il nuovo contratto parte con
      // 26+4 e 28+4 parte dopo 3 anni dall'inizio di 26+4 nel vecchio contratto
      vp = new VacationPeriod();
      vp.contract = contract;
      vp.vacationCode = VacationCode.CODE_28_4;
      vp.beginDate = twentysixplus4.beginDate.plusYears(3);
      vp.endDate = contract.endDate;
    }
    if (vp != null) {
      List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(vp, false);
      RecomputeRecap recomputeRecap =
          periodManager.buildRecap(wrappedContract.getContractDateInterval().getBegin(),
              Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
              periodRecaps, Optional.fromNullable(contract.sourceDateResidual));

      recomputeRecap.initMissing = wrappedContract.initializationMissing();
      periodManager.updatePeriods(vp, true);
      contract = contractDao.getContractById(contract.id);
      contract.person.refresh();
      if (recomputeRecap.needRecomputation) {
        recomputeContract(contract,
            Optional.fromNullable(recomputeRecap.recomputeFrom), false, false);
      }
    }

  }

  /**
   * Ripristina i normali piani ferie del nuovo contratto come non continuativo del precedente.
   * @param actualContract il contratto attuale
   */
  public void splitVacationPeriods(Contract actualContract) {

    for (VacationPeriod vp : actualContract.getVacationPeriods()) {
      vp.delete();
    }
    actualContract.save();
    JPA.em().flush();
    actualContract.refresh(); 

    actualContract.vacationPeriods.addAll(contractVacationPeriods(actualContract));
    for (VacationPeriod vacationPeriod : actualContract.getVacationPeriods()) {
      vacationPeriod.save();
    }

    recomputeContract(actualContract, Optional.<LocalDate>absent(), true, false);

  }

  /**
   * Verifica se è possibile associare un precedente contratto al contratto attuale. 
   */
  public boolean canAppyPreviousContractLink(Contract contract) {
    IWrapperPerson wrapperPerson = wrapperFactory.create(contract.person);
    Optional<Contract> previousContract = wrapperPerson.getPreviousContract();
    return previousContract.isPresent();
  }

  /**
   * Inserisce il collegamento al contratto precedente se il parametro linkedToPreviousContract
   * è true.
   * 
   * @param contract il contratto su cui impostare o rimuovere il collegamento al contratto 
   *     precedente.
   * @param linkedToPreviousContract indica se il contratto precedente deve essere collegato o meno.
   * @return true se è possibile impostare o rimuovore il link al contratto precedente, false
   *     se si è richiesto di impostare il contratto precedente ma questo non è presente. 
   */
  public boolean applyPreviousContractLink(Contract contract, boolean linkedToPreviousContract) {
    //Controllo se il contratto deve essere linkato al precedente...
    if (linkedToPreviousContract) {
      if (contract.getPreviousContract() == null) {
        IWrapperPerson wrapperPerson = wrapperFactory.create(contract.person);
        Optional<Contract> previousContract = wrapperPerson.getPreviousContract();
        if (previousContract.isPresent()) {
          contract.setPreviousContract(previousContract.get());          
          if (contract.beginDate.minusDays(1).isEqual(previousContract.get().endDate)) {
            mergeVacationPeriods(contract, previousContract.get());            
          }
        } else {
          return false;
        }
      }    
    } else {
      Contract temp = contract.getPreviousContract();
      if (temp != null) {        
        contract.setPreviousContract(null);

        if (contract.beginDate.minusDays(1).isEqual(temp.endDate)) {
          splitVacationPeriods(contract);
        }         
      }
    }
    return true;
  }
}
