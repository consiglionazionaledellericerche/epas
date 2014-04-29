package controllers;

import it.cnr.iit.epas.ActionMenuItem;
import it.cnr.iit.epas.DateUtility;

import java.io.IOException;
import java.sql.SQLException;

import models.User;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

@With(RequestInit.class)
public class SwitchTemplate extends Controller{

	public static final String USERNAME_SESSION_KEY = "username";

	public static void dispatch() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
		
		/*
		User userLogged = Security.getUser();
		if(userLogged==null)
		{
			flash.error("Nessun utente risulta loggato");
			Application.index(); 	
		}
		
		LocalDate now = new LocalDate();
		session.put("dispatched", "true");
		
		String method = params.get("method");
		if (method == null)
		{
			flash.error(String.format("La action da eseguire è: %s", method));
			Application.indexAdmin();
		}
		
		session.put("methodSelected", method);
		ActionMenuItem menuItem = ActionMenuItem.valueOf(method);
	
		//Year from routes/form (otherwise now)
		Integer year = params.get("year") != null ? Integer.valueOf(params.get("year")) : now.getYear(); 
		session.put("yearSelected", year);
				
		//Month from routes/form (otherwise now)
		Integer month = params.get("month") != null  ? Integer.valueOf(params.get("month")) : now.getMonthOfYear();
		session.put("monthSelected", month);
		session.put("monthSelectedName", DateUtility.getName(month));
				
		//Day from routes/form (otherwise now)
		Integer day = params.get("day") != null ? Integer.valueOf(params.get("day")) : now.getDayOfMonth();
		session.put("daySelected", day);

		//get person selected
		
		//personId from routes (otherwise security)
		Long personId;
		if(params.get("personId")!=null)
			personId = Long.parseLong(params.get("personId"));
		else if(userLogged.person != null)
			personId = userLogged.person.id;
		else
			personId = 1l; //admin id
		session.put("personSelected", personId);
		
		//Se personId è una persona reale (1 admin, 0 tutti) eseguo il controllo
		if(personId > 1)
		{
			if( !Security.canUserSeePerson(userLogged, personId) )
			{
				flash.error("Non si può accedere alla funzionalità per la persona con id %d", personId);
				Application.indexAdmin();
			}
		}
		
		switch (menuItem) {
		
		case charts:
			Charts.indexCharts();
			break;
		
		case manageAttachments:
			Absences.manageAttachmentsPerCode(year, month);
			break;

		case offices:
			Offices.showOffices();
			break;
		
		case missingStamping:
			Stampings.missingStamping(year, month);
			break;

		case monthRecap:
			MonthRecaps.show(year, month);
			break;

		case separateMenu:
			flash.error("Selezionare una delle opzioni possibili");
			Application.indexAdmin();
			break;

		case stampingsAdmin:
			Stampings.personStamping(personId, year, month);
			break;

		case printTag:
			PrintTags.listPersonForPrintTags(year, month);
			break;

		case yearlyAbsences:
			YearlyAbsences.yearlyAbsences(personId, year);
			break;

		case totalMonthlyAbsences:
			YearlyAbsences.showGeneralMonthlyAbsences(year, month, null);
			break;

		case manageAbsenceCode:
			Absences.manageAbsenceCode(null, null);
			break;

		case vacationsAdmin:
			VacationsAdmin.list(year, null, null);		
			break;

		case competencesAdmin:
			Competences.showCompetences(year, month, null, null, null);
			break;

		case changePassword:
			Persons.changePassword();
			break;

		case manageWorkingTime:
			WorkingTimes.manageWorkingTime();
			break;

		case confParameters:
			Configurations.showConfGeneral(Security.getUser().person.office.id);
			break;

		case personList:
			Persons.list(null);
			break;

		case administrator:
			Administrators.list();
			break;

		case dailyPresence:
			Stampings.dailyPresence(year, month, day);
			break;

		case mealTicketSituation:
			Stampings.mealTicketSituation(year, month);
			break;

		case manageCompetence:
			Competences.manageCompetenceCode();
			break;

		case uploadSituation:
			UploadSituation.show(year, month);
			break;

		case stampings:
			Stampings.stampings(year, month);
			break;

		case absences:
			Absences.absences(year, month); 
			break;

		case absencesperperson:
			YearlyAbsences.absencesPerPerson(personId, year);
			break;

		case vacations:
			Vacations.show(personId, year);
			break;

		case competences:
			Competences.competences(personId, year, month);
			break;

		case hourrecap:
			PersonMonths.hourRecap(personId,year);
			break;

		case printPersonTag:
			PrintTags.showPersonTag(year, month);
			break;
		
		case trainingHours:
			PersonMonths.trainingHours(personId, year, month);
			break;

		default: 
			break;

		}
		
		renderText("ok");
		*/

	}
	
	private static void executeAction(String action) {
		
		Integer year = Integer.parseInt(session.get("yearSelected"));
		Integer month = Integer.parseInt(session.get("monthSelected"));
		Long personId = Long.parseLong(session.get("personSelected"));
		
		session.put("actionSelected", action);
		
		if(action.equals("Stampings.stampings")) {
			
			Stampings.stampings(year, month);
		}
		
		if(action.equals("Stampings.personStamping")) {
			
			Stampings.personStamping(personId, year, month);
		}
		
		if(action.equals("PersonMonths.trainingHours")) {
			
			PersonMonths.trainingHours(personId, year, month);
		}
		
		if(action.equals("PersonMonths.hourRecap")) {
			
			PersonMonths.hourRecap(personId, year);
		}
		
		if(action.equals("Vacations.show")) {
			
			Vacations.show(personId, year);
		}
		
		if(action.equals("Persons.changePassword")) {
			
			Persons.changePassword();
		}
		
		if(action.equals("Absences.absences")) {
			
			Absences.absences(year, month);
		}
		
		if(action.equals("YearlyAbsences.absencesPerPerson")) {
			YearlyAbsences.absencesPerPerson(personId, year);
		}
	}
	
	
	public static void updateMonth(Integer month) throws Throwable {
		
		String action = session.get("actionSelected");
		if( action==null ) {
			
			flash.error("La sessione è scaduta. Effettuare nuovamente login.");
			Secure.login();
		}
		
		if(month == null || month < 1 || month > 12) {
			
			Application.index();	
		}
		
		session.put("monthSelected", month);
		
		executeAction(action);
		
	}
	
	public static void updateYear(Integer year) throws Throwable {
		
		String action = session.get("actionSelected");
		if( action==null ) {
			
			flash.error("La sessione è scaduta. Effettuare nuovamente login.");
			Secure.login();
		}
		
		if(year == null ) {	/* TODO check bound year */
			
			Application.index();	
		}
		
		session.put("yearSelected", year);
		
		executeAction(action);
		
	}
	
	public static void updatePerson(Long personId) throws Throwable {
	
		String action = session.get("actionSelected");
		if( action==null ) {
			
			flash.error("La sessione è scaduta. Effettuare nuovamente login.");
			Secure.login();
		}
		
		if(personId == null ) {	/* TODO check bound year */
			
			Application.index();	
		}
		
		session.put("personSelected", personId);
		
		executeAction(action);
		
	}
	
	
}


