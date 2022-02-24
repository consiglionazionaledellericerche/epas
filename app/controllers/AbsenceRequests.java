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

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import dao.AbsenceRequestDao;
import dao.GroupDao;
import dao.PersonDao;
import dao.PersonReperibilityDayDao;
import dao.PersonShiftDayDao;
import dao.UsersRolesOfficesDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import helpers.TemplateExtensions;
import helpers.Web;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.AbsenceManager;
import manager.NotificationManager;
import manager.NotificationManager.Crud;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.flows.AbsenceRequestManager;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import manager.services.absences.model.PeriodChain;
import manager.services.absences.model.VacationSituation;
import models.Contract;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultGroup;
import models.flows.AbsenceRequest;
import models.flows.Group;
import models.flows.enumerate.AbsenceRequestEventType;
import models.flows.enumerate.AbsenceRequestType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;


/**
 * Controller per la gestione delle richieste di assenza dei dipendenti.
 *
 * @author Cristian Lucchesi
 */
@Slf4j
@With(Resecure.class)
public class AbsenceRequests extends Controller {

  @Inject
  static PersonDao personDao;

  @Inject
  static AbsenceRequestManager absenceRequestManager;

  @Inject
  static AbsenceRequestDao absenceRequestDao;

  @Inject
  static SecurityRules rules;

  @Inject
  static UsersRolesOfficesDao uroDao;

  @Inject
  static AbsenceService absenceService;

  @Inject
  static AbsenceManager absenceManager;

  @Inject
  static NotificationManager notificationManager;

  @Inject
  static GroupDao groupDao;

  @Inject
  static PersonStampingRecapFactory stampingsRecapFactory;

  @Inject
  static ConfigurationManager configurationManager;

  @Inject
  static AbsenceComponentDao absenceComponentDao;

  @Inject
  static IWrapperFactory wrapperFactory;

  @Inject
  static PersonShiftDayDao personShiftDayDao;

  @Inject
  static PersonReperibilityDayDao personReperibilityDayDao;


  /**
   * Lista delle richiesta di assenza di tipo ferie.
   */
  public static void vacations() {
    list(AbsenceRequestType.VACATION_REQUEST);
  }

  /**
   * Lista delle richiesta di assenza di tipo riposo compensativo.
   */
  public static void compensatoryRests() {
    list(AbsenceRequestType.COMPENSATORY_REST);
  }

  /**
   * Lista delle richiesta di assenza di tipo permesso personale.
   */
  public static void personalPermissions() {
    list(AbsenceRequestType.PERSONAL_PERMISSION);
  }

  /**
   * Lista delle richiesta di assenza di tipo ferie dell'anno passato oltre la scadenza.
   */
  public static void vacationsPastYearAfterDeadline() {
    list(AbsenceRequestType.VACATION_PAST_YEAR_AFTER_DEADLINE_REQUEST);
  }
  
  /**
   * Lista delle richiesta di assenza di tipo permesso breve.
   */
  public static void shortTermPermit() {
    list(AbsenceRequestType.SHORT_TERM_PERMIT);
  }

  /**
   * Lista delle richiesta di assenza di tipo ferie da approvare.
   */
  public static void vacationsToApprove() {
    listToApprove(AbsenceRequestType.VACATION_REQUEST);
  }

  /**
   * Lista delle richiesta di assenza di tipo riposo compensativo da approvare.
   */
  public static void compensatoryRestsToApprove() {
    listToApprove(AbsenceRequestType.COMPENSATORY_REST);
  }
  
  /**
   * Lista delle richiesta di assenza di tipo permesso personale da approvare.
   */
  public static void permissionsToApprove() {
    listToApprove(AbsenceRequestType.PERSONAL_PERMISSION);
  }
  
  /**
   * Lista delle richiesta di assenza di tipo ferie anno passato oltre scadenza da approvare.
   */
  public static void vacationsPastYearAfterDeadlineToApprove() {
    listToApprove(AbsenceRequestType.VACATION_PAST_YEAR_AFTER_DEADLINE_REQUEST);
  }

  /**
   * Lista delle richiesta di assenza di tipo permesso breve da approvare.
   */
  public static void shortTermPermitToApprove() {
    listToApprove(AbsenceRequestType.SHORT_TERM_PERMIT);
  }

