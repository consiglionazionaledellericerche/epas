/**
 * 
 */
package controllers;

import it.cnr.iit.epas.ActionMenuItem;
import it.cnr.iit.epas.DateUtility;
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
public class RequestInit extends Controller {

	public static class TemplateUtility {
		
		public String monthName(String month) {
			
			return DateUtility.getName(Integer.parseInt(month));
		}
	}
	
	@Before
	public static void injectUtility() {
		
		TemplateUtility templateUtility = new TemplateUtility();
		renderArgs.put("templateUtility", templateUtility);
		
		
		
	}
	
	
	@Before 
	public static void injectMenu() { 
		
		session.put("actionSelected", 
				computeActionSelected(Http.Request.current().action));
		
		Integer year;
		if ( params.get("year") != null ) {
			
			year = Integer.valueOf(params.get("year"));
			session.put("yearSelected", year);
		} 
		else if (session.get("yearSelected") != null ){
			
			year = Integer.valueOf(session.get("yearSelected"));
		}
		else {
			
			year = LocalDate.now().getYear();
		}
		
		Integer month;
		if ( params.get("month") != null ) {
			
			month = Integer.valueOf(params.get("month"));
			session.put("monthSelected", month);
		} 
		else if ( session.get("monthSelected") != null ){
			
			month = Integer.valueOf(session.get("monthSelected"));
		}
		else {
			
			month = LocalDate.now().getMonthOfYear();
		}
		
		Integer personId;
		if ( params.get("personId") != null ) {
			
			personId = Integer.valueOf(params.get("personId"));
			session.put("personSelected", personId);
		} 
		else if ( session.get("personSelected") != null ){
			
			personId = Integer.valueOf(session.get("personSelected"));
		}
		else if( Security.getUser().person != null ){
			
			session.put("personSelected", Security.getUser().person.id);
		}
		else {
			
			session.put("personSelected", 1);
		}
		
		
		if(Security.getUser().person != null) {
			
			List<Person> persons = Person.getActivePersonsInMonth(month, year, Security.getOfficeAllowed(), false);
			renderArgs.put("navPersons", persons);
		} 
		else {
			
			List<Office> allOffices = Office.findAll();
			List<Person> persons = Person.getActivePersonsInMonth(month, year, allOffices, false);
			renderArgs.put("navPersons", persons);
		}
		
		
		/*
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
			session.put("monthSelectedName", DateUtility.getName(month));
			
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
		*/
	}
	
	/*
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
	*/
	
	private static String computeActionSelected(String action) {
		
		
		if( action.startsWith("Stampings.")) {
			
			if(action.equals("Stampings.stampings")) {
				
				renderArgs.put("dropDown", "dropDown1");
				return "Stampings.stampings";
			}
			
			if(action.equals("Stampings.personStamping")) {
				
				renderArgs.put("dropDown", "dropDown2");
				return "Stampings.personStamping";
			}
			
		}
		
		if( action.startsWith("PersonMonths.")) {
			
			if(action.equals("PersonMonths.trainingHours")) {
				
				renderArgs.put("dropDown", "dropDown1");
				return "PersonMonths.trainingHours";
			}
			
			if(action.equals("PersonMonths.hourRecap")) {
				
				renderArgs.put("noPerson", true);
				renderArgs.put("dropDown", "dropDown1");
				return "PersonMonths.hourRecap";
			}
		}
		
		if( action.startsWith("Vacations.")) {
			
			if(action.equals("Vacations.show")) {
				
				renderArgs.put("noPerson", true);
				renderArgs.put("dropDown", "dropDown1");
				return "Vacations.show";
			}
		}
		
		if( action.startsWith("Persons.")) {
			
			if(action.equals("Persons.changePassword")) {
				
				renderArgs.put("dropDown", "dropDown1");
				return "Persons.changePassword";
			}
			if(action.equals("Persons.list")) {
				
				renderArgs.put("noData", true);
				renderArgs.put("noPerson", true);
				renderArgs.put("dropDown", "dropDown2");
				return "Persons.list";
			}
			
			if(action.equals("Persons.edit")) {
				
				renderArgs.put("noData", true);
				renderArgs.put("noPerson", true);
				renderArgs.put("dropDown", "dropDown2");
				return "Persons.edit";
			}
		}
		
		if(action.startsWith("Absences.")) {
			
			if(action.equals("Absences.absences")) {
				
				renderArgs.put("dropDown", "dropDown1");
				return "Absences.absences";
			}
		}
		
		if(action.startsWith("YearlyAbsences.")) {
			
			if(action.equals("YearlyAbsences.absencesPerPerson")) {
				
				renderArgs.put("noPerson", true);
				renderArgs.put("dropDown", "dropDown1");
				return "YearlyAbsences.absencesPerPerson";
			}
		}
		
		if(action.startsWith("Competences.")) {
			
			if(action.equals("Competences.competences")) {
				
				renderArgs.put("noPerson", true);
				renderArgs.put("dropDown", "dropDown1");
				return "Competences.competences";
			}
			
			if(action.equals("Competences.showCompetences")) {
				
				renderArgs.put("noPerson", true);
				renderArgs.put("dropDown", "dropDown2");
				return "Competences.showCompetences";
			}
			
			if(action.equals("Competences.overtime")) {
				
				renderArgs.put("noPerson", true);
				renderArgs.put("dropDown", "dropDown2");
				return "Competences.overtime";
			}
			
			if(action.equals("Competences.totalOvertimeHours")) {
				
				renderArgs.put("noPerson", true);
				renderArgs.put("dropDown", "dropDown2");
				return "Competences.totalOvertimeHours";
			}
			
			if(action.equals("Competences.enabledCompetences")) {
				
				renderArgs.put("noData", true);
				renderArgs.put("noPerson", true);
				renderArgs.put("dropDown", "dropDown2");
				return "Competences.enabledCompetences";
			}
			
			if(action.equals("Competences.exportCompetences")) {
				
				renderArgs.put("noData", true);
				renderArgs.put("noPerson", true);
				renderArgs.put("dropDown", "dropDown2");
				return "Competences.exportCompetences";
			}

		}
		
		
		
		return session.get("actionSelected");
	}
		
}
