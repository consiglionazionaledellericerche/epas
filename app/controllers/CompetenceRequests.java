
package controllers;

import java.util.List;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import dao.CompetenceRequestDao;
import dao.GroupDao;
import dao.PersonDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperFactory;
import helpers.Web;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.NotificationManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.flows.CompetenceRequestManager;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.flows.AbsenceRequest;
import models.flows.CompetenceRequest;
import models.flows.Group;
import models.flows.enumerate.AbsenceRequestEventType;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.enumerate.CompetenceRequestEventType;
import models.flows.enumerate.CompetenceRequestType;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

/**
 * Controller per la gestione delle richieste di straordinario dei dipendenti.
 * 
 * @author dario
 *
 */
@Slf4j
@With(Resecure.class)
public class CompetenceRequests extends Controller{

  @Inject
  static PersonDao personDao;
  @Inject
  static CompetenceRequestManager competenceRequestManager;
  @Inject
  static SecurityRules rules;
  @Inject
  static NotificationManager notificationManager;
  @Inject
  static GroupDao groupDao;
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static CompetenceRequestDao competenceRequestDao;
  @Inject
  static UsersRolesOfficesDao uroDao;
  @Inject
  private static PersonStampingRecapFactory stampingsRecapFactory;
  

  public static void changeReperibility() {
    list(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST);
  }

  public static void changeReperibilityToApprove() {
    listToApprove(CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST);
  }

  /**
   * Lista delle richieste di straordinario dell'utente corrente.
   * @param type
   */
  public static void list(CompetenceRequestType competenceType) {
    Verify.verifyNotNull(competenceType);

    val currentUser = Security.getUser().get();
    if (currentUser.person == null) {
      flash.error("L'utente corrente non ha associata una persona, non può vedere le proprie "
          + "richieste di assenza");
      Application.index();
      return;
    }
    val person = currentUser.person;
    val fromDate = LocalDateTime.now().dayOfYear().withMinimumValue();
    log.debug("Prelevo le richieste di tipo {} per {} a partire da {}",
        competenceType, person, fromDate);

    val config = competenceRequestManager.getConfiguration(competenceType, person);
    List<CompetenceRequest> myResults = competenceRequestDao
        .findByPersonAndDate(person, fromDate, Optional.absent(), competenceType, true);
    List<CompetenceRequest> closed = competenceRequestDao
        .findByPersonAndDate(person, fromDate, Optional.absent(), competenceType, false);
    val onlyOwn = true;
    boolean persist = false;

    render(config, myResults, competenceType, onlyOwn, persist, closed);
  }

  /**
   * Lista delle richieste di assenza da approvare da parte dell'utente corrente.
   *
   * @param type tipo opzionale di tipo di richiesta di assenza.
   */
  public static void listToApprove(CompetenceRequestType type) {
    Verify.verifyNotNull(type);

    val person = Security.getUser().get().person;
    val fromDate = LocalDateTime.now().dayOfYear().withMinimumValue();
    log.debug("Prelevo le richieste da approvare di assenze di tipo {} a partire da {}",
        type, fromDate);
    List<Group> groups = groupDao.groupsByOffice(person.office, Optional.absent(), Optional.absent());
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(person.user);
    List<CompetenceRequest> results = competenceRequestDao
        .allResults(roleList, fromDate, Optional.absent(), type, groups, person);
    List<CompetenceRequest> myResults =
        competenceRequestDao.toApproveResults(roleList, fromDate, Optional.absent(),
            type, groups, person);
    List<CompetenceRequest> approvedResults =
        competenceRequestDao
        .totallyApproved(roleList, fromDate, Optional.absent(), type, groups, person);
    val config = competenceRequestManager.getConfiguration(type, person);
    val onlyOwn = false;
    render(config, results, type, onlyOwn, approvedResults, myResults);
  }

  public static void blank(Optional<Long> personId, int year, int month, CompetenceRequestType competenceType) {
    Verify.verifyNotNull(competenceType);
    Person person;
    if (personId.isPresent()) {
      rules.check("CompetenceRequests.blank4OtherPerson");
      person = personDao.getPersonById(personId.get());
    } else {
      if (Security.getUser().isPresent() && Security.getUser().get().person != null) {
        person = Security.getUser().get().person;
      } else {
        flash.error("L'utente corrente non ha associato una persona.");
        list(competenceType);
        return;
      }
    }
    notFoundIfNull(person);

    val configurationProblems = competenceRequestManager.checkconfiguration(competenceType, person);
    if (!configurationProblems.isEmpty()) {
      flash.error(Joiner.on(" ").join(configurationProblems));
      list(competenceType);
      return;
    }

    val competenceRequest = new CompetenceRequest();
    competenceRequest.type = competenceType;
    competenceRequest.person = person;
    PersonStampingRecap psDto = null;
    boolean isOvertime = false;
    if (competenceType.equals(CompetenceRequestType.OVERTIME_REQUEST)) {
      isOvertime = true;
      psDto = stampingsRecapFactory.create(person,
          year, month, true);  
    }
    competenceRequest.startAt = competenceRequest.endTo = LocalDateTime.now().plusDays(1);
    render("@edit", psDto, competenceRequest, isOvertime, competenceType, year, month);
  }

  public static void edit(CompetenceRequest competenceRequest, int year, int month) {

    rules.checkIfPermitted(competenceRequest);
    boolean insertable = true;
    
    render(competenceRequest, insertable);
    
  }

