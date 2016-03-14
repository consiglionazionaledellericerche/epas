package controllers;


import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDao.PersonLite;
import dao.UsersRolesOfficesDao;

import manager.SecureManager;

import models.Office;
import models.Role;
import models.User;

import org.joda.time.LocalDate;

import controllers.Resecure.NoCheck;
import play.i18n.Messages;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;

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
  @NoCheck
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
    if (user.get().person != null) {

      Set<Office> officeList = secureManager.officesReadAllowed(user.get());
      if (!officeList.isEmpty()) {
        // List<Person> persons = personDao
        // .getActivePersonInMonth(officeList, new YearMonth(year, month));
        List<PersonLite> persons = personDao.liteList(officeList, year, month);
        renderArgs.put("navPersons", persons);
        first = Optional.fromNullable(officeList.iterator().next());
        renderArgs.put("navOffices", officeList);
      }
    } else {

      List<Office> allOffices = officeDao.getAllOffices();
      if (allOffices != null && !allOffices.isEmpty()) {
        // List<Person> persons = personDao.getActivePersonInMonth(
        // Sets.newHashSet(allOffices), new YearMonth(year, month));
        List<PersonLite> persons = personDao.liteList(Sets.newHashSet(allOffices), year, month);
        renderArgs.put("navPersons", persons);
        first = Optional.fromNullable(allOffices.iterator().next());
        renderArgs.put("navOffices", allOffices);
      }
    }

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

    // TODO: un metodo per popolare il menu degli anni umano.
    List<Integer> years = Lists.newArrayList();

    years.add(2016);
    years.add(2015);
    years.add(2014);
    years.add(2013);

    renderArgs.put("navYears", years);


    renderArgs.put("currentData",
        new CurrentData(year, month, day,
            Long.valueOf(session.get("personSelected")),
            Long.valueOf(session.get("officeSelected"))));

  }

  private static void computeActionSelected() {

    final String currentAction = Http.Request.current().action;

    if (currentAction.startsWith("SwitchTemplate")) {
      return;
    }

    session.put("actionSelected", currentAction);

    if (currentAction.startsWith("Stampings.")) {
      if (currentAction.equals("Stampings.stampings")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
      }

      if (currentAction.equals("Stampings.personStamping")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchPerson", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Stampings.missingStamping")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Stampings.holidaySituation")) {
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Stampings.dailyPresence")) {
        renderArgs.put("switchDay", true);
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Stampings.dailyPresenceForPersonInCharge")) {
        renderArgs.put("switchDay", true);
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
      }
    }

    if (currentAction.startsWith("PersonMonths.")) {
      if (currentAction.equals("PersonMonths.trainingHours")) {
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
      }

      if (currentAction.equals("PersonMonths.hourRecap")) {
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
      }
    }

    if (currentAction.startsWith("Vacations.")) {
      if (currentAction.equals("Vacations.show")) {
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
      }
    }

    if (currentAction.startsWith("VacationsAdmin.")) {
      if (currentAction.equals("VacationsAdmin.list")) {
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }
    }

    if (currentAction.startsWith("Persons.")) {
      if (currentAction.equals("Persons.changePassword")) {
        renderArgs.put("dropDown", "dropDownEmployee");
      }
      if (currentAction.equals("Persons.list")) {
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Persons.edit")) {
        renderArgs.put("dropDown", "dropDownAdministration");
      }
    }

    if (currentAction.startsWith("Contracts.")) {
      if (currentAction.equals("Contracts.personContracts")) {
        renderArgs.put("switchPerson", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }
    }

    if (currentAction.startsWith("Absences.")) {
      if (currentAction.equals("Absences.absences")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
      }

      if (currentAction.equals("Absences.manageAttachmentsPerCode")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Absences.manageAttachmentsPerPerson")) {

        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchPerson", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Absences.absenceInPeriod")) {
        renderArgs.put("switchPerson", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Absences.absencesPerPerson")) {
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
      }

      if (currentAction.equals("Absences.showGeneralMonthlyAbsences")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Absences.yearlyAbsences")) {
        renderArgs.put("switchYear", true);
        renderArgs.put("switchPerson", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }
    }

    if (currentAction.startsWith("Competences.")) {
      if (currentAction.equals("Competences.competences")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
      }

      if (currentAction.equals("Competences.showCompetences")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Competences.monthlyOvertime")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
      }

      if (currentAction.equals("Competences.totalOvertimeHours")) {
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Competences.enabledCompetences")) {
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Competences.approvedCompetenceInYear")) {
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("Competences.exportCompetences")) {

        renderArgs.put("dropDown", "dropDownAdministration");
      }

    }

    if (currentAction.startsWith("MonthRecaps.")) {
      if (currentAction.equals("MonthRecaps.show")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("MonthRecaps.showRecaps")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }

      if (currentAction.equals("MonthRecaps.customRecap")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
      }
    }

    if (currentAction.startsWith("UploadSituation.")) {
      if (currentAction.equals("UploadSituation.uploadData")) {
        renderArgs.put("dropDown", "dropDownAdministration");
      }
    }

    if (currentAction.startsWith("WorkingTimes.")) {
      if (currentAction.equals("WorkingTimes.manageWorkingTime")) {
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownConfiguration");
      }
      if (currentAction.equals("WorkingTimes.manageOfficeWorkingTime")) {
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownConfiguration");
      }
    }

    if (currentAction.startsWith("MealTickets.")) {
      if (currentAction.equals("MealTickets.recapMealTickets")) {
        renderArgs.put("dropDown", "dropDownAdministration");
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
      }
      if (currentAction.equals("MealTickets.returnedMealTickets")) {
        renderArgs.put("dropDown", "dropDownAdministration");
        renderArgs.put("switchOffice", true);
      }
    }

    if (currentAction.startsWith("Configurations.")) {
      if (currentAction.equals("Configurations.show")) {
        renderArgs.put("dropDown", "dropDownAdministration");
        renderArgs.put("switchOffice", true);
      }
    }

    if (currentAction.startsWith("MealTickets.")) {
      if (currentAction.equals("MealTickets.personMealTickets")) {
        renderArgs.put("switchPerson", true);
      }
    }

    session.put("actionSelected", currentAction);

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

