package controllers;

import models.Person;
import models.YearRecap;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;

public class Vacations extends Controller{
	
	@Before
    static void checkPerson() {
        if(session.get(Application.PERSON_ID_SESSION_KEY) == null) {
            flash.error("Please log in first");
            Application.index();
        }
    }
	
	private static void show(Long id) {
    	Person person = Person.findById(id);
    	String anno = params.get("year");
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
	
	public static void show() {
    	show(Long.parseLong(session.get(Application.PERSON_ID_SESSION_KEY)));
    }
	
}
