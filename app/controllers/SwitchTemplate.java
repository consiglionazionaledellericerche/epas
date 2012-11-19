package controllers;

import it.cnr.iit.epas.ActionEmployeesMenuItem;
import it.cnr.iit.epas.ActionMenuItem;

import org.joda.time.LocalDate;

import com.ning.http.util.DateUtil.DateParseException;

import models.MonthRecap;
import models.Person;
import models.Stamping;
import models.YearRecap;
import play.Logger;
import play.mvc.Controller;

public class SwitchTemplate extends Controller{

	public static final String USERNAME_SESSION_KEY = "username";
	
	public static void dispatch() throws InstantiationException, IllegalAccessException, DateParseException{
		LocalDate now = new LocalDate();
		
		Long id = params.get("personId", Long.class);
		Person p = Person.findById(id);
		if(p.permissions.size() > 1){
			/**
			 * Sono nel caso di un utente con più permessi, un amministratore
			 */
			String action = params.get("action");
			Logger.debug("La action è: %s", action);
			if (action == null) {
				
				flash.error(String.format("La action da eseguire è: %s", action));
				Application.indexAdmin();
				
			}
			ActionMenuItem menuItem = ActionMenuItem.valueOf(action);
			
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
			
			int day = now.getDayOfMonth();
			if(params.get("day") != null){
				day = params.get("day", Integer.class);
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
				Absences.show(personId, year, month);
				break;
			case yearlyAbsences:
				YearlyAbsences.show(personId, year, month);
				break;
			case totalMonthlyAbsences:
				YearlyAbsences.showGeneralMonthlyAbsences(year, month);
				break;
			case manageAbsenceCode:
				Absences.manageAbsenceCode();
				break;
			case vacations:
				VacationsAdmin.manageVacationCode();		
				break;
			case competences:
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
			default:		
				break;
			}
		}
		else{
			String action = params.get("action");
			Logger.debug("La action è: %s", action);
			if (action == null) {
				
				flash.error(String.format("La action da eseguire è: %s", action));
				Application.indexAdmin();
				
			}
			ActionEmployeesMenuItem menuItem = ActionEmployeesMenuItem.valueOf(action);
			
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
			
			int day = now.getDayOfMonth();
			if(params.get("day") != null){
				day = params.get("day", Integer.class);
			}
			
			switch (menuItem) {
			case stampings:
				Stampings.show(personId, year, month); //vediamo se va bene questa o se c'è necessità di farne una nuova per l'impiegato
				break;
			case absences:
				Absences.show(personId, year, month); //vediamo se va bene questa o se c'è necessità di farne una nuova per l'impiegato
				break;
			case absencesPerPerson:
				YearlyAbsences.show(personId, year, month);
				break;
			case vacations:
				Vacations.show();
				break;
			case competences:
				Competences.show();
				break;
			case hourRecap:
				PersonMonths.hourRecap(personId,year);
				break;
			case changePassword:
				Persons.changePassword(personId);
				break;
				default: 
					break;
			}
		}
		
		render(p.permissions);
		
	}
//	
//	public static void switchTemplateEmployees() throws InstantiationException, IllegalAccessException, DateParseException{
//		LocalDate now = new LocalDate();
//		
//		
//	}

}