  /**
   * Lista delle richieste di assenza dell'utente corrente.
   *
   * @param type tipo opzionale di tipo di richiesta di assenza.
   */
  public static void list(AbsenceRequestType type) {
    Verify.verifyNotNull(type);

    val currentUser = Security.getUser().get();
    if (currentUser.person == null) {
      flash.error("L'utente corrente non ha associata una persona, non può vedere le proprie "
          + "richieste di assenza");
      Application.index();
      return;
    }
    val person = currentUser.person;

    val fromDate = LocalDateTime.now().dayOfYear().withMinimumValue().minusMonths(1);
    log.debug("Prelevo le richieste di assenze di tipo {} per {} a partire da {}", type, person,
        fromDate);

    val config = absenceRequestManager.getConfiguration(type, person);
    List<AbsenceRequest> myResults =
        absenceRequestDao.findByPersonAndDate(person, fromDate, Optional.absent(), type, true);
    List<AbsenceRequest> closed =
        absenceRequestDao.findByPersonAndDate(person, fromDate, Optional.absent(), type, false);
    val onlyOwn = true;
    boolean persist = false;

    render(config, myResults, type, onlyOwn, persist, closed);
  }

  /**
   * Lista delle richieste di assenza da approvare da parte dell'utente corrente.
   *
   * @param type tipo opzionale di tipo di richiesta di assenza.
   */
  public static void listToApprove(AbsenceRequestType type) {
    Verify.verifyNotNull(type);

    long start = System.currentTimeMillis();
    val person = Security.getUser().get().person;
    val fromDate = LocalDateTime.now().dayOfYear().withMinimumValue().minusMonths(1);
    log.debug("Prelevo le richieste da approvare di assenze di tipo {} a partire da {}", type,
        fromDate);
    List<Group> groups = 
        groupDao.groupsByOffice(person.getCurrentOffice().get(), Optional.absent(), 
            Optional.of(false));
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(person.user);
    List<AbsenceRequest> results =
        absenceRequestDao.allResults(roleList, fromDate, Optional.absent(), type, groups, person);
    List<AbsenceRequest> myResults = absenceRequestDao.toApproveResults(roleList, Optional.absent(),
        Optional.absent(), type, groups, person);
    List<AbsenceRequest> approvedResults = absenceRequestDao.totallyApproved(roleList, fromDate,
        Optional.absent(), type, groups, person);
    val config = absenceRequestManager.getConfiguration(type, person);
    val onlyOwn = false;
    log.debug("Preparate richieste da approvare per {} in {} ms. Da approvare = {}", 
        person.getFullname(), System.currentTimeMillis() - start, myResults.size());
    render(config, results, type, onlyOwn, approvedResults, myResults);
  }


  /**
   * Form per la richiesta di assenza da parte del dipendente.
   *
   * @param personId persona selezionata
   * @param from data selezionata
   * @param type di richiesta di assenza
   */
  public static void blank(Optional<Long> personId, LocalDate from, AbsenceRequestType type) {
    Verify.verifyNotNull(type);

    Person person;
    if (personId.isPresent()) {
      rules.check("AbsenceRequests.blank4OtherPerson");
      person = personDao.getPersonById(personId.get());
    } else {
      if (Security.getUser().isPresent() && Security.getUser().get().person != null) {
        person = Security.getUser().get().person;
      } else {
        flash.error("L'utente corrente non ha associato una persona.");
        list(type);
        return;
      }
    }
    notFoundIfNull(person);

    val configurationProblems = absenceRequestManager.checkconfiguration(type, person);
    if (!configurationProblems.isEmpty()) {
      flash.error(Joiner.on(" ").join(configurationProblems));
      list(type);
      return;
    }
    // extra info per richiesta riposo compensativo o ferie
    int compensatoryRestAvailable = 0;
    List<VacationSituation> vacationSituations = Lists.newArrayList();

    boolean handleCompensatoryRestSituation = false;
    val absenceRequest = new AbsenceRequest();
    absenceRequest.type = type;
    absenceRequest.person = person;

    if (type.equals(AbsenceRequestType.COMPENSATORY_REST) && person.isTopQualification()) {
      PersonStampingRecap psDto = stampingsRecapFactory.create(person, LocalDate.now().getYear(),
          LocalDate.now().getMonthOfYear(), true, Optional.absent());
      int maxDays = (Integer) configurationManager.configValue(person.getCurrentOffice().get(),
          EpasParam.MAX_RECOVERY_DAYS_13, LocalDate.now().getYear());
      compensatoryRestAvailable = maxDays - psDto.numberOfCompensatoryRestUntilToday;
      handleCompensatoryRestSituation = true;
    }
    if (type.equals(AbsenceRequestType.VACATION_REQUEST)) {
      GroupAbsenceType vacationGroup =
          absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
      IWrapperPerson wperson = wrapperFactory.create(person);

      for (Contract contract : wperson.orderedYearContracts(LocalDate.now().getYear())) {
        VacationSituation vacationSituation = absenceService.buildVacationSituation(contract,
            LocalDate.now().getYear(), vacationGroup, Optional.absent(), false);
        vacationSituations.add(vacationSituation);
      }
    }
    GroupAbsenceType permissionGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.G_661.name()).get();
    PeriodChain periodChain = absenceService
        .residual(person, permissionGroup, LocalDate.now());

