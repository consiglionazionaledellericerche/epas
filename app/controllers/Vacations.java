package controllers;

import models.Contract;
import models.Person;
import models.rendering.VacationsRecap;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, RequestInit.class} )
public class Vacations extends Controller{
		
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void show(Long personId, Integer anno) {

		//controllo dei parametri
		Person person = Security.getSelfPerson(personId);
		if( person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
			return;
		}
		
		//default l'anno corrente
    	if(anno==null)
			anno = new LocalDate().getYear(); 

		Contract contract = person.getCurrentContract();
    	
    	VacationsRecap vacationsRecap = null;
    	try { 
    		vacationsRecap = new VacationsRecap(person, anno, contract, new LocalDate(), true);
    	} catch(IllegalStateException e) {
    		flash.error("Impossibile calcolare la situazione ferie. Definire i dati di inizializzazione per %s %s.", person.name, person.surname);
    		renderTemplate("Application/indexAdmin.html");
    		return;
    	}
    	
    	if(vacationsRecap.vacationPeriodList==null)
    	{
    		Logger.debug("Period e' null");
    		flash.error("Piano ferie inesistente per %s %s", person.name, person.surname);
    		render(vacationsRecap);
    	}
    	
    	//rendering
       	render(vacationsRecap);
    
    	
    }
	

	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void vacationsCurrentYear(Long personId, Integer anno){
		
		Person person = Security.getSelfPerson(personId);
		if( person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
			return;
		}

    	//Costruzione oggetto di riepilogo per la persona
		Contract contract = person.getCurrentContract();
		
		VacationsRecap vacationsRecap = null;
    	try { 
    		vacationsRecap = new VacationsRecap(person, anno, contract, new LocalDate(), true);
    	} catch(IllegalStateException e) {
    		flash.error("Impossibile calcolare la situazione ferie. Definire i dati di inizializzazione per %s %s.", person.name, person.surname);
    		renderTemplate("Application/indexAdmin.html");
    		return;
    	}
		    	
    	if(vacationsRecap.vacationPeriodList==null)
    	{
    		Logger.debug("Period e' null");
    		flash.error("Piano ferie inesistente per %s %s", person.name, person.surname);
    		render(vacationsRecap);
    	}
    	
    	//rendering
       	render(vacationsRecap);
	}
	

	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void vacationsLastYear(Long personId, Integer anno){
		
		Person person = Security.getSelfPerson(personId);
		if( person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
			return;
		}
    	
    	//Costruzione oggetto di riepilogo per la persona
    	Contract contract = person.getCurrentContract();
    	
    	VacationsRecap vacationsRecap = null;
    	try { 
    		vacationsRecap = new VacationsRecap(person, anno, contract, new LocalDate(), true);
    	} catch(IllegalStateException e) {
    		flash.error("Impossibile calcolare la situazione ferie. Definire i dati di inizializzazione per %s %s.", person.name, person.surname);
    		renderTemplate("Application/indexAdmin.html");
    		return;
    	}
    	
    	if(vacationsRecap.vacationPeriodList==null)
    	{
    		Logger.debug("Period e' null");
    		flash.error("Piano ferie inesistente per %s %s", person.name, person.surname);
    		render(vacationsRecap);
    	}
    	
    	//rendering
       	render(vacationsRecap);
	}
	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void permissionCurrentYear(Long personId, Integer anno){
		
		Person person = Security.getSelfPerson(personId);
		if( person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
			return;
		}
		
    	//Costruzione oggetto di riepilogo per la persona
		Contract contract = person.getCurrentContract();
		
    	VacationsRecap vacationsRecap = null;
    	try { 
    		vacationsRecap = new VacationsRecap(person, anno, contract, new LocalDate(), true);
    	} catch(IllegalStateException e) {
    		flash.error("Impossibile calcolare la situazione ferie. Definire i dati di inizializzazione per %s %s.", person.name, person.surname);
    		renderTemplate("Application/indexAdmin.html");
    		return;
    	}
    	
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
