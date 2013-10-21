package controllers;

import it.cnr.iit.epas.ActionMenuItem;
import it.cnr.iit.epas.MainMenu;

import java.util.List;

import org.joda.time.LocalDate;

import models.Permission;
import models.Person;
import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Application extends Controller {
    
    public static void indexAdmin() {
		Logger.debug("chiamato metodo indexAdmin dell'Application controller");
       	render();
    }
    
	
    public static void index() {
    	if(Security.getPerson().username.equals("admin")){
    		Persons.list();
    		return;
    	}
		
		session.put("monthSelected", new LocalDate().getMonthOfYear());
		session.put("yearSelected", new LocalDate().getYear());
		session.put("personSelected", Security.getPerson().id);
    	if (Security.check(Security.INSERT_AND_UPDATE_STAMPING)) {
    		Application.indexAdmin();
    		session.put("methodSelected", ActionMenuItem.stampingsAdmin.getDescription());
    	} else {
    		Stampings.stampings(null, null);
    		session.put("methodSelected", ActionMenuItem.stampings.getDescription());
    	}
    }
	public static void success(){
			
		render();
	}
    
    
}

