package controllers;

import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import models.Person;
import models.PersonDay;
import models.YearRecap;
import models.rendering.VacationsRecap;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Vacations extends Controller{
	
	/* corrisponde alla voce di menu selezionata */
	private final static ActionMenuItem actionMenuItem = ActionMenuItem.vacations;
		
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void show(Long personId, Integer anno) {
		//String menuItem = actionMenuItem.toString();											//inutile
		
		//controllo dei parametri
		Person person = null;
		if(personId != null)
		{
			person = Person.findById(personId);
			Logger.debug("Dati persona: %s %s",person.name, person.surname);
		}
		else
			person = Security.getPerson();
    	if(anno==null)
			anno = new LocalDate().getYear(); //default l'anno corrente

    	//Costruzione oggetto di riepilogo per la persona
    	VacationsRecap vacationsRecap = new VacationsRecap(person, (short)anno.intValue());
    	if(vacationsRecap.vacationPeriodList==null)
    	{
    		Logger.debug("Period e' null");
    		flash.error("Piano ferie inesistente per %s %s", person.name, person.surname);
    		render(vacationsRecap);
    	}
    	
    	//rendering
       	render(vacationsRecap);
    
    	
    }
	
//	@Check(Security.VIEW_PERSONAL_SITUATION)
//	public static void show() {
//    	show(Security.getPerson());
//    }
	
	public static void vacations(Long personId, Integer anno){
    	//String anno = params.get("year");
    	Person person = null;
    	if(personId != null)
    		person = Person.findById(personId);
    	else
    		person = Security.getPerson();
		
		Logger.trace("Anno: "+anno);
    	
    	if(anno==null){
    		        	
        	LocalDate now = new LocalDate();
        	YearRecap yearRecap = YearRecap.byPersonAndYear(person, (short)now.getYear());
            render(yearRecap);
    	}
    	else{
    		Logger.info("Sono dentro il ramo else della creazione del month recap");
    		//Integer year = new Integer(params.get("year"));
			
    		YearRecap yearRecap = YearRecap.byPersonAndYear(person, (short)anno.intValue());
    		    		
            render(yearRecap);
    	}
	}
	
//	public static void vacations() {
//    	vacations(Security.getPerson());
//    }
	
	//@Check({Security.VIEW_PERSONAL_SITUATION, Security.INSERT_AND_UPDATE_VACATIONS})
	public static void vacationsCurrentYear(Long personId, Integer anno){
		Person person = null;
		if(personId != null)
    		person = Person.findById(personId);
    	else
    		person = Security.getPerson();
		//String year = params.get("year");
    	Logger.trace("Anno: "+anno);
    	
    	//Costruzione oggetto di riepilogo per la persona
    	VacationsRecap vacationsRecap = new VacationsRecap(person, (short)anno.intValue());
    	if(vacationsRecap.vacationPeriodList==null)
    	{
    		Logger.debug("Period e' null");
    		flash.error("Piano ferie inesistente per %s %s", person.name, person.surname);
    		render(vacationsRecap);
    	}
    	
    	//rendering
       	render(vacationsRecap);
	}
	
//	@Check({Security.VIEW_PERSONAL_SITUATION, Security.INSERT_AND_UPDATE_VACATIONS})
//	public static void vacationsCurrentYear() {
//    	vacationsCurrentYear(Security.getPerson());
//    }
	
	//@Check({Security.VIEW_PERSONAL_SITUATION, Security.INSERT_AND_UPDATE_VACATIONS})
	public static void vacationsLastYear(Long personId, Integer anno){
		Person person = null;
    	if(personId != null)
    		person = Person.findById(personId);
    	else
    		person = Security.getPerson();
    	
    	//Costruzione oggetto di riepilogo per la persona
    	VacationsRecap vacationsRecap = new VacationsRecap(person, (short)anno.intValue());
    	if(vacationsRecap.vacationPeriodList==null)
    	{
    		Logger.debug("Period e' null");
    		flash.error("Piano ferie inesistente per %s %s", person.name, person.surname);
    		render(vacationsRecap);
    	}
    	
    	//rendering
       	render(vacationsRecap);
	}
	
}
