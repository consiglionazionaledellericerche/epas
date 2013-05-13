package controllers;

import java.io.IOException;

import it.cnr.iit.epas.ActionMenuItem;
import models.Person;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Controller;

public class SwitchTemplate extends Controller{

	public static final String USERNAME_SESSION_KEY = "username";

	public static void dispatch() throws InstantiationException, IllegalAccessException, IOException {
		LocalDate now = new LocalDate();

		String method = params.get("method");
		Logger.debug("Nella switchTemplate La action è: %s", method);
		if (method == null) {

			flash.error(String.format("La action da eseguire è: %s", method));
			Application.indexAdmin();

		}
		ActionMenuItem menuItem = ActionMenuItem.valueOf(method);
		Logger.debug("Nella switchTemplate Il menuItem è: %s relativo alla action: %s", menuItem, method);
		Person person = Security.getPerson();
		
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

		Long personId = null;
		Logger.debug("Il personId preso dai params vale: %s", params.get("personId"));
		if (params.get("personId") != null) {
			personId = params.get("personId", Long.class);
			
			Logger.debug("L'id selezionato è: %d", personId);

			if(personId != 0)	
				person = Person.findById(personId);
		}	
		
		params.flash();
		
		switch (menuItem) {

		case missingStamping:
			if(personId != 0){
				flash.error("Il metodo %s deve essere chiamato senza selezionare alcuna persona", menuItem.getDescription());
				Application.indexAdmin();
			}
			else
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
			PrintTags.listPersonForPrintTags();
			break;

		case yearlyAbsences:
			YearlyAbsences.yearlyAbsences(personId, year);
			break;
		
		case totalMonthlyAbsences:
			YearlyAbsences.showGeneralMonthlyAbsences(year, month);
			break;
		case manageAbsenceCode:
			if(personId != 0){
				flash.error("Il metodo %s deve essere chiamato senza selezionare alcuna persona", menuItem.getDescription());
				Application.indexAdmin();
			}
			else
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
			if(personId != 0){
				flash.error("Il metodo %s deve essere chiamato senza selezionare alcuna persona", menuItem.getDescription());
				Application.indexAdmin();
			}
			else
				WorkingTimes.manageWorkingTime();
			break;
		case confParameters:
			if(personId != 0){
				flash.error("Il metodo %s deve essere chiamato senza selezionare alcuna persona", menuItem.getDescription());
				Application.indexAdmin();
			}
			else
				Configurations.list();
			break;
		case personList:
			if(personId != 0){
				flash.error("Il metodo %s deve essere chiamato senza selezionare alcuna persona", menuItem.getDescription());
				Application.indexAdmin();
			}
			else
				Persons.list();
			break;
		case administrator:
			if(personId != 0){
				flash.error("Il metodo %s deve essere chiamato senza selezionare alcuna persona", menuItem.getDescription());
				Application.indexAdmin();
			}
			else
				Administrators.list();
			break;
		
		case dailyPresence:
			if(personId != 0){
				flash.error("Il metodo %s deve essere chiamato senza selezionare alcuna persona", menuItem.getDescription());
				Application.indexAdmin();
			}
			else
				Stampings.dailyPresence(year, month, day);
			break;
		case mealTicketSituation:
			if(personId != 0){
				flash.error("Il metodo %s deve essere chiamato senza selezionare alcuna persona", menuItem.getDescription());
				Application.indexAdmin();
			}
			else
				Stampings.mealTicketSituation(year, month);
			break;
		case manageCompetence:
			Competences.manageCompetenceCode();
			break;
		case uploadSituation:
			if(personId != 0){
				flash.error("Il metodo %s deve essere chiamato senza selezionare alcuna persona", menuItem.getDescription());
				Application.indexAdmin();
			}
			else
				UploadSituation.uploadSituation(year, month);
			break;
		case stampings:
			Stampings.stampings(year, month);
			break;
		case absences:
			Absences.absences(personId, year, month); 
			break;
		case absencesperperson:
			YearlyAbsences.absencesPerPerson(year);
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
		//flash.put("method", menuItem.getDescription());
		params.flash();
	}
}


