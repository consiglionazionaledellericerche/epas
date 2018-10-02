package controllers;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import dao.AbsenceRequestDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import dao.absences.AbsenceComponentDao;

import helpers.Web;
import helpers.jpa.ModelQuery.SimpleResults;

import javax.inject.Inject;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import manager.AbsenceManager;
import manager.NotificationManager;
import manager.flows.AbsenceRequestManager;
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;

import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.absences.AbsenceType;
import models.absences.CategoryTab;
import models.absences.GroupAbsenceType;
import models.absences.definitions.DefaultTab;
import models.flows.AbsenceRequest;
import models.flows.enumerate.AbsenceRequestEventType;
import models.flows.enumerate.AbsenceRequestType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;



/**
 * Controller per la gestione delle richieste di assenza dei dipendenti.
 * 
 * @author cristian
 *
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
  static RoleDao roleDao;
  
  @Inject
  static UsersRolesOfficesDao uroDao;
  
  @Inject
  static AbsenceService absenceService;
  
  @Inject
  static AbsenceManager absenceManager;
  
  @Inject
  static NotificationManager notificationManager;
 
 
  public static void vacations() {
    list(AbsenceRequestType.VACATION_REQUEST);
  }

  public static void compensatoryRests() {
    list(AbsenceRequestType.COMPENSATORY_REST);
  }

  public static void shortTermPermit() {
    list(AbsenceRequestType.SHORT_TERM_PERMIT);
  }
  
  public static void vacationsToApprove() {
    listToApprove(AbsenceRequestType.VACATION_REQUEST);
  }

  public static void compensatoryRestsToApprove() {
    listToApprove(AbsenceRequestType.COMPENSATORY_REST);
  }

  public static void shortTermPermitToApprove() {
    listToApprove(AbsenceRequestType.SHORT_TERM_PERMIT);
  }

  /**
   * Lista delle richieste di assenza dell'utente corrente. 
   * @param type tipo opzionale di tipo di richiesta di assenza.
   * 
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
     
    val fromDate = LocalDateTime.now().dayOfYear().withMinimumValue();
    log.debug("Prelevo le richieste di assenze di tipo {} per {} a partire da {}", 
        type, person, fromDate);
    
    val config = absenceRequestManager.getConfiguration(type, person);
    List<AbsenceRequest> results = absenceRequestDao
        .findByPersonAndDate(person, fromDate, Optional.absent(), type, true);
    List<AbsenceRequest> closed = absenceRequestDao
        .findByPersonAndDate(person, fromDate, Optional.absent(), type, false);
    val onlyOwn = true;
    boolean persist = false;
    
    render(config, results, type, onlyOwn, persist, closed);
  }
  
  /**
   * Lista delle richieste di assenza da approvare da parte dell'utente corrente. 
   * @param type tipo opzionale di tipo di richiesta di assenza.
   * 
   */
  public static void listToApprove(AbsenceRequestType type) {
    Verify.verifyNotNull(type);

    val person = Security.getUser().get().person; 
    val fromDate = LocalDateTime.now().dayOfYear().withMinimumValue();
    log.debug("Prelevo le richieste da approvare di assenze di tipo {} a partire da {}", 
        type, fromDate);   
    
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(person.user);
    List<AbsenceRequest> results = absenceRequestDao
        .findRequestsToApprove(roleList, fromDate, Optional.absent(), type);
    List<AbsenceRequest> approvedResults = absenceRequestDao
        .findRequestsApproved(roleList, fromDate, Optional.absent(), type);
    val config = absenceRequestManager.getConfiguration(type, person);  
    val onlyOwn = false;
    
    render(config, results, type, onlyOwn, approvedResults);
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
    
    val absenceRequest = new AbsenceRequest();
    absenceRequest.type = type;
    absenceRequest.person = person;
    boolean persist = false;
    render("@edit", absenceRequest, persist);

  }

  /**
   * Form di modifica di una richiesta di assenza.
   * 
   * @param id id della richiesta di assenza da modificare.
   */
  public static void edit(long id) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    notFoundIfNull(absenceRequest);
    rules.checkIfPermitted(absenceRequest);
    
    render(absenceRequest);
  }
  
  /**
   * Salvataggio di una richiesta di assenza.
   */
  public static void save(@Required @Valid AbsenceRequest absenceRequest, boolean persist) {

    if (absenceRequest.startAt == null || absenceRequest.endTo == null) {
      Validation.addError("absenceRequest.startAt", 
          "Entrambi i campi data devono essere valorizzati");
      Validation.addError("absenceRequest.endTo", 
          "Entrambi i campi data devono essere valorizzati");
      response.status = 400;
      render("@edit", absenceRequest);
    }
    if (absenceRequest.startAt.isAfter(absenceRequest.endTo)) {
      Validation.addError("absenceRequest.startAt", 
          "La data di inizio non può essere successiva alla data di fine");      
    }
    //verifico che non esista già una richiesta (non rifiutata) 
    //di assenza che interessa i giorni richiesti
    if (!absenceRequestManager.checkAbsenceRequest(absenceRequest)) {
      Validation.addError("absenceRequest.startAt", "Esiste già una richiesta in questa data");
      Validation.addError("absenceRequest.endTo", "Esiste già una richiesta in questa data");
      response.status = 400;
      render("@edit", absenceRequest);
    }
    
    if (Validation.hasErrors()) {
      response.status = 400;
      flash.error(Web.msgHasErrors());
      render("@edit", absenceRequest);
      return;
    }
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
 
    GroupAbsenceType groupAbsenceType = absenceRequestManager.getGroupAbsenceType(absenceRequest);
    AbsenceType absenceType = null;
    AbsenceForm absenceForm =
        absenceService.buildAbsenceForm(absenceRequest.person, absenceRequest.startAtAsDate(), null,
            absenceRequest.endToAsDate(), null, groupAbsenceType, false, absenceType, 
            null, null, null, false, true);
    InsertReport insertReport = absenceService.insert(absenceRequest.person, 
        absenceForm.groupSelected, absenceForm.from, absenceForm.to,
        absenceForm.absenceTypeSelected, absenceForm.justifiedTypeSelected, 
        null, null, false, absenceManager);
    if (!persist) {
      persist = true;
      render("@edit", persist, absenceRequest, insertReport, absenceForm);
    } else {
      boolean isNewRequest = !absenceRequest.isPersistent();
      absenceRequest.save();
      
      //Avvia il flusso se necessario.
      if (isNewRequest || !absenceRequest.flowStarted) {
        absenceRequestManager.executeEvent(
            absenceRequest, absenceRequest.person, 
            AbsenceRequestEventType.STARTING_APPROVAL_FLOW, Optional.absent());
        //invio la notifica al primo che deve validare la mia richiesta 
        notificationManager
        .notificationAbsenceRequestPolicy(absenceRequest.person.user, absenceRequest, true);
        //TODO: devo anche mandare una mail con la stessa logica presente nel notificationManager.
      } 
      
      if (absenceRequest.person.user.hasRoles(Role.SEAT_SUPERVISOR)) {
        approval(absenceRequest.id);
      }
      flash.success("Operazione effettuata correttamente");
      list(absenceRequest.type);
    }
    
  }
  
  /**
   * Metodo che "pulisce" la richiesta di assenza in caso di errori prima della conferma.
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
   * @param id l'id della richiesta da approvare
   */
  public static void approval(long id) {

    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    User user = Security.getUser().get(); 
    //verifico se posso inserire l'assenza
    if (absenceRequest.managerApprovalRequired && absenceRequest.managerApproved == null
        && user.hasRoles(Role.GROUP_MANAGER)) {
      //caso di approvazione da parte del responsabile di gruppo.
      absenceRequestManager.managerApproval(id);
    }
    if (absenceRequest.administrativeApprovalRequired 
        && absenceRequest.administrativeApproved == null
        && user.hasRoles(Role.PERSONNEL_ADMIN)) {
      //caso di approvazione da parte dell'amministratore del personale
      absenceRequestManager.personnelAdministratorApproval(id);
    }
    if (absenceRequest.officeHeadApprovalRequired && absenceRequest.officeHeadApproved == null 
        && user.hasRoles(Role.SEAT_SUPERVISOR)) {
      //caso di approvazione da parte del responsabile di sede
      absenceRequestManager.officeHeadApproval(id);
    }
    notificationManager.notificationAbsenceRequestPolicy(user, absenceRequest, true);
    flash.success("Operazione conclusa correttamente");
    render("@show", absenceRequest, user);
  }

  /**
   * Dispatcher che instrada al corretto metodo l'operazione da fare sulla richiesta a 
   *     seconda dei parametri.
   * @param id l'id della richiesta di assenza
   */
  public static void disapproval(long id) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    User user = Security.getUser().get(); 
    if (absenceRequest.managerApprovalRequired && absenceRequest.managerApproved == null
        && user.hasRoles(Role.GROUP_MANAGER)) {
      //caso di approvazione da parte del responsabile di gruppo.
      absenceRequestManager.managerDisapproval(id);
      flash.error("Richiesta respinta");
      render("@show", absenceRequest, user);
    }
    if (absenceRequest.administrativeApprovalRequired 
        && absenceRequest.administrativeApproved == null
        && user.hasRoles(Role.PERSONNEL_ADMIN)) {
      //caso di approvazione da parte dell'amministratore del personale
      absenceRequestManager.personnelAdministratorDisapproval(id);
      flash.error("Richiesta respinta");
      render("@show", absenceRequest, user);
    }
    if (absenceRequest.officeHeadApprovalRequired && absenceRequest.officeHeadApproved == null 
        && user.hasRoles(Role.SEAT_SUPERVISOR)) {
      //caso di approvazione da parte del responsabile di sede
      absenceRequestManager.officeHeadDisapproval(id);
      flash.error("Richiesta respinta");
      render("@show", absenceRequest, user);
    }
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
    absenceRequestManager.executeEvent(
        absenceRequest, Security.getUser().get().person, 
        AbsenceRequestEventType.DELETE, Optional.absent());
    flash.success(Web.msgDeleted(AbsenceRequest.class));
    list(absenceRequest.type);
  }
  
  /**
   * Mostra al template la richiesta.
   * @param id l'id della richiesta da visualizzare
   */
  public static void show(long id, AbsenceRequestType type) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    notFoundIfNull(absenceRequest);
    rules.checkIfPermitted(absenceRequest);
    User user = Security.getUser().get();
    render(absenceRequest, type, user);
  }
}
