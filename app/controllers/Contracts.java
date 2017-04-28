package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import helpers.Web;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.ContractManager;
import manager.PeriodManager;
import manager.attestati.dto.internal.clean.ContrattoAttestati;
import manager.attestati.service.ICertificationService;
import manager.recaps.recomputation.RecomputeRecap;
import manager.services.absences.AbsenceService;

import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.VacationPeriod;
import models.WorkingTimeType;
import models.base.IPropertyInPeriod;

import org.apache.commons.compress.utils.Lists;
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

    render(person, contract, wrappedContract, beginDate, endDate, endContract,
        onCertificate);
  }

  /**
   * Aggiornamento date e on certificate contratto.
   *
   * @param contract      contract
   * @param beginDate     inizio
   * @param endDate       scadenza
   * @param endContract   terminazione
   * @param onCertificate in attestati
   * @param confirmed     step di conferma
   */
  public static void update(@Valid Contract contract, @Required LocalDate beginDate,
      @Valid LocalDate endDate, @Valid LocalDate endContract,
      boolean onCertificate, boolean confirmed) {

    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrappedContract = wrapperFactory.create(contract);

    if (!Validation.hasErrors()) {
      if (contract.sourceDateResidual != null
          && contract.sourceDateResidual.isBefore(beginDate)) {
        Validation.addError("beginDate",
            "non può essere successiva alla data di inizializzazione");
      }
      if (endDate != null && endDate.isBefore(beginDate)) {
        Validation.addError("endDate", "non può precedere l'inizio del contratto.");
      }
      if (endContract != null && endContract.isBefore(beginDate)) {
        Validation.addError("endContract", "non può precedere l'inizio del contratto.");
      }
      if (endDate != null && endContract != null && endContract.isAfter(endDate)) {
        Validation.addError("endContract", "non può essere successivo alla scadenza del contratto");
      }
    }

    if (Validation.hasErrors()) {
      response.status = 400;
      render("@edit", contract, wrappedContract, beginDate, endDate, endContract,
          onCertificate);
    }

    // Salvo la situazione precedente
    final DateInterval previousInterval = wrappedContract.getContractDatabaseInterval();

    // Attribuisco il nuovo stato al contratto per effettuare il controllo incrociato
    contract.beginDate = beginDate;
    contract.endDate = endDate;
    contract.endContract = endContract;
    contract.onCertificate = onCertificate;
    if (!contractManager.isContractNotOverlapping(contract)) {
      Validation.addError("contract.crossValidationFailed",
          "Il contratto non può intersecarsi" + " con altri contratti del dipendente.");
      render("@edit", contract, wrappedContract, beginDate, endDate, endContract,
          onCertificate);
    }

    DateInterval newInterval = wrappedContract.getContractDatabaseInterval();
    RecomputeRecap recomputeRecap = periodManager.buildTargetRecap(previousInterval, newInterval,
        wrappedContract.initializationMissing());

    //conferma
    if (!confirmed) {
      confirmed = true;
      response.status = 400;
      render("@edit", contract, wrappedContract, beginDate, endDate, endContract, confirmed,
          onCertificate, recomputeRecap);
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

      if (contract.endDate != null
          && contract.endDate.isBefore(contract.beginDate)) {
        Validation.addError("contract.endDate", "non può precedere l'inizio del contratto.");

      } else if (!contractManager.properContractCreate(contract, wtt, true)) {
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

    contract.delete();
    flash.success(Web.msgDeleted(Contract.class));
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

    IWrapperContract wrContract = wrapperFactory.create(contract);
    if (contract.sourceDateResidual == null) {
      contractManager.cleanResidualInitialization(contract);
    }

    IWrapperOffice wrOffice = wrapperFactory.create(contract.person.office);
    IWrapperPerson wrPerson = wrapperFactory.create(contract.person);

    Integer sourceRemainingMinutesCurrentYear = contract.sourceRemainingMinutesCurrentYear; 
    Integer sourceRemainingMinutesLastYear = contract.sourceRemainingMinutesLastYear;
    
    render(contract, wrContract, wrOffice, wrPerson, 
        sourceRemainingMinutesCurrentYear, sourceRemainingMinutesLastYear);
  }
  
  /**
   * Pagina aggiornamento dati iniziali ferie e permessi del contratto.
   */
  public static void updateSourceContractVacation(Long contractId) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);

    IWrapperContract wrContract = wrapperFactory.create(contract);
    if (contract.sourceDateResidual == null) {
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
      @Valid final LocalDate sourceDateResidual, Integer sourceRemainingMinutesCurrentYear, 
      Integer sourceRemainingMinutesLastYear,
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
      log.warn("validation errors: {}", validation.errorsMap());
      render("@updateSourceContract", contract, wrContract, wrPerson, wrOffice, 
          sourceRemainingMinutesCurrentYear, sourceRemainingMinutesLastYear, sourceDateResidual);
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
          sourceRemainingMinutesCurrentYear, sourceRemainingMinutesLastYear,
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
          sourceRemainingMinutesCurrentYear, sourceRemainingMinutesLastYear,
          confirmedResidual, sourceNew, sourceUpdate, recomputeFrom, recomputeTo, days);
    }

    //esecuzione
    if (confirmedResidual) {
      contract.sourceDateResidual = sourceDateResidual;
      contract.sourceRemainingMinutesCurrentYear = sourceRemainingMinutesCurrentYear;
      contract.sourceRemainingMinutesLastYear = sourceRemainingMinutesLastYear;
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
      log.warn("validation errors: {}", validation.errorsMap());
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
  
  /**
   * Preleva i contratti della sede in attestati.
   * @param officeId sede
   * @param year anno
   * @param month mese
   */
  public static void certificationContracts(Long officeId, Integer year, Integer month) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    try { 

      Map<Integer, ContrattoAttestati> contrattiAttestati = 
          certService.getCertificationContracts(office, year, month);
      
      for (ContrattoAttestati contrattoAttestati : contrattiAttestati.values()) {
        log.info("Matricola: {}, DataInizio: {}, DataFine:{}", contrattoAttestati.matricola, 
            contrattoAttestati.beginContract, contrattoAttestati.endContract);
      }

      renderText("ok");
      
    } catch (Exception ex) {
      //TODO: se serve usare Certifications.cleanMessage(Exception ex)
      log.error("Errore nel recupero delle informazioni dal server di attestati"
          + " per la sede {}: {}", office, ex);
      renderText("nok");
    }
    
  }

}
