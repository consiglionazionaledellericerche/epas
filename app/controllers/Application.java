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
    	if (Security.check(Security.VIEW_PERSONAL_SITUATION)) {
    		Stampings.stampings(null, null);
    	} else {
    		Application.indexAdmin();
    	}
    }
	public static void success(){
			
		render();
	}
    
    
}

