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
		
		//inizializzazione functional menu
		//month
		String monthSelected = session.get("monthSelected");
		if(monthSelected!=null && !monthSelected.equals("")){
			session.put("monthSelected", monthSelected);
		}
		else{
			session.put("monthSelected", new LocalDate().getMonthOfYear());
		}
		
		//year
		String yearSelected = session.get("yearSelected");
		if(yearSelected!=null && !yearSelected.equals("")){
			session.put("yearSelected", yearSelected);
		}
		else{
			session.put("yearSelected", new LocalDate().getYear());
		}
		
		//person
		String personSelected = session.get("personSelected");
		if(personSelected!=null && !personSelected.equals("")){
			session.put("personSelected", personSelected);
		}
		else{
			session.put("personSelected", Security.getPerson().id);
		}
		
		//method
		String methodSelected = session.get("methodSelected");
		if(methodSelected!=null && !methodSelected.equals(""))
		{
			Logger.info("put1: %s ", methodSelected);
			session.put("methodSelected", methodSelected);
		}
		else
		{
			Logger.info("put2: %s", methodSelected);
			session.put("methodSelected", "stampingsAdmin");
		}
		
		render();
       
    }
    
	
    public static void index() {
    	if(Security.getPerson().username.equals("epas.clocks")){
    		Clocks.show();
    		return;
    	}
    	if(Security.getPerson().username.equals("admin")){
    		Persons.list();
    		return;
    	}
		
    	//inizializzazione functional menu dopo login
    	
		session.put("monthSelected", new LocalDate().getMonthOfYear());
		session.put("yearSelected", new LocalDate().getYear());
		session.put("personSelected", Security.getPerson().id);
		
		//method
    	if (Security.check(Security.INSERT_AND_UPDATE_STAMPING)) {
    		Logger.info("put3:%s ", "stampingsAdmin");
    		session.put("methodSelected", "stampingsAdmin");
    		Application.indexAdmin();
    	} else {
    		Logger.info("put4:%s ", "stampings");
    		session.put("methodSelected", "stampings");
    		Stampings.stampings(new LocalDate().getYear(), new LocalDate().getMonthOfYear());
    		
    	}
    }
    
    
	public static void success(){
			
		render();
	}
    
    
}