  public static void save(@Required @Valid CompetenceRequest competenceRequest, 
      Integer value, int year, int month) {
    log.debug("CompetenceRequest.startAt = {}", competenceRequest.startAt);

    if (!Security.getUser().get().person.equals(competenceRequest.person)) {
      rules.check("CompetenceRequests.blank4OtherPerson");
    } 

    notFoundIfNull(competenceRequest.person);
        
    competenceRequest.year = year;
    competenceRequest.month = month;    
    competenceRequest.startAt = LocalDateTime.now();
        
    CompetenceRequest existing = competenceRequestManager.checkCompetenceRequest(competenceRequest);
    if (existing != null) {
      Validation.addError("competenceRequest.value", "Esiste già una richiesta di questo tipo per questo anno/mese");
      response.status = 400;      
      render("@edit", competenceRequest, existing);
    }
    if (!competenceRequest.person.checkLastCertificationDate(
        new YearMonth(competenceRequest.year,
            competenceRequest.month))) {
      Validation.addError("competenceRequest.value",
          "Non è possibile fare una richiesta per una data di un mese già processato in Attestati");
      response.status = 400;      
      render("@edit", competenceRequest);
    }
    
//    if (Validation.hasErrors()) {
//      response.status = 400;      
//      flash.error(Web.msgHasErrors());
//      render("@edit", competenceRequest);
//      return;
//    }
//    
    competenceRequestManager.configure(competenceRequest);

    if (competenceRequest.endTo == null) {
      competenceRequest.endTo = competenceRequest.startAt;
    }

    boolean isNewRequest = !competenceRequest.isPersistent();
    competenceRequest.save();

    //Avvia il flusso se necessario.
    if (isNewRequest || !competenceRequest.flowStarted) {
      competenceRequestManager.executeEvent(
          competenceRequest, competenceRequest.person,
          CompetenceRequestEventType.STARTING_APPROVAL_FLOW, Optional.absent());

      //invio la notifica al primo che deve validare la mia richiesta 
      notificationManager
      .notificationCompetenceRequestPolicy(competenceRequest.person.user, competenceRequest, true);
      // invio anche la mail
      notificationManager
      .sendEmailCompetenceRequestPolicy(competenceRequest.person.user, competenceRequest, true);


    }
    flash.success("Operazione effettuata correttamente");

    CompetenceRequests.list(competenceRequest.type);
  }

  public static void delete(long id) {
    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    notFoundIfNull(competenceRequest);
    rules.checkIfPermitted(competenceRequest);
    competenceRequestManager.executeEvent(
        competenceRequest, Security.getUser().get().person,
        CompetenceRequestEventType.DELETE, Optional.absent());
    flash.success(Web.msgDeleted(AbsenceRequest.class));
    list(competenceRequest.type);
  }

  public static void show(long id, CompetenceRequestType type) {
    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    notFoundIfNull(competenceRequest);
    rules.checkIfPermitted(competenceRequest);
    User user = Security.getUser().get();
    boolean disapproval = false;
    render(competenceRequest, type, user, disapproval);
  }
  
  public static void approval(long id, boolean approval) {
    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    User user = Security.getUser().get();
    if (!approval) {
      approval = true;
      render(competenceRequest, approval);
    }
    if (competenceRequest.managerApprovalRequired && competenceRequest.managerApproved == null
        && user.hasRoles(Role.GROUP_MANAGER)) {
      //caso di approvazione da parte del responsabile di gruppo.
      competenceRequestManager.managerApproval(id, user);
      if (user.usersRolesOffices.stream()
          .anyMatch(uro -> uro.role.name.equals(Role.SEAT_SUPERVISOR))
          && competenceRequest.officeHeadApprovalRequired) {
        // se il responsabile di gruppo è anche responsabile di sede faccio un'unica approvazione
        competenceRequestManager.officeHeadApproval(id, user);
      }
    }
    if (competenceRequest.officeHeadApprovalRequired && competenceRequest.officeHeadApproved == null
        && user.hasRoles(Role.SEAT_SUPERVISOR)) {
      //caso di approvazione da parte del responsabile di sede
      competenceRequestManager.officeHeadApproval(id, user);
    }
    notificationManager.sendEmailToUser(Optional.<AbsenceRequest>absent(), 
        Optional.of(competenceRequest));
    flash.success("Operazione conclusa correttamente");
    CompetenceRequests.listToApprove(competenceRequest.type);
  }
  
  public static void disapproval(long id, boolean disapproval, String reason) {
    CompetenceRequest competenceRequest = CompetenceRequest.findById(id);
    User user = Security.getUser().get();
    if (!disapproval) {
      disapproval = true;
      render(competenceRequest, disapproval);
    }
    if (competenceRequest.managerApprovalRequired && competenceRequest.managerApproved == null
        && user.hasRoles(Role.GROUP_MANAGER)) {
      //caso di approvazione da parte del responsabile di gruppo.
      competenceRequestManager.managerDisapproval(id, reason);
      flash.error("Richiesta respinta");
      render("@show", competenceRequest, user);
    }
    if (competenceRequest.officeHeadApprovalRequired && competenceRequest.officeHeadApproved == null
        && user.hasRoles(Role.SEAT_SUPERVISOR)) {
      //caso di approvazione da parte del responsabile di sede
      competenceRequestManager.officeHeadDisapproval(id, reason);
      flash.error("Richiesta respinta");
      render("@show", competenceRequest, user);
    }
    render("@show", competenceRequest, user);
  }
}
