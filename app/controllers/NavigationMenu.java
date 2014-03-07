/**
 * 
 */
package controllers;

import java.util.ArrayList;
import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import it.cnr.iit.epas.MainMenu;
import it.cnr.iit.epas.PersonUtility;
import models.Contract;
import models.Person;

import org.joda.time.LocalDate;

import play.Logger;
import play.cache.Cache;
import play.mvc.After;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Scope;

/**
 * @author cristian
 *
 */
public class NavigationMenu extends Controller {

	@Before 
	public static void injectMenu() { 
		LocalDate now = new LocalDate();
		Person personLogged = Security.getPerson();
		
		Integer year;
		Integer month;
		Integer day;
		Long personId;
		String method = "";
		
		if(session.get("dispatched")!= null && session.get("dispatched").equals("true"))
		{
			year = Integer.parseInt(session.get("yearSelected"));
			month = Integer.parseInt(session.get("monthSelected"));
			day = Integer.parseInt(session.get("daySelected"));
			personId = Long.parseLong(session.get("personSelected"));
			method = session.get("methodSelected");
			
		}
		else
		{
			//Year from routes (otherwise now)
			year = params.get("year") != null ? Integer.valueOf(params.get("year")) : now.getYear(); 
			session.put("yearSelected", year);
			
			//Month from routes (otherwise now)
			month = params.get("month") != null  ? Integer.valueOf(params.get("month")) : now.getMonthOfYear();
			session.put("monthSelected", month);
			
			//Day from routes (otherwise now)
			day = params.get("day") != null ? Integer.valueOf(params.get("day")) : now.getDayOfMonth();
			session.put("daySelected", day);
			
			//personId from routes (otherwise security)
			personId = params.get("personId") != null ? Long.parseLong(params.get("personId")) : Security.getPerson().id; 
			session.put("personSelected", personId);
			
			//Method from Http.Request
			method = getFormAction(Http.Request.current().action);
			session.put("methodSelected", method);
			
		}
		
		session.put("dispatched", "false");
		
		
		List<Person> persons = Person.getActivePersonsInMonth(month, year, Security.getPerson().getOfficeAllowed(), false);
		
		
		ActionMenuItem action;
		if(method != null && !method.equals("")) 
			action = ActionMenuItem.valueOf(method);
		else
			action = ActionMenuItem.stampingsAdmin;
		
		MainMenu mainMenu = null;
		if(action.getDescription().equals("Riepilogo mensile"))
		{
			mainMenu = new MainMenu(year, month, action);
		}
		if(action.getDescription().equals("Presenza giornaliera"))
		{
			mainMenu = new MainMenu(personId, year, month, day, action, persons);			
		}
		else
		{
			mainMenu = new MainMenu(personId, year, month, action, persons);
		}		
		
		
		
		Person p = PersonUtility.getPersonRightsBased(personLogged, personId);
		if(p == null){
			flash.error("Non si può accedere alla funzionalità per la persona con id %d", personId);
			renderArgs.put("mainMenu", mainMenu);
			Application.indexAdmin();
		}
		renderArgs.put("mainMenu", mainMenu);

	}
	
	private static String getFormAction(String controller)
	{
		if(controller.equals("Stampings.personStamping"))
			return "stampingsAdmin";
		if(controller.equals("Stampings.stampings"))
			return "stampings";
		
		
		if(controller.equals("Persons.list") || controller.equals("Persons.edit") || controller.equals("Persons.personCompetence") || controller.equals("Persons.insertPerson"))
			return "personList";
		
		if(controller.equals("YearlyAbsences.yearlyAbsences"))
			return "yearlyAbsences";
		
		if(controller.equals("VacationsAdmin.manageVacationCode"))
			return  "vacationsAdmin";
		
		if(controller.equals("Competences.showCompetences") || controller.equals("Competences.overtime") || controller.equals("Competences.totalOvertimeHours") || controller.equals("Competences.recapCompetences"))
			return "competencesAdmin";
		
		if(controller.equals("WorkingTimes.manageWorkingTime"))
			return "manageWorkingTime";
		
		if(controller.equals("Configurations.showConfGeneral") || controller.equals("Configurations.showConfYear"))
			return "confParameters";
			
		if(controller.equals("Stampings.mealTicketSituation"))
			return "mealTicketSituation";
		
		if(controller.equals("Stampings.missingStamping"))
			return "missingStamping";
		
		if(controller.equals("Stampings.dailyPresence"))
			return "dailyPresence";
		
		if(controller.equals("Administrators.list"))
			return "administrator";
		
		if(controller.equals("YearlyAbsences.showGeneralMonthlyAbsences"))
			return "totalMonthlyAbsences";
		
		if(controller.equals("MonthRecaps.show"))
			return "monthRecap";
		
		if(controller.equals("Absences.manageAbsenceCode") || controller.equals("Absences.editCode"))
			return "manageAbsenceCode";
		
		if(controller.equals("Competences.manageCompetence"))
			return "manageCompetenceCode";
		
		if(controller.equals("Persons.changePassword"))
			return "changePassword";
		
		if(controller.equals("Absences.absences"))
			return "absences";
		
		if(controller.equals("YearlyAbsences.absencesPerPerson"))
			return "absencesperperson";
		
		if(controller.equals("Vacations.show"))
			return "vacations";
		
		if(controller.equals("PersonMonths.hourRecap"))
			return "hourrecap";
		
		return null;
		
	
	}
		
}
