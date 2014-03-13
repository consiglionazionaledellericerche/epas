package controllers;

import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import models.Contract;
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
		
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void show(Long personId, Integer anno) {

		//controllo dei parametri
		Person person = null;
		if(personId != null)
		{
			person = Person.findById(personId);
		}
		else
			person = Security.getPerson();
		
		//default l'anno corrente
    	if(anno==null)
			anno = new LocalDate().getYear(); 

    	Contract contract = person.getCurrentContract();
    	VacationsRecap vacationsRecap = new VacationsRecap(person, anno, contract, new LocalDate(), true);
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
		Person person = null;
		if(personId != null)
    		person = Person.findById(personId);
    	else
    		person = Security.getPerson();

    	//Costruzione oggetto di riepilogo per la persona
		Contract contract = person.getCurrentContract();
    	VacationsRecap vacationsRecap = new VacationsRecap(person, anno, contract, new LocalDate(), true);
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
		Person person = null;
    	if(personId != null)
    		person = Person.findById(personId);
    	else
    		person = Security.getPerson();
    	
    	//Costruzione oggetto di riepilogo per la persona
    	Contract contract = person.getCurrentContract();
    	VacationsRecap vacationsRecap = new VacationsRecap(person, anno, contract, new LocalDate(), true);
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
