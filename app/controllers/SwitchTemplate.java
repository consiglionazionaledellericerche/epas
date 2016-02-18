package controllers;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

@With(RequestInit.class)
public class SwitchTemplate extends Controller {

  public static final String USERNAME_SESSION_KEY = "username";

  private static void executeAction(String action) {

    Integer year = Integer.parseInt(session.get("yearSelected"));
    Integer month = Integer.parseInt(session.get("monthSelected"));
    Integer day = Integer.parseInt(session.get("daySelected"));
    Long personId = Long.parseLong(session.get("personSelected"));
    Long officeId = Long.parseLong(session.get("officeSelected"));

    session.put("actionSelected", action);

    if (action.equals("Stampings.stampings")) {

      Stampings.stampings(year, month);
    }

    if (action.equals("Stampings.personStamping")) {

      Stampings.personStamping(personId, year, month);
    }

    if (action.equals("Contracts.personContracts")) {
      Contracts.personContracts(personId);
    }

    if (action.equals("PersonMonths.trainingHours")) {

      PersonMonths.trainingHours(year);
    }

    if (action.equals("PersonMonths.hourRecap")) {

      PersonMonths.hourRecap(year);
    }

    if (action.equals("Vacations.show")) {

      Vacations.show(year);
    }

    if (action.equals("VacationsAdmin.list")) {

      VacationsAdmin.list(year, officeId);
    }


    if (action.equals("Persons.changePassword")) {

      Persons.changePassword();
    }

    if (action.equals("Absences.absences")) {

      Absences.absences(year, month);
    }

    if (action.equals("Absences.absencesPerPerson")) {

      Absences.absencesPerPerson(year);
    }

    if (action.equals("Absences.showGeneralMonthlyAbsences")) {

      Absences.showGeneralMonthlyAbsences(year, month, officeId);
    }

    if (action.equals("Absences.yearlyAbsences")) {

      Absences.yearlyAbsences(personId, year);
    }

    if (action.equals("Absences.absenceInPeriod")) {

      Absences.absenceInPeriod(personId, null, null);
    }

    if (action.equals("Competences.competences")) {

      Competences.competences(year, month);
    }
    
    if (action.equals("Configurations.show")) {
      Configurations.show(officeId);
    }
    
    if (action.equals("Competences.showCompetences")) {

      Competences.showCompetences(year, month, officeId, null, null, null);
    }

    if (action.equals("Competences.totalOvertimeHours")) {

      Competences.totalOvertimeHours(year, officeId);
    }

    if (action.equals("Competences.enabledCompetences")) {

      Competences.enabledCompetences(officeId);
    }

    if (action.equals("Competences.approvedCompetenceInYear")) {

      Competences.approvedCompetenceInYear(year, false, officeId);
    }

    if (action.equals("Competences.exportCompetences")) {

      Competences.exportCompetences();
    }

    if (action.equals("Stampings.missingStamping")) {

      Stampings.missingStamping(year, month, officeId);
    }

    if (action.equals("Stampings.dailyPresence")) {

      Stampings.dailyPresence(year, month, day, officeId);
    }

    if (action.equals("Stampings.dailyPresenceForPersonInCharge")) {

      Stampings.dailyPresenceForPersonInCharge(year, month, day);
    }

    if (action.equals("Competences.monthlyOvertime")) {

      Competences.monthlyOvertime(year, month, null, null);
    }

    if (action.equals("Absences.manageAttachmentsPerCode")) {

      Absences.manageAttachmentsPerCode(year, month);
    }

    if (action.equals("Absences.manageAttachmentsPerPerson")) {

      Absences.manageAttachmentsPerPerson(personId, year, month);
    }

    if (action.equals("Absences.absenceInPeriod")) {

      Absences.absenceInPeriod(null, null, null);
    }

    if (action.equals("WorkingTimes.manageWorkingTime")) {

      WorkingTimes.manageWorkingTime(officeId);
    }
    if (action.equals("WorkingTimes.manageOfficeWorkingTime")) {

      WorkingTimes.manageOfficeWorkingTime(officeId);
    }
    if (action.equals("MealTickets.recapMealTickets")) {

      MealTickets.recapMealTickets(year, month, officeId);
    }

    if (action.equals("MonthRecaps.showRecaps")) {

      MonthRecaps.showRecaps(year, month, officeId);
    }

    if (action.equals("MonthRecaps.customRecap")) {

      MonthRecaps.customRecap(year, month, officeId);
    }


  }

  public static void updateDay(Integer day) throws Throwable {

    String action = session.get("actionSelected");
    if (action == null) {

      flash.error("La sessione è scaduta. Effettuare nuovamente login.");
      Secure.login();
    }

    if (day == null || day < 1 || day > 31) {

      Application.index();
    }

    executeAction(action);

  }


  public static void updateMonth(Integer month) throws Throwable {

    String action = session.get("actionSelected");
    if (action == null) {

      flash.error("La sessione è scaduta. Effettuare nuovamente login.");
      Secure.login();
    }

    if (month == null || month < 1 || month > 12) {

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
    if (action == null) {

      flash.error("La sessione è scaduta. Effettuare nuovamente login.");
      Secure.login();
    }

    if (year == null) { /* TODO check bound year */

      Application.index();
    }

    session.put("yearSelected", year);

    executeAction(action);

  }

  public static void updatePerson(Long personId) throws Throwable {

    String action = session.get("actionSelected");
    if (action == null) {

      flash.error("La sessione è scaduta. Effettuare nuovamente login.");
      Secure.login();
    }

    if (personId == null) {

      Application.index();
    }

    session.put("personSelected", personId);

    executeAction(action);
  }

  public static void updateOffice(Long officeId) throws Throwable {

    String action = session.get("actionSelected");
    if (action == null) {
      flash.error("La sessione è scaduta. Effettuare nuovamente login.");
      Secure.login();
    }
    if (officeId == null) {
      Application.index();
    }

    session.put("officeSelected", officeId);

    executeAction(action);

  }


}


