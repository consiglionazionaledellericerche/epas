package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import dao.AbsenceRequestDao;
import dao.PersonDao;
import helpers.Web;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.flows.AbsenceRequestManager;
import models.Person;
import models.flows.AbsenceRequest;
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
  
  public static void vacations() {
    list(AbsenceRequestType.VACATION_REQUEST);
  }

  public static void compensatoryRests() {
    list(AbsenceRequestType.COMPENSATORY_REST);
  }

  public static void shortTermPermit() {
    list(AbsenceRequestType.SHORT_TERM_PERMIT);
  }

  /**
   * Lista delle richieste di assenza dell'utente corrente. 
   * @param type tipo opzionale di tipo di richiesta di assenza.
   * 
   */
  public static void list(AbsenceRequestType type) {
    Verify.verifyNotNull(type);

    val person = Security.getUser().get().person; 
    val fromDate = LocalDateTime.now();
    val results = absenceRequestDao.findByPersonAndDate(person, fromDate, Optional.absent(), type);
    render(results, type);
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
    //TODO: verificare che l'utente corrente possa inserire richieste di assenza per altri utenti
    if (personId.isPresent()) {
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
  public static void edit(int id) {
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
    log.info("AbsenceRequest.startAt = {}", absenceRequest.startAt);
    
    if (!Security.getUser().get().person.equals(absenceRequest.person)) {
      //TODO verificare se è l'utente corrente ha i permessi di inserire le richieste di assenza
      //non sue
      forbidden();
    } else {
      absenceRequest.person = Security.getUser().get().person;
    }

    notFoundIfNull(absenceRequest.person);      
    absenceRequestManager.configure(absenceRequest);
    absenceRequest.save();
    list(absenceRequest.type);
  }  


  /**
   * Form di modifica di una richiesta di assenza.
   * 
   * @param id id della richiesta di assenza da modificare.
   */
  public static void delete(int id) {
    AbsenceRequest absenceRequest = AbsenceRequest.findById(id);
    notFoundIfNull(absenceRequest);
    rules.checkIfPermitted(absenceRequest);
    
    todo();
  }
}
