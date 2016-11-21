
package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import dao.PersonDao;
import dao.UserDao;
import dao.WorkingTimeTypeDao;
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
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.PeriodChain;

import models.Person;
import models.PersonDay;
import models.Role;
import models.User;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.DefaultGroup;
import models.absences.InitializationGroup;
import models.absences.JustifiedType;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Valid;
import play.data.validation.Validation;
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
  @Inject
  private static WorkingTimeTypeDao workingTimeTypeDao;

  /**
   * End point per la visualizzazione dei gruppi assenze definiti.
   * @param categoryId filtro categoria
   */
  public static void show(Long categoryId) {

    List<GroupAbsenceType> groups = GroupAbsenceType.findAll();
    List<CategoryGroupAbsenceType> categories = CategoryGroupAbsenceType.findAll();
    CategoryGroupAbsenceType category = categories.iterator().next();
    if (categoryId != null) {
      category = CategoryGroupAbsenceType.findById(categoryId);
    }
    
    render(groups, categories, category);

  }
  
  /**
   * End point per la modifica del gruppo.
   * @param groupAbsenceTypeId gruppo
   */
  public static void editGroup(Long groupAbsenceTypeId) {
    GroupAbsenceType groupAbsenceType = GroupAbsenceType.findById(groupAbsenceTypeId);
    notFoundIfNull(groupAbsenceType);
    
    render(groupAbsenceType);
  }
  
  /**
   * End point per il salvataggio del gruppo.
   * @param groupAbsenceType gruppo
   */
  public static void updateGroup(GroupAbsenceType groupAbsenceType) {
    groupAbsenceType.save();
    show(groupAbsenceType.category.id);
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
        absenceManager.sendReperibilityShiftEmail(person, insertReport.reperibilityShiftDate());
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
      Long personId, Long groupAbsenceTypeId, LocalDate date) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    if (date == null) {
      date = LocalDate.now();
    }
    
    List<GroupAbsenceType> initializableGroups = initializablesGroups();
    GroupAbsenceType groupAbsenceType = initializableGroups.iterator().next();
    
    if (groupAbsenceTypeId != null) {
      groupAbsenceType = GroupAbsenceType.findById(groupAbsenceTypeId);
      notFoundIfNull(groupAbsenceType);
      Verify.verify(initializableGroups.contains(groupAbsenceType));
    }
    
    PeriodChain periodChain = absenceService.residual(person, groupAbsenceType, date);
    if (periodChain.periods.isEmpty()) {
      render(initializableGroups, person, periodChain);
    }
    
    AbsencePeriod absencePeriod = periodChain.periods.iterator().next();
    InitializationDto initializationDto = new InitializationDto();
    initializationDto.takenAmountType = absencePeriod.takeAmountType;
    initializationDto.complationAmountType = absencePeriod.complationAmountType;
    
    render(initializableGroups, person, groupAbsenceType, date, periodChain, absencePeriod, initializationDto);

  }
  
  public static void saveInitialization(Long personId, Long groupAbsenceTypeId, LocalDate date, 
      @Valid InitializationDto initializationDto) {
    
    // Lo stato della richiesta
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    GroupAbsenceType groupAbsenceType = GroupAbsenceType.findById(groupAbsenceTypeId);
    notFoundIfNull(groupAbsenceType);
    notFoundIfNull(date);
    PeriodChain periodChain = absenceService.residual(person, 
        groupAbsenceType, date);
    Preconditions.checkState(!periodChain.periods.isEmpty());
    AbsencePeriod absencePeriod = periodChain.periods.iterator().next();
    if (absencePeriod.isTakableWithLimit()) {
      initializationDto.takenAmountType = absencePeriod.takeAmountType;
    }
    initializationDto.complationAmountType = absencePeriod.complationAmountType;

    // Gli errori della richiesta inserimento inizializzazione
    if (Validation.hasErrors()) {
      flash.error("Correggere gli errori indicati");
      render("@initialization", person, groupAbsenceType, date, absencePeriod, periodChain, 
          initializationDto);
    }
    
    Optional<WorkingTimeTypeDay> wttd = workingTimeTypeDao.getWorkingTimeTypeDay(date, person);
    if (!wttd.isPresent() || wttd.get().workingTime <= 0) {
      //TODO: add error scegliere una data attiva e feriale.
    }
    
    InitializationGroup initialization = absenceService.populateInitialization(person, date, 
        groupAbsenceType, wttd.get().workingTime, absencePeriod.initialization, initializationDto);
    if (initialization == null) {
      //TODO: add error errore inatteso
    }
    
    initialization.save();
    
    flash.success("Inizializzazione salvata con successo.");
    
    initialization(person.id, groupAbsenceType.id, date);
  }

  private static List<GroupAbsenceType> initializablesGroups() {
    List<GroupAbsenceType> initializables = Lists.newArrayList();
    List<GroupAbsenceType> allGroups = GroupAbsenceType.findAll();
    for (GroupAbsenceType group : allGroups) {
      if (group.name.equals(DefaultGroup.G_09.name()) 
          || group.name.equals(DefaultGroup.G_89.name())
          || group.name.equals(DefaultGroup.G_661.name())
          || group.name.equals(DefaultGroup.G_18.name())
          || group.name.equals(DefaultGroup.G_19.name())
          || group.name.equals(DefaultGroup.G_23.name())
          || group.name.equals(DefaultGroup.G_25.name())
          || group.name.equals(DefaultGroup.G_232.name())
          || group.name.equals(DefaultGroup.G_252.name())
          || group.name.equals(DefaultGroup.G_233.name())
          || group.name.equals(DefaultGroup.G_253.name())) {
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
  
  public static class InitializationDto {
    
    public AmountType takenAmountType;
    public AmountType complationAmountType;
    
    @Min(0)
    public Integer takenHours = 0;
    @Min(0) @Max(59)
    public Integer takenMinutes = 0;
    @Min(0) @Max(99)
    public Integer takenUnits = 0;
       
    
    public InitializationGroup populateInitialization(InitializationGroup initialization, 
        int workTime) {
      
      //reset
      initialization.complationUsed = 0;
      initialization.takableUsed = 0;
      
      //Takable used
      if (isMinuteTakable()) {
        initialization.takableUsed = minutes(this.takenHours, this.takenMinutes);
      }
      if (isDayTakable()) {
        initialization.takableUsed = (this.takenUnits * 100) 
            + workingTypePercent(this.takenHours, this.takenMinutes, workTime);
      }
      
      //Complation used
      if (isMinuteComplation()) {
        initialization.complationUsed = minutes(this.takenHours, this.takenMinutes);
      }
      if (isDayComplation()) {
        initialization.complationUsed = 
            workingTypePercentModule(this.takenHours, this.takenMinutes, workTime);
      }
      if (isMinuteTakable() && isMinuteComplation()) {
        //eccezione 661.. forse il gruppo è da modellare meglio
        initialization.complationUsed = this.takenMinutes;
      }
      
      return initialization;
    }
    
    public boolean isDayTakable() {
      return this.takenAmountType != null && this.takenAmountType == AmountType.units; 
    }
    
    public boolean isMinuteTakable() {
      return this.takenAmountType != null && this.takenAmountType == AmountType.minutes;
    }
    
    public boolean isDayComplation() {
      return this.complationAmountType != null && this.complationAmountType == AmountType.units; 
    }
    
    public int minutes(int hours, int minutes) {
      return hours * 60 + minutes;
    }
    
    public boolean isMinuteComplation() {
      return this.complationAmountType != null && this.complationAmountType == AmountType.minutes;
    }
    
    public int workingTypePercent(int hours, int minutes, int workTime) {
      return (minutes(hours, minutes) / workTime) * 100;
      
    }
    
    public int workingTypePercentModule(int hours, int minutes, int workTime) {
      return workingTypePercent(hours, minutes, workTime) % 100;
    }
    
    /**
     * I minuti inseribili...
     * @return list
     */
    public List<Integer> selectableMinutes() {
      List<Integer> hours = Lists.newArrayList();
      for (int i = 0; i <= 59; i++) {
        hours.add(i);
      }
      return hours;
    }
  }
}
