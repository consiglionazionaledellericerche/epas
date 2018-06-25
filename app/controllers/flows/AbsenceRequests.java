package controllers.flows;

import controllers.Resecure;
import helpers.Web;
import javax.validation.Valid;
import models.flows.AbsenceRequest;
import models.flows.enumerate.AbsenceRequestType;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione delle richieste di assenza dei dipendenti.
 * 
 * @author cristian
 *
 */
@With(Resecure.class)
public class AbsenceRequests extends Controller {

  /**
   * Form per la richiesta di assenza da parte del dipendente. 
   */
  public static void blank(AbsenceRequestType type) {
    todo();
  }

  /**
   * Salvataggio di una richiesta di assenza.
   */
  public static void save(@Required @Valid AbsenceRequest absenceRequest) {
    
    if (Validation.hasErrors()) {
      flash.error(Web.msgHasErrors());
      render("@edit", absenceRequest);
    } else {
      
    }
    
  }
  
}
