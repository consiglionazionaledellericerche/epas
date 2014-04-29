package controllers;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.SessionEpas;
import models.User;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, RequestInit.class} )
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
    		    		
    		session.put("methodSelected", "stampingsAdmin");
    		session.put("actionSelected", "Stampings.stampings");
    		Stampings.stampings(new LocalDate().getYear(), new LocalDate().getMonthOfYear());

    	} else {
    		
    		session.put("methodSelected", "stampings");
    		session.put("actionSelected", "Stampings.stampings");
    		Stampings.stampings(new LocalDate().getYear(), new LocalDate().getMonthOfYear());
    	}
    	
    }
    
    
    
}

