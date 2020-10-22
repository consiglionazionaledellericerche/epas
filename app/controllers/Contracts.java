package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;
import helpers.Web;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
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
import models.VacationPeriod;
import models.WorkingTimeType;
import models.absences.Absence;
import models.base.IPropertyInPeriod;
import org.joda.time.LocalDate;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

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

  /**
   * I contratti del dipendente.
   *
   * @param personId personId
   */
  public static void personContracts(final Long personId) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    IWrapperPerson wrPerson = wrapperFactory.create(person);
    rules.checkIfPermitted(person.office);

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
    rules.checkIfPermitted(contract.person.office);
    Person person = contract.person;

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    LocalDate beginDate = contract.beginDate;
    LocalDate endDate = contract.endDate;
    LocalDate endContract = contract.endContract;
    boolean onCertificate = contract.onCertificate;
    boolean isTemporaryMissing = contract.isTemporaryMissing;
    boolean linkedToPreviousContract = contract.getPreviousContract() != null ? true : false;
    String perseoId = contract.perseoId;
    LocalDate sourceDateRecoveryDay = contract.sourceDateRecoveryDay;
    render(person, contract, wrappedContract, beginDate, endDate, endContract,
        onCertificate, isTemporaryMissing, perseoId, sourceDateRecoveryDay, 
        linkedToPreviousContract);
  }

  /**
   * Renderizza la pagina in cui si può provvedere allo split di un contratto.
   * @param contractId l'identificativo del contratto da splittare
   */
  public static void split(Long contractId) {
    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);

    LocalDate dateToSplit = new LocalDate();

    render(contract, dateToSplit);
  }

  /**
   * Provvede alla divisione del contratto contract in due contratti distinti, uno che termina il 
   * giorno precedente dateToSplit e l'altro che inizia a dateToSplit.
   * @param contract il contratto da splittare
   * @param dateToSplit la data a cui splittarlo
   */
  public static void splitContract(@Valid Contract contract, @Required LocalDate dateToSplit, 
      boolean attestatiSync) {
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);

    if (!DateUtility.isDateIntoInterval(dateToSplit, 
        new DateInterval(contract.beginDate, contract.calculatedEnd()))) {
      Validation.addError("dateToSplit", "La data deve appartenere al contratto!!!");
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      render("@split", contract, dateToSplit);
    }

    Optional<LocalDate> to = contract.endDate != null 
        ? Optional.fromNullable(contract.endDate) : Optional.absent();

    //1) si eliminano le assenze a partire da dateToSplit
    List<Absence> copy = Lists.newArrayList();
    List<Absence> list = contractService
        .getAbsencesInContract(contract.person, dateToSplit, to);
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
    log.debug("Rimosse {} assenze per {}", count, contract.person.getFullname());

    //2) si splitta il contratto in due contratti nuovi
    log.info("Inizio procedura di split del contratto {}", contract.toString());
    final DateInterval previousInterval = wrappedContract.getContractDatabaseInterval();
    contract.endDate = dateToSplit.minusDays(1);
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
    IWrapperPerson wrappedPerson = wrapperFactory.create(contract.person);
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
   * @param contractId l'identificativo del contratto da fondere
   */
  public static void merge(Long contractId) {
    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);
    Person person = contract.person;

    Contract previousContract = personDao.getPreviousPersonContract(contract);    

    render(person, contract, previousContract);
  }

  /**
   * Fonde insieme due contratti contigui.
   * @param contract il contratto attuale
   * @param previousContract il contratto precedente
   */
  public static void mergeContract(@Valid Contract contract, @Valid Contract previousContract, 
      boolean attestatiSync) {
    notFoundIfNull(contract);
    notFoundIfNull(previousContract);
    rules.checkIfPermitted(contract.person.office);
    Optional<LocalDate> to = contract.endDate != null 
        ? Optional.fromNullable(contract.endDate) : Optional.absent();

    //1) si eliminano le assenze a partire da contract.beginDate
    List<Absence> copy = Lists.newArrayList();
    List<Absence> list = contractService
        .getAbsencesInContract(contract.person, contract.getBeginDate(), to);
    if (!attestatiSync) {
      copy.addAll(list);
    }
    log.debug("Lista assenze contiene {} elementi", list.size());
    int count = 0;
    for (Absence abs : list) {      
      abs.delete();  
      count++;
    }
    log.debug("Cancellate {} assenze", count);
    //2) cancello il contratto più recente
    contract.delete();
    log.debug("Cancellato contratto {}", contract.toString());

    //3) prorogo la data fine di previousContract a contract.endDate
    IWrapperContract wrappedContract = wrapperFactory.create(previousContract);
    IWrapperPerson wrappedPerson = wrapperFactory.create(previousContract.person);
    final DateInterval previousInterval = wrappedContract.getContractDatabaseInterval();

    previousContract.endDate = to.isPresent() ? to.get() : null;
    log.debug("Prorogo il precedente contratto {} alla data {}", 
        previousContract, previousContract.endDate);
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
        contractService.resetAbsences(copy);
        log.info("Procedura completata.");
      } else {
        log.info("Scaricamento e persistenza assenze da Attestati a partire da {}", 
            contract.beginDate);
        contractService.saveAbsenceOnNewContract(wrappedPerson.getValue(), contract.beginDate);
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
    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrappedContract = wrapperFactory.create(contract);
    
    if (Validation.hasErrors()) {
      log.info("ValidationErrors = {}", validation.errorsMap());
      response.status = 400;
      render("@edit", contract, wrappedContract, sourceDateRecoveryDay);
    }

    // Salvo la situazione precedente
    final DateInterval previousInterval = wrappedContract.getContractDatabaseInterval();

    // Attribuisco il nuovo stato al contratto per effettuare il controllo incrociato
    contract.sourceDateRecoveryDay = sourceDateRecoveryDay;
    if (isTemporaryMissing != null) {
      contract.isTemporaryMissing = isTemporaryMissing;  
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

    rules.checkIfPermitted(person.office);

    Contract contract = new Contract();
    contract.person = person;

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

    rules.checkIfPermitted(contract.person.office);

    if (!Validation.hasErrors()) {
      if (!contractManager.properContractCreate(contract, Optional.of(wtt), true)) {
        Validation.addError("contract.beginDate", "i contratti non possono sovrapporsi.");
        if (contract.endDate != null) {
          Validation.addError("contract.endDate", "i contratti non possono sovrapporsi.");
        }
      }
    }

    if (Validation.hasErrors()) {
      response.status = 400;
      render("@insert", contract, wtt);
    }

    flash.success(Web.msgCreated(Contract.class));

    Contracts.personContracts(contract.person.id);
  }

  /**
   * Rimozione contratto.
   *
   * @param contractId contractId
   */
  public static void delete(Long contractId, boolean confirmed) {

    Contract contract = contractDao.getContractById(contractId);

    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    if (!confirmed) {
      render("@delete", contract);
    }
    List<PersonDay> pdList = personDayDao.getPersonDayInPeriod(contract.person, 
        contract.beginDate, Optional.fromNullable(contract.endDate));

    long count = pdList.stream().filter(pd -> pd.absences.isEmpty()).count();
    if (pdList.size() == 0 || count == pdList.size()) {
      contract.delete();
      flash.success(Web.msgDeleted(Contract.class));
    } else {
      flash.error("Non è possibile cancellare il contratto di %s perchè sono già presenti giornate"
          + " ad esso collegate contenenti assenze! Rimuoverle prima di eliminare il contratto", 
          contract.person.fullName());
    }    

    Persons.edit(contract.person.id);
  }

  /**
   * Crud gestione periodi tipo orario.
   *
   * @param id contratto
   */
  public static void updateContractWorkingTimeType(Long id) {

    Contract contract = contractDao.getContractById(id);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
    cwtt.contract = contract;

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
    notFoundIfNull(cwtt.contract);
    notFoundIfNull(cwtt.workingTimeType);
    Verify.verify(cwtt.contract.isPersistent());
    Verify.verify(cwtt.workingTimeType.isPersistent());

    rules.checkIfPermitted(cwtt.contract.person.office);

    IWrapperContract wrappedContract = wrapperFactory.create(cwtt.contract);
    Contract contract = cwtt.contract;

    if (!Validation.hasErrors()) {
      if (!DateUtility.isDateIntoInterval(cwtt.beginDate,
          wrappedContract.getContractDateInterval())) {
        Validation.addError("cwtt.beginDate", "deve appartenere al contratto");
      }
      if (cwtt.endDate != null && !DateUtility.isDateIntoInterval(cwtt.endDate,
          wrappedContract.getContractDateInterval())) {
        Validation.addError("cwtt.endDate", "deve appartenere al contratto");
      }
    }

    if (Validation.hasErrors()) {
      response.status = 400;

      render("@updateContractWorkingTimeType", cwtt, contract);
    }

    rules.checkIfPermitted(cwtt.workingTimeType.office);

    //riepilogo delle modifiche
    List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(cwtt, false);
    RecomputeRecap recomputeRecap =
        periodManager.buildRecap(wrappedContract.getContractDateInterval().getBegin(),
            Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
            periodRecaps, Optional.fromNullable(contract.sourceDateResidual));

    recomputeRecap.initMissing = wrappedContract.initializationMissing();

    if (!confirmed) {
      confirmed = true;
      render("@updateContractWorkingTimeType", contract, cwtt, confirmed, recomputeRecap);
    } else {

      periodManager.updatePeriods(cwtt, true);
      contract = contractDao.getContractById(contract.id);
      contract.person.refresh();
      if (recomputeRecap.needRecomputation) {
        contractManager.recomputeContract(contract,
            Optional.fromNullable(recomputeRecap.recomputeFrom), false, false);
      }

      flash.success(Web.msgSaved(ContractWorkingTimeType.class));

      updateContractWorkingTimeType(contract.id);
    }

  }

  /**
   * Crud gestione periodi fascia oraria obbligatoria.
   *
   * @param id contratto
   */
  public static void updateContractMandatoryTimeSlot(Long id, ContractMandatoryTimeSlot cmts) {

    Contract contract = contractDao.getContractById(id);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    if (cmts == null) {
      cmts = new ContractMandatoryTimeSlot();
    }
    cmts.contract = contract;

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
    notFoundIfNull(cmts.contract);
    notFoundIfNull(cmts.timeSlot);
    Verify.verify(cmts.contract.isPersistent());
    Verify.verify(cmts.timeSlot.isPersistent());

    rules.checkIfPermitted(cmts.contract.person.office);

    IWrapperContract wrappedContract = wrapperFactory.create(cmts.contract);
    Contract contract = cmts.contract;

    if (!Validation.hasErrors()) {
      if (!DateUtility.isDateIntoInterval(cmts.beginDate,
          wrappedContract.getContractDateInterval())) {
        Validation.addError("cmts.beginDate", "deve appartenere al contratto");
      }
      if (cmts.endDate != null && !DateUtility.isDateIntoInterval(cmts.endDate,
          wrappedContract.getContractDateInterval())) {
        Validation.addError("cmts.endDate", "deve appartenere al contratto");
      }
    }

    if (Validation.hasErrors()) {
      render("@updateContractMandatoryTimeSlot", cmts, contract);
    }

    rules.checkIfPermitted(cmts.timeSlot.office);

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
    cmts.setRecomputeFrom(cmts.beginDate);   
    periodRecaps.add(cmts);

    RecomputeRecap recomputeRecap =
        periodManager.buildRecap(wrappedContract.getContractDateInterval().getBegin(),
            Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
            periodRecaps, Optional.fromNullable(contract.sourceDateResidual));

    recomputeRecap.initMissing = wrappedContract.initializationMissing();

    if (!confirmed) {
      confirmed = true;
      render("@updateContractMandatoryTimeSlot", contract, cmts, confirmed, recomputeRecap);
    } else {
      cmts.save();
      contract = contractDao.getContractById(contract.id);
      contract.person.refresh();
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
   * @param id id della fascia oraria da cancellare
   * @param confirmed se è confirmed lo cancella altrimenti mostra una pagina di
   *     riepilogo e conferma.
   */
  public void deleteContractMandatoryTimeSlot(Long id, boolean confirmed) {
    ContractMandatoryTimeSlot cmts = ContractMandatoryTimeSlot.findById(id);

    notFoundIfNull(cmts);

    rules.checkIfPermitted(cmts.contract.person.office);

    boolean deletion = true;
    val contract = cmts.contract;
    if (!confirmed) {
      IWrapperContract wrappedContract = wrapperFactory.create(cmts.contract);
      val currentPeriods = cmts.getOwner().periods(cmts.getType());
      val periodRecaps = Lists.newArrayList(currentPeriods);

      periodRecaps.remove(cmts);
      RecomputeRecap recomputeRecap =
          periodManager.buildRecap(cmts.beginDate,
              Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
              periodRecaps, Optional.fromNullable(contract.sourceDateResidual));
      recomputeRecap.recomputeFrom = cmts.beginDate;
      periodManager.setDays(recomputeRecap);
      confirmed = true;
      render("@updateContractMandatoryTimeSlot", cmts, contract, deletion, recomputeRecap);
    } else {
      cmts.delete();
      flash.success(Web.msgDeleted(ContractMandatoryTimeSlot.class));
    }
    updateContractMandatoryTimeSlot(cmts.contract.id, null);

  }

  /**
   * Crud gestione periodi ferie.
   *
   * @param id contratto
   */
  public static void updateContractVacationPeriod(Long id) {

    Contract contract = contractDao.getContractById(id);
    notFoundIfNull(contract);

    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    VacationPeriod vp = new VacationPeriod();
    vp.contract = contract;

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
    notFoundIfNull(vp.contract);

    rules.checkIfPermitted(vp.contract.person.office);

    IWrapperContract wrappedContract = wrapperFactory.create(vp.contract);
    Contract contract = vp.contract;

    if (!Validation.hasErrors()) {
      if (!DateUtility.isDateIntoInterval(vp.beginDate,
          wrappedContract.getContractDateInterval())) {
        Validation.addError("vp.beginDate", "deve appartenere al contratto");
      }
      if (vp.endDate != null && !DateUtility.isDateIntoInterval(vp.endDate,
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
            periodRecaps, Optional.fromNullable(contract.sourceDateResidual));

    recomputeRecap.initMissing = wrappedContract.initializationMissing();

    if (!confirmed) {
      confirmed = true;
      render("@updateContractVacationPeriod", contract, vp, confirmed, recomputeRecap);
    } else {

      periodManager.updatePeriods(vp, true);
      contract = contractDao.getContractById(contract.id);
      contract.person.refresh();
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

    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    ContractStampProfile csp = new ContractStampProfile();
    csp.contract = contract;

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
    notFoundIfNull(csp.contract);

    rules.checkIfPermitted(csp.contract.person.office);

    IWrapperContract wrappedContract = wrapperFactory.create(csp.contract);
    Contract contract = csp.contract;

    if (!Validation.hasErrors()) {
      if (!DateUtility.isDateIntoInterval(csp.beginDate,
          wrappedContract.getContractDateInterval())) {
        Validation.addError("csp.beginDate", "deve appartenere al contratto");
      }
      if (csp.endDate != null && !DateUtility.isDateIntoInterval(csp.endDate,
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
            periodRecaps, Optional.fromNullable(contract.sourceDateResidual));

    recomputeRecap.initMissing = wrappedContract.initializationMissing();

    if (!confirmed) {
      confirmed = true;
      render("@updateContractStampProfile", contract, csp, confirmed, recomputeRecap);
    } else {

      periodManager.updatePeriods(csp, true);
      contract = contractDao.getContractById(contract.id);
      contract.person.refresh();
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
    rules.checkIfPermitted(contract.person.office);

    if (contract.sourceDateResidual == null) {
      contractManager.cleanResidualInitialization(contract);
    }

    IWrapperOffice wrOffice = wrapperFactory.create(contract.person.office);
    IWrapperPerson wrPerson = wrapperFactory.create(contract.person);

    Integer hoursCurrentYear = 0;
    Integer minutesCurrentYear = 0;
    if (contract.sourceRemainingMinutesCurrentYear != null) {
      hoursCurrentYear = contract.sourceRemainingMinutesCurrentYear / 60;
      minutesCurrentYear = contract.sourceRemainingMinutesCurrentYear % 60;
    }
    Integer hoursLastYear = 0;
    Integer minutesLastYear = 0;
    if (contract.sourceRemainingMinutesLastYear != null) {
      hoursLastYear = contract.sourceRemainingMinutesLastYear / 60;
      minutesLastYear = contract.sourceRemainingMinutesLastYear % 60;
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
    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrContract = wrapperFactory.create(contract);
    if (contract.sourceDateVacation == null) {
      contractManager.cleanVacationInitialization(contract);
    }

    Integer sourceVacationLastYearUsed = contract.sourceVacationLastYearUsed;
    Integer sourceVacationCurrentYearUsed = contract.sourceVacationCurrentYearUsed;
    Integer sourcePermissionUsed = contract.sourcePermissionUsed;

    IWrapperOffice wrOffice = wrapperFactory.create(contract.person.office);
    IWrapperPerson wrPerson = wrapperFactory.create(contract.person);

    render(contract, wrContract, wrOffice, wrPerson, 
        sourceVacationLastYearUsed, sourceVacationCurrentYearUsed, sourcePermissionUsed);
  }

  /**
   * Pagina aggiornamento buoni pasto iniziali del contratto.
   */
  public static void updateSourceContractMeal(Long contractId) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrContract = wrapperFactory.create(contract);
    if (contract.sourceDateMealTicket == null) {
      contractManager.cleanMealTicketInitialization(contract);
    }

    IWrapperOffice wrOffice = wrapperFactory.create(contract.person.office);
    IWrapperPerson wrPerson = wrapperFactory.create(contract.person);

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

    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrContract = wrapperFactory.create(contract);
    IWrapperOffice wrOffice = wrapperFactory.create(contract.person.office);
    IWrapperPerson wrPerson = wrapperFactory.create(contract.person);

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
      boolean removeMandatory = contract.beginDate.isBefore(wrContract.dateForInitialization());
      boolean removeUnnecessary = !removeMandatory;
      if (removeUnnecessary) {
        recomputeFrom = contract.beginDate;
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
      if (contract.sourceDateResidual == null) {
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
      contract.sourceDateResidual = sourceDateResidual;
      contract.sourceRemainingMinutesCurrentYear = hoursCurrentYear * 60 + minutesCurrentYear;
      contract.sourceRemainingMinutesLastYear = hoursLastYear * 60 + minutesLastYear;;
      contractManager.setSourceContractProperly(contract);
      contractManager.properContractUpdate(contract, recomputeFrom, false);

      flash.success("Contratto di %s inizializzato correttamente.", contract.person.fullName());
    }
    initializationsStatus(contract.person.office.id);

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

    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrContract = wrapperFactory.create(contract);
    IWrapperOffice wrOffice = wrapperFactory.create(contract.person.office);
    IWrapperPerson wrPerson = wrapperFactory.create(contract.person);

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
      if (contract.sourceDateVacation != null) {
        sourceUpdate = true;
      }
      render("@updateSourceContractVacation", contract, wrContract, wrPerson, wrOffice, 
          sourceDateVacation,
          sourceVacationLastYearUsed, sourceVacationCurrentYearUsed, sourcePermissionUsed, 
          confirmedVacation, sourceUpdate);
    }

    //esecuzione
    if (confirmedVacation) {
      contract.sourceDateVacation = sourceDateVacation;
      contract.sourceVacationLastYearUsed = sourceVacationLastYearUsed;
      contract.sourceVacationCurrentYearUsed = sourceVacationCurrentYearUsed;
      contract.sourcePermissionUsed = sourcePermissionUsed;

      contractManager.setSourceContractProperly(contract);

      absenceService.emptyVacationCache(contract);

      flash.success("Contratto di %s inizializzato correttamente.", contract.person.fullName());
    }
    initializationsVacation(contract.person.office.id);

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

    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrContract = wrapperFactory.create(contract);
    IWrapperOffice wrOffice = wrapperFactory.create(contract.person.office);
    IWrapperPerson wrPerson = wrapperFactory.create(contract.person);

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
      if (contract.sourceDateResidual != null) {
        recomputeFrom = contract.sourceDateResidual;
      } else {
        recomputeFrom = contract.beginDate;
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
      if (contract.sourceDateResidual == null) {
        sourceNew = true;
      } else {
        sourceUpdate = true;
      }

      render("@updateSourceContractMeal", contract, wrContract, wrPerson, wrOffice,
          sourceDateMealTicket, confirmedMeal,
          sourceNew, sourceUpdate, recomputeFrom, recomputeTo, months);

    }

    // Conferma ricevuta
    contract.sourceDateMealTicket = sourceDateMealTicket;
    contractManager.setSourceContractProperly(contract);
    contractManager.properContractUpdate(contract, recomputeFrom, true);

    flash.success("Buoni pasto di %s inizializzati correttamente.", contract.person.fullName());

    initializationsMeal(contract.person.office.id);
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
        if (wrPerson.getCurrentContract().get().sourceDateResidual != null) {
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

      if (wrPerson.getCurrentContract().get().sourceDateVacation != null) {
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
        } else if (wrContract.getValue().sourceDateMealTicket != null) {
          correctInitialized.add(wrContract);
        } else {
          correctNotInitialized.add(wrContract);
        }
      }

    }

    render(initializationsBeforeGeneral, correctInitialized, correctNotInitialized, office);

  }

}
