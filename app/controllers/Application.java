package controllers;

import dao.OfficeDao;
import dao.wrapper.IWrapperFactory;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import javax.inject.Inject;

@With({Resecure.class, RequestInit.class})
public class Application extends Controller {

  @Inject
  static OfficeDao officeDao;
  @Inject
  static IWrapperFactory wrapperFactory;

  public static void indexAdmin() {
    Logger.debug("chiamato metodo indexAdmin dell'Application controller");
    render();

  }

  public static void index() {

//    	Utenti di sistema (developer,admin)
    if (Security.getUser().get().person == null) {

      Persons.list(null);
      return;
    }

    //inizializzazione functional menu dopo login
    session.put("monthSelected", new LocalDate().getMonthOfYear());
    session.put("yearSelected", new LocalDate().getYear());
    session.put("personSelected", Security.getUser().get().person.id);

    session.put("methodSelected", "stampingsAdmin");
    session.put("actionSelected", "Stampings.stampings");

    Stampings.stampings(new LocalDate().getYear(), new LocalDate().getMonthOfYear());
  }

}

