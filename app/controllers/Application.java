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
    	if(Security.getUser().username.equals("epas.clocks")){
    		Clocks.show();
    		return;
    	}
    	if(Security.getUser().username.equals("admin")){
    		Persons.list(null);
    		return;
    	}
		
    	//inizializzazione functional menu dopo login
    	
		session.put("monthSelected", new LocalDate().getMonthOfYear());
		session.put("yearSelected", new LocalDate().getYear());
		session.put("personSelected", Security.getUser().person.id);
		
		//method
    	if (Security.check(Security.INSERT_AND_UPDATE_STAMPING)) {
    		Logger.debug("put3:%s ", "stampingsAdmin");
    		session.put("methodSelected", "stampingsAdmin");
    		Application.indexAdmin();
    	} else {
    		Logger.debug("put4:%s ", "stampings");
    		session.put("methodSelected", "stampings");
    		Stampings.stampings(new LocalDate().getYear(), new LocalDate().getMonthOfYear());
    		
    	}
    }
    
    
	public static void success(){
			
		render();
	}
    
    
}

