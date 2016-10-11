package controllers;


import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import dao.OfficeDao;
import dao.PersonDao;
import dao.UserDao;

import helpers.TemplateDataInjector;

import manager.SecureManager;

import models.Office;
import models.User;

import org.joda.time.LocalDate;

import play.i18n.Messages;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;


/**
 * @author cristian.
 */
@With(TemplateDataInjector.class)
public class RequestInit extends Controller {

  @Inject
  static SecureManager secureManager;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static PersonDao personDao;
  @Inject
  static UserDao userDao;

  @Before(priority = 1)
  static void injectMenu() {

    Optional<User> user = Security.getUser();

    if (!user.isPresent()) {
      return;
    }

    final User currentUser = user.get();

    renderArgs.put("currentUser", currentUser);

    // year init /////////////////////////////////////////////////////////////////
    Integer year;
    if (params.get("year") != null) {
      year = Integer.valueOf(params.get("year"));
    } else if (session.get("yearSelected") != null) {
      year = Integer.valueOf(session.get("yearSelected"));
    } else {
      year = LocalDate.now().getYear();
    }

    session.put("yearSelected", year);

    // month init ////////////////////////////////////////////////////////////////
    Integer month;
    if (params.get("month") != null) {
      month = Integer.valueOf(params.get("month"));
    } else if (session.get("monthSelected") != null) {
      month = Integer.valueOf(session.get("monthSelected"));
    } else {
      month = LocalDate.now().getMonthOfYear();
    }

    session.put("monthSelected", month);

    // day init //////////////////////////////////////////////////////////////////
    Integer day;
    if (params.get("day") != null) {
      day = Integer.valueOf(params.get("day"));
    } else if (session.get("daySelected") != null) {
      day = Integer.valueOf(session.get("daySelected"));
    } else {
      day = LocalDate.now().getDayOfMonth();
    }

    session.put("daySelected", day);

    // Tutti gli uffici sui quali si ha almeno un ruolo (qualsiasi)
    Set<Office> offices = secureManager.ownOffices(currentUser);

    // person init //////////////////////////////////////////////////////////////
    Long personId;
    if (params.get("personId") != null) {
      personId = Long.parseLong(params.get("personId"));
    } else if (session.get("personSelected") != null) {
      personId = Long.parseLong(session.get("personSelected"));
    } else if (currentUser.person != null) {
      personId = currentUser.person.id;
    } else {
      personId = personDao.liteList(offices, year, month).iterator().next().id;
    }

    session.put("personSelected", personId);

    // Popolamento del dropdown degli anni
    List<Integer> years = Lists.newArrayList();
    int minYear = LocalDate.now().getYear();

    for (Office office : offices) {
      if (office.beginDate.getYear() < minYear) {
        minYear = office.beginDate.getYear();
      }
    }
    for (int i = minYear; i <= LocalDate.now().getYear(); i++) {
      years.add(i);
    }
    renderArgs.put("navYears", years);

    // office init (l'office selezionato, andrà combinato con la lista persone sopra)

    Long officeId;
    if (params.get("officeId") != null) {
      officeId = Long.valueOf(params.get("officeId"));
    } else if (session.get("officeSelected") != null) {
      officeId = Long.valueOf(session.get("officeSelected"));
    } else {
      officeId = offices.stream().sorted((o, o1) -> o.name.compareTo(o1.name)).findFirst().get().id;
    }

    session.put("officeSelected", officeId);

    //TODO: Da offices rimuovo la sede di cui ho solo il ruolo employee

    computeActionSelected(currentUser, offices, year, month);
    renderArgs.put("currentData", new CurrentData(year, month, day, personId, officeId));
  }

