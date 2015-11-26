package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;

import dao.ContractDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import helpers.Web;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.extern.slf4j.Slf4j;

import manager.ContractManager;

import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Person;
import models.WorkingTimeType;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;

import javax.inject.Inject;


@Slf4j
@With({Resecure.class, RequestInit.class})
public class Contracts extends Controller {
  
  private final static DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/YYY");

  @Inject
  private static PersonDao personDao;
  @Inject
  private static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  private static ContractManager contractManager;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static ContractDao contractDao;
  @Inject
  private static IWrapperFactory wrapperFactory;

  /**
   * I contratti del dipendente.
   * @param personId personId
   */
  public static void personContracts(final Long personId) {

    flash.keep();

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);

    List<IWrapperContract> contractList = 
        FluentIterable.from(contractDao.getPersonContractList(person))
            .transform(wrapperFunctionFactory.contract()).toList();

    List<ContractStampProfile> contractStampProfileList = contractDao
        .getPersonContractStampProfile(Optional.fromNullable(person), Optional.<Contract>absent());


    render(person, contractList, contractStampProfileList);
  }
  
  /**
   * Edit date e on certificate contratto.
   * @param contractId contractId
   */
  public static void edit(Long contractId) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);
    Person person = contract.person;

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    LocalDate beginContract = contract.beginContract;
    LocalDate expireContract = contract.expireContract;
    LocalDate endContract = contract.endContract;
    boolean onCertificate = contract.onCertificate;

    render(person, contract, wrappedContract, beginContract, expireContract, endContract,
        onCertificate);
  }

  /**
   * Aggiornamento date e on certificate contratto.
   * @param contract contract
   * @param beginContract inizio
   * @param expireContract scadenza
   * @param endContract terminazione
   * @param onCertificate in attestati
   * @param confirmed step di conferma
   */
  public static void update(@Valid Contract contract, @Required LocalDate beginContract,
      @Valid LocalDate expireContract, @Valid LocalDate endContract, boolean onCertificate,
      boolean confirmed) {

    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    if (!validation.hasErrors()) {
      if (contract.sourceDateResidual != null
          && contract.sourceDateResidual.isBefore(beginContract)) {
        validation.addError("beginContract",
            "non può essere successiva alla data di inizializzazione");
      }
      if (expireContract != null && expireContract.isBefore(beginContract)) {
        validation.addError("expireContract", "non può precedere l'inizio del contratto.");
      }
      if (endContract != null && endContract.isBefore(beginContract)) {
        validation.addError("endContract", "non può precedere l'inizio del contratto.");
      }
      if (expireContract != null && endContract != null && !endContract.isBefore(beginContract)) {
        validation.addError("endContract", "non può essere successivo alla scadenza del contratto");
      }
    }

    if (validation.hasErrors()) {

      response.status = 400;
      flash.error(Web.msgHasErrors());

      log.warn("validation errors: {}", validation.errorsMap());

      render("@edit", contract, wrappedContract, beginContract, expireContract, endContract,
          onCertificate);
    }

    // Salvo la situazione precedente
    DateInterval previousInterval = wrappedContract.getContractDatabaseInterval();

    // Attribuisco il nuovo stato al contratto per effettuare il controllo incrociato
    contract.beginContract = beginContract;
    contract.expireContract = expireContract;
    contract.endContract = endContract;
    contract.onCertificate = onCertificate;
    if (!contractManager.isProperContract(contract)) {
      validation.addError("contract.crossValidationFailed",
          "Il contratto non può intersecarsi" + " con altri contratti del dipendente.");
      render("@edit", contract, wrappedContract, beginContract, expireContract, endContract,
          onCertificate);
    }

    //riepilogo delle modifiche
    boolean initMissing = false;
    boolean changeBegin = false;
    boolean reduceEnd = false;          //onlyRecap
    boolean incrementEndInPast = false;
    LocalDate recomputeFrom = null;
    LocalDate recomputeTo = null;

    DateInterval newInterval = wrappedContract.getContractDatabaseInterval();
    if (!newInterval.getBegin().isEqual(previousInterval.getBegin())) {
      changeBegin = true;
      if (newInterval.getBegin().isBefore(LocalDate.now())) {
        recomputeFrom = newInterval.getBegin();
      }
    }
    if (recomputeFrom == null) {
      if (!newInterval.getEnd().isEqual(previousInterval.getEnd())) {
        // scorcio allora solo riepiloghi
        if (newInterval.getEnd().isBefore(previousInterval.getEnd())) {
          reduceEnd = true;
          recomputeFrom = newInterval.getEnd();
        }
        // allungo ma se inglobo passato allora ricalcolo
        if (newInterval.getEnd().isAfter(previousInterval.getEnd())
            && previousInterval.getEnd().isBefore(LocalDate.now())) {
          incrementEndInPast = true;
          recomputeFrom = previousInterval.getEnd();
        }
      }
    }
    if (recomputeFrom != null) {
      recomputeTo = newInterval.getEnd();
      if (!recomputeTo.isBefore(LocalDate.now())) {
        recomputeTo = LocalDate.now();
      }
    }
    if (wrappedContract.initializationMissing()) {
      initMissing = true;
      recomputeFrom = null;
    }

    //conferma 
    if (!confirmed) {
      confirmed = true;
      int days = 0;
      if (recomputeFrom != null) {
        days = DateUtility.daysInInterval(new DateInterval(recomputeFrom, recomputeTo));
      }
      response.status = 400;
      render("@edit", contract, wrappedContract, beginContract, expireContract, endContract,
          onCertificate, changeBegin, reduceEnd, incrementEndInPast, initMissing, 
          recomputeFrom, recomputeTo, days);
    } else {
      if (recomputeFrom != null) {  
        contractManager.properContractUpdate(contract, recomputeFrom, false);
      }
      boolean success = true;
      confirmed = false;
      Person person = contract.person;
      render("@edit", person, contract, wrappedContract, beginContract, expireContract, endContract,
          onCertificate, success);
    }

  }

  /**
   * Nuovo contratto.
   * @param person person
   */
  public static void insert(Person person) {

    notFoundIfNull(person);

    rules.checkIfPermitted(person.office);

    Contract contract = new Contract();
    contract.person = person;

    render(contract);
  }

  /**
   * Salva nuovo contratto.
   * @param contract contract
   * @param wtt il tipo orario
   */
  public static void save(@Valid Contract contract, @Valid WorkingTimeType wtt) {

    notFoundIfNull(contract);
    notFoundIfNull(wtt);

    rules.checkIfPermitted(contract.person.office);


    if (!validation.hasErrors()) {

      if (contract.expireContract != null
          && contract.expireContract.isBefore(contract.beginContract)) {
        validation.addError("contract.expireContract", "non può precedere l'inizio del contratto.");

      } else if (!contractManager.properContractCreate(contract, wtt)) {
        validation.addError("contract.beginContract", "i contratti non possono sovrapporsi.");
        if (contract.expireContract != null) {
          validation.addError("contract.expireContract", "i contratti non possono sovrapporsi.");
        }
      }

    }

    if (validation.hasErrors()) {

      response.status = 400;
      flash.error(Web.msgHasErrors());

      log.warn("validation errors: {}", validation.errorsMap());

      render("@insert", contract, wtt);
    }

    flash.success("Contratto inserito correttamente");

    Contracts.personContracts(contract.person.id);
  }

  /**
   * Rimozione contratto.
   * 
   * @param contractId contractId
   */
  public static void delete(Long contractId) {

    Contract contract = contractDao.getContractById(contractId);

    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    for (ContractStampProfile csp : contract.contractStampProfile) {
      csp.delete();
    }

    contract.delete();

    flash.error("Contratto eliminato con successo.");
    edit(contract.person.id);
  }


  public static void updateContractWorkingTimeType(Long id) {

    Contract contract = contractDao.getContractById(id);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrappedContract = wrapperFactory.create(contract);
    
    ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
    cwtt.contract = contract;

    render(wrappedContract, contract, cwtt);
  }

  public static void saveContractWorkingTimeType(@Valid ContractWorkingTimeType cwtt, 
      boolean confirmed) {
    
    notFoundIfNull(cwtt);
    notFoundIfNull(cwtt.contract);
    
    rules.checkIfPermitted(cwtt.contract.person.office);
       
    IWrapperContract wrappedContract = wrapperFactory.create(cwtt.contract);
    Contract contract = cwtt.contract;
    
    if (!validation.hasErrors()) {
      if (!DateUtility.isDateIntoInterval(cwtt.beginDate, wrappedContract.getContractDateInterval())) {
        validation.addError("cwtt.beginDate", "deve appartenere al contratto");
      }
      if (cwtt.endDate != null && 
          !DateUtility.isDateIntoInterval(cwtt.endDate, wrappedContract.getContractDateInterval())) {
        validation.addError("cwtt.endDate", "deve appartenere al contratto");
      }
    }
    
    if (validation.hasErrors()) {
      response.status = 400;
      flash.error(Web.msgHasErrors());

      log.warn("validation errors: {}", validation.errorsMap());

      render("@updateContractWorkingTimeType", cwtt, contract);
    }
    
    rules.checkIfPermitted(cwtt.workingTimeType.office);
    
    //riepilogo delle modifiche
    List<ContractWorkingTimeType> cwttRecaps = contractManager.changedRecap(contract, cwtt, false);
    LocalDate recomputeFrom = null;
    LocalDate recomputeTo = wrappedContract.getContractDateInterval().getEnd();
    for (ContractWorkingTimeType item : cwttRecaps) {
      if (item.recomputeFrom != null && item.recomputeFrom.isBefore(LocalDate.now())) {
        recomputeFrom = item.recomputeFrom;
      }
    }
    if (recomputeFrom != null) {
      if (recomputeTo.isAfter(LocalDate.now())) {
        recomputeTo = LocalDate.now();
      }
      if (recomputeFrom.isBefore(wrappedContract.getContractDatabaseInterval().getBegin())) {
        recomputeFrom = wrappedContract.getContractDatabaseInterval().getBegin();
      }
    }
    
    if (!confirmed) {
      confirmed = true;
      int days = 0;
      if (recomputeFrom != null) {
        days = DateUtility.daysInInterval(new DateInterval(recomputeFrom, recomputeTo));
      }
      render("@updateContractWorkingTimeType", contract, cwtt, cwttRecaps, confirmed, 
          recomputeFrom, recomputeTo, days);
    } else {
      
      contractManager.changedRecap(contract, cwtt, true);
      contract = contractDao.getContractById(contract.id);
      contract.person.refresh();
      if (recomputeFrom != null) {
        contractManager.recomputeContract(contract, 
            Optional.fromNullable(recomputeFrom), false, false);
      }
      //todo il messaggio di conferma nel flash.
      updateContractWorkingTimeType(contract.id);
    }

  }

  /**
   * Pagina aggiornamento dati iniziali del contratto.
   * 
   * @param contractId
   */
  public static void updateSourceContract(Long contractId) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wContract = wrapperFactory.create(contract);
    if (contract.sourceDateResidual == null) {
      contractManager.cleanResidualInitialization(contract);
    }
    if (contract.sourceDateMealTicket == null) {
      contractManager.cleanMealTicketInitialization(contract);
    }

    IWrapperOffice wOffice = wrapperFactory.create(contract.person.office);
    IWrapperPerson wPerson = wrapperFactory.create(contract.person);

    render(contract, wContract, wOffice, wPerson);
  }

  /**
   * Salva l'inizializzazione. 
   * @param contract contract
   * @param sourceDateResidual nuova data inizializzazione
   * @param confirmed step di conferma ricevuta
   */
  public static void saveResidualSourceContract(@Valid final Contract contract,
      @Valid final LocalDate sourceDateResidual, boolean confirmed) {

    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wContract = wrapperFactory.create(contract);
    IWrapperOffice wOffice = wrapperFactory.create(contract.person.office);
    IWrapperPerson wPerson = wrapperFactory.create(contract.person);

    if (!validation.hasErrors()) {
      if (sourceDateResidual != null && 
          sourceDateResidual.isBefore(wContract.dateForInitialization())) {
        validation.addError("sourceDateResidual",
            "deve essere uguale o successiva a " + wContract.dateForInitialization().toString(dtf));
      }
      if (sourceDateResidual != null &&
          sourceDateResidual.isAfter(wContract.getContractDateInterval().getEnd())) {
        validation.addError("sourceDateResidual",
            "deve essere precedente o uguale alla fine del contratto");
      }
      if (sourceDateResidual == null && contract.sourceDateResidual == null) {
        validation.addError("sourceDateResidual",
            "per definire l'inizializzazione è obbligatorio questo campo");
      }
    }

    if (validation.hasErrors()) {
      response.status = 400;
      flash.error(Web.msgHasErrors());

      log.warn("validation errors: {}", validation.errorsMap());

      render("@updateSourceContract", contract, wContract, wPerson, wOffice, sourceDateResidual);
    }
    
    LocalDate recomputeFrom = sourceDateResidual;
    LocalDate recomputeTo = wContract.getContractDateInterval().getEnd();
    if (recomputeTo.isAfter(LocalDate.now())) {
      recomputeTo = LocalDate.now();
    }
    //eliminazione inizializzazione.
    if (sourceDateResidual == null) {
      contractManager.cleanResidualInitialization(contract);
      boolean removeMandatory = false;
      boolean removeUnnecessary = false;
      if (contract.beginContract.isBefore(wContract.dateForInitialization())) {
        removeMandatory = true;
      } else {
        removeUnnecessary = true;
        recomputeFrom = contract.beginContract;
      }
      if (!confirmed) {
        confirmed = true;
        int days = 0;
        if (recomputeFrom != null) {
          days = DateUtility.daysInInterval(new DateInterval(recomputeFrom, recomputeTo));
        }
        render("@updateSourceContract", contract, wContract, wPerson, wOffice,
          confirmed, removeMandatory, removeUnnecessary, recomputeFrom, recomputeTo, days);
      } else {
        //calcoli
      }
    }

    //configurazione inizializzazione
    if (!confirmed) {
      confirmed = true;
      int days = DateUtility.daysInInterval(new DateInterval(recomputeFrom, recomputeTo));
      boolean sourceNew = false;
      boolean sourceUpdate = false;
      if (contract.sourceDateResidual == null) {
        sourceNew = true;
      } else {
        sourceUpdate = true;
      }
      
      render("@updateSourceContract", contract, wContract, wPerson, wOffice, sourceDateResidual,
          confirmed, sourceNew, sourceUpdate, recomputeFrom, recomputeTo, days);
    
    } else {
      contract.sourceDateResidual = sourceDateResidual;
      contractManager.setSourceContractProperly(contract);
      contractManager.properContractUpdate(contract, recomputeFrom, false);
      boolean success = true;
      confirmed = false;
      render("@updateSourceContract", sourceDateResidual, contract, confirmed, 
          success, wContract, wOffice, wPerson);
    }

  }

  /**
   * Salva l'inizializzazione. 
   * @param contract contract
   * @param sourceDateMealTicket nuova data inizializzazione
   * @param confirmed step di conferma ricevuta
   */
  public static void saveMealTicketSourceContract(@Valid final Contract contract,
      @Valid @Required final LocalDate sourceDateMealTicket, boolean confirmed) {

    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wContract = wrapperFactory.create(contract);
    IWrapperOffice wOffice = wrapperFactory.create(contract.person.office);
    IWrapperPerson wPerson = wrapperFactory.create(contract.person);

    // TODO: per adesso per definire il residuo dei buoni devo avere l'inizializazione generale
    // quando richiesta.
    Preconditions.checkState(!wContract.initializationMissing());
    if (!validation.hasErrors()) {
      if (sourceDateMealTicket.isBefore(wContract.dateForInitialization())) {
        validation.addError("sourceDateResidual",
            "deve essere uguale o successiva a " + wContract.dateForInitialization().toString(dtf));
      }
    }

    if (validation.hasErrors()) {
      response.status = 400;
      flash.error(Web.msgHasErrors());

      log.warn("validation errors: {}", validation.errorsMap());

      render("@updateSourceContract", contract, wContract, wPerson, wOffice, sourceDateMealTicket);
    }

    LocalDate recomputeFrom = sourceDateMealTicket;
    LocalDate recomputeTo = wContract.getContractDateInterval().getEnd();
    if (recomputeTo.isAfter(LocalDate.now())) {
      recomputeTo = LocalDate.now();
    }
    if (!confirmed) {
      confirmed = true;
      int months = DateUtility.monthsInInterval(new DateInterval(recomputeFrom, recomputeTo));
      boolean sourceNew = false;
      boolean sourceUpdate = false;
      if (contract.sourceDateResidual == null) {
        sourceNew = true;
      } else {
        sourceUpdate = true;
      }
      
      render("@updateSourceContract", contract, wContract, wPerson, wOffice, sourceDateMealTicket,
          confirmed, sourceNew, sourceUpdate, recomputeFrom, recomputeTo, months);
    
    } 
        
    // Conferma ricevuta
    contract.sourceDateMealTicket = sourceDateMealTicket;
    contractManager.setSourceContractProperly(contract);
    contractManager.properContractUpdate(contract, recomputeFrom, true);
    boolean success = true;
    confirmed = false;
    render("@updateSourceContract", sourceDateMealTicket, contract, confirmed, 
        success, wContract, wOffice, wPerson);
  }

  
}
