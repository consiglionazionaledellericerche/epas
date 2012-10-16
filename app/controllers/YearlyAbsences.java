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
	
	@Check(Security.VIEW_PERSON_LIST)
	public static void show(Long personId, int year, int month) {

		
    	Integer anno = params.get("year", Integer.class);
    	//Long personId = params.get("personId", Long.class);
    	Logger.debug("L'id della persona è: %s", personId);
    	Person person = Person.findById(personId);
    	Logger.info("Anno: "+anno);
    	
    	if(anno==null){
    		        	
        	LocalDate now = new LocalDate();
        	YearRecap yearRecap = YearRecap.byPersonAndYear(person, (short)now.getYear());
            render(yearRecap);
    	}
    	else{
    		Logger.info("Sono dentro il ramo else della creazione del month recap");
    		//Integer year = new Integer(params.get("year"));
			MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year, month);
    		YearRecap yearRecap = YearRecap.byPersonAndYear(person, (short)year);
    		    		
            render(yearRecap, monthRecap);
    	}
    	
    }
	
//	@Check(Security.VIEW_PERSON_LIST)
//	public static void show() {
//		
//    	show(Security.getPerson());
//    }
}
