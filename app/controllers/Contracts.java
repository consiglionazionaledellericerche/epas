package controllers;

import helpers.Web;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import manager.ConfGeneralManager;
import manager.ConsistencyManager;
import manager.ContractManager;
import manager.ContractStampProfileManager;
import manager.ContractWorkingTimeTypeManager;
import manager.EmailManager;
import manager.OfficeManager;
import manager.PersonManager;
import manager.SecureManager;
import manager.UserManager;
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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import dao.ContractDao;
import dao.PersonChildrenDao;
import dao.PersonDao;
import dao.UserDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;


@Slf4j
@With({Resecure.class, RequestInit.class})
public class Contracts extends Controller {
  
  final static DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/YYY");

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
  @Inject
  private static ContractWorkingTimeTypeManager contractWorkingTimeTypeManager;

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
   * Edit contratto.
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
   * Aggiornamento contratto.
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

    // Salvo la situazione precedente
    DateInterval previousInterval = wrappedContract.getContractDatabaseInterval();

    // Se non ho avuto conferma la data da cui ricalcolare
    boolean changeBegin = false;
    boolean changeEnd = false;
    boolean onlyRecap = false;
    LocalDate recomputeFrom = null;
    if (!confirmed) {
      DateInterval newInterval = wrappedContract.getContractDatabaseInterval();
      if (!newInterval.getBegin().isEqual(previousInterval.getBegin())) {
        changeBegin = true;
        if (newInterval.getBegin().isBefore(LocalDate.now())) {
          recomputeFrom = newInterval.getBegin();
        }
      }
      if (recomputeFrom == null) {
        if (!newInterval.getEnd().isEqual(previousInterval.getEnd())) {
          changeEnd = true;
          // scorcio allora solo riepiloghi
          if (newInterval.getEnd().isBefore(previousInterval.getEnd())) {
            onlyRecap = true;
            recomputeFrom = newInterval.getEnd();
          }
          // allungo ma se inglobo passato allora ricalcolo
          if (newInterval.getEnd().isAfter(previousInterval.getEnd())
              && previousInterval.getEnd().isBefore(LocalDate.now())) {
            recomputeFrom = previousInterval.getEnd();
          }
        }
      }
      if (recomputeFrom != null) {

        LocalDate recomputeTo = newInterval.getEnd();
        if (!recomputeTo.isBefore(LocalDate.now())) {
          recomputeTo = LocalDate.now();
        }

        response.status = 400;
        render("@edit", contract, wrappedContract, beginContract, expireContract, endContract,
            onCertificate, changeBegin, changeEnd, recomputeFrom, recomputeTo, onlyRecap);
      }
    }

    // Conferma ricevuta
    if (recomputeFrom != null) {
      contractManager.properContractUpdate(contract, recomputeFrom, false);
    }

    contract.save();

    flash.success("Aggiornato contratto per il dipendente %s %s", contract.person.name,
        contract.person.surname);

    Contracts.edit(contract.id);

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
   * @param contractId contractId
   */
  public static void delete(Long contractId) {

    Contract contract = contractDao.getContractById(contractId);

    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    render(contract);
  }

  public static void deleteConfirmed(Long contractId) {

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

    render(wrappedContract, contract);
  }

  /**
   * Divide il periodo tipo orario in due periodi.
   * 
   * @param cwtt
   * @param splitDate
   */
  public static void splitContractWorkingTimeType(@Valid ContractWorkingTimeType cwtt,
      @Required LocalDate splitDate) {

    notFoundIfNull(cwtt);

    rules.checkIfPermitted(cwtt.contract.person.office);

    Contract contract = cwtt.contract;
    IWrapperContract wrappedContract = wrapperFactory.create(cwtt.contract);

    if (!validation.hasErrors()) {

      // errori particolari

      if (!DateUtility.isDateIntoInterval(splitDate,
          new DateInterval(cwtt.beginDate, cwtt.endDate))) {
        validation.addError("splitDate", "riechiesta entro l'intervallo");
      } else if (splitDate.isEqual(cwtt.beginDate)) {
        validation.addError("splitDate", "non può essere il primo giorno dell'intervallo");
      }
    }

    if (validation.hasErrors()) {

      response.status = 400;
      flash.error(Web.msgHasErrors());

      log.warn("validation errors: {}", validation.errorsMap());

      render("@updateContractWorkingTimeType", cwtt, wrappedContract, contract, splitDate);
    }

    // agire
    contractWorkingTimeTypeManager.saveSplitContractWorkingTimeType(cwtt, splitDate);

    flash.success("Operazione eseguita.");

    personContracts(contract.person.id);
  }

