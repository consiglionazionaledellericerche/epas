/**
 * 
 */
package controllers;

import it.cnr.iit.epas.ActionMenuItem;
import it.cnr.iit.epas.MainMenu;

import java.util.List;

import models.Office;
import models.Person;
import models.User;

import org.joda.time.LocalDate;

import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;

/**
 * @author cristian
 *
 */
public class NavigationMenu extends Controller {

	@Before 
	public static void injectMenu() { 
		LocalDate now = new LocalDate();
		User userLogged = Security.getUser();
		if(userLogged==null)
		{
			flash.error("Nessun utente risulta loggato");
			Application.index(); 	
		}
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
			if(params.get("personId")!=null)
				personId = Long.parseLong(params.get("personId"));
			else if(userLogged.person != null)
				personId = userLogged.person.id;
			else
				personId = 1l; //admin id
			
			//personId = params.get("personId") != null ? Long.parseLong(params.get("personId")) : Security.getUser().person.id; 
			session.put("personSelected", personId);
			
			//Method from Http.Request
			method = getFormAction(Http.Request.current().action);
			session.put("methodSelected", method);
			
		}
		
		session.put("dispatched", "false");
		
		List<Person> persons = null;
		if(userLogged.person != null)
		{
			persons = Person.getActivePersonsInMonth(month, year, Security.getOfficeAllowed(), false);
		}
		else
		{
			List<Office> allOffices = Office.findAll();
			persons = Person.getActivePersonsInMonth(month, year, allOffices, false);
		}
		
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
		
		//Se personId è una persona reale (1 admin, 0 tutti) eseguo il controllo
		if( personId > 1 )
		{
			if( !Security.canUserSeePerson(userLogged, personId) )
			{
				flash.error("Non si può accedere alla funzionalità per la persona con id %d", personId);
				renderArgs.put("mainMenu", mainMenu);
				Application.indexAdmin();
			}
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
		
		if(controller.equals("VacationsAdmin.manageVacationCode") || controller.equals("VacationsAdmin.list"))
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
