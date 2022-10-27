/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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
    for (Contract c : contract.getPerson().getContracts()) {

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

    if (contract.getEndDate() != null
        && contract.getEndDate().isBefore(contract.getBeginDate())) {
      return false;
    }

    if (contract.getEndContract() != null 
        && contract.getEndContract().isBefore(contract.getBeginDate())) {
      return false;
    }

    if (contract.getEndDate() != null && contract.getEndContract() != null
        && contract.getEndDate().isBefore(contract.getEndContract())) {
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

    if (contract.getBeginDate() == null) {
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

    contract.getVacationPeriods().addAll(contractVacationPeriods(contract));
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
    cwtt.setBeginDate(contract.getBeginDate());
    cwtt.setEndDate(contract.calculatedEnd());
    cwtt.setWorkingTimeType(wtt.get());
    cwtt.setContract(contract);
    cwtt.save();
    contract.getContractWorkingTimeType().add(cwtt);

    ContractStampProfile csp = new ContractStampProfile();
    csp.setContract(contract);
    csp.setBeginDate(contract.getBeginDate());
    csp.setEndDate(contract.calculatedEnd());
    csp.setFixedworkingtime(false);
    csp.save();
    contract.getContractStampProfile().add(csp);

    contract.setSourceDateResidual(null);
    contract.save();

    // FIXME: comando JPA per aggiornare la person
    contract.getPerson().getContracts().add(contract);

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
    personDayInTroubleManager.cleanPersonDayInTrouble(contract.getPerson());

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
      for (ContractMonthRecap cmr : contract.getContractMonthRecaps()) {
        if (!yearMonthFrom.isAfter(new YearMonth(cmr.getYear(), cmr.getMonth()))) {
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
   *
   * @param contract contratto
   * @param vacationCode vacationCode
   * @param beginFrom da
   * @param endTo a
   * @return il vacationPeriod
   */
  private static VacationPeriod buildVacationPeriod(final Contract contract, 
      final VacationCode vacationCode, final LocalDate beginFrom, final LocalDate endTo) {

    VacationPeriod vacationPeriod = new VacationPeriod();
    vacationPeriod.setContract(contract);
    vacationPeriod.setBeginDate(beginFrom);
    vacationPeriod.setEndDate(endTo);
    vacationPeriod.setVacationCode(vacationCode);
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

    for (ContractWorkingTimeType cwtt : contract.getContractWorkingTimeType()) {

      if (DateUtility.isDateIntoInterval(date, 
          new DateInterval(cwtt.getBeginDate(), cwtt.getEndDate()))) {
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

    if (contract.getSourceVacationLastYearUsed() == null) {
      contract.setSourceVacationLastYearUsed(0);
    }
    if (contract.getSourceVacationCurrentYearUsed() == null) {
      contract.setSourceVacationCurrentYearUsed(0);
    }
    if (contract.getSourcePermissionUsed() == null) {
      contract.setSourcePermissionUsed(0);
    }
    if (contract.getSourceRemainingMinutesCurrentYear() == null) {
      contract.setSourceRemainingMinutesCurrentYear(0);
    }
    if (contract.getSourceRemainingMinutesLastYear() == null) {
      contract.setSourceRemainingMinutesLastYear(0);
    }
    if (contract.getSourceRecoveryDayUsed() == null) {
      contract.setSourceRecoveryDayUsed(0);
    }
    if (contract.getSourceRemainingMealTicket() == null) {
      contract.setSourceRemainingMealTicket(0);
    }
    contract.save();
  }

  /**
   * Azzera l'inizializzazione del contratto.
   *
   * @param contract contract
   */
  public final void cleanResidualInitialization(final Contract contract) {
    contract.setSourceRemainingMinutesCurrentYear(0);
    contract.setSourceRemainingMinutesLastYear(0);
  }

  /**
   * Azzera l'inizializzazione del contratto.
   *
   * @param contract contract
   */
  public final void cleanVacationInitialization(final Contract contract) {
    contract.setSourceVacationLastYearUsed(0);
    contract.setSourceVacationCurrentYearUsed(0);
    contract.setSourcePermissionUsed(0);
  }

  /**
   * Azzera l'inizializzazione del buono pasto.
   *
   * @param contract contract
   */
  public final void cleanMealTicketInitialization(final Contract contract) {
    contract.setSourceDateMealTicket(contract.getSourceDateResidual());
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

    for (ContractMandatoryTimeSlot cmts : contract.getContractMandatoryTimeSlots()) {
      if (DateUtility.isDateIntoInterval(date, 
          new DateInterval(cmts.getBeginDate(), cmts.getEndDate()))) {
        return Optional.of(cmts);
      }
    }

    return Optional.absent();
  }

  /**
   * Sistema le durate dei piani ferie in relazione alle date dei piani ferie del contratto 
   * precedente.
   *
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
      if (vpPrevious.getVacationCode().equals(VacationCode.CODE_26_4)) {
        twentysixplus4 = vpPrevious;
      } else if (vpPrevious.getVacationCode().equals(VacationCode.CODE_28_4)) {
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
      vp.setContract(contract);
      vp.setBeginDate(contract.getBeginDate());
      vp.setVacationCode(VacationCode.CODE_28_4);
      vp.setEndDate(contract.getEndDate());
    }
    if (twentysixplus4 != null && twentyeightplus4 == null) {
      // si calcola la durata del piano ferie 26+4 e in base a quello il nuovo contratto parte con
      // 26+4 e 28+4 parte dopo 3 anni dall'inizio di 26+4 nel vecchio contratto
      vp = new VacationPeriod();
      vp.setContract(contract);
      vp.setVacationCode(VacationCode.CODE_28_4);
      vp.setBeginDate(twentysixplus4.getBeginDate().plusYears(3));
      vp.setEndDate(contract.getEndDate());
    }
    if (vp != null) {
      List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(vp, false);
      RecomputeRecap recomputeRecap =
          periodManager.buildRecap(wrappedContract.getContractDateInterval().getBegin(),
              Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
              periodRecaps, Optional.fromNullable(contract.getSourceDateResidual()));

      recomputeRecap.initMissing = wrappedContract.initializationMissing();
      periodManager.updatePeriods(vp, true);
      contract = contractDao.getContractById(contract.id);
      contract.getPerson().refresh();
      if (recomputeRecap.needRecomputation) {
        recomputeContract(contract,
            Optional.fromNullable(recomputeRecap.recomputeFrom), false, false);
      }
    }

  }

  /**
   * Ripristina i normali piani ferie del nuovo contratto come non continuativo del precedente.
   *
   * @param actualContract il contratto attuale
   */
  public void splitVacationPeriods(Contract actualContract) {

    for (VacationPeriod vp : actualContract.getVacationPeriods()) {
      vp.delete();
    }
    actualContract.save();
    JPA.em().flush();
    actualContract.refresh(); 

    actualContract.getVacationPeriods().addAll(contractVacationPeriods(actualContract));
    for (VacationPeriod vacationPeriod : actualContract.getVacationPeriods()) {
      vacationPeriod.save();
    }

    recomputeContract(actualContract, Optional.<LocalDate>absent(), true, false);

  }

  /**
   * Verifica se è possibile associare un precedente contratto al contratto attuale. 
   */
  public boolean canAppyPreviousContractLink(Contract contract) {
    return contractDao.getPreviousContract(contract).isPresent();
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
      log.debug("linkedToPreviousContract is true, getPreviousContract() =  {}",
          contract.getPreviousContract());
      if (contract.getPreviousContract() == null) {
        Optional<Contract> previousContract = contractDao.getPreviousContract(contract);
        if (previousContract.isPresent()) {
          contract.setPreviousContract(previousContract.get());
          if (contract.getBeginDate().minusDays(1).isEqual(previousContract.get().getEndDate())) {
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
        if (contract.getBeginDate().minusDays(1).isEqual(temp.getEndDate())) {
          splitVacationPeriods(contract);
        }
      }
    }
    return true;
  }
  
  public void fixPreviousContractLink(Contract contract) {
    log.info("Contratto id={} di {} con previousContract da correggere", contract.getId(), contract.getPerson().getFullname());
    log.info("Contract id={}, previousContract = {}", contract.getId(), contract.getPreviousContract().getId());
    if (contract.getPerson().getContracts().size() == 1) {
      log.debug("Contratto id={} singolo con riferimento al previousContract uguale a se stesso", contract.getId());
      log.debug("contratto precedente corretto = {}", contractDao.getPreviousContract(contract));
      applyPreviousContractLink(contract, false);
      log.info("Rimosso previousContract su contratto id={}", contract.getId());
    } else {
      log.info("Sono presenti più contratti per {} e quello con id = {} presenta dei problemi",
          contract.getPerson().getFullname(), contract.getId());
      contract.setPreviousContract(null);
      //applyPreviousContractLink(contract, false);
      log.info("Rimosso temporaneamente previousContratct su contratto id={}", contract.getId());
      contract.save();
      log.debug("contratto precedente corretto = {}", contractDao.getPreviousContract(contract));
      applyPreviousContractLink(contract, true);
      log.info("Impostato previousContract id = {} su contratto id={}", contract.getPreviousContract(), contract.getId());
      contract.save();
    }
    log.info("Dopo la correzione -> contract id={}, previousContract = {}", contract.getId(), contract.getPreviousContract().getId());
    contract.save();
  }
}