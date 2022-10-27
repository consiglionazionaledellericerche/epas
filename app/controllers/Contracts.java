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

package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import common.security.SecurityRules;
import dao.AbsenceDao;
import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.history.ContractHistoryDao;
import dao.history.HistoryValue;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;
import helpers.Web;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.ContractManager;
import manager.PeriodManager;
import manager.attestati.service.ICertificationService;
import manager.recaps.recomputation.RecomputeRecap;
import manager.service.contracts.ContractService;
import manager.services.absences.AbsenceService;
import models.Contract;
import models.ContractMandatoryTimeSlot;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.PersonDay;
import models.PersonalWorkingTime;
import models.VacationPeriod;
import models.WorkingTimeType;
import models.absences.Absence;
import models.base.IPropertyInPeriod;
import org.joda.time.LocalDate;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione dei contratti.
 */
@Slf4j
@With({Resecure.class})
public class Contracts extends Controller { 

  @Inject
  static PersonDao personDao;
  @Inject
  static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  static ContractManager contractManager;
  @Inject
  static SecurityRules rules;
  @Inject
  static ContractDao contractDao;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static PeriodManager periodManager;
  @Inject
  static ICertificationService certService;
  @Inject
  static AbsenceService absenceService;
  @Inject
  static PersonDayDao personDayDao;
  @Inject
  static ContractService contractService;
  @Inject
  static ContractHistoryDao historyDao;
  @Inject
  static AbsenceDao absenceDao;

  /**
   * I contratti del dipendente.
   *
   * @param personId personId
   */
  public static void personContracts(final Long personId) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    IWrapperPerson wrPerson = wrapperFactory.create(person);
    rules.checkIfPermitted(person.getOffice());

    IWrapperContract wrCurrentContract = null;
    if (wrPerson.getCurrentContract().isPresent()) {
      wrCurrentContract = wrapperFactory.create(wrPerson.getCurrentContract().get());
    }
    List<IWrapperContract> contractList =
        FluentIterable.from(contractDao.getPersonContractList(person))
        .transform(wrapperFunctionFactory.contract()).toList();

