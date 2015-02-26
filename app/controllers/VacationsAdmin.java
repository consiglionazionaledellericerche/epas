package controllers;

import helpers.ModelQuery.SimpleResults;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import manager.ConfYearManager;
import models.Office;
import models.Person;
import models.rendering.VacationsRecap;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;

import dao.OfficeDao;
import dao.PersonDao;

@With( {Secure.class, RequestInit.class} )
public class VacationsAdmin extends Controller{

	@Inject
	static SecurityRules rules;
	
	
	public static void list(Integer year, String name, Integer page){
		
		if(page==null)
			page = 0;
		rules.checkIfPermitted("");
		LocalDate date = new LocalDate();
		
		SimpleResults<Person> simpleResults = PersonDao.list(Optional.fromNullable(name), 
				OfficeDao.getOfficeAllowed(Security.getUser().get()), false, date, date, true);
		
		List<Person> personList = simpleResults.paginated(page).getResults();
		
		List<VacationsRecap> vacationsList = new ArrayList<VacationsRecap>();
		
		List<Person> personsWithVacationsProblems = new ArrayList<Person>();

		for(Person person: personList)
		{
			person.refresh();
			Logger.info("%s", person.surname);
			VacationsRecap vr = null;
			try {
				vr = VacationsRecap.Factory.build(year, person.getCurrentContract(), new LocalDate(), true);
				vacationsList.add(vr);
			}
			catch(IllegalStateException e){
				personsWithVacationsProblems.add(person);
			}
		}
				
		Office office = Security.getUser().get().person.office;
		Integer monthExpiryVacationPastYear = Integer.parseInt(ConfYearManager.getFieldValue("month_expiry_vacation_past_year", year, office));
		Integer dayExpiryVacationPastYear = Integer.parseInt(ConfYearManager.getFieldValue("day_expiry_vacation_past_year", year, office));
		LocalDate expireDate = LocalDate.now().withMonthOfYear(monthExpiryVacationPastYear).withDayOfMonth(dayExpiryVacationPastYear);
		
		boolean isVacationLastYearExpired = VacationsRecap.isVacationsLastYearExpired(year, expireDate);
		render(vacationsList, isVacationLastYearExpired, personsWithVacationsProblems, year, simpleResults, name);
	}
	
	
	
	public static void vacationsCurrentYear(Long personId, Integer anno){
		
		Person person = PersonDao.getPersonById(personId);
		if( person == null ) {
			error();	/* send a 500 error */
		}
		rules.checkIfPermitted(person.office);
    	//Costruzione oggetto di riepilogo per la persona
		
		VacationsRecap vacationsRecap = null;
    	try { 
    		vacationsRecap = VacationsRecap.Factory.build(anno, person.getCurrentContract(), new LocalDate(), true);
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
    	renderTemplate("Vacations/vacationsCurrentYear.html", vacationsRecap);
	}
	

	
	public static void vacationsLastYear(Long personId, Integer anno){
		
		Person person = PersonDao.getPersonById(personId);
		if( person == null ) {
			error();	/* send a 500 error */
		}
    	rules.checkIfPermitted(person.office);
    	//Costruzione oggetto di riepilogo per la persona
    	
    	VacationsRecap vacationsRecap = null;
    	try { 
    		vacationsRecap = VacationsRecap.Factory.build(anno, person.getCurrentContract(), new LocalDate(), true);
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
    	renderTemplate("Vacations/vacationsLastYear.html", vacationsRecap);
	}
	
	
	public static void permissionCurrentYear(Long personId, Integer anno){
		
		Person person = PersonDao.getPersonById(personId);
		if( person == null ) {
			error();	/* send a 500 error */
		}
		rules.checkIfPermitted(person.office);
    	//Costruzione oggetto di riepilogo per la persona
		
    	VacationsRecap vacationsRecap = null;
    	try { 
    		vacationsRecap = VacationsRecap.Factory.build(anno, person.getCurrentContract(), new LocalDate(), true);
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
    	renderTemplate("Vacations/permissionCurrentYear.html", vacationsRecap);
	}
	
}
