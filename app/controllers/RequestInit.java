package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dao.OfficeDao;
import dao.PersonDao;
import dao.UserDao;
import helpers.TemplateDataInjector;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import manager.SecureManager;
import models.Office;
import models.User;
import models.flows.enumerate.CompetenceRequestType;
import org.joda.time.LocalDate;
import play.i18n.Messages;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.With;


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

    // Tutti gli uffici sui quali si ha ruolo di amministrazione (tecnico e personale)
    Set<Office> offices = secureManager.officesForNavbar(currentUser);

    // person init //////////////////////////////////////////////////////////////
    Long personId = null;
    if (params.get("personId") != null) {
      personId = Long.parseLong(params.get("personId"));
    } else if (session.get("personSelected") != null) {
      personId = Long.parseLong(session.get("personSelected"));
    } else if (currentUser.person != null) {
      personId = currentUser.person.id;
    } else {
      val personList = personDao.liteList(offices, year, month);
      if (!personList.isEmpty()) {
        personId = personList.iterator().next().id;
        session.put("personSelected", personId);
      }
    }
    
    // CompetenceRequestType init /////////////////////////////////////////////////
    CompetenceRequestType competenceType = null;
    if (params.get("competenceType") != null) {
      competenceType = CompetenceRequestType.valueOf(params.get("competenceType"));
    } else if (session.get("competenceType") != null) {
      competenceType = CompetenceRequestType.valueOf(session.get("competenceType"));      
    } else {      
      competenceType = CompetenceRequestType.OVERTIME_REQUEST;
      session.put("competenceType", CompetenceRequestType.OVERTIME_REQUEST);
    }

    // Popolamento del dropdown degli anni
    List<Integer> years = Lists.newArrayList();
    int minYear = LocalDate.now().getYear();
    for (Office office : offices) {
      if (office.beginDate.getYear() < minYear) {
        minYear = office.beginDate.getYear();
      }
    }
    // Oltre alle sedi amminisitrate anche gli anni della propria sede per le viste dipendente.
    if (user.get().person != null && user.get().person.office != null) {
      minYear = user.get().person.office.beginDate.getYear();
    }
    for (int i = minYear; i <= LocalDate.now().plusYears(1).getYear(); i++) {
      years.add(i);
    }
    renderArgs.put("navYears", years);

    // office init (l'office selezionato, andrà combinato con la lista persone sopra)

    Long officeId = null;
    if (params.get("officeId") != null) {
      officeId = Long.valueOf(params.get("officeId"));
      //la not equals("null") è a causa di un problema con il play 1.4.2
    } else if (session.get("officeSelected") != null 
        && !session.get("officeSelected").equals("null")) {
      officeId = Long.valueOf(session.get("officeSelected"));
    } else if (!offices.isEmpty()) {
      officeId = offices.stream()
            .sorted((o, o1) -> o.name.compareTo(o1.name)).findFirst().get().id;        
    } else if (currentUser.person != null && currentUser.person.office != null) {
      officeId = currentUser.person.office.id;      
    }
    
    session.put("officeSelected", officeId);

    //TODO: Da offices rimuovo la sede di cui ho solo il ruolo employee

    computeActionSelected(currentUser, offices, year, month, day, personId, officeId, competenceType);
    renderArgs.put("currentData", new CurrentData(year, month, day, personId, officeId, competenceType));
  }

  private static void computeActionSelected(
      User user, Set<Office> offices, Integer year, Integer month, Integer day, 
      Long personId, Long officeId, CompetenceRequestType type) {

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
        "Charts.overtimeOnPositiveResidual",
        "Charts.listForExcelFile",
        "Charts.exportTimesheetSituation",
        "AbsenceGroups.absenceTroubles",
        "Stampings.stampingsByAdmin",
        "PrintTags.listPersonForPrintTags",
        "CompetenceRequests.blank",
        "CompetenceRequests.edit",
        "CompetenceRequests.show",
        "CompetenceRequests.list");


    final Collection<String> yearSwitcher = ImmutableList.of(
        "Absences.yearlyAbsences",
        "Absences.absencesPerPerson",
        "Competences.totalOvertimeHours",
        "Competences.approvedCompetenceInYear",
        "Stampings.holidaySituation",
        "PersonMonths.trainingHours",
        "PersonMonths.hourRecap",
        "Vacations.show",
        "Vacations.list",
        "Certifications.certifications",
        "Certifications.processAll",
        "Certifications.emptyCertifications",
        "Charts.overtimeOnPositiveResidualInYear",
        "AbsenceGroups.certificationsAbsences");

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
        "AbsenceGroups.certificationsAbsences");

    final Collection<String> officeSwitcher = ImmutableList.of(
        "Stampings.missingStamping",
        "Stampings.dailyPresence",
        "Vacations.list",
        "Absences.showGeneralMonthlyAbsences",
        "Absences.manageAttachmentsPerCode",
        "Competences.showCompetences",
        "Competences.totalOvertimeHours",
        "Competences.enabledCompetences",
        "Competences.approvedCompetenceInYear",
        "Competences.exportCompetences",
        "Competences.getOvertimeInYear",
        "MonthRecaps.showRecaps",
        "MonthRecaps.customRecap",
        "WorkingTimes.manageWorkingTime",
        "WorkingTimes.manageOfficeWorkingTime",
        "MealTickets.recapMealTickets",
        "MealTickets.returnedMealTickets",
        "Configurations.show",
        "Synchronizations.people",
        "Synchronizations.badges",
        "Synchronizations.eppn",
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
        "Charts.exportTimesheetSituation",
        "Competences.activateServices",
        "Groups.showGroups",
        "Contracts.initializationsStatus",
        "Contracts.initializationsVacation",
        "Contracts.initializationsMeal",
        "AbsenceGroups.absenceInitializations",
        "AbsenceGroups.absenceTroubles",
        "AbsenceGroups.importCertificationsAbsences",
        "Stampings.stampingsByAdmin",
        "PrintTags.listPersonForPrintTags",
        "TimeVariations.show");

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
        "Vacations.list",
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
        "PersonMonths.visualizePeopleTrainingHours",
        "Contracts.initializationsStatus",
        "Contracts.initializationsMeal",
        "AbsenceGroups.absenceInitializations",
        "AbsenceGroups.absenceTroubles",
        "TimeVariations.show");

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
      //Patch: caso in cui richiedo una operazione con switch person (ex il tabellone timbrature) 
      //su me stesso, ma la mia sede non appartiene alle sedi che amministro
      //OSS: le action switch person sono tutte in sola lettura quindi il redirect è poco rischioso
      if (!offices.isEmpty() && user.person != null && user.person.id.equals(personId)) {
        if (!offices.contains(user.person.office)) {
          Long personSelected = persons.iterator().next().id;
          session.put("personSelected", personSelected);
          Map<String, Object> args = Maps.newHashMap();
          args.put("year", year);
          args.put("month", month);
          args.put("day", day);
          args.put("personId", personSelected);
          args.put("officeId", officeId);
          redirect(Router.reverse(currentAction, args).url);
        }
      }
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
    public final CompetenceRequestType type;

    CurrentData(Integer year, Integer month, Integer day, Long personId, Long officeId,
        CompetenceRequestType type) {
      this.year = year;
      this.month = month;
      this.day = day;
      this.personId = personId;
      this.officeId = officeId;
      this.type = type;
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

