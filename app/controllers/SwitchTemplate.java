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
		
		Person person = Security.getPerson();
		
		LocalDate now = new LocalDate();
		int month = now.getMonthOfYear();
		int year = now.getYear();
		int day = now.getDayOfMonth();
		Long personId = Security.getPerson().id;
		
		String method = params.get("method");
		if (method == null)
		{
			flash.error(String.format("La action da eseguire è: %s", method));
			Application.indexAdmin();
		}
		Logger.info("aaa %s", method);
		session.put("methodSelected", method);
		
		ActionMenuItem menuItem = ActionMenuItem.valueOf(method);
	
		//get month selected
		if (params.get("month") != null) 
			month = params.get("month", Integer.class);
		session.put("monthSelected", month);
		
		//get year selected
		if (params.get("year") != null)
			year = params.get("year", Integer.class);
		session.put("yearSelected", year);
		
		//get day selected
		if(params.get("day") != null)
			day = params.get("day", Integer.class);
		session.put("daySelected", day);
		
		//get person selected
		if (params.get("personId") != null) 
		{
			personId = params.get("personId", Long.class);
			Logger.debug("L'id selezionato è: %d", personId);
			if(personId != 0)	
				person = Person.findById(personId);
		}
		session.put("personSelected", personId);

		
		
		params.flash();
		
		switch (menuItem) {

		case missingStamping:
			Stampings.missingStamping(year, month);
			break;
			
		case monthRecap:
			//Logger.debug("Nella switchTemplate chiamo il metodo monthRecap");
			if(personId != 0){
				flash.error("Il metodo %s deve essere chiamato senza selezionare alcuna persona", menuItem.getDescription());
				Application.indexAdmin();
			}
			else
				MonthRecaps.show(year, month);
			break;
		
		case separateMenu:
			flash.error("Selezionare una delle opzioni possibili");
			Application.indexAdmin();
			break;

		case stampingsAdmin:
			Logger.debug("sto per chiamare il metodo show");

			if (personId != 0) {
				Logger.debug("sto per chiamare il metodo showAdmin con personId = %s, year = %s, month = %s", personId, year, month);
				Stampings.personStamping(person.getId(), year, month);
			} else {
				Logger.debug("sto per chiamare il metodo show con personId = %s, year = %s, month = %s", personId, year, month);
				Stampings.stampings(year, month);
			}

			break;
		case printTag:
			Logger.debug("sto per chiamare la stampa cartellino");
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
			UploadSituation.uploadSituation(year, month);
			break;
		case stampings:
			Stampings.stampings(year, month);
			break;
		case absences:
			Absences.absences(year, month); 
			break;
		case absencesperperson:
			if(personId == null || personId == 0)
				personId = Security.getPerson().id;
			YearlyAbsences.absencesPerPerson(personId, year);
			break;
		case vacations:
			if(personId == null || personId == 0)
				personId = Security.getPerson().id;
			Vacations.show(personId, year);
			break;
		case competences:
			if(personId == null || personId == 0)				
				personId = Security.getPerson().id;
			
			Competences.competences(personId, year, month);
			break;
		case hourrecap:
			if(personId == null || personId == 0)				
				personId = Security.getPerson().id;
			PersonMonths.hourRecap(personId,year);
			break;
		case printPersonTag:
			if(personId == null || personId == 0)				
				personId = Security.getPerson().id;
			PrintTags.showPersonTag(year, month);
			break;
			
			
			
			

		default: 
			break;

		}
		//flash.put("method", menuItem.getDescription());
		
		params.flash();
	}
}


