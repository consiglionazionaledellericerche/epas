package controllers;

import it.cnr.iit.epas.ActionMenuItem;

import org.joda.time.LocalDate;

import models.MonthRecap;
import models.Person;
import models.Stamping;
import models.YearRecap;
import play.Logger;
import play.mvc.Controller;

public class SwitchTemplate extends Controller{

	public static final String USERNAME_SESSION_KEY = "username";
	
	public static void dispatch(){
		LocalDate now = new LocalDate();
		
		String action = params.get("action");
		ActionMenuItem menuItem = ActionMenuItem.valueOf(action);
		
		if (action == null) {
			/* fare qualcosa! Reindirizzare l'utente verso una pagina con l'errore? Rimanere sulla stessa pagina mostrando l'errore? */
			return;
		}

		Person person = Security.getPerson();
		
		Long personId = null;
		
		if (params.get("personId") != null) {
			personId = params.get("personId", Long.class);
			person = Person.findById(personId);
		} 
				
		int month = now.getMonthOfYear();
		if (params.get("month") != null) {
			month = params.get("month", Integer.class);
		}
		
		int year = now.getYear();
		if (params.get("year") != null) {
			year = params.get("year", Integer.class);
		}
		
		switch (menuItem) {
		
		case stampings:
			Logger.debug("sto per chiamare il metodo show");
			
			if (personId != null) {
				Logger.debug("sto per chiamare il metodo showAdmin con personId = %s, year = %s, month = %s", personId, year, month);
				Stampings.personStamping(person.getId(), year, month);
			} else {
				Logger.debug("sto per chiamare il metodo show con personId = %s, year = %s, month = %s", personId, year, month);
				Stampings.show(person.getId(), year, month);
			}

			break;
			
		case absences:
			Absences.show();
			break;
		case yearlyAbsences:
			YearlyAbsences.show();
			break;
		case vacations:
			
			if (personId != null) {
				Logger.debug("sto per chiamare il metodo showAdmin con personId = %s, year = %s, month = %s", personId, year, month);
				Vacations.show();
			} else {
				Logger.debug("sto per chiamare il metodo show con personId = %s, year = %s, month = %s", personId, year, month);
				Vacations.show();
			}
			
			break;
		case competences:
			Competences.show();
			break;
		case changePassword:
			Persons.changePassword(person.id);
			break;
		case manageWorkingTime:
			WorkingTimes.manageWorkingTime();
			break;
		case confParameters:
			Configurations.show();
			break;
		case personList:
			Persons.list();
			break;
		default:
			break;
		}
		
	}

}
