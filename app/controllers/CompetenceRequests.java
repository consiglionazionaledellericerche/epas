package controllers;

import java.util.List;
import javax.inject.Inject;
import org.joda.time.LocalDateTime;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import dao.CompetenceRequestDao;
import dao.GroupDao;
import dao.PersonDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperFactory;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.NotificationManager;
import manager.configurations.ConfigurationManager;
import manager.flows.CompetenceRequestManager;
import models.UsersRolesOffices;
import models.flows.AbsenceRequest;
import models.flows.CompetenceRequest;
import models.flows.Group;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.enumerate.CompetenceRequestType;
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
  
  
  
  public static void overtimes() {
    list(CompetenceRequestType.OVERTIME_REQUEST);
  }
  
  public static void overtimesToApprove() {
    listToApprove(CompetenceRequestType.OVERTIME_REQUEST);
  }
  
  /**
   * Lista delle richieste di straordinario dell'utente corrente.
   * @param type
   */
  public static void list(CompetenceRequestType type) {
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
    log.debug("Prelevo le richieste di tipo {} per {} a partire da {}",
        type, person, fromDate);
    
    val config = competenceRequestManager.getConfiguration(type, person);
    List<CompetenceRequest> myResults = competenceRequestDao
        .findByPersonAndDate(person, fromDate, Optional.absent(), type, true);
    List<CompetenceRequest> closed = competenceRequestDao
        .findByPersonAndDate(person, fromDate, Optional.absent(), type, false);
    val onlyOwn = true;
    boolean persist = false;

    render(config, myResults, type, onlyOwn, persist, closed);
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
    List<Group> groups = groupDao.groupsByOffice(person.office, Optional.absent());
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
}
