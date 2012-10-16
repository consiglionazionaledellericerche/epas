package controllers;

import it.cnr.iit.epas.ActionMenuItem;
import models.MonthRecap;
import models.Person;
import models.YearRecap;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class YearlyAbsences extends Controller{

	/* corrisponde alla voce di menu selezionata */
//	private final static ActionMenuItem actionMenuItem = ActionMenuItem.yearlyAbsences;
	
	@Check(Security.VIEW_PERSON_LIST)
	private static void show(Person person) {
//		String menuItem = actionMenuItem.toString();
		
    	Integer anno = params.get("year", Integer.class);
    	Long personId = params.get("personId", Long.class);
    	Logger.debug("L'id della persona è: %s", personId);
    	Logger.info("Anno: "+anno);
    	
    	if(anno==null){
    		        	
        	LocalDate now = new LocalDate();
        	YearRecap yearRecap = YearRecap.byPersonAndYear(person, (short)now.getYear());
            render(yearRecap);
    	}
    	else{
    		Logger.info("Sono dentro il ramo else della creazione del month recap");
    		Integer year = new Integer(params.get("year"));
			
    		YearRecap yearRecap = YearRecap.byPersonAndYear(person, (short)year.intValue());
    		    		
            render(yearRecap);
    	}
    	
    }
	
	@Check(Security.VIEW_PERSON_LIST)
	public static void show() {
    	show(Security.getPerson());
    }
}
