package controllers;

import it.cnr.iit.epas.ActionMenuItem;
import models.Person;
import models.YearRecap;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;

public class Vacations extends Controller{
	
	/* corrisponde alla voce di menu selezionata */
	private final static ActionMenuItem actionMenuItem = ActionMenuItem.vacations;
	
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
    	
    	if(anno==null){
    		        	
        	LocalDate now = new LocalDate();
        	YearRecap yearRecap = YearRecap.byPersonAndYear(person, (short)now.getYear());
            render(yearRecap, menuItem);
    	}
    	else{
    		Logger.info("Sono dentro il ramo else della creazione del month recap");
    		Integer year = new Integer(params.get("year"));
			
    		YearRecap yearRecap = YearRecap.byPersonAndYear(person, (short)year.intValue());
    		    		
            render(yearRecap, menuItem);
    	}
    	
    }
	
	public static void show() {
    	show(Security.getPerson());
    }
	
	private static void vacations(Person person){
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
	
	public static void vacations() {
    	vacations(Security.getPerson());
    }
	
	private static void vacationsCurrentYear(Person person){
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
	
	public static void vacationsCurrentYear() {
    	vacationsCurrentYear(Security.getPerson());
    }
	
}
