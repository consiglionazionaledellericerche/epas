package controllers;

import it.cnr.iit.epas.ActionMenuItem;
import models.MonthRecap;
import models.Person;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;

public class Competences extends Controller{

	/* corrisponde alla voce di menu selezionata */
	private final static ActionMenuItem actionMenuItem = ActionMenuItem.competences;
	
	@Before
    static void checkPerson() {
		if (!Security.isConnected()) {
            flash.error("Please log in first");
            Application.index();
        }
    }
	
	private static void show(Person person) {
		String menuItem = actionMenuItem.toString();
		
    	String anno = params.get("year");
    	Logger.info("Anno: "+anno);
    	String mese= params.get("month");
    	Logger.info("Mese: "+mese);
    	if(anno==null || mese==null){
    		        	
        	LocalDate now = new LocalDate();
        	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
            render(monthRecap, menuItem);
    	}
    	else{
    		Logger.info("Sono dentro il ramo else della creazione del month recap");
    		Integer year = new Integer(params.get("year"));
			Integer month = new Integer(params.get("month"));
    		MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year.intValue(), month.intValue());
    		Logger.info("Il month recap è formato da: " +person.id+ ", " +year.intValue()+ ", " +month.intValue());
    		
            render(monthRecap, menuItem);
    	}
    	
    }
	
	public static void show() {
    	show(Security.getPerson());
    }
	
}
