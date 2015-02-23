package controllers;

import models.Person;
import models.User;
import models.rendering.VacationsRecap;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;

import dao.ContractDao;
import dto.VacationsShowDto;

@With( {Resecure.class, RequestInit.class} )
public class Vacations extends Controller{
		
	
	public static void show(Integer year) {
		
		//Controllo parametri
		Optional<User> currentUser = Security.getUser();
		
		if( ! currentUser.isPresent() || currentUser.get().person == null ) {
		
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
		}
		
		User user = currentUser.get();

		//Logica
    	try {
    		
    		if(year == null) {
    			year = new LocalDate().getYear(); 
        	}
    		
    		VacationsRecap vacationsRecap = VacationsRecap.Factory.build(year, 
    				ContractDao.getCurrentContract(user.person), new LocalDate(), true);
    		
    		VacationsRecap vacationsRecapPrevious = VacationsRecap.Factory.build(year-1, 
    				ContractDao.getCurrentContract(user.person), new LocalDate(year-1,12,31), true);
    		
    	 	VacationsShowDto vacationShowDto = VacationsShowDto.build(year, vacationsRecap, vacationsRecapPrevious);
    	 	
    	 	render(vacationsRecap, vacationsRecapPrevious, vacationShowDto);
    	 	
    	} catch(IllegalStateException e) {
    		
    		flash.error("Impossibile calcolare la situazione ferie. Definire i dati di inizializzazione per %s %s.", user.person.name, user.person.surname);
    		renderTemplate("Application/indexAdmin.html");
    	}
    	
    	return;
    }
	

	
	public static void vacationsCurrentYear(Integer anno){
		
		Person person = Security.getUser().get().person;
		if( person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
			return;
		}

    	//Costruzione oggetto di riepilogo per la persona
		
		VacationsRecap vacationsRecap = VacationsRecap.Factory.build(anno, ContractDao.getCurrentContract(person), new LocalDate(), true);
    	if(vacationsRecap == null) {
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
	

	
	public static void vacationsLastYear(Integer anno){
		
		Person person = Security.getUser().get().person;
		if( person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
			return;
		}
    	
    	//Costruzione oggetto di riepilogo per la persona
		
		VacationsRecap vacationsRecap = VacationsRecap.Factory.build(anno, ContractDao.getCurrentContract(person), new LocalDate(), true);
    	if(vacationsRecap == null) {
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
	
	
	public static void permissionCurrentYear(Integer anno){
		
		Person person = Security.getUser().get().person;
		if( person == null ) {
			flash.error("Accesso negato.");
			renderTemplate("Application/indexAdmin.html");
			return;
		}
		
    	//Costruzione oggetto di riepilogo per la persona
		
		VacationsRecap vacationsRecap = VacationsRecap.Factory.build(anno, ContractDao.getCurrentContract(person), new LocalDate(), true);
    	if(vacationsRecap == null) {
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
