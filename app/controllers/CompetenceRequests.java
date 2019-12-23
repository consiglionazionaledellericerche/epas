package controllers;

import javax.inject.Inject;
import com.google.common.base.Verify;
import dao.GroupDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.NotificationManager;
import manager.configurations.ConfigurationManager;
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
  static SecurityRules rules;
  @Inject
  static NotificationManager notificationManager;
  @Inject
  static GroupDao groupDao;
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static IWrapperFactory wrapperFactory;
  
  public static void overtimes() {
    list(CompetenceRequestType.OVERTIME_REQUEST);
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
  }
}
