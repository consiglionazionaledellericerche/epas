package controllers;

import it.cnr.iit.epas.ActionMenuItem;
import models.Person;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;

public class SwitchTemplate extends Controller{

	public static final String USERNAME_SESSION_KEY = "username";

	public static void dispatch() throws InstantiationException, IllegalAccessException {
		LocalDate now = new LocalDate();

		String method = params.get("method");
		Logger.debug("La action è: %s", method);
		if (method == null) {

			flash.error(String.format("La action da eseguire è: %s", method));
			Application.indexAdmin();

		}
		ActionMenuItem menuItem = ActionMenuItem.valueOf(method);
		Logger.debug("Il menuItem è: %s relativo alla action: %s", menuItem, method);
		Person person = Security.getPerson();

		Long personId = null;

		if (params.get("personId") != null) {
			personId = params.get("personId", Long.class);
			person = Person.findById(personId);
		} 

		int month;
		if (params.get("month") != null) {
			month = params.get("month", Integer.class);
		}
		else 
			month = now.getMonthOfYear();

		int year;
		if (params.get("year") != null) {
			year = params.get("year", Integer.class);
		}
		else 
			year = now.getYear();

		int day ;
		if(params.get("day") != null){
			day = params.get("day", Integer.class);
		}
		else
			day = now.getDayOfMonth();
		
		params.flash();
		
		switch (menuItem) {

		case stampingsAdmin:
			Logger.debug("sto per chiamare il metodo show");

			if (personId != null) {
				Logger.debug("sto per chiamare il metodo showAdmin con personId = %s, year = %s, month = %s", personId, year, month);
				Stampings.personStamping(person.getId(), year, month);
			} else {
				Logger.debug("sto per chiamare il metodo show con personId = %s, year = %s, month = %s", personId, year, month);
				Stampings.stampings(person.getId(), year, month);
			}

			break;

		
//		case absencesAdmin:
//			Absences.absences(personId, year, month);
//			break;
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
			Persons.changePassword(person.id);
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
		case missingStamping:
			Stampings.missingStamping(year, month);
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
		case stampings:
			Stampings.stampings(personId, year, month);
			break;
		case absences:
			Absences.absences(personId, year, month); 
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

		default: 
			break;

		}
	}
}


