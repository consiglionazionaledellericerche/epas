package controllers;

import it.cnr.iit.epas.ActionMenuItem;

import org.joda.time.LocalDate;

import models.MonthRecap;
import models.Person;
import models.YearRecap;
import play.Logger;
import play.mvc.Controller;

public class SwitchTemplate extends Controller{

	public static final String USERNAME_SESSION_KEY = "username";
	
	public static void dispatch(){
 
		String s = params.get("menuItem");
		Person person = renderArgs.get(USERNAME_SESSION_KEY, Person.class);
		if (s == null) {
			/* fare qualcosa! Reindirizzare l'utente verso una pagina con l'errore? Rimanere sulla stessa pagina mostrando l'errore? */
			return;
		}
		
		ActionMenuItem menuItem = ActionMenuItem.valueOf(s);
		
		switch (menuItem) {
		case stampings:
			Stampings.show();
			break;
		case absences:
			Absences.show();
		case yearlyAbsences:
			YearlyAbsences.show();
		case vacations:
			Vacations.show();
		case competences:
			Competences.show();
		case changePassword:
			Persons.changePassword(person);
		case manageWorkingTime:
			WorkingTimes.manageWorkingTime();
		case confParameters:
			ConfParameters.show();
		default:
			break;
		}
		
	}
	
}