  private static void computeActionSelected(User user, Set<Office> offices, Integer year, Integer month) {

    final String currentAction = Http.Request.current().action;

    renderArgs.put("actionSelected", currentAction);
    session.put("actionSelected", currentAction);

    final Collection<String> dayMonthYearSwitcher = ImmutableList.of(
        "Stampings.dailyPresence",
        "Absences.absencesVisibleForEmployee",
        "Stampings.dailyPresenceForPersonInCharge");

    final Collection<String> monthYearSwitcher = ImmutableList.of(
        "Stampings.stampings",
        "Stampings.insertWorkingOffSitePresence",
        "Absences.absences",
        "Competences.competences",
        "Competences.enabledCompetences",
        "Stampings.personStamping",
        "Absences.manageAttachmentsPerPerson",
        "Stampings.missingStamping", "Stampings.dailyPresence",
        "Stampings.dailyPresenceForPersonInCharge",
        "Absences.manageAttachmentsPerCode",
        "Absences.showGeneralMonthlyAbsences",
        "Competences.showCompetences",
        "Competences.monthlyOvertime",
        "MonthRecaps.show",
        "MonthRecaps.showRecaps",
        "MonthRecaps.customRecap",
        "MealTickets.recapMealTickets",
        "Certifications.certifications",
        "Certifications.processAll",
        "Certifications.emptyCertifications",
        "PersonMonths.visualizePeopleTrainingHours",
        "Absences.forceAbsences",
        "Charts.overtimeOnPositiveResidual",
        "Charts.listForExcelFile");


    final Collection<String> yearSwitcher = ImmutableList.of(
        "Absences.yearlyAbsences",
        "Absences.absencesPerPerson",
        "Competences.totalOvertimeHours",
        "Competences.approvedCompetenceInYear",
        "Stampings.holidaySituation",
        "PersonMonths.trainingHours",
        "PersonMonths.hourRecap",
        "Vacations.show",
        "VacationsAdmin.list",
        "Certifications.certifications",
        "Certifications.processAll",
        "Certifications.emptyCertifications",
        "Charts.overtimeOnPositiveResidualInYear");

    final Collection<String> personSwitcher = ImmutableList.of(
        "Stampings.personStamping",
        "Absences.manageAttachmentsPerPerson",
        "Contracts.personContracts",
        "Configurations.personShow",
        "BadgeSystems.personBadges",
        "Persons.children",
        "Persons.edit",
        "Absences.absenceInPeriod",
        "Absences.yearlyAbsences",
        "MealTickets.personMealTickets",
        "MealTickets.editPersonMealTickets",
        "MealTickets.recapPersonMealTickets",
        "Absences.forceAbsences");

    final Collection<String> officeSwitcher = ImmutableList.of(
        "Stampings.missingStamping",
        "Stampings.dailyPresence",
        "VacationsAdmin.list",
        "Absences.showGeneralMonthlyAbsences",
        "Absences.manageAttachmentsPerCode",
        "Competences.showCompetences",
        "Competences.totalOvertimeHours",
        "Competences.enabledCompetences",
        "Competences.approvedCompetenceInYear",
        "MonthRecaps.showRecaps",
        "MonthRecaps.customRecap",
        "WorkingTimes.manageWorkingTime",
        "WorkingTimes.manageOfficeWorkingTime",
        "MealTickets.recapMealTickets",
        "MealTickets.returnedMealTickets",
        "Configurations.show",
        "Synchronizations.people",
        "Synchronizations.oldPeople",
        "Synchronizations.activeContracts",
        "Synchronizations.oldActiveContracts",
        "Certifications.certifications",
        "Certifications.processAll",
        "Certifications.emptyCertifications",
        "PersonMonths.visualizePeopleTrainingHours",
        "Persons.list",
        "Charts.checkLastYearAbsences",
        "Charts.overtimeOnPositiveResidual",
        "Charts.overtimeOnPositiveResidualInYear",
        "Charts.listForExcelFile",
        "Competences.activateServices");

    final Collection<String> dropDownEmployeeActions = ImmutableList.of(
        "Stampings.insertWorkingOffSitePresence",
        "Stampings.stampings",
        "Absences.absences",
        "Competences.competences",
        "PersonMonths.trainingHours",
        "PersonMonths.hourRecap",
        "Vacations.show",
        "Persons.changePassword",
        "Absences.absencesPerPerson");

    final Collection<String> dropDownAdministrationActions = ImmutableList.of(
        "Stampings.personStamping",
        "Absences.manageAttachmentsPerPerson",
        "Stampings.missingStamping",
        "Stampings.holidaySituation",
        "Stampings.dailyPresence",
        "VacationsAdmin.list",
        "Persons.list",
        "Persons.edit",
        "Contracts.personContracts",
        "Absences.manageAttachmentsPerCode",
        "Absences.absenceInPeriod",
        "Absences.showGeneralMonthlyAbsences",
        "Absences.yearlyAbsences",
        "Competences.showCompetences",
        "Competences.totalOvertimeHours",
        "Competences.enabledCompetences",
        "Competences.approvedCompetenceInYear",
        "Competences.exportCompetences",
        "MonthRecaps.show",
        "MonthRecaps.showRecaps",
        "MonthRecaps.customRecap",
        "UploadSituation.uploadData",
        "MealTickets.recapMealTickets",
        "MealTickets.returnedMealTickets",
        "Configurations.show",
        "Certifications.certifications",
        "Certifications.processAll",
        "PersonMonths.visualizePeopleTrainingHours");

    final Collection<String> dropDownConfigurationActions = ImmutableList.of(
        "WorkingTimes.manageWorkingTime",
        "WorkingTimes.manageOfficeWorkingTime");

    if (dayMonthYearSwitcher.contains(currentAction)) {
      renderArgs.put("switchDay", true);
      renderArgs.put("switchMonth", true);
      renderArgs.put("switchYear", true);
    }
    if (monthYearSwitcher.contains(currentAction)) {
      renderArgs.put("switchMonth", true);
      renderArgs.put("switchYear", true);
    }
    if (yearSwitcher.contains(currentAction)) {
      renderArgs.put("switchYear", true);
    }

    if (personSwitcher.contains(currentAction) && userDao.hasAdminRoles(user)) {
      List<PersonDao.PersonLite> persons = personDao.liteList(offices, year, month);
      renderArgs.put("navPersons", persons);
      renderArgs.put("switchPerson", true);
    }
    if (officeSwitcher.contains(currentAction)) {

      renderArgs.put("navOffices", offices.stream().sorted((o, o1) -> o.name.compareTo(o1.name))
          .collect(Collectors.toList()));
      renderArgs.put("switchOffice", true);
    }
    if (dropDownEmployeeActions.contains(currentAction)) {
      renderArgs.put("dropDown", "dropDownEmployee");
    }
    if (dropDownAdministrationActions.contains(currentAction)) {
      renderArgs.put("dropDown", "dropDownAdministration");
    }
    if (dropDownConfigurationActions.contains(currentAction)) {
      renderArgs.put("dropDown", "dropDownConfiguration");
    }

  }

  /**
   * Contiene i dati di sessione raccolti per il template.
   *
   * @author alessandro
   */
  public static class CurrentData {
    public final Integer year;
    public final Integer month;
    public final Integer day;
    public final Long personId;
    public final Long officeId;

    CurrentData(Integer year, Integer month, Integer day, Long personId, Long officeId) {
      this.year = year;
      this.month = month;
      this.day = day;
      this.personId = personId;
      this.officeId = officeId;
    }

    /**
     * Il day in sessione per il mese passato, oppure il massimo se non appartiene al range.
     */
    public Integer getDayOfMonth(Integer month) {
      try {
        new LocalDate(year, month, day);
      } catch (Exception ex) {
        return new LocalDate(year, month, 1).dayOfMonth().withMaximumValue().getDayOfMonth();
      }
      return day;
    }

    /**
     * Il numero massimo di giorni per il mese in sessione.
     */
    public Integer daysInMonth() {
      return new LocalDate(year, month, day).dayOfMonth().withMaximumValue().getDayOfMonth();
    }

    public String getMonthLabel() {
      return Messages.get("Month." + month);
    }
  }

}