    boolean showVacationPeriods = false;
    boolean retroactiveAbsence = false;
    absenceRequest.startAt = absenceRequest.endTo = LocalDateTime.now().plusDays(1);
    boolean insertable = true;
    GroupAbsenceType groupAbsenceType = absenceRequestManager.getGroupAbsenceType(absenceRequest);
    AbsenceType absenceType = null;
    AbsenceForm absenceForm = absenceService.buildAbsenceForm(absenceRequest.person,
        absenceRequest.startAtAsDate(), null, absenceRequest.endToAsDate(), null, groupAbsenceType,
        false, absenceType, null, null, null, false, true);
    InsertReport insertReport =
        absenceService.insert(absenceRequest.person, absenceForm.groupSelected, absenceForm.from,
            absenceForm.to, absenceForm.absenceTypeSelected, absenceForm.justifiedTypeSelected,
            null, null, false, absenceManager);
    
    render("@edit", absenceRequest, insertable, insertReport, vacationSituations,
        compensatoryRestAvailable, handleCompensatoryRestSituation, showVacationPeriods,
        retroactiveAbsence, absenceForm, periodChain);

  }

  /**
   * Form di modifica di una richiesta di assenza.
   *
   * @param absenceRequest richiesta di assenza da modificare.
   */
  public static void edit(AbsenceRequest absenceRequest, boolean retroactiveAbsence,
      GroupAbsenceType groupAbsenceType, AbsenceType absenceType, 
      JustifiedType justifiedType, Integer hours, Integer minutes) {

    rules.checkIfPermitted(absenceRequest);
    boolean insertable = true;
    GroupAbsenceType permissionGroup = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.G_661.name()).get();
    PeriodChain periodChain = absenceService
        .residual(absenceRequest.person, permissionGroup, LocalDate.now());

    if (absenceRequest.startAt == null || absenceRequest.endTo == null) {
      Validation.addError("absenceRequest.startAt",
          "Entrambi i campi data devono essere valorizzati");
      Validation.addError("absenceRequest.endTo",
          "Entrambi i campi data devono essere valorizzati");
      response.status = 400;
      insertable = false;
      render("@edit", absenceRequest, insertable, retroactiveAbsence, 
          groupAbsenceType, absenceType, justifiedType, hours, minutes, periodChain);
    }
    if (absenceRequest.startAt.isAfter(absenceRequest.endTo)) {
      absenceRequest.endTo = absenceRequest.startAt;
      flash.success("Aggiornata la data di fine della richiesta a %s", 
          TemplateExtensions.format(absenceRequest.endTo.toDate()));
    }
    if (absenceRequest.endTo.compareTo(absenceRequest.startAt.plusMonths(6)) > 0) {
      Validation.addError("absenceRequest.endTo",
          "Le richieste di assenza non possono essere per periodi maggiori di sei mesi. "
          + "Eventualmente effettuare più richieste di assenza.");
    }

    // verifico che non esista già una richiesta (non rifiutata)
    // di assenza che interessa i giorni richiesti
    AbsenceRequest existing = absenceRequestManager.checkAbsenceRequest(absenceRequest);
    if (existing != null) {
      Validation.addError("absenceRequest.startAt", "Esiste già una richiesta in questa data");
      Validation.addError("absenceRequest.endTo", "Esiste già una richiesta in questa data");
      response.status = 400;
      insertable = false;
      render("@edit", absenceRequest, insertable, existing, retroactiveAbsence, periodChain);
    }
    if (!absenceRequest.person
        .checkLastCertificationDate(new YearMonth(absenceRequest.startAtAsDate().getYear(),
            absenceRequest.startAtAsDate().getMonthOfYear()))) {
      Validation.addError("absenceRequest.startAt",
          "Non è possibile fare una richiesta per una data di un mese già processato in Attestati");
      response.status = 400;
      insertable = false;
      render("@edit", absenceRequest, insertable, retroactiveAbsence, periodChain);
    }

    if (Validation.hasErrors()) {
      response.status = 400;
      insertable = false;
      flash.error(Web.msgHasErrors());

      render("@edit", absenceRequest, insertable, retroactiveAbsence, periodChain);
      //return;
    }

    if (absenceRequest.startAtAsDate().isBefore(LocalDate.now())) {
      retroactiveAbsence = true;
      if (Strings.isNullOrEmpty(absenceRequest.note)) {
        Validation.addError("absenceRequest.note",
            "Inserire una motivazione per l'assenza nel passato");
        response.status = 400;
        insertable = false;
        render("@edit", absenceRequest, insertable, retroactiveAbsence, periodChain);
      }
    }
    if (groupAbsenceType == null || !groupAbsenceType.isPersistent()) {
      groupAbsenceType = absenceRequestManager.getGroupAbsenceType(absenceRequest);
    }
    
    if (groupAbsenceType.name.equals(DefaultGroup.FERIE_CNR_PROROGA.name())) {
      absenceType = absenceComponentDao.absenceTypeByCode(DefaultAbsenceType.A_37.getCode()).get();
    }
    AbsenceForm absenceForm = absenceService.buildAbsenceForm(absenceRequest.person,
        absenceRequest.startAtAsDate(), null, absenceRequest.endToAsDate(), null, groupAbsenceType,
        false, absenceType, justifiedType, hours, minutes, false, true);
    InsertReport insertReport =
        absenceService.insert(absenceRequest.person, absenceForm.groupSelected, absenceForm.from,
            absenceForm.to, absenceForm.absenceTypeSelected, absenceForm.justifiedTypeSelected,
            absenceForm.hours, absenceForm.minutes, false, absenceManager);
    
    absenceRequest = absenceRequestManager.checkAbsenceRequestDates(absenceRequest, insertReport);

    int compensatoryRestAvailable = 0;
    if (absenceRequest.type.equals(AbsenceRequestType.COMPENSATORY_REST)
        && absenceRequest.person.isTopQualification()) {
      PersonStampingRecap psDto = stampingsRecapFactory.create(absenceRequest.person,
          LocalDate.now().getYear(), LocalDate.now().getMonthOfYear(), true, Optional.absent());
      int maxDays = (Integer) configurationManager.configValue(absenceRequest.person
          .getCurrentOffice().get(), EpasParam.MAX_RECOVERY_DAYS_13, LocalDate.now().getYear());
      compensatoryRestAvailable = maxDays - psDto.numberOfCompensatoryRestUntilToday;

    }
    List<VacationSituation> vacationSituations = Lists.newArrayList();
    if (absenceRequest.type.equals(AbsenceRequestType.VACATION_REQUEST)) {
      GroupAbsenceType vacationGroup =
          absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
      IWrapperPerson wperson = wrapperFactory.create(absenceRequest.person);

      for (Contract contract : wperson.orderedYearContracts(LocalDate.now().getYear())) {
        VacationSituation vacationSituation = absenceService.buildVacationSituation(contract,
            LocalDate.now().getYear(), vacationGroup, Optional.absent(), false);
        vacationSituations.add(vacationSituation);
      }
    }
    boolean allDay = true;
    if (absenceForm.absenceTypeSelected != null 
        && absenceForm.absenceTypeSelected.code.equals("661M")) {
      allDay = false;
    }
    render(absenceRequest, insertReport, absenceForm, insertable, vacationSituations,
        compensatoryRestAvailable, retroactiveAbsence, periodChain, allDay);
  }

  /**
   * Salvataggio di una richiesta di assenza.
   */
  public static void save(@Required @Valid AbsenceRequest absenceRequest, 
      Integer hours, Integer minutes, boolean allDay) {

    log.debug("AbsenceRequest.startAt = {}", absenceRequest.startAt);

    if (!Security.getUser().get().person.equals(absenceRequest.person)) {
      rules.check("AbsenceRequests.blank4OtherPerson");
    } else {
      absenceRequest.person = Security.getUser().get().person;
    }

    notFoundIfNull(absenceRequest.person);
    absenceRequestManager.configure(absenceRequest);

    if (absenceRequest.endTo == null) {
      absenceRequest.endTo = absenceRequest.startAt;
    }
    
    if (!allDay) {
      absenceRequest.hours = hours;
      if (minutes != null) {
        absenceRequest.minutes = minutes;
      } else {
        absenceRequest.minutes = 0;
      }      
    }
    

    boolean isNewRequest = !absenceRequest.isPersistent();
    List<LocalDate> troubleDays = absenceRequestManager.getTroubleDays(absenceRequest);
    if (!troubleDays.isEmpty()) {
      absenceRequest.note = absenceRequestManager.generateNoteForShiftOrReperibility(troubleDays);
    }

    absenceRequest.save();

    // Avvia il flusso se necessario.
    if (isNewRequest || !absenceRequest.flowStarted) {
      absenceRequestManager.executeEvent(absenceRequest, absenceRequest.person,
          AbsenceRequestEventType.STARTING_APPROVAL_FLOW, Optional.absent());
      if (absenceRequest.person.isSeatSupervisor() || (absenceRequest.person.isGroupManager()
          && !absenceRequest.officeHeadApprovalForManagerRequired)) {
        approval(absenceRequest.id);
      } else {
        // invio la notifica al primo che deve validare la mia richiesta
        notificationManager.notificationAbsenceRequestPolicy(absenceRequest.person.user,
            absenceRequest, Crud.CREATE);
        // invio anche la mail
        notificationManager.sendEmailAbsenceRequestPolicy(absenceRequest.person.user,
            absenceRequest, true);
      }

    }
    flash.success("Operazione effettuata correttamente");

    AbsenceRequests.list(absenceRequest.type);
  }

  // }

  /**
   * Metodo che "pulisce" la richiesta di assenza in caso di errori prima della conferma.
   *
   * @param type il tipo di richiesta di assenza
   */
  public static void flush(AbsenceRequestType type, long personId) {

    AbsenceRequest absenceRequest = new AbsenceRequest();
    absenceRequest.type = type;
    Person person = personDao.getPersonById(personId);
    absenceRequest.person = person;

    boolean persist = false;
    render("@edit", absenceRequest, persist);
  }

  /**
   * Metodo dispatcher che chiama il corretto metodo per approvare la richiesta.
   *
   * @param id l'id della richiesta da approvare
   */
  public static void approval(long id) {

    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    notFoundIfNull(absenceRequest);
    User user = Security.getUser().get();

    boolean approved = absenceRequestManager.approval(absenceRequest, user);

    if (approved) {
      notificationManager.sendEmailToUser(Optional.fromNullable(absenceRequest), 
          Optional.absent(), Optional.absent(), true);

      flash.success("Operazione conclusa correttamente");
    } else {
      flash.error("Problemi nel completare l'operazione contattare il supporto tecnico di ePAS.");
    }
    AbsenceRequests.listToApprove(absenceRequest.type);

  }

  /**
   * Dispatcher che instrada al corretto metodo l'operazione da fare sulla richiesta a seconda dei
   * parametri.
   *
   * @param id l'id della richiesta di assenza
   */
  public static void disapproval(long id, boolean disapproval, String reason) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    User user = Security.getUser().get();
    if (!disapproval) {
      disapproval = true;
      render(absenceRequest, disapproval);
    }
    if (absenceRequest.managerApprovalRequired && absenceRequest.managerApproved == null
        && user.hasRoles(Role.GROUP_MANAGER)) {
      // caso di approvazione da parte del responsabile di gruppo.
      absenceRequestManager.managerDisapproval(id, reason);
    }
    if (absenceRequest.administrativeApprovalRequired
        && absenceRequest.administrativeApproved == null && user.hasRoles(Role.PERSONNEL_ADMIN)) {
      // caso di approvazione da parte dell'amministratore del personale
      absenceRequestManager.personnelAdministratorDisapproval(id, reason);
    }
    if (absenceRequest.officeHeadApprovalRequired && absenceRequest.officeHeadApproved == null
        && user.hasRoles(Role.SEAT_SUPERVISOR)) {
      // caso di approvazione da parte del responsabile di sede
      absenceRequestManager.officeHeadDisapproval(id, reason);
    }
    notificationManager.sendEmailToUser(Optional.fromNullable(absenceRequest), 
        Optional.absent(), Optional.absent(), false);
    flash.error("Richiesta respinta");
    render("@show", absenceRequest, user);
  }


  /**
   * Form di modifica di una richiesta di assenza.
   *
   * @param id id della richiesta di assenza da modificare.
   */
  public static void delete(long id) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    notFoundIfNull(absenceRequest);
    rules.checkIfPermitted(absenceRequest);
    absenceRequestManager.executeEvent(absenceRequest, Security.getUser().get().person,
        AbsenceRequestEventType.DELETE, Optional.absent());
    flash.success(Web.msgDeleted(AbsenceRequest.class));
    // invio la notifica a chi doveva validare la mia richiesta
    notificationManager.notificationAbsenceRequestPolicy(absenceRequest.person.user,
        absenceRequest, Crud.DELETE);
    list(absenceRequest.type);
  }

  /**
   * Mostra al template la richiesta.
   *
   * @param id l'id della richiesta da visualizzare
   */
  public static void show(long id, AbsenceRequestType type) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    notFoundIfNull(absenceRequest);
    rules.checkIfPermitted(absenceRequest);
    User user = Security.getUser().get();
    boolean disapproval = false;
    render(absenceRequest, type, user, disapproval);
  }
}