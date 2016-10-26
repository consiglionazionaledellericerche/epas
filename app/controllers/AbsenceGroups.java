
package controllers;

import dao.PersonDao;
import dao.UserDao;
import dao.history.AbsenceHistoryDao;
import dao.history.HistoryValue;

import lombok.extern.slf4j.Slf4j;

import manager.AbsenceManager;
import manager.ConsistencyManager;
import manager.PersonDayManager;
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceForm.AbsenceInsertTab;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import manager.services.absences.model.PeriodChain;

import models.Person;
import models.PersonDay;
import models.Role;
import models.User;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import play.data.validation.Valid;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;

import javax.inject.Inject;

@Slf4j
@With({Resecure.class, RequestInit.class})
public class AbsenceGroups extends Controller {

  @Inject
  private static PersonDao personDao;
  @Inject
  private static PersonDayManager personDayManager;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static AbsenceManager absenceManager;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static AbsenceHistoryDao absenceHistoryDao;
  @Inject
  private static UserDao userDao;

  /**
   * End point per la visualizzazione dei gruppi assenze definiti.
   */
  public static void show() {

    List<GroupAbsenceType> groups = GroupAbsenceType.findAll();
    render(groups);

  }


  /**
   * End point per la simulazione di inserimento assenze.s
   *
   * @param personId         persona
   * @param from             data inizio
   * @param absenceInsertTab web tab
   * @param to               data fine
   * @param groupAbsenceType gruppo assenze
   * @param switchGroup      se cambio gruppo di assenze
   * @param absenceType      tipo assenza
   * @param justifiedType    tipo giustificativo
   * @param hours            ore
   * @param minutes          minuti
   */
  public static void insert(
      Long personId, LocalDate from, AbsenceInsertTab absenceInsertTab,      //tab
      LocalDate to, GroupAbsenceType groupAbsenceType, boolean switchGroup, //group
      AbsenceType absenceType, JustifiedType justifiedType,                  //confGroup 
      Integer hours, Integer minutes, boolean forceInsert) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);

    rules.checkIfPermitted(person);

    AbsenceForm absenceForm =
        absenceService.buildAbsenceForm(person, from, absenceInsertTab,
            to, groupAbsenceType, switchGroup, absenceType, justifiedType, hours, minutes, false);

    InsertReport insertReport = absenceService.insert(person,
        absenceForm.groupSelected,
        absenceForm.from, absenceForm.to,
        absenceForm.absenceTypeSelected, absenceForm.justifiedTypeSelected,
        absenceForm.hours, absenceForm.minutes, forceInsert, absenceManager);
    render(absenceForm, insertReport, forceInsert);

  }

  /**
   * End Point per il salvataggio di assenze.
   *
   * @param personId         persona
   * @param from             data inizio
   * @param to               data fine
   * @param groupAbsenceType gruppo assenze
   * @param absenceType      tipo assenza
   * @param justifiedType    giustificativo
   * @param hours            ore
   * @param minutes          minuti
   * @param forceInsert      forza inserimento
   */
  public static void save(Long personId, LocalDate from, LocalDate to,
      GroupAbsenceType groupAbsenceType, AbsenceType absenceType,
      JustifiedType justifiedType, Integer hours, Integer minutes, boolean forceInsert) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
    notFoundIfNull(groupAbsenceType);
    notFoundIfNull(absenceType);
    notFoundIfNull(justifiedType);

    if (!absenceType.isPersistent()) {
      absenceType = null;
    }

    InsertReport insertReport = absenceService.insert(person, groupAbsenceType, from, to,
        absenceType, justifiedType, hours, minutes, forceInsert, absenceManager);

    //Persistenza
    if (!insertReport.absencesToPersist.isEmpty()) {
      for (Absence absence : insertReport.absencesToPersist) {
        PersonDay personDay = personDayManager
            .getOrCreateAndPersistPersonDay(person, absence.getAbsenceDate());
        absence.personDay = personDay;
        personDay.absences.add(absence);
        rules.checkIfPermitted(absence);
        absence.save();
        personDay.save();
      }
      if (!insertReport.reperibilityShiftDate().isEmpty()) {
        //absenceManager.sendReperibilityShiftEmail(person, insertReport.reperibilityShiftDate());
        log.info("Inserite assenze con reperibilità e turni {} {}. Le email sono disabilitate.",
            person.fullName(), insertReport.reperibilityShiftDate());
      }
      JPA.em().flush();
      consistencyManager.updatePersonSituation(person.id, from);
      flash.success("Codici di assenza inseriti.");
    }

    
    //String referer = request.headers.get("referer").value();
    
    // FIXME utilizzare un parametro proveniente dalla vista per rifarne il redirect
    final User currentUser = Security.getUser().get();
    if (!currentUser.isSystemUser()) {
      if (currentUser.person.id.equals(person.id)
          && !currentUser.hasRoles(Role.PERSONNEL_ADMIN)) {
        Stampings.stampings(from.getYear(), from.getMonthOfYear());
      }
    }

    Stampings.personStamping(person.id, from.getYear(), from.getMonthOfYear());

  }

  /**
   * End point per visualizzare lo stato di un gruppo assenze alla data.
   *
   * @param personId         persona
   * @param groupAbsenceType gruppo
   * @param from             data
   */
  public static void groupStatus(Long personId, GroupAbsenceType groupAbsenceType, LocalDate from) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
    
    rules.checkIfPermitted(person);

    groupAbsenceType = groupAbsenceType.firstOfChain();
    
    AbsenceForm categorySwitcher = absenceService
        .buildForCateogorySwitch(person, from, groupAbsenceType);
    
    PeriodChain periodChain = absenceService.residual(person, categorySwitcher.groupSelected, from);
    
    
    final User currentUser = Security.getUser().get();
    boolean isAdmin = false;
    //se l'user è amministratore visualizzo lo switcher del gruppo
    if (currentUser.isSystemUser() 
        || userDao.getUsersWithRoles(person.office, Role.PERSONNEL_ADMIN, Role.PERSONNEL_ADMIN_MINI)
        .contains(currentUser)) {
      isAdmin = true;
    }

    render(from, categorySwitcher, groupAbsenceType, periodChain, isAdmin);
  }

  /**
   * End point per definire l'inizializzazione di un gruppo.
   *
   * @param personId         persona
   * @param groupAbsenceType gruppo
   * @param date             data
   */
  public static void initialization(
      Long personId, GroupAbsenceType groupAbsenceType, LocalDate date) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);

    //costruire la situazione residuale per la data
    //AbsenceEngine absenceEngine = absenceService.residual(person, groupAbsenceType, date);

    List<GroupAbsenceType> initializableGroups = initializablesGroups();

    render(initializableGroups, person);

  }

  private static List<GroupAbsenceType> initializablesGroups() {
    List<GroupAbsenceType> initializables = Lists.newArrayList();
    List<GroupAbsenceType> allGroups = GroupAbsenceType.findAll();
    for (GroupAbsenceType group : allGroups) {
      if (!group.pattern.equals(GroupAbsenceTypePattern.simpleGrouping)) {
        initializables.add(group);
      }
    }
    return initializables;

  }


  /**
   * metodo che renderizza la pagina di modifica di una determinata assenza.
   *
   * @param absenceId id dell'assenza
   */
  public static void edit(final long absenceId) {

    final Absence absence = Absence.findById(absenceId);

    notFoundIfNull(absence);

    rules.checkIfPermitted(absence);

    List<HistoryValue<Absence>> historyAbsence = absenceHistoryDao.absences(absence.id);

    LocalDate dateFrom = absence.personDay.date;
    LocalDate dateTo = absence.personDay.date;

    render(absence, dateFrom, dateTo, historyAbsence);
  }

  /**
   * metodo che cancella una certa assenza fino ad un certo periodo.
   *
   * @param absenceId id dell'assenza
   * @param dateTo    la data di fine periodo
   */
  public static void delete(final long absenceId, @Valid LocalDate dateTo) {

    final Absence absence = Absence.findById(absenceId);

    notFoundIfNull(absence);

    rules.checkIfPermitted(absence);

    Person person = absence.personDay.person;
    LocalDate dateFrom = absence.personDay.date;

    if (dateTo != null && dateTo.isBefore(dateFrom)) {
      flash.error("Errore nell'inserimento del campo Fino a, inserire una data valida. "
          + "Operazione annullata");
    }
    if (flash.contains("error")) {
      Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
    }

    //Logica
    int deleted = absenceManager
        .removeAbsencesInPeriod(person, dateFrom, dateTo, absence.absenceType);

    if (deleted > 0) {
      flash.success("Rimossi %s codici assenza di tipo %s", deleted, absence.absenceType.code);
    }

    

    // FIXME utilizzare un parametro proveniente dalla vista per rifarne il redirect
    final User currentUser = Security.getUser().get();
    if (!currentUser.isSystemUser()) {
      if (currentUser.person.id.equals(person.id)
          && !currentUser.hasRoles(Role.PERSONNEL_ADMIN)) {
        Stampings.stampings(dateFrom.getYear(), dateFrom.getMonthOfYear());
      }
    }

    Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
  }
}