    render(person, wrPerson, wrCurrentContract, contractList);
  }

  /**
   * Edit date e on certificate contratto.
   *
   * @param contractId contractId
   */
  public static void edit(Long contractId) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.getPerson().getOffice());
    Person person = contract.getPerson();

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    LocalDate beginDate = contract.getBeginDate();
    LocalDate endDate = contract.getEndDate();
    LocalDate endContract = contract.getEndContract();
    boolean onCertificate = contract.isOnCertificate();
    boolean isTemporaryMissing = contract.isTemporaryMissing();
    boolean linkedToPreviousContract = contract.getPreviousContract() != null ? true : false;
    String perseoId = contract.getPerseoId();
    LocalDate sourceDateRecoveryDay = contract.getSourceDateRecoveryDay();
    render(person, contract, wrappedContract, beginDate, endDate, endContract,
        onCertificate, isTemporaryMissing, perseoId, sourceDateRecoveryDay, 
        linkedToPreviousContract);
  }

  /**
   * Renderizza la pagina in cui si può provvedere allo split di un contratto.
   *
   * @param contractId l'identificativo del contratto da splittare
   */
  public static void split(Long contractId) {
    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.getPerson().getOffice());

    LocalDate dateToSplit = new LocalDate();

    render(contract, dateToSplit);
  }

  /**
   * Provvede alla divisione del contratto contract in due contratti distinti, uno che termina il 
   * giorno precedente dateToSplit e l'altro che inizia a dateToSplit.
   *
   * @param contract il contratto da splittare
   * @param dateToSplit la data a cui splittarlo
   */
  public static void splitContract(@Valid Contract contract, @Required LocalDate dateToSplit, 
      boolean attestatiSync) {
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.getPerson().getOffice());

    if (!DateUtility.isDateIntoInterval(dateToSplit, 
        new DateInterval(contract.getBeginDate(), contract.calculatedEnd()))) {
      Validation.addError("dateToSplit", "La data deve appartenere al contratto!!!");
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      render("@split", contract, dateToSplit);
    }

    Optional<LocalDate> to = contract.getEndDate() != null 
        ? Optional.fromNullable(contract.getEndDate()) : Optional.absent();

    //1) si eliminano le assenze a partire da dateToSplit
    List<Absence> copy = Lists.newArrayList();
    List<Absence> list = contractService
        .getAbsencesInContract(contract.getPerson(), dateToSplit, to);
    log.debug("Lista assenze contiene {} elementi", list.size());
    if (!attestatiSync) {
      copy.addAll(list);
    }
    int count = 0;
    for (Absence abs : list) {      
      abs.delete();  
      count++;
    }

    IWrapperContract wrappedContract = wrapperFactory.create(contract);
    log.debug("Rimosse {} assenze per {}", count, contract.getPerson().getFullname());

    //2) si splitta il contratto in due contratti nuovi
    log.info("Inizio procedura di split del contratto {}", contract.toString());
    final DateInterval previousInterval = wrappedContract.getContractDatabaseInterval();
    contract.setEndDate(dateToSplit.minusDays(1));
    if (!contractManager.isContractNotOverlapping(contract)) {
      Validation.addError("contract.crossValidationFailed",
          "Il contratto non può intersecarsi" + " con altri contratti del dipendente.");
      render("@split", contract, wrappedContract, dateToSplit);
    }
    DateInterval newInterval = wrappedContract.getContractDatabaseInterval();
    RecomputeRecap recomputeRecap = periodManager.buildTargetRecap(previousInterval, newInterval,
        wrappedContract.initializationMissing());
    if (recomputeRecap.recomputeFrom != null) {
      contractManager.properContractUpdate(contract, recomputeRecap.recomputeFrom, false);
    } else {
      contractManager.properContractUpdate(contract, LocalDate.now(), false);
    }
    log.info("Termine procedura di split del contratto {}", contract.toString());

    //3) creo il nuovo contratto a partire da dateToSplit
    log.info("Creazione nuovo contratto");
    IWrapperPerson wrappedPerson = wrapperFactory.create(contract.getPerson());
    Optional<WorkingTimeType> wtt = wrappedPerson.getCurrentWorkingTimeType();

    Contract newContract = contractService.createNewContract(wrappedPerson.getValue(),
        dateToSplit, wtt, previousInterval);
    contractManager.properContractCreate(newContract, wtt, true);
    log.info("Fine creazione nuovo contratto: {}", newContract.toString());

    //4) riassegno le assenze sul nuovo contratto...


    if (count != 0) {
      if (!attestatiSync) {
        log.info("Si resettano le assenze salvate in precedenza.");
        contractService.resetAbsences(list);
        log.info("Procedura completata.");
      } else {
        log.info("Scaricamento e persistenza assenze da Attestati a partire da {}", dateToSplit);
        contractService.saveAbsenceOnNewContract(wrappedPerson.getValue(), dateToSplit);
        log.info("Terminata persistenza assenze.");
      }

    }

    flash.success(Web.msgSaved(Contract.class));
    personContracts(wrappedPerson.getValue().id);
  }

  /**
   * Renderizza la pagina in cui si può fondere l'attuale contratto col precedente, se esiste.
   *
   * @param contractId l'identificativo del contratto da fondere
   */
  public static void merge(Long contractId) {
    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.getPerson().getOffice());
    Person person = contract.getPerson();

    Contract previousContract = personDao.getPreviousPersonContract(contract);    

    render(person, contract, previousContract);
  }

  /**
   * Fonde insieme due contratti contigui.
   *
   * @param contract il contratto attuale
   * @param previousContract il contratto precedente
   */
  public static void mergeContract(@Valid Contract contract, @Valid Contract previousContract, 
      boolean attestatiSync) {
    notFoundIfNull(contract);
    notFoundIfNull(previousContract);
    rules.checkIfPermitted(contract.getPerson().getOffice());
    Optional<LocalDate> to = contract.getEndDate() != null 
        ? Optional.fromNullable(contract.getEndDate()) : Optional.absent();

    //1) si eliminano le assenze a partire da contract.beginDate
    List<Absence> copy = Lists.newArrayList();
    List<Absence> list = contractService
        .getAbsencesInContract(contract.getPerson(), contract.getBeginDate(), to);
    if (!attestatiSync) {
      copy.addAll(list);
    }
    log.debug("Lista assenze contiene {} elementi", list.size());
    int count = 0;
    for (Absence abs : list) {
      //abs.delete();
      count++;
    }
    log.debug("Cancellate {} assenze", count);
    
    //1-bis) si salvano i tipi orario di lavoro
    Set<ContractWorkingTimeType> cwtts = Sets.newHashSet(contract. getContractWorkingTimeType());

    //2-bis) assegno i precedenti orari di lavoro al contratto che rimarrà
    cwtts.stream().forEach(cwtt -> {
      cwtt.setContract(previousContract);
      cwtt.save();
      previousContract.getContractWorkingTimeType().add(cwtt);
      previousContract.save();
      previousContract.refresh();
    });

    //2) cancello il contratto più recente
    contract.delete();
    contract.getPerson().getContracts().remove(contract);
    log.debug("Cancellato contratto {}", contract.toString());


    JPA.em().flush();
    //3) prorogo la data fine di previousContract a contract.endDate
    IWrapperContract wrappedContract = wrapperFactory.create(previousContract);
    IWrapperPerson wrappedPerson = wrapperFactory.create(previousContract.getPerson());
    final DateInterval previousInterval = wrappedContract.getContractDatabaseInterval();

    previousContract.setEndDate(to.isPresent() ? to.get() : null);
    log.debug("Prorogo il precedente contratto {} alla data {}", 
        previousContract, previousContract.getEndDate());
    DateInterval newInterval = wrappedContract.getContractDatabaseInterval();
    RecomputeRecap recomputeRecap = periodManager.buildTargetRecap(previousInterval, newInterval,
        wrappedContract.initializationMissing());
    if (recomputeRecap.recomputeFrom != null) {
      contractManager.properContractUpdate(previousContract, recomputeRecap.recomputeFrom, false);
    } else {
      contractManager.properContractUpdate(previousContract, LocalDate.now(), false);
    }

    //4) riassegno le assenze sul nuovo contratto...
    if (count != 0) {
      if (!attestatiSync) {
        log.info("Si resettano le assenze salvate in precedenza.");
        //contractService.resetAbsences(copy);
        log.info("Procedura completata.");
      } else {
        log.info("Scaricamento e persistenza assenze da Attestati a partire da {}", 
            contract.getBeginDate());
        contractService.saveAbsenceOnNewContract(wrappedPerson.getValue(), contract.getBeginDate());
        log.info("Terminata persistenza assenze.");

      }
    }

    flash.success(Web.msgSaved(Contract.class));
    personContracts(wrappedPerson.getValue().id);

  }

  /**
   * Aggiornamento date e on certificate contratto.
   *
   * @param contract      contract
   * @param confirmed     step di conferma
   */
  public static void update(@Valid Contract contract, 
      boolean confirmed, Boolean isTemporaryMissing,
      LocalDate sourceDateRecoveryDay, boolean linkedToPreviousContract) { 

    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.getPerson().getOffice());

    IWrapperContract wrappedContract = wrapperFactory.create(contract);
    
    if (Validation.hasErrors()) {
      log.info("ValidationErrors = {}", validation.errorsMap());
      response.status = 400;
      render("@edit", contract, wrappedContract, sourceDateRecoveryDay);
    }

    // Salvo la situazione precedente
    final DateInterval previousInterval = wrappedContract.getContractDatabaseInterval();

    // Attribuisco il nuovo stato al contratto per effettuare il controllo incrociato
    contract.setSourceDateRecoveryDay(sourceDateRecoveryDay);
    if (isTemporaryMissing != null) {
      contract.setTemporaryMissing(isTemporaryMissing);  
    }
    //Controllo se il contratto deve e può essere linkato al precedente...
    if (linkedToPreviousContract && !contractManager.canAppyPreviousContractLink(contract)) {
      Validation.addError("linkedToPreviousContract", 
          "Non esiste alcun contratto precedente cui linkare il contratto attuale");
      render("@edit", contract, wrappedContract,
          sourceDateRecoveryDay, linkedToPreviousContract);
    }
    if (confirmed) {
      contractManager.applyPreviousContractLink(contract, linkedToPreviousContract);
    }

    DateInterval newInterval = wrappedContract.getContractDatabaseInterval();
    RecomputeRecap recomputeRecap = periodManager.buildTargetRecap(previousInterval, newInterval,
        wrappedContract.initializationMissing());

    //conferma
    if (!confirmed) {
      confirmed = true;
      response.status = 400;
      render("@edit", contract, wrappedContract, 
          confirmed, isTemporaryMissing, recomputeRecap, sourceDateRecoveryDay, 
          linkedToPreviousContract);
    } else {
      // Controllo eventuali assenze future prima di chiudere il contratto.
      List<HistoryValue<Contract>> list = historyDao.lastRevision(contract.id);
      
      HistoryValue<Contract> item = list.get(0);
      if ((item.value.getEndDate() == null && contract.getEndDate() != null) 
          || (item.value.getEndContract() == null && contract.getEndContract() != null)) {
        PersonDay pd = personDayDao.getMoreFuturePersonDay(contract.getPerson());
        if ((contract.getEndDate() != null && pd.getDate().isAfter(contract.getEndDate())) 
            || (contract.getEndContract() != null && pd.getDate().isAfter(contract.getEndContract()))) {
          //Cancellazione dei personday successivi alla fine del contratto
          LocalDate from = null;
          if (contract.getEndDate() != null && contract.getEndContract() == null) {
            from = contract.getEndDate();
          } 
          if (contract.getEndContract() != null && contract.getEndDate() == null) {
            from = contract.getEndContract();
          }
          if (contract.getEndContract() != null && contract.getEndDate() != null) {
            from = contract.getEndContract();
          }
          List<Absence> absenceFutureList = absenceDao
              .absenceInPeriod(contract.getPerson(), from, Optional.fromNullable(pd.getDate()));
          for (Absence absence : absenceFutureList) {
            absence.delete();
          }
        }
      }
      if (recomputeRecap.recomputeFrom != null) {
        contractManager.properContractUpdate(contract, recomputeRecap.recomputeFrom, false);
      } else {
        contractManager.properContractUpdate(contract, LocalDate.now(), false);
      }

      flash.success(Web.msgSaved(Contract.class));
      confirmed = false;
      edit(contract.id);
    }

  }

  /**
   * Nuovo contratto.
   *
   * @param person person
   */
  public static void insert(Person person) {

    notFoundIfNull(person);

    rules.checkIfPermitted(person.getOffice());

    Contract contract = new Contract();
    contract.setPerson(person);

    render(contract);
  }

  /**
   * Salva nuovo contratto.
   *
   * @param contract contract
   * @param wtt      il tipo orario
   */
  public static void save(@Valid Contract contract, @Valid WorkingTimeType wtt) {

    notFoundIfNull(contract);
    notFoundIfNull(wtt);

    rules.checkIfPermitted(contract.getPerson().getOffice());

    if (!Validation.hasErrors()) {
      if (!contractManager.properContractCreate(contract, Optional.of(wtt), true)) {
        Validation.addError("contract.beginDate", "i contratti non possono sovrapporsi.");
        if (contract.getEndDate() != null) {
          Validation.addError("contract.endDate", "i contratti non possono sovrapporsi.");
        }
      }
    }

    if (Validation.hasErrors()) {
      response.status = 400;
      render("@insert", contract, wtt);
    }

    flash.success(Web.msgCreated(Contract.class));

    Contracts.personContracts(contract.getPerson().id);
  }

  /**
   * Rimozione contratto.
   *
   * @param contractId contractId
   */
  public static void delete(Long contractId, boolean confirmed) {

    Contract contract = contractDao.getContractById(contractId);

    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.getPerson().getOffice());

    if (!confirmed) {
      render("@delete", contract);
    }
    List<PersonDay> pdList = personDayDao.getPersonDayInPeriod(contract.getPerson(), 
        contract.getBeginDate(), Optional.fromNullable(contract.getEndDate()));

    long count = pdList.stream().filter(pd -> pd.getAbsences().isEmpty()).count();
    if (pdList.size() == 0 || count == pdList.size()) {
      contract.delete();
      flash.success(Web.msgDeleted(Contract.class));
    } else {
      flash.error("Non è possibile cancellare il contratto di %s perchè sono già presenti giornate"
          + " ad esso collegate contenenti assenze! Rimuoverle prima di eliminare il contratto", 
          contract.getPerson().fullName());
    }    

    Persons.edit(contract.getPerson().id);
  }

  /**
   * Crud gestione periodi tipo orario.
   *
   * @param id contratto
   */
  public static void updateContractWorkingTimeType(Long id) {

    Contract contract = contractDao.getContractById(id);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.getPerson().getOffice());

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
    cwtt.setContract(contract);

    render(wrappedContract, contract, cwtt);
  }

  /**
   * Salva il nuovo periodo orario.
   *
   * @param cwtt      nuovo periodo di orario.
   * @param confirmed se conferma ricevuta.
   */
  public static void saveContractWorkingTimeType(ContractWorkingTimeType cwtt, boolean confirmed) {

    // IMPORTANTE!!! Rimosso @Valid da cwtt e effettuata la validazione mancante a mano.
    // Perchè si comprometteva il funzionamento dell drools. Issue #166

    notFoundIfNull(cwtt);
    notFoundIfNull(cwtt.getContract());
    notFoundIfNull(cwtt.getWorkingTimeType());
    Verify.verify(cwtt.getContract().isPersistent());
    Verify.verify(cwtt.getWorkingTimeType().isPersistent());

    rules.checkIfPermitted(cwtt.getContract().getPerson().getOffice());

    IWrapperContract wrappedContract = wrapperFactory.create(cwtt.getContract());
    Contract contract = cwtt.getContract();

    if (!Validation.hasErrors()) {
      if (!DateUtility.isDateIntoInterval(cwtt.getBeginDate(),
          wrappedContract.getContractDateInterval())) {
        Validation.addError("cwtt.beginDate", "deve appartenere al contratto");
      }
      if (cwtt.getEndDate() != null && !DateUtility.isDateIntoInterval(cwtt.getEndDate(),
          wrappedContract.getContractDateInterval())) {
        Validation.addError("cwtt.endDate", "deve appartenere al contratto");
      }
    }

    if (Validation.hasErrors()) {
      response.status = 400;

      render("@updateContractWorkingTimeType", cwtt, contract);
    }

    rules.checkIfPermitted(cwtt.getWorkingTimeType().getOffice());

    //riepilogo delle modifiche
    List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(cwtt, false);
    RecomputeRecap recomputeRecap =
        periodManager.buildRecap(wrappedContract.getContractDateInterval().getBegin(),
            Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
            periodRecaps, Optional.fromNullable(contract.getSourceDateResidual()));

    recomputeRecap.initMissing = wrappedContract.initializationMissing();

    if (!confirmed) {
      confirmed = true;
      render("@updateContractWorkingTimeType", contract, cwtt, confirmed, recomputeRecap);
    } else {

      periodManager.updatePeriods(cwtt, true);
      contract = contractDao.getContractById(contract.id);
      contract.getPerson().refresh();
      if (recomputeRecap.needRecomputation) {
        contractManager.recomputeContract(contract,
            Optional.fromNullable(recomputeRecap.recomputeFrom), false, false);
      }

      flash.success(Web.msgSaved(ContractWorkingTimeType.class));

      updateContractWorkingTimeType(contract.id);
    }

  }
  
  /**
   * Crud gestione periodi di orario di lavoro personalizzato.
   *
   * @param id contratto
   */
  public static void updatePersonalWorkingTime(Long id, PersonalWorkingTime pwt) {

    Contract contract = contractDao.getContractById(id);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.getPerson().getOffice());

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    if (pwt == null) {
      pwt = new PersonalWorkingTime();
    }
    pwt.setContract(contract);

    render("@updatePersonalWorkingTime", wrappedContract, contract, pwt);
  }
  
  /**
   * Salva il nuovo periodo di orario di lavoro personalizzato.
   *
   * @param pwt l'orario di lavoro personalizzato
   * @param confirmed conferma se true
   */
  public static void savePersonalWorkingTime(PersonalWorkingTime pwt, boolean confirmed) {
    
    notFoundIfNull(pwt);
    notFoundIfNull(pwt.getContract());
    notFoundIfNull(pwt.getTimeSlot());
    Verify.verify(pwt.getContract().isPersistent());
    Verify.verify(pwt.getTimeSlot().isPersistent());
    
    rules.checkIfPermitted(pwt.getContract().getPerson().getOffice());

    IWrapperContract wrappedContract = wrapperFactory.create(pwt.getContract());
    Contract contract = pwt.getContract();
        
    if (!Validation.hasErrors()) {
      if (!DateUtility.isDateIntoInterval(pwt.getBeginDate(),
          wrappedContract.getContractDateInterval())) {
        Validation.addError("pwt.beginDate", "deve appartenere al contratto");
      }
      if (pwt.getEndDate() != null && !DateUtility.isDateIntoInterval(pwt.getEndDate(),
          wrappedContract.getContractDateInterval())) {
        Validation.addError("pwt.endDate", "deve appartenere al contratto");
      }
    }

    if (Validation.hasErrors()) {
      render("@updatePersonalWorkingTime", pwt, wrappedContract, contract);
    }
    
    val currentPeriods = pwt.getOwner().periods(pwt.getType());
    if (pwt.isPersistent()) {
      currentPeriods.remove(pwt);
    }
    
    if (periodManager.isOverlapped(pwt, currentPeriods)) {
      flash.error("Il periodo %s si sovrappone con altre periodi esistenti, "
          + "impossibile aggiungerlo", pwt.getLabel());
      response.status = 400;
      log.warn("Il periodo {} si sovrappone con altre periodi esistenti, impossibile aggiungerlo", 
          pwt.getLabel());
      render("@updatePersonalWorkingTime", pwt, wrappedContract, contract);      
    }

    val periodRecaps = Lists.newArrayList(currentPeriods);
    pwt.setRecomputeFrom(pwt.getBeginDate());   
    periodRecaps.add(pwt);

    RecomputeRecap recomputeRecap =
        periodManager.buildRecap(wrappedContract.getContractDateInterval().getBegin(),
            Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
            periodRecaps, Optional.fromNullable(contract.getSourceDateResidual()));

    recomputeRecap.initMissing = wrappedContract.initializationMissing();

    if (!confirmed) {
      confirmed = true;
      render("@updatePersonalWorkingTime", contract, pwt, confirmed, recomputeRecap);
    } else {
      
      pwt.save();
      contract = contractDao.getContractById(contract.id);
      contract.getPerson().refresh();
      if (recomputeRecap.needRecomputation) {
        contractManager.recomputeContract(contract,
            Optional.fromNullable(recomputeRecap.recomputeFrom), false, false);
      }

      flash.success(Web.msgSaved(PersonalWorkingTime.class));
      updatePersonalWorkingTime(contract.id, null);
    }
  }

  /**
   * Cancella un orario di lavoro personalizzato.
   *
   * @param id identificativo dell'orario da cancellare
   * @param confirmed conferma se true
   */
  public static void deletePersonalWorkingTime(Long id, boolean confirmed) {
    
    PersonalWorkingTime pwt = PersonalWorkingTime.findById(id);

    notFoundIfNull(pwt);

    rules.checkIfPermitted(pwt.getContract().getPerson().getOffice());

    boolean deletion = true;
    val contract = pwt.getContract();
    if (!confirmed) {
      IWrapperContract wrappedContract = wrapperFactory.create(pwt.getContract());
      val currentPeriods = pwt.getOwner().periods(pwt.getType());
      val periodRecaps = Lists.newArrayList(currentPeriods);

      periodRecaps.remove(pwt);
      RecomputeRecap recomputeRecap =
          periodManager.buildRecap(pwt.getBeginDate(),
              Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
              periodRecaps, Optional.fromNullable(contract.getSourceDateResidual()));
      recomputeRecap.recomputeFrom = pwt.getBeginDate();
      periodManager.setDays(recomputeRecap);
      confirmed = true;
      render("@updatePersonalWorkingTime", pwt, contract, deletion, recomputeRecap);
    } else {
      pwt.delete();
      flash.success(Web.msgDeleted(PersonalWorkingTime.class));
    }
    updatePersonalWorkingTime(pwt.getContract().id, null);
  }
  
  
  /**
   * Crud gestione periodi fascia oraria obbligatoria.
   *
   * @param id contratto
   */
  public static void updateContractMandatoryTimeSlot(Long id, ContractMandatoryTimeSlot cmts) {

    Contract contract = contractDao.getContractById(id);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.getPerson().getOffice());

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    if (cmts == null) {
      cmts = new ContractMandatoryTimeSlot();
    }
    cmts.setContract(contract);
    render("@updateContractMandatoryTimeSlot", wrappedContract, contract, cmts);
  }

  /**
   * Salva il nuovo periodo di fascia di orario obbligatorio.
   *
   * @param cmts nuovo periodo di fascia oraria obbligatoria.
   * @param confirmed se conferma ricevuta.
   */
  public static void saveContractMandatoryTimeSlot(
      ContractMandatoryTimeSlot cmts, boolean confirmed) {

    // IMPORTANTE!!! Rimosso @Valid da cmts ed effettuata la validazione mancante a mano.
    // Perchè si comprometteva il funzionamento dell drools. Issue #166

    notFoundIfNull(cmts);
    notFoundIfNull(cmts.getContract());
    notFoundIfNull(cmts.getTimeSlot());
    Verify.verify(cmts.getContract().isPersistent());
    Verify.verify(cmts.getTimeSlot().isPersistent());

    rules.checkIfPermitted(cmts.getContract().getPerson().getOffice());

    IWrapperContract wrappedContract = wrapperFactory.create(cmts.getContract());
    Contract contract = cmts.getContract();

    if (!Validation.hasErrors()) {
      if (!DateUtility.isDateIntoInterval(cmts.getBeginDate(),
          wrappedContract.getContractDateInterval())) {
        Validation.addError("cmts.beginDate", "deve appartenere al contratto");
      }
      if (cmts.getEndDate() != null && !DateUtility.isDateIntoInterval(cmts.getEndDate(),
          wrappedContract.getContractDateInterval())) {
        Validation.addError("cmts.endDate", "deve appartenere al contratto");
      }
    }

    if (Validation.hasErrors()) {
      render("@updateContractMandatoryTimeSlot", cmts, contract);
    }

    rules.checkIfPermitted(cmts.getTimeSlot().getOffice());

    val currentPeriods = cmts.getOwner().periods(cmts.getType());
    if (cmts.isPersistent()) {
      currentPeriods.remove(cmts);
    }

    if (periodManager.isOverlapped(cmts, currentPeriods)) {
      flash.error("Il periodo %s si sovrappone con altre periodi esistenti, "
          + "impossibile aggiungerlo", cmts.getLabel());
      response.status = 400;
      log.warn("Il periodo {} si sovrappone con altre periodi esistenti, impossibile aggiungerlo", 
          cmts.getLabel());
      render("@updateContractMandatoryTimeSlot", cmts, contract);      
    }

    val periodRecaps = Lists.newArrayList(currentPeriods);
    cmts.setRecomputeFrom(cmts.getBeginDate());   
    periodRecaps.add(cmts);

    RecomputeRecap recomputeRecap =
        periodManager.buildRecap(wrappedContract.getContractDateInterval().getBegin(),
            Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
            periodRecaps, Optional.fromNullable(contract.getSourceDateResidual()));

    recomputeRecap.initMissing = wrappedContract.initializationMissing();

    if (!confirmed) {
      confirmed = true;
      render("@updateContractMandatoryTimeSlot", contract, cmts, confirmed, recomputeRecap);
    } else {
      cmts.save();
      contract = contractDao.getContractById(contract.id);
      contract.getPerson().refresh();
      if (recomputeRecap.needRecomputation) {
        contractManager.recomputeContract(contract,
            Optional.fromNullable(recomputeRecap.recomputeFrom), false, false);
      }

      flash.success(Web.msgSaved(ContractMandatoryTimeSlot.class));
      updateContractMandatoryTimeSlot(contract.id, null);
    }

  }

  /**
   * Cancella una fascia oraria di presenza obbligatoria.
   *
   * @param id id della fascia oraria da cancellare
   * @param confirmed se è confirmed lo cancella altrimenti mostra una pagina di
   *     riepilogo e conferma.
   */
  public void deleteContractMandatoryTimeSlot(Long id, boolean confirmed) {
    ContractMandatoryTimeSlot cmts = ContractMandatoryTimeSlot.findById(id);

    notFoundIfNull(cmts);

    rules.checkIfPermitted(cmts.getContract().getPerson().getOffice());

    boolean deletion = true;
    val contract = cmts.getContract();
    if (!confirmed) {
      IWrapperContract wrappedContract = wrapperFactory.create(cmts.getContract());
      val currentPeriods = cmts.getOwner().periods(cmts.getType());
      val periodRecaps = Lists.newArrayList(currentPeriods);

      periodRecaps.remove(cmts);
      RecomputeRecap recomputeRecap =
          periodManager.buildRecap(cmts.getBeginDate(),
              Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
              periodRecaps, Optional.fromNullable(contract.getSourceDateResidual()));
      recomputeRecap.recomputeFrom = cmts.getBeginDate();
      periodManager.setDays(recomputeRecap);
      confirmed = true;
      render("@updateContractMandatoryTimeSlot", cmts, contract, deletion, recomputeRecap);
    } else {
      cmts.delete();
      flash.success(Web.msgDeleted(ContractMandatoryTimeSlot.class));
    }
    updateContractMandatoryTimeSlot(cmts.getContract().id, null);

  }

  /**
   * Crud gestione periodi ferie.
   *
   * @param id contratto
   */
  public static void updateContractVacationPeriod(Long id) {

    Contract contract = contractDao.getContractById(id);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.getPerson().getOffice());

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    VacationPeriod vp = new VacationPeriod();
    vp.setContract(contract);

    render(wrappedContract, contract, vp);
  }

  /**
   * Salva il nuovo periodo ferie.
   *
   * @param vp nuovo periodo ferie
   * @param confirmed se conferma ricevuta.
   */
  public static void saveContractVacationPeriod(@Valid VacationPeriod vp, boolean confirmed) {

    notFoundIfNull(vp);
    notFoundIfNull(vp.getContract());

    rules.checkIfPermitted(vp.getContract().getPerson().getOffice());

    IWrapperContract wrappedContract = wrapperFactory.create(vp.getContract());
    Contract contract = vp.getContract();

    if (!Validation.hasErrors()) {
      if (!DateUtility.isDateIntoInterval(vp.getBeginDate(),
          wrappedContract.getContractDateInterval())) {
        Validation.addError("vp.beginDate", "deve appartenere al contratto");
      }
      if (vp.getEndDate() != null && !DateUtility.isDateIntoInterval(vp.getEndDate(),
          wrappedContract.getContractDateInterval())) {
        Validation.addError("vp.endDate", "deve appartenere al contratto");
      }
    }

    if (Validation.hasErrors()) {
      response.status = 400;

      render("@updateContractVacationPeriod", vp, contract);
    }

    //riepilogo delle modifiche
    List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(vp, false);
    RecomputeRecap recomputeRecap =
        periodManager.buildRecap(wrappedContract.getContractDateInterval().getBegin(),
            Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
            periodRecaps, Optional.fromNullable(contract.getSourceDateResidual()));

    recomputeRecap.initMissing = wrappedContract.initializationMissing();

    if (!confirmed) {
      confirmed = true;
      render("@updateContractVacationPeriod", contract, vp, confirmed, recomputeRecap);
    } else {

      periodManager.updatePeriods(vp, true);
      contract = contractDao.getContractById(contract.id);
      contract.getPerson().refresh();
      if (recomputeRecap.needRecomputation) {
        contractManager.recomputeContract(contract,
            Optional.fromNullable(recomputeRecap.recomputeFrom), false, false);
      }

      flash.success(Web.msgSaved(VacationPeriod.class));

      updateContractVacationPeriod(contract.id);
    }

  }

  /**
   * Crud gestione periodi presenza automatica.
   *
   * @param id contratto
   */
  public static void updateContractStampProfile(Long id) {

    Contract contract = contractDao.getContractById(id);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.getPerson().getOffice());

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    ContractStampProfile csp = new ContractStampProfile();
    csp.setContract(contract);

    render(wrappedContract, contract, csp);
  }

  /**
   * Salva il nuovo periodo presenza automatica.
   *
   * @param csp       nuovo periodo di presenza automatica.
   * @param confirmed se conferma ricevuta.
   */
  public static void saveContractStampProfile(@Valid ContractStampProfile csp, boolean confirmed) {

    notFoundIfNull(csp);
    notFoundIfNull(csp.getContract());

    rules.checkIfPermitted(csp.getContract().getPerson().getOffice());

    IWrapperContract wrappedContract = wrapperFactory.create(csp.getContract());
    Contract contract = csp.getContract();

    if (!Validation.hasErrors()) {
      if (!DateUtility.isDateIntoInterval(csp.getBeginDate(),
          wrappedContract.getContractDateInterval())) {
        Validation.addError("csp.beginDate", "deve appartenere al contratto");
      }
      if (csp.getEndDate() != null && !DateUtility.isDateIntoInterval(csp.getEndDate(),
          wrappedContract.getContractDateInterval())) {
        Validation.addError("csp.endDate", "deve appartenere al contratto");
      }
    }

    if (Validation.hasErrors()) {
      response.status = 400;

      render("@updateContractStampProfile", csp, contract);
    }

    //riepilogo delle modifiche
    List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(csp, false);
    RecomputeRecap recomputeRecap =
        periodManager.buildRecap(wrappedContract.getContractDateInterval().getBegin(),
            Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
            periodRecaps, Optional.fromNullable(contract.getSourceDateResidual()));

    recomputeRecap.initMissing = wrappedContract.initializationMissing();

    if (!confirmed) {
      confirmed = true;
      render("@updateContractStampProfile", contract, csp, confirmed, recomputeRecap);
    } else {

      periodManager.updatePeriods(csp, true);
      contract = contractDao.getContractById(contract.id);
      contract.getPerson().refresh();
      if (recomputeRecap.needRecomputation) {
        contractManager.recomputeContract(contract,
            Optional.fromNullable(recomputeRecap.recomputeFrom), false, false);
      }

      flash.success(Web.msgSaved(ContractStampProfile.class));

      updateContractStampProfile(contract.id);
    }

  }

  /**
   * Pagina aggiornamento dati iniziali del contratto.
   */
  public static void updateSourceContract(Long contractId) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.getPerson().getOffice());

    if (contract.getSourceDateResidual() == null) {
      contractManager.cleanResidualInitialization(contract);
    }

    IWrapperOffice wrOffice = wrapperFactory.create(contract.getPerson().getOffice());
    IWrapperPerson wrPerson = wrapperFactory.create(contract.getPerson());

    Integer hoursCurrentYear = 0;
    Integer minutesCurrentYear = 0;
    if (contract.getSourceRemainingMinutesCurrentYear() != null) {
      hoursCurrentYear = contract.getSourceRemainingMinutesCurrentYear() / 60;
      minutesCurrentYear = contract.getSourceRemainingMinutesCurrentYear() % 60;
    }
    Integer hoursLastYear = 0;
    Integer minutesLastYear = 0;
    if (contract.getSourceRemainingMinutesLastYear() != null) {
      hoursLastYear = contract.getSourceRemainingMinutesLastYear() / 60;
      minutesLastYear = contract.getSourceRemainingMinutesLastYear() % 60;
    }

    IWrapperContract wrContract = wrapperFactory.create(contract);    

    render(contract, wrContract, wrOffice, wrPerson, hoursCurrentYear, minutesCurrentYear,
        hoursLastYear, minutesLastYear);
  }

  /**
   * Pagina aggiornamento dati iniziali ferie e permessi del contratto.
   */
  public static void updateSourceContractVacation(Long contractId) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.getPerson().getOffice());

    IWrapperContract wrContract = wrapperFactory.create(contract);
    if (contract.getSourceDateVacation() == null) {
      contractManager.cleanVacationInitialization(contract);
    }

    Integer sourceVacationLastYearUsed = contract.getSourceVacationCurrentYearUsed();
    Integer sourceVacationCurrentYearUsed = contract.getSourceVacationCurrentYearUsed();
    Integer sourcePermissionUsed = contract.getSourcePermissionUsed();

    IWrapperOffice wrOffice = wrapperFactory.create(contract.getPerson().getOffice());
    IWrapperPerson wrPerson = wrapperFactory.create(contract.getPerson());

    render(contract, wrContract, wrOffice, wrPerson, 
        sourceVacationLastYearUsed, sourceVacationCurrentYearUsed, sourcePermissionUsed);
  }

  /**
   * Pagina aggiornamento buoni pasto iniziali del contratto.
   */
  public static void updateSourceContractMeal(Long contractId) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.getPerson().getOffice());

    IWrapperContract wrContract = wrapperFactory.create(contract);
    if (contract.getSourceDateMealTicket() == null) {
      contractManager.cleanMealTicketInitialization(contract);
    }

    IWrapperOffice wrOffice = wrapperFactory.create(contract.getPerson().getOffice());
    IWrapperPerson wrPerson = wrapperFactory.create(contract.getPerson());

    render(contract, wrContract, wrOffice, wrPerson);
  }


  /**
   * Salva l'inizializzazione ore.
   *
   * @param contractId contract.
   * @param sourceDateResidual nuova data inizializzazione.
   * @param confirmedResidual  step di conferma ricevuta.
   */
  public static void saveResidualSourceContract(Long contractId, 
      @Valid final LocalDate sourceDateResidual, 
      Integer hoursLastYear, Integer minutesLastYear,
      Integer hoursCurrentYear, Integer minutesCurrentYear,
      boolean confirmedResidual) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.getPerson().getOffice());

    IWrapperContract wrContract = wrapperFactory.create(contract);
    IWrapperOffice wrOffice = wrapperFactory.create(contract.getPerson().getOffice());
    IWrapperPerson wrPerson = wrapperFactory.create(contract.getPerson());

    if (sourceDateResidual != null) {
      validation.future(sourceDateResidual.toDate(),
          wrContract.dateForInitialization().minusDays(1).toDate())
      .key("sourceDateResidual").message("validation.after");

      validation.past(sourceDateResidual.toDate(),
          wrContract.getContractDateInterval().getEnd().toDate())
      .key("sourceDateResidual").message("validation.before");
    }

    if (Validation.hasErrors()) {
      response.status = 400;
      render("@updateSourceContract", contract, wrContract, wrPerson, wrOffice, 
          hoursLastYear, minutesLastYear, hoursCurrentYear, minutesCurrentYear,
          sourceDateResidual);
    }

    LocalDate recomputeFrom = sourceDateResidual;
    LocalDate recomputeTo = wrContract.getContractDateInterval().getEnd();
    if (recomputeTo.isAfter(LocalDate.now())) {
      recomputeTo = LocalDate.now();
    }

    //simulazione eliminazione inizializzazione.
    if (sourceDateResidual == null && !confirmedResidual) {
      contractManager.cleanResidualInitialization(contract);
      boolean removeMandatory = contract.getBeginDate().isBefore(wrContract.dateForInitialization());
      boolean removeUnnecessary = !removeMandatory;
      if (removeUnnecessary) {
        recomputeFrom = contract.getBeginDate();
      }
      confirmedResidual = true;
      int days = 0;
      if (recomputeFrom != null) {
        days = DateUtility.daysInInterval(new DateInterval(recomputeFrom, recomputeTo));
      }
      render("@updateSourceContract", contract, wrContract, wrPerson, wrOffice,
          hoursLastYear, minutesLastYear, hoursCurrentYear, minutesCurrentYear,
          confirmedResidual, removeMandatory, removeUnnecessary,
          recomputeFrom, recomputeTo, days);
    }

    //simulazione nuova inizializzazione
    if (sourceDateResidual != null && !confirmedResidual) {

      confirmedResidual = true;
      int days = DateUtility.daysInInterval(new DateInterval(recomputeFrom, recomputeTo));
      boolean sourceNew = false;
      boolean sourceUpdate = false;
      if (contract.getSourceDateResidual() == null) {
        sourceNew = true;
      } else {
        sourceUpdate = true;
      }

      render("@updateSourceContract", contract, wrContract, wrPerson, wrOffice, sourceDateResidual,
          hoursLastYear, minutesLastYear, hoursCurrentYear, minutesCurrentYear,
          confirmedResidual, sourceNew, sourceUpdate, recomputeFrom, recomputeTo, days);
    }

    //esecuzione
    if (confirmedResidual) {
      contract.setSourceDateResidual(sourceDateResidual);
      contract.setSourceRemainingMinutesCurrentYear(hoursCurrentYear * 60 + minutesCurrentYear);
      contract.setSourceRemainingMinutesLastYear(hoursLastYear * 60 + minutesLastYear);
      contractManager.setSourceContractProperly(contract);
      contractManager.properContractUpdate(contract, recomputeFrom, false);

      flash.success("Contratto di %s inizializzato correttamente.", contract.getPerson().fullName());
    }
    initializationsStatus(contract.getPerson().getOffice().id);

  }

  /**
   * Salva l'inizializzazione ore.
   */
  public static void saveVacationSourceContract(Long contractId, 
      @Valid final LocalDate sourceDateVacation, Integer sourceVacationLastYearUsed,
      Integer sourceVacationCurrentYearUsed, Integer sourcePermissionUsed,
      boolean confirmedVacation) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.getPerson().getOffice());

    IWrapperContract wrContract = wrapperFactory.create(contract);
    IWrapperOffice wrOffice = wrapperFactory.create(contract.getPerson().getOffice());
    IWrapperPerson wrPerson = wrapperFactory.create(contract.getPerson());

    if (sourceDateVacation != null) {
      validation.future(sourceDateVacation.toDate(),
          wrContract.dateForInitialization().minusDays(1).toDate())
      .key("sourceDateVacation").message("validation.after");

      validation.past(sourceDateVacation.toDate(),
          wrContract.getContractDateInterval().getEnd().toDate())
      .key("sourceDateVacation").message("validation.before");
    }

    if (Validation.hasErrors()) {
      response.status = 400;
      render("@updateSourceContractVacation", contract, wrContract, wrPerson, wrOffice, 
          sourceVacationLastYearUsed, sourceVacationCurrentYearUsed, sourcePermissionUsed, 
          sourceDateVacation);
    }

    //simulazione eliminazione inizializzazione.
    if (sourceDateVacation == null && !confirmedVacation) {
      contractManager.cleanVacationInitialization(contract);
      boolean remove = true;
      confirmedVacation = true;
      render("@updateSourceContractVacation", contract, wrContract, wrPerson, wrOffice,
          sourceVacationLastYearUsed, sourceVacationCurrentYearUsed, sourcePermissionUsed, 
          remove, confirmedVacation);
    }

    //simulazione nuova inizializzazione
    if (sourceDateVacation != null && !confirmedVacation) {

      confirmedVacation = true;
      boolean sourceUpdate = false;
      if (contract.getSourceDateVacation() != null) {
        sourceUpdate = true;
      }
      render("@updateSourceContractVacation", contract, wrContract, wrPerson, wrOffice, 
          sourceDateVacation,
          sourceVacationLastYearUsed, sourceVacationCurrentYearUsed, sourcePermissionUsed, 
          confirmedVacation, sourceUpdate);
    }

    //esecuzione
    if (confirmedVacation) {
      contract.setSourceDateVacation(sourceDateVacation);
      contract.setSourceVacationLastYearUsed(sourceVacationLastYearUsed);
      contract.setSourceVacationCurrentYearUsed(sourceVacationCurrentYearUsed);
      contract.setSourcePermissionUsed(sourcePermissionUsed);

      contractManager.setSourceContractProperly(contract);

      absenceService.emptyVacationCache(contract);

      flash.success("Contratto di %s inizializzato correttamente.", contract.getPerson().fullName());
    }
    initializationsVacation(contract.getPerson().getOffice().id);

  }

  /**
   * Salva l'inizializzazione buoni pasto.
   *
   * @param contract             contract.
   * @param sourceDateMealTicket nuova data inizializzazione.
   * @param confirmedMeal        step di conferma ricevuta.
   */
  public static void saveMealTicketSourceContract(@Valid final Contract contract,
      @Valid final LocalDate sourceDateMealTicket,
      boolean confirmedMeal) {

    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.getPerson().getOffice());

    IWrapperContract wrContract = wrapperFactory.create(contract);
    IWrapperOffice wrOffice = wrapperFactory.create(contract.getPerson().getOffice());
    IWrapperPerson wrPerson = wrapperFactory.create(contract.getPerson());

    // TODO: per adesso per definire il residuo dei buoni devo avere l'inizializazione generale
    // quando richiesta.
    Preconditions.checkState(!wrContract.initializationMissing());

    log.debug("saveMealTicketSourceContract, contratto inizializzato {}", 
        wrContract.initializationMissing());
    if (sourceDateMealTicket != null) {
      validation.future(sourceDateMealTicket.toDate(), 
          wrContract.dateForMealInitialization().minusDays(1).toDate())
      .key("sourceDateMealTicket").message("validation.after");
    }

    if (Validation.hasErrors()) {
      response.status = 400;
      render("@updateSourceContractMeal", contract, wrContract, wrPerson, wrOffice,
          sourceDateMealTicket);
    }

    LocalDate recomputeFrom = sourceDateMealTicket;
    if (recomputeFrom == null) {
      //generale
      if (contract.getSourceDateResidual() != null) {
        recomputeFrom = contract.getSourceDateResidual();
      } else {
        recomputeFrom = contract.getBeginDate();
      }
    }
    LocalDate recomputeTo = wrContract.getContractDateInterval().getEnd();
    if (recomputeTo.isAfter(LocalDate.now())) {
      recomputeTo = LocalDate.now();
    }
    if (!confirmedMeal) {
      confirmedMeal = true;
      int months = DateUtility.monthsInInterval(new DateInterval(recomputeFrom, recomputeTo));
      boolean sourceNew = false;
      boolean sourceUpdate = false;
      if (contract.getSourceDateResidual() == null) {
        sourceNew = true;
      } else {
        sourceUpdate = true;
      }

      render("@updateSourceContractMeal", contract, wrContract, wrPerson, wrOffice,
          sourceDateMealTicket, confirmedMeal,
          sourceNew, sourceUpdate, recomputeFrom, recomputeTo, months);

    }

    // Conferma ricevuta
    contract.setSourceDateMealTicket(sourceDateMealTicket);
    contractManager.setSourceContractProperly(contract);
    contractManager.properContractUpdate(contract, recomputeFrom, true);

    flash.success("Buoni pasto di %s inizializzati correttamente.", contract.getPerson().fullName());

    initializationsMeal(contract.getPerson().getOffice().id);
  }

  /**
   * Gestore delle inizializzazioni ore della sede.
   */
  public static void initializationsStatus(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    List<IWrapperContract> initializationsMissing = Lists.newArrayList();
    List<IWrapperContract> correctInitialized = Lists.newArrayList();
    List<IWrapperContract> correctNotInitialized = Lists.newArrayList();

    //Tutti i dipendenti sotto forma di wrapperPerson
    for (IWrapperPerson wrPerson : FluentIterable.from(personDao.listFetched(Optional.absent(),
        ImmutableSet.of(office), false, null, null, false).list())
        .transform(wrapperFunctionFactory.person()).toList()) {

      if (!wrPerson.getCurrentContract().isPresent()) {
        continue;
      }

      if (wrPerson.currentContractInitializationMissing()) {
        initializationsMissing.add(wrapperFactory.create(wrPerson.getCurrentContract().get()));
      } else {
        if (wrPerson.getCurrentContract().get().getSourceDateResidual() != null) {
          correctInitialized.add(wrapperFactory.create(wrPerson.getCurrentContract().get()));
        } else {
          correctNotInitialized.add(wrapperFactory.create(wrPerson.getCurrentContract().get()));
        }
      }

    }

    render(initializationsMissing, correctInitialized, correctNotInitialized, office);
  }

  /**
   * Gestore delle inizializzazioni ferie della sede.
   */
  public static void initializationsVacation(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    List<IWrapperContract> correctInitialized = Lists.newArrayList();
    List<IWrapperContract> correctNotInitialized = Lists.newArrayList();

    //Tutti i dipendenti sotto forma di wrapperPerson
    for (IWrapperPerson wrPerson : FluentIterable.from(personDao.listFetched(Optional.absent(),
        ImmutableSet.of(office), false, null, null, false).list())
        .transform(wrapperFunctionFactory.person()).toList()) {

      if (!wrPerson.getCurrentContract().isPresent()) {
        continue;
      }

      if (wrPerson.getCurrentContract().get().getSourceDateVacation() != null) {
        correctInitialized.add(wrapperFactory.create(wrPerson.getCurrentContract().get()));
      } else {
        correctNotInitialized.add(wrapperFactory.create(wrPerson.getCurrentContract().get()));
      }
    }

    render(correctInitialized, correctNotInitialized, office);
  }

  /**
   * Gestore delle inizializzazioni buoni pasto della sede.
   */
  public static void initializationsMeal(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    List<IWrapperContract> initializationsBeforeGeneral = Lists.newArrayList();
    List<IWrapperContract> correctInitialized = Lists.newArrayList();
    List<IWrapperContract> correctNotInitialized = Lists.newArrayList();

    //Tutti i dipendenti sotto forma di wrapperPerson TODO: rifattorizzare
    for (IWrapperPerson wrPerson : FluentIterable.from(personDao.listFetched(Optional.absent(),
        ImmutableSet.of(office), false, null, null, false).list())
        .transform(wrapperFunctionFactory.person()).toList()) {

      if (!wrPerson.getCurrentContract().isPresent()) {
        continue;
      }

      if (wrPerson.currentContractInitializationMissing()) {
        //initializationsMissing.add(wrapperFactory.create(wrPerson.getCurrentContract().get()));
      } else {
        IWrapperContract wrContract = wrapperFactory.create(wrPerson.getCurrentContract().get());
        if (wrContract.mealTicketInitBeforeGeneralInit()) {
          initializationsBeforeGeneral.add(wrContract);
        } else if (wrContract.getValue().getSourceDateMealTicket() != null) {
          correctInitialized.add(wrContract);
        } else {
          correctNotInitialized.add(wrContract);
        }
      }

    }

    render(initializationsBeforeGeneral, correctInitialized, correctNotInitialized, office);

  }

}