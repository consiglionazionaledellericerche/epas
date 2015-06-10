package controllers;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

@With(RequestInit.class)
public class SwitchTemplate extends Controller{

	public static final String USERNAME_SESSION_KEY = "username";

	private static void executeAction(String action) {

		Integer year = Integer.parseInt(session.get("yearSelected"));
		Integer month = Integer.parseInt(session.get("monthSelected"));
		Integer day = Integer.parseInt(session.get("daySelected"));
		Long personId = Long.parseLong(session.get("personSelected"));

		session.put("actionSelected", action);

		if(action.equals("Stampings.stampings")) {

			Stampings.stampings(year, month);
		}

		if(action.equals("Stampings.personStamping")) {

			Stampings.personStamping(personId, year, month);
		}

		if(action.equals("PersonMonths.trainingHours")) {

			PersonMonths.trainingHours(year);
		}

		if(action.equals("PersonMonths.hourRecap")) {

			PersonMonths.hourRecap(year);
		}

		if(action.equals("Vacations.show")) {

			Vacations.show(year);
		}

		if(action.equals("Persons.changePassword")) {

			Persons.changePassword();
		}

		if(action.equals("Absences.absences")) {

			Absences.absences(year, month);
		}

		if(action.equals("YearlyAbsences.absencesPerPerson")) {

			YearlyAbsences.absencesPerPerson(year);
		}

		if(action.equals("Competences.competences")) {

			Competences.competences(year, month);
		}

		if(action.equals("Competences.showCompetences")) {

			Competences.showCompetences(year, month, null, null,  null,  null);
		}

		if(action.equals("Competences.overtime")) {

			Competences.overtime(year, month, null, null,  null);
		}

		if(action.equals("Competences.totalOvertimeHours")) {

			Competences.totalOvertimeHours(year, null);
		}

		if(action.equals("Competences.enabledCompetences")) {

			Competences.enabledCompetences(null, null);
		}

		if(action.equals("Competences.exportCompetences")) {

			Competences.exportCompetences();
		}

		if(action.equals("Stampings.missingStamping")) {

			Stampings.missingStamping(year, month);
		}

		if(action.equals("Stampings.dailyPresence")) {

			Stampings.dailyPresence(year, month, day);
		}

		if(action.equals("Stampings.dailyPresenceForPersonInCharge")) {

			Stampings.dailyPresenceForPersonInCharge(year, month, day);
		}
		
		if(action.equals("Competences.monthlyOvertime")) {

			Competences.monthlyOvertime(year, month, null, null);
		}

		if(action.equals("Stampings.mealTicketSituation")) {

			Stampings.mealTicketSituation(year, month, null, null);
		}

		if(action.equals("UploadSituation.show")) {

			UploadSituation.show();
		}

		if(action.equals("YearlyAbsences.showGeneralMonthlyAbsences")) {

			YearlyAbsences.showGeneralMonthlyAbsences(year, month, null, null);
		}

		if(action.equals("YearlyAbsences.yearlyAbsences")) {

			YearlyAbsences.yearlyAbsences(personId, year);
		}

		if(action.equals("Absences.manageAttachmentsPerCode")) {

			Absences.manageAttachmentsPerCode(year, month);
		}

		if(action.equals("Absences.manageAttachmentsPerPerson")) {

			Absences.manageAttachmentsPerPerson(personId, year, month);
		}

		if(action.equals("Absences.absenceInPeriod")) {

			Absences.absenceInPeriod(null,null,null);
		}

		if(action.equals("WorkingTimes.manageWorkingTime")) {

			WorkingTimes.manageWorkingTime(null);
		}


	}

	public static void updateDay(Integer day) throws Throwable {

		String action = session.get("actionSelected");
		if( action==null ) {

			flash.error("La sessione è scaduta. Effettuare nuovamente login.");
			Secure.login();
		}

		if(day == null || day < 1 || day > 31) {

			Application.index();	
		}

		executeAction(action);

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

	// esempio se si volesse utilizzare l'anno nei parametri.
	public static int currentYear() {
		Integer year = request.params.get("year", Integer.class);
		if (year == null) {
			return LocalDate.now().getYear();
		} else {
			return year;
		}
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

		if(personId == null ) {	

			Application.index();	
		}

		session.put("personSelected", personId);

		executeAction(action);

	}


}


