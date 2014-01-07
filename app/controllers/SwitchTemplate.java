package controllers;

import java.io.IOException;
import java.sql.SQLException;

import it.cnr.iit.epas.ActionMenuItem;
import models.Person;

import org.joda.time.LocalDate;

import play.Logger;
import play.cache.Cache;
import play.mvc.Controller;

public class SwitchTemplate extends Controller{

	public static final String USERNAME_SESSION_KEY = "username";

	public static void dispatch() throws InstantiationException, IllegalAccessException, IOException, ClassNotFoundException, SQLException {
		
		LocalDate now = new LocalDate();
		
		session.put("dispatched", "true");
		
		String method = params.get("method");
		session.put("methodSelected", method);
		ActionMenuItem menuItem = ActionMenuItem.valueOf(method);
	
		//Year from routes/form (otherwise now)
		Integer year = params.get("year") != null ? Integer.valueOf(params.get("year")) : now.getYear(); 
		session.put("yearSelected", year);
				
		//Month from routes/form (otherwise now)
		Integer month = params.get("month") != null  ? Integer.valueOf(params.get("month")) : now.getMonthOfYear();
		session.put("monthSelected", month);
				
		//Day from routes/form (otherwise now)
		Integer day = params.get("day") != null ? Integer.valueOf(params.get("day")) : now.getDayOfMonth();
		session.put("daySelected", day);

		//get person selected
		Long personId = params.get("personId") != null ? Long.parseLong(params.get("personId")) : Security.getPerson().id; 
		session.put("personSelected", personId);
		

		switch (menuItem) {

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
			YearlyAbsences.showGeneralMonthlyAbsences(year, month);
			break;

		case manageAbsenceCode:
			Absences.manageAbsenceCode();
			break;

		case vacationsAdmin:
			VacationsAdmin.manageVacationCode();		
			break;

		case competencesAdmin:
			Competences.showCompetences(year, month);
			break;

		case changePassword:
			Persons.changePassword(personId);
			break;

		case manageWorkingTime:
			WorkingTimes.manageWorkingTime();
			break;

		case confParameters:
			Configurations.list();
			break;

		case personList:
			Persons.list();
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

		default: 
			break;

		}

	}
}


