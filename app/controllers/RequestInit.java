package controllers;


import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.OfficeDao;
import dao.PersonDao;
import dao.UsersRolesOfficesDao;

import manager.SecureManager;

import models.Office;
import models.Role;
import models.User;

import org.joda.time.LocalDate;

import play.i18n.Messages;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;


/**
 * @author cristian.
 */
public class RequestInit extends Controller {

  @Inject
  static SecureManager secureManager;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static PersonDao personDao;
  @Inject
  static TemplateUtility templateUtility;
  @Inject
  static UsersRolesOfficesDao uroDao;

  @Before(priority = 1)
  static void injectUtility() {

    renderArgs.put("templateUtility", templateUtility);
  }

  @Before(priority = 1)
  static void injectMenu() {

    Optional<User> user = Security.getUser();

    ItemsPermitted ip = new ItemsPermitted(user);
    renderArgs.put("ip", ip);

    if (!user.isPresent()) {
      return;
    }

    if (user.get().person != null) {
      renderArgs.put("isPersonInCharge", user.get().person.isPersonInCharge);
    }
    computeActionSelected();

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

    // person init //////////////////////////////////////////////////////////////
    Integer personId;
    if (params.get("personId") != null) {
      personId = Integer.valueOf(params.get("personId"));
      session.put("personSelected", personId);
    } else if (session.get("personSelected") != null) {
      personId = Integer.valueOf(session.get("personSelected"));
    } else if (user.get().person != null) {
      session.put("personSelected", user.get().person.id);
    } else {
      session.put("personSelected", 1);
    }

    Optional<Office> first = Optional.<Office>absent();
    Set<Office> officeList = Sets.newHashSet();

    if (user.get().person != null) {

      officeList = secureManager.officesReadAllowed(user.get());
      if (!officeList.isEmpty()) {
        // List<Person> persons = personDao
        // .getActivePersonInMonth(officeList, new YearMonth(year, month));
        List<PersonDao.PersonLite> persons = personDao.liteList(officeList, year, month);
        renderArgs.put("navPersons", persons);
        first = Optional.fromNullable(officeList.iterator().next());
        renderArgs.put("navOffices", officeList);
      }
    } else {

      officeList = Sets.newHashSet(officeDao.getAllOffices());
      if (officeList != null && !officeList.isEmpty()) {
        // List<Person> persons = personDao.getActivePersonInMonth(
        // Sets.newHashSet(allOffices), new YearMonth(year, month));
        List<PersonDao.PersonLite> persons = personDao.liteList(Sets.newHashSet(officeList), year, month);
        renderArgs.put("navPersons", persons);
        first = Optional.fromNullable(officeList.iterator().next());
        renderArgs.put("navOffices", officeList);
      }
    }

    List<Integer> years = Lists.newArrayList();
    int minYear = 0;

    for (Office office : officeList) {
      if (minYear == 0 || office.beginDate.getYear() < minYear) {
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
    } else if (first.isPresent()) {

      officeId = first.get().id;
    } else {
      officeId = -1L;
    }

    session.put("officeSelected", officeId);

    // day lenght (provvisorio)
    try {

      Integer dayLenght =
          new LocalDate(year, month, day).dayOfMonth().withMaximumValue().getDayOfMonth();
      renderArgs.put("dayLenght", dayLenght);
    } catch (Exception e) {
      //FIXME: perché è previsto il tracciamento di questa eccezione??
    }

    renderArgs.put("currentData",
        new CurrentData(year, month, day,
            Long.valueOf(session.get("personSelected")),
            Long.valueOf(session.get("officeSelected"))));

  }

  private static void computeActionSelected() {

    final String currentAction = Http.Request.current().action;

    renderArgs.put("actionSelected", currentAction);
    session.put("actionSelected", currentAction);

    final Collection<String> dayMonthYearSwitcher = ImmutableList.of(
        "Stampings.dailyPresence",
        "Stampings.dailyPresenceForPersonInCharge");

    final Collection<String> monthYearSwitcher = ImmutableList.of(
        "Stampings.stampings",
        "Absences.absences",
        "Competences.competences",
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
        "MealTickets.recapMealTickets");

    final Collection<String> yearSwitcher = ImmutableList.of(
        "Absences.yearlyAbsences",
        "Absences.absencesPerPerson",
        "Competences.totalOvertimeHours",
        "Competences.approvedCompetenceInYear",
        "Stampings.holidaySituation",
        "PersonMonths.trainingHours",
        "PersonMonths.hourRecap",
        "Vacations.show",
        "VacationsAdmin.list");

    final Collection<String> personSwitcher = ImmutableList.of(
        "Stampings.personStamping",
        "Absences.manageAttachmentsPerPerson",
        "Contracts.personContracts",
        "Absences.absenceInPeriod",
        "Absences.yearlyAbsences",
        "MealTickets.personMealTickets",
        "MealTickets.editPersonMealTickets",
        "MealTickets.recapPersonMealTickets");

    final Collection<String> officeSwitcher = ImmutableList.of(
        "Stampings.missingStamping",
        "Stampings.dailyPresence",
        "VacationsAdmin.list",
        "Absences.showGeneralMonthlyAbsences",
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
        "Configurations.show");

    final Collection<String> dropDownEmployeeActions = ImmutableList.of(
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
        "Configurations.show");

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

    if (personSwitcher.contains(currentAction)) {
      renderArgs.put("switchPerson", true);
    }
    if (officeSwitcher.contains(currentAction)) {
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

  private static void switchYear() {

    Set<Office> officeList = secureManager.officesReadAllowed(Security.getUser().get());

    List<Integer> years = Lists.newArrayList();
    int minYear = 0;

    for (Office office : officeList) {
      if (minYear == 0 || office.beginDate.getYear() < minYear) {
        minYear = office.beginDate.getYear();
      }

      for (int i = minYear; i <= LocalDate.now().getYear(); i++) {
        years.add(i);
      }

      renderArgs.put("navYears", years);
      renderArgs.put("switchYear", true);
    }
  }

  private static void officeSwitcher() {


//    Optional<Office> first = Optional.<Office>absent();

//    int minYear = 0;
//
//    if (user.get().person != null) {
//
//      Set<Office> officeList = secureManager.officesReadAllowed(user.get());
//
//      for (Office office : officeList) {
//        if (minYear == 0 || office.beginDate.getYear() < minYear) {
//          minYear = office.beginDate.getYear();
//        }
//      }
//
//      if (!officeList.isEmpty()) {
//        // List<Person> persons = personDao
//        // .getActivePersonInMonth(officeList, new YearMonth(year, month));
//        List<PersonLite> persons = personDao.liteList(officeList, year, month);
//        renderArgs.put("navPersons", persons);
//        first = Optional.fromNullable(officeList.iterator().next());
//        renderArgs.put("navOffices", officeList);
//      }
//    } else {
//
//      List<Office> allOffices = officeDao.getAllOffices();
//
//      for (Office office : allOffices) {
//        if (minYear == 0 || office.beginDate.getYear() < minYear) {
//          minYear = office.beginDate.getYear();
//        }
//      }
//
//      if (allOffices != null && !allOffices.isEmpty()) {
//        // List<Person> persons = personDao.getActivePersonInMonth(
//        // Sets.newHashSet(allOffices), new YearMonth(year, month));
//        List<PersonLite> persons = personDao.liteList(Sets.newHashSet(allOffices), year, month);
//        renderArgs.put("navPersons", persons);
//        first = Optional.fromNullable(allOffices.iterator().next());
//        renderArgs.put("navOffices", allOffices);
//      }
//    }

  }

  /**
   * Oggetto che modella i permessi abilitati per l'user TODO: esportare questa classe in un nuovo
   * file che modella la view.
   */
  public static class ItemsPermitted {

    public boolean isEmployee = false;

    public boolean isDeveloper = false;

    public boolean viewPerson = false;
    public boolean viewPersonDay = false;
    public boolean viewOffice = false;
    public boolean viewCompetence = false;
    public boolean editCompetence = false;
    public boolean uploadSituation = false;
    public boolean viewWorkingTimeType = false;
    public boolean editWorkingTimeType = false;
    public boolean viewAbsenceType = false;
    public boolean editAbsenceType = false;
    public boolean viewCompetenceCode = false;
    public boolean editCompetenceCode = false;


    public ItemsPermitted(Optional<User> user) {

      if (!user.isPresent()) {
        return;
      }

      List<Role> roles = uroDao.getUserRole(user.get());

      for (Role role : roles) {

        if (role.name.equals(Role.ADMIN)) {
          this.viewPerson = true;
          this.viewOffice = true;
          this.viewWorkingTimeType = true;

        } else if (role.name.equals(Role.DEVELOPER)) {
          this.isDeveloper = true;
          this.viewPerson = true;
          this.viewOffice = true;
          this.viewWorkingTimeType = true;

        } else if (role.name.equals(Role.EMPLOYEE)) {
          this.isEmployee = true;
        }

        if (this.isDeveloper || role.name.equals(Role.PERSONNEL_ADMIN_MINI)
            || role.name.equals(Role.PERSONNEL_ADMIN)) {
          this.viewPerson = true;
          this.viewPersonDay = true;
          // this.viewOffice = true;
          this.viewCompetence = true;
          this.viewWorkingTimeType = true;
          this.viewCompetenceCode = true;
          this.viewAbsenceType = true;
        }

        if (role.name.equals(Role.TECNICAL_ADMIN)) {
          this.viewOffice = true;
        }

        if (this.isDeveloper || role.name.equals(Role.PERSONNEL_ADMIN)) {
          this.editCompetence = true;
          this.uploadSituation = true;
          this.editCompetenceCode = true;
          this.editAbsenceType = true;
          this.editWorkingTimeType = true;
        }
      }
    }

    /**
     * Se l'user può vedere il menu del Employee.
     */
    public boolean isEmployeeVisible() {
      return isEmployee;
    }

    /**
     * Se l'user ha i permessi per vedere Amministrazione.
     */
    public boolean isAdministrationVisible() {

      return viewPerson || viewPersonDay || viewCompetence || uploadSituation;
    }

    /**
     * Se l'user ha i permessi per vedere Configurazione.
     */
    public boolean isConfigurationVisible() {

      return viewOffice || viewWorkingTimeType || viewAbsenceType;
    }

    /**
     * Se l'user ha i permessi per vedere Tools.
     */
    public boolean isToolsVisible() {
      return isDeveloper;
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

    public String getMonthLabel() {
      return Messages.get("Month." + month);
    }
  }

}

