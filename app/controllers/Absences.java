package controllers;

import models.MonthRecap;
import models.Person;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;

public class Absences extends Controller{
	
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
    	String mese= params.get("month");
    	Logger.info("Mese: "+mese);
    	if(anno==null || mese==null){
    		        	
        	LocalDate now = new LocalDate();
        	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
            render(monthRecap);
    	}
    	else{
    		Logger.info("Sono dentro il ramo else della creazione del month recap");
    		Integer year = new Integer(params.get("year"));
			Integer month = new Integer(params.get("month"));
    		MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year.intValue(), month.intValue());
    		Logger.info("Il month recap è formato da: " +person.id+ ", " +year.intValue()+ ", " +month.intValue());
    		
            render(monthRecap);
    	}
    	
    }
	
	public static void show() {
    	show(Long.parseLong(session.get(Application.PERSON_ID_SESSION_KEY)));
    }

}
