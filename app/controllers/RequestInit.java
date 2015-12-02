package controllers;


import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import controllers.Resecure.NoCheck;

import dao.AbsenceTypeDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDao.PersonLite;
import dao.QualificationDao;
import dao.RoleDao;
import dao.StampingDao;
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

import java.util.List;
import java.util.Set;

import javax.inject.Inject;


/**
 * @author cristian.
 */
public class RequestInit extends Controller {

  @Inject
  protected static SecureManager secureManager;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static PersonDao personDao;
  @Inject
  static QualificationDao qualificationDao;
  @Inject
  static AbsenceTypeDao absenceTypeDao;
  @Inject
  static StampingDao stampingDao;
  @Inject
  static RoleDao roleDao;
  @Inject
  static TemplateUtility templateUtility;
  @Inject
  private static UsersRolesOfficesDao uroDao;

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
    String action = computeActionSelected(Http.Request.current().action);
    session.put("actionSelected", action);

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
      officeId = -1l;
    }

    session.put("officeSelected", officeId);

    // day lenght (provvisorio)
    try {

      Integer dayLenght =
              new LocalDate(year, month, day).dayOfMonth().withMaximumValue().getDayOfMonth();
      renderArgs.put("dayLenght", dayLenght);
    } catch (Exception e) {

    }

    // TODO: un metodo per popolare il menu degli anni umano.
    List<Integer> years = Lists.newArrayList();

    years.add(2016);
    years.add(2015);
    years.add(2014);
    years.add(2013);

    renderArgs.put("navYears", years);


    renderArgs.put("currentData", new CurrentData(year, month, day,
            Long.valueOf(session.get("personSelected")), Long.valueOf(session.get("officeSelected"))));

  }

  private static String computeActionSelected(String action) {


    if (action.startsWith("Stampings.")) {


      if (action.equals("Stampings.stampings")) {

        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
        return "Stampings.stampings";
      }

      if (action.equals("Stampings.personStamping")) {

        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchPerson", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Stampings.personStamping";
      }

      if (action.equals("Stampings.missingStamping")) {

        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Stampings.missingStamping";
      }

      if (action.equals("Stampings.holidaySituation")) {
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Stampings.holidaySituation";
      }

      if (action.equals("Stampings.dailyPresence")) {

        renderArgs.put("switchDay", true);
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Stampings.dailyPresence";
      }

      if (action.equals("Stampings.dailyPresenceForPersonInCharge")) {

        renderArgs.put("switchDay", true);
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        return "Stampings.dailyPresenceForPersonInCharge";
      }
    }

    if (action.startsWith("PersonMonths.")) {

      if (action.equals("PersonMonths.trainingHours")) {


        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
        return "PersonMonths.trainingHours";
      }

      if (action.equals("PersonMonths.hourRecap")) {

        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
        return "PersonMonths.hourRecap";
      }
    }

    if (action.startsWith("Vacations.")) {

      if (action.equals("Vacations.show")) {

        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
        return "Vacations.show";
      }
    }

    if (action.startsWith("VacationsAdmin.")) {

      if (action.equals("VacationsAdmin.list")) {
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "VacationsAdmin.list";
      }
    }

    if (action.startsWith("Persons.")) {

      if (action.equals("Persons.changePassword")) {

        renderArgs.put("dropDown", "dropDownEmployee");
        return "Persons.changePassword";
      }
      if (action.equals("Persons.list")) {

        renderArgs.put("dropDown", "dropDownAdministration");
        return "Persons.list";
      }

      if (action.equals("Persons.edit")) {

        renderArgs.put("dropDown", "dropDownAdministration");
        return "Persons.edit";
      }
    }

    if (action.startsWith("Contracts.")) {
      if (action.equals("Contracts.personContracts")) {

        renderArgs.put("switchPerson", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Contracts.personContracts";
      }
    }

    if (action.startsWith("Absences.")) {

      if (action.equals("Absences.absences")) {

        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
        return "Absences.absences";
      }

      if (action.equals("Absences.manageAttachmentsPerCode")) {

        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Absences.manageAttachmentsPerCode";
      }

      if (action.equals("Absences.manageAttachmentsPerPerson")) {

        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchPerson", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Absences.manageAttachmentsPerPerson";
      }

      if (action.equals("Absences.absenceInPeriod")) {
        renderArgs.put("switchPerson", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Absences.absenceInPeriod";
      }
      if (action.equals("Absences.absencesPerPerson")) {

        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
        return "Absences.absencesPerPerson";
      }

      if (action.equals("Absences.showGeneralMonthlyAbsences")) {

        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Absences.showGeneralMonthlyAbsences";
      }

      if (action.equals("Absences.yearlyAbsences")) {

        renderArgs.put("switchYear", true);
        renderArgs.put("switchPerson", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Absences.yearlyAbsences";
      }
    }

    if (action.startsWith("Competences.")) {

      if (action.equals("Competences.competences")) {

        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownEmployee");
        return "Competences.competences";
      }

      if (action.equals("Competences.showCompetences")) {

        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Competences.showCompetences";
      }

      if (action.equals("Competences.monthlyOvertime")) {

        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        return "Competences.monthlyOvertime";
      }

      if (action.equals("Competences.totalOvertimeHours")) {

        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Competences.totalOvertimeHours";
      }

      if (action.equals("Competences.enabledCompetences")) {
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Competences.enabledCompetences";
      }
      
      if (action.equals("Competences.approvedCompetenceInYear")) {
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "Competences.approvedCompetenceInYear";
      }

      if (action.equals("Competences.exportCompetences")) {

        renderArgs.put("dropDown", "dropDownAdministration");
        return "Competences.exportCompetences";
      }

    }

    if (action.startsWith("MonthRecaps.")) {

      if (action.equals("MonthRecaps.show")) {

        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "MonthRecaps.show";
      }

      if (action.equals("MonthRecaps.showRecaps")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "MonthRecaps.showRecaps";
      }

      if (action.equals("MonthRecaps.customRecap")) {
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        renderArgs.put("switchOffice", true);
        renderArgs.put("dropDown", "dropDownAdministration");
        return "MonthRecaps.customRecap";
      }
    }

    if (action.startsWith("UploadSituation.")) {

      if (action.equals("UploadSituation.show")) {

        renderArgs.put("dropDown", "dropDownAdministration");
        return "UploadSituation.show";
      }

      if (action.equals("UploadSituation.loginAttestati")) {

        renderArgs.put("dropDown", "dropDownAdministration");
        return "UploadSituation.loginAttestati";
      }

      if (action.equals("UploadSituation.processAttestati")) {

        renderArgs.put("dropDown", "dropDownAdministration");
        return "UploadSituation.processAttestati";
      }
    }

    if (action.startsWith("WorkingTimes.")) {

      if (action.equals("WorkingTimes.manageWorkingTime")) {

        renderArgs.put("dropDown", "dropDownConfiguration");
        return "WorkingTimes.manageWorkingTime";
      }
    }

    if (action.startsWith("MealTickets.")) {

      if (action.equals("MealTickets.recapMealTickets")) {

        renderArgs.put("dropDown", "dropDownAdministration");
        renderArgs.put("switchMonth", true);
        renderArgs.put("switchYear", true);
        return "MealTickets.recapMealTickets";
      }
    }

    return session.get("actionSelected");
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
          this.viewOffice = true;
          this.viewCompetence = true;
          this.viewWorkingTimeType = true;
          this.viewCompetenceCode = true;
          this.viewAbsenceType = true;
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

