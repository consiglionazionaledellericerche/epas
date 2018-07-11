package controllers;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import dao.AbsenceRequestDao;
import dao.PersonDao;
import dao.RoleDao;

import helpers.Web;
import helpers.jpa.ModelQuery.SimpleResults;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.flows.AbsenceRequestManager;
import models.Person;
import models.Role;
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
    val results = absenceRequestDao.findByPersonAndDate(person, fromDate, Optional.absent(), type);
    val onlyOwn = true;
    
    render(config, results, type, onlyOwn);
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
    log.debug("Prelevo le richieste di assenze di tipo {} per {} a partire da {}", 
        type, person, fromDate);   
    
    Role role = null;
    if (person.user.hasRoles(Role.GROUP_MANAGER)) {
      role = roleDao.getRoleByName(Role.GROUP_MANAGER);
    }
    if (person.user.hasRoles(Role.PERSONNEL_ADMIN)) {      
      role = roleDao.getRoleByName(Role.PERSONNEL_ADMIN);      
    }
    if (person.user.hasRoles(Role.SEAT_SUPERVISOR)) {
      role = roleDao.getRoleByName(Role.SEAT_SUPERVISOR);      
    }
    val results = absenceRequestDao.findRequestsToApprove(role, fromDate, Optional.absent(), type);
    val config = absenceRequestManager.getConfiguration(type, person);  
    val onlyOwn = false;
    
    render(config, results, type, onlyOwn);
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
    
    render("@edit", absenceRequest);

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
  public static void save(@Required @Valid AbsenceRequest absenceRequest) {

    if (Validation.hasErrors()) {
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
       
    boolean isNewRequest = !absenceRequest.isPersistent();
    absenceRequest.save();

    //Avvia il flusso se necessario.
    if (isNewRequest || !absenceRequest.flowStarted) {
      log.info("absenceRequest.person = {}", absenceRequest.person);
      absenceRequestManager.executeEvent(
          absenceRequest, absenceRequest.person, 
          AbsenceRequestEventType.STARTING_APPROVAL_FLOW, Optional.absent());      
    } 

    list(absenceRequest.type);
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
    flash.success(Web.msgDeleted(AbsenceRequest.class));
    absenceRequest.delete();
    list(absenceRequest.type);
  }
}