  /**
   * Elimina il periodo tipo orario. Non può essere rimosso il primo tipo orario.
   * 
   * @param cwtt periodo tipo orario da rimuovere
   */
  public static void deleteContractWorkingTimeType(@Valid ContractWorkingTimeType cwtt) {

    notFoundIfNull(cwtt);

    rules.checkIfPermitted(cwtt.contract.person.office);

    Contract contract = cwtt.contract;

    IWrapperContract wrappedContract = wrapperFactory.create(cwtt.contract);

    if (validation.hasErrors()) {

      response.status = 400;
      flash.error(Web.msgHasErrors());

      log.warn("validation errors: {}", validation.errorsMap());

      render("@updateContractWorkingTimeType", cwtt, wrappedContract, contract);
    }

    List<ContractWorkingTimeType> contractsWtt =
        Lists.newArrayList(contract.contractWorkingTimeType);

    Collections.sort(contractsWtt);
    int index = contractsWtt.indexOf(cwtt);
    Preconditions.checkState(index > 0);

    ContractWorkingTimeType previous = contractsWtt.get(index - 1);
    contractWorkingTimeTypeManager.deleteContractWorkingTimeType(contract, cwtt, previous);

    // Ricalcolo a partire dall'inizio del periodo che ho eliminato.
    contractManager.recomputeContract(cwtt.contract, 
        Optional.fromNullable(cwtt.beginDate), false, false);

    flash.success("Operazione eseguita.");

    personContracts(contract.person.id);
  }

  public static void changeTypeOfContractWorkingTimeType(ContractWorkingTimeType cwtt,
      WorkingTimeType newWtt) {

    notFoundIfNull(cwtt);
    notFoundIfNull(newWtt);

    rules.checkIfPermitted(cwtt.contract.person.office);
    rules.checkIfPermitted(newWtt.office);

    Contract contract = cwtt.contract;

    cwtt.workingTimeType = newWtt;
    cwtt.save();

    // Ricalcolo valori
    contractManager.recomputeContract(cwtt.contract, 
        Optional.fromNullable(cwtt.beginDate), false, false);

    flash.success("Operazione eseguita.");

    personContracts(contract.person.id);

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

  // public static void approveAutomatedSource(Long contractId) {
  //
  // Contract contract = contractDao.getContractById(contractId);
  //
  // notFoundIfNull(contract);
  //
  // rules.checkIfPermitted(contract.person.office);
  //
  // contract.sourceByAdmin = true;
  // contract.save();
  //
  // flash.success("Operazione conclusa con successo.");
  //
  // //list(null);
  //
  // }


  /**
   * Salva l'inizializzazione. 
   * @param contract contract
   * @param sourceDateResidual nuova data inizializzazione
   * @param confirmed step di conferma ricevuta
   */
  public static void saveResidualSourceContract(@Valid final Contract contract,
      @Valid @Required final LocalDate sourceDateResidual, boolean confirmed) {

    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wContract = wrapperFactory.create(contract);
    IWrapperOffice wOffice = wrapperFactory.create(contract.person.office);
    IWrapperPerson wPerson = wrapperFactory.create(contract.person);

    if (!validation.hasErrors()) {
      if (sourceDateResidual.isBefore(wContract.dateForInitialization())) {
        validation.addError("sourceDateResidual",
            "deve essere uguale o successiva a " + wContract.dateForInitialization().toString(dtf));
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
    
    } 
        
    // Conferma ricevuta
    contract.sourceDateResidual = sourceDateResidual;
    contractManager.setSourceContractProperly(contract);
    contractManager.properContractUpdate(contract, recomputeFrom, false);
    boolean success = true;
    confirmed = false;
    render("@updateSourceContract", sourceDateResidual, contract, confirmed, 
        success, wContract, wOffice, wPerson);
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
