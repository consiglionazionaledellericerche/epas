
package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import dao.PersonDao;
import dao.QualificationDao;
import dao.UserDao;
import dao.WorkingTimeTypeDao;
import dao.absences.AbsenceComponentDao;
import dao.history.AbsenceHistoryDao;
import dao.history.HistoryValue;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.extern.slf4j.Slf4j;

import manager.AbsenceManager;
import manager.ConsistencyManager;
import manager.PersonDayManager;
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.PeriodChain;

import models.Person;
import models.PersonDay;
import models.Qualification;
import models.Role;
import models.User;
import models.WorkingTimeType;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.CategoryTab;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.InitializationGroup;
import models.absences.JustifiedType;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeAmountAdjustment;
import models.enumerate.QualificationMapping;

import org.assertj.core.util.Sets;
import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;
import java.util.Set;

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
  @Inject
  private static QualificationDao qualificationDao;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;

  /**
   * La lista delle categorie definite.
   */
  public static void showCategories() {
    
    List<CategoryGroupAbsenceType> categories = CategoryGroupAbsenceType.findAll();
    List<CategoryTab> categoryTabs = CategoryTab.findAll();
    
    //    if (categoryTabs.isEmpty()) {
    //      int count = 1;
    //      for (AbsenceInsertTab absenceInsertTab : AbsenceInsertTab.values()) {
    //        CategoryTab categoryTab = new CategoryTab();
    //        categoryTab.name = absenceInsertTab.name();
    //        categoryTab.priority = count;
    //        if (AbsenceInsertTab.defaultTab() == absenceInsertTab) {
    //          categoryTab.isDefault = true;
    //        }
    //        categoryTab.save();
    //        count++;
    //      }
    //      categoryTabs = CategoryTab.findAll();
    //    }
    
    render(categories, categoryTabs);
  }
  
  /**
   * Nuova tab.
   */
  public static void insertCategoryTab() {
    CategoryTab categoryTab = new CategoryTab();
    render("@editCategoryTab", categoryTab);
  }
  
  /**
   * Edit tab.
   * @param categoryTabId id
   */
  public static void editCategoryTab(Long categoryTabId) {
    CategoryTab categoryTab = CategoryTab.findById(categoryTabId);
    notFoundIfNull(categoryTab);
    render(categoryTab);
  }
  
  /**
   * Save tab.
   * @param categoryTab tab
   */
  public static void saveCategoryTab(@Valid CategoryTab categoryTab) {
    
    if (validation.hasErrors()) {
      flash.error("Correggere gli errori indicati");
      render("@editCategoryType", categoryTab);
    }
    categoryTab.save();
    flash.success("Operazione eseguita.");
    editCategoryTab(categoryTab.id);
  }
  
  /**
   * Rimuove la tab.
   * @param categoryTabId tab
   */
  public static void deleteCategoryTab(Long categoryTabId) {
    CategoryTab categoryTab = CategoryTab.findById(categoryTabId);
    notFoundIfNull(categoryTab);
    if (!categoryTab.categoryGroupAbsenceTypes.isEmpty()) {
      flash.error("Non è possibile eliminare una tab associata a categorie");
      editCategoryTab(categoryTabId);
    }
    categoryTab.delete();
    flash.success("Operazione effettuata.");
    showCategories();
  }
  
  /**
   * Nuova category.
   */
  public static void insertCategoryGroupAbsenceType() {
    CategoryGroupAbsenceType categoryGroupAbsenceType = new CategoryGroupAbsenceType();
    List<CategoryTab> allCategoryTab = CategoryTab.findAll();
    render("@editCategoryGroupAbsenceType", categoryGroupAbsenceType, allCategoryTab);
  }
  
  /**
   * Edit category.
   * @param categoryGroupAbsenceTypeId id
   */
  public static void editCategoryGroupAbsenceType(Long categoryGroupAbsenceTypeId) {
    CategoryGroupAbsenceType categoryGroupAbsenceType = 
        CategoryGroupAbsenceType.findById(categoryGroupAbsenceTypeId);
    notFoundIfNull(categoryGroupAbsenceType);
    List<CategoryTab> allCategoryTab = CategoryTab.findAll();
    render(categoryGroupAbsenceType, allCategoryTab);
  }
  
  /**
   * Save category.
   * @param categoryGroupAbsenceType tab
   */
  public static void saveCategoryGroupAbsenceType(
      @Valid CategoryGroupAbsenceType categoryGroupAbsenceType) {
    
    if (validation.hasErrors()) {
      List<CategoryTab> allCategoryTab = CategoryTab.findAll();
      flash.error("Correggere gli errori indicati");
      render("@editCategoryGroupAbsenceType", categoryGroupAbsenceType, allCategoryTab);
    }
    categoryGroupAbsenceType.save();
    flash.success("Operazione eseguita.");
    editCategoryGroupAbsenceType(categoryGroupAbsenceType.id);
  }
  
  /**
   * Rimuove la category.
   * @param categoryGroupAbsenceTypeId tab
   */
  public static void deleteCategoryGroupAbsenceType(Long categoryGroupAbsenceTypeId) {
    CategoryGroupAbsenceType categoryGroupAbsenceType = 
        CategoryGroupAbsenceType.findById(categoryGroupAbsenceTypeId);
    notFoundIfNull(categoryGroupAbsenceType);
    if (!categoryGroupAbsenceType.groupAbsenceTypes.isEmpty()) {
      flash.error("Non è possibile eliminare una categoria associata a un gruppo");
      editCategoryGroupAbsenceType(categoryGroupAbsenceTypeId);
    }
    categoryGroupAbsenceType.delete();
    flash.success("Operazione effettuata.");
    showCategories();
  }
  
  
  /**
   * End point per la visualizzazione dei gruppi assenze definiti. EX show
   * @param categoryId filtro categoria
   */
  public static void showGroups(Long categoryId) {

    List<CategoryTab> categoryTabs = absenceComponentDao.tabsByPriority();
    
    CategoryGroupAbsenceType selected = categoryTabs.get(0).firstByPriority();
    if (categoryId != null) {
      selected = CategoryGroupAbsenceType.findById(categoryId);
    }
    
    render(categoryTabs, selected);

  }
  
  /**
   * End point per la creazione di un gruppo.
   */
  public static void insertGroup(Long categoryId) {
    CategoryGroupAbsenceType categoryGroupAbsenceType = 
        CategoryGroupAbsenceType.findById(categoryId);
    notFoundIfNull(categoryGroupAbsenceType);
    
    GroupAbsenceType groupAbsenceType = new GroupAbsenceType();
    groupAbsenceType.category = categoryGroupAbsenceType;
    groupAbsenceType.pattern = GroupAbsenceTypePattern.simpleGrouping;
    
    List<CategoryGroupAbsenceType> allCategories = CategoryGroupAbsenceType.findAll();
    List<AbsenceType> allAbsenceTypes = AbsenceType.findAll();
    
    //Default pattern
    Integer fixedLimit = -1;
    AmountType takeAmountType = AmountType.units;
    
    render("@editGroup", groupAbsenceType, fixedLimit, takeAmountType, 
        allCategories, allAbsenceTypes);
  }
  
  /**
   * End point per la modifica del gruppo.
   * @param groupAbsenceTypeId gruppo
   */
  public static void editGroup(Long groupAbsenceTypeId) {
    GroupAbsenceType groupAbsenceType = GroupAbsenceType.findById(groupAbsenceTypeId);
    notFoundIfNull(groupAbsenceType);
    List<CategoryGroupAbsenceType> allCategories = CategoryGroupAbsenceType.findAll();
    List<AbsenceType> allAbsenceTypes = AbsenceType.findAll();
    
    //Stato precedente gruppo
    AmountType takeAmountType = groupAbsenceType.takableAbsenceBehaviour.amountType;
    Integer fixedLimit = groupAbsenceType.takableAbsenceBehaviour.fixedLimit;
    Set<AbsenceType> takableCodes = groupAbsenceType.takableAbsenceBehaviour.takableCodes;
    TakeAmountAdjustment takableAmountAdjustment = groupAbsenceType
        .takableAbsenceBehaviour.takableAmountAdjustment;
    AmountType complationAmountType = null;
    Set<AbsenceType> complationCodes = Sets.newHashSet();
    Set<AbsenceType> replacingCodes = Sets.newHashSet();
    if (groupAbsenceType.complationAbsenceBehaviour != null) {
      complationAmountType = groupAbsenceType.complationAbsenceBehaviour.amountType;
      complationCodes = groupAbsenceType.complationAbsenceBehaviour.complationCodes;
      replacingCodes = groupAbsenceType.complationAbsenceBehaviour.replacingCodes;
    }
      
    render(groupAbsenceType, takeAmountType, fixedLimit, takableCodes, takableAmountAdjustment, 
        complationAmountType, complationCodes, replacingCodes, 
        allCategories, allAbsenceTypes);
  }
  
  /**
   * End point per il salvataggio del gruppo.
   * @param groupAbsenceType gruppo
   */
  public static void saveGroup(@Valid GroupAbsenceType groupAbsenceType, 
      List<AbsenceType> takableCodes, 
      @Required AmountType takeAmountType, 
      @Required Integer fixedLimit, 
      TakeAmountAdjustment takableAmountAdjustment, 
      
      AmountType complationAmountType,
      List<AbsenceType> complationCodes, List<AbsenceType> replacingCodes) {
    
    if (takableCodes.isEmpty()) {
      Validation.addError("takableCodes", "Deve contenere almeno un codice.");
    }
    if (complationAmountType != null) {
      if (complationCodes.isEmpty()) {
        Validation.addError("complationCodes", "Deve contenere almeno un codice.");
      }
      if (replacingCodes.isEmpty()) {
        Validation.addError("replacingCodes", "Deve contenere almeno un codice.");
      }
    }
    
    if (validation.hasErrors()) {
      List<CategoryGroupAbsenceType> allCategories = CategoryGroupAbsenceType.findAll();
      List<AbsenceType> allAbsenceTypes = AbsenceType.findAll();
      render("@editGroup", groupAbsenceType, 
          takableCodes, takeAmountType, fixedLimit, takableAmountAdjustment, 
          complationAmountType, complationCodes, replacingCodes, 
          allCategories, allAbsenceTypes );
    }
    
    if (!groupAbsenceType.isPersistent()) {
            
      if (groupAbsenceType.pattern != GroupAbsenceTypePattern.simpleGrouping) {
        
        //simple grouping creation
        groupAbsenceType.periodType = PeriodType.always;
        TakableAbsenceBehaviour takableAbsenceBehaviour = new TakableAbsenceBehaviour();
        takableAbsenceBehaviour.name = "T_" + groupAbsenceType.name;
        takableAbsenceBehaviour.fixedLimit = -1;
        takableAbsenceBehaviour.amountType = AmountType.units;
        takableAbsenceBehaviour.takableCodes = Sets.newHashSet(takableCodes);
        takableAbsenceBehaviour.save();
        groupAbsenceType.takableAbsenceBehaviour = takableAbsenceBehaviour;
        groupAbsenceType.save();
      }
    }
    flash.success("Operazione eseguita con successo");
    showGroups(groupAbsenceType.category.id);
  }
  
  
  /**
   * End point lista codici assenza.
   */
  public static void showAbsenceTypes() {
    List<AbsenceType> absenceTypes = AbsenceType.findAll();
    render(absenceTypes);
  }
  
  /**
   * Visualizza il codice di assenza.
   *
   * @param absenceTypeId id
   */
  public static void viewAbsenceType(Long absenceTypeId) {

    AbsenceType absenceType = AbsenceType.findById(absenceTypeId);
    notFoundIfNull(absenceType);
    render(absenceType);
    
  }
  
  /**
   * Inserimento nuovo codice di assenza.
   */
  public static void insertAbsenceType() {
    AbsenceType absenceType = new AbsenceType();
    List<JustifiedType> allJustifiedType = JustifiedType.findAll();
    List<GroupAbsenceType> allSimpleGroup = absenceComponentDao
        .groupAbsenceTypeOfPattern(GroupAbsenceTypePattern.simpleGrouping);
    render("@editAbsenceType", absenceType, allJustifiedType, allSimpleGroup);
  }
  
  
  
  /**
   * Modifica codice assenza.
   *
   * @param absenceTypeId id
   */
  public static void editAbsenceType(Long absenceTypeId) {

    AbsenceType absenceType = AbsenceType.findById(absenceTypeId);
    notFoundIfNull(absenceType);

    boolean tecnologi = false;
    boolean tecnici = false;

    for (Qualification q : absenceType.qualifications) {
      tecnologi = !tecnologi ? QualificationMapping.TECNOLOGI.contains(q) : tecnologi;
      tecnici = !tecnici ? QualificationMapping.TECNICI.contains(q) : tecnici;
    }

    List<JustifiedType> allJustifiedType = JustifiedType.findAll();
    List<GroupAbsenceType> allSimpleGroup = absenceComponentDao
        .groupAbsenceTypeOfPattern(GroupAbsenceTypePattern.simpleGrouping);

    render(absenceType, allSimpleGroup, tecnologi, tecnici, allJustifiedType);
  }
  
  /**
   * Salva il nuovo/modificato codice di assenza.
   *
   * @param absenceType il tipo di assenza
   * @param tecnologi   se il codice di assenza è valido per i tecnologi
   * @param tecnici     se il codice di assenza è valido per i tecnici
   */
  public static void saveAbsenceType(@Valid AbsenceType absenceType,
      boolean tecnologi, boolean tecnici) {

    List<JustifiedType> allJustifiedType = JustifiedType.findAll();
    
    if (validation.hasErrors()) {
      flash.error("Correggere gli errori indicati");
      render("@editAbsenceType", absenceType, allJustifiedType, tecnologi, tecnici);
    }

    absenceType.qualifications.clear();
    if (tecnici) {
      absenceType.qualifications.addAll(
          qualificationDao.getByQualificationMapping(QualificationMapping.TECNICI));
    }
    if (tecnologi) {
      absenceType.qualifications.addAll(
          qualificationDao.getByQualificationMapping(QualificationMapping.TECNOLOGI));
    }
    
    if (absenceType.qualifications.isEmpty()) {
      flash.error("Selezionare almeno una categoria tra Tecnologi e Tecnici");
      render("@editAbsenceType", absenceType, allJustifiedType, tecnologi, tecnici);
    }
    if (absenceType.justifiedTypesPermitted.isEmpty()) {
      flash.error("Selezionare almeno una tipologia di Tempo Giustificato");
      render("@editAbsenceType", absenceType, allJustifiedType, tecnologi, tecnici);
    }
    if (absenceType.isAbsenceTypeMinutesPermitted() && absenceType.justifiedTime == null) {
      flash.error("Specificare i minuti del tipo assenza.");
      render("@editAbsenceType", absenceType, allJustifiedType, tecnologi, tecnici);
    }

    absenceType.save();
    flash.success("Inserito/modificato codice di assenza %s", absenceType.code);

    editAbsenceType(absenceType.id);
  }


  /**
   * End point per la simulazione di inserimento assenze.s
   *
   * @param personId         persona
   * @param from             data inizio
   * @param categoryTab      tab
   * @param to               data fine
   * @param groupAbsenceType gruppo assenze
   * @param switchGroup      se cambio gruppo di assenze
   * @param absenceType      tipo assenza
   * @param justifiedType    tipo giustificativo
   * @param hours            ore
   * @param minutes          minuti
   */
  public static void insert(
      Long personId, LocalDate from, CategoryTab categoryTab,                      //tab
      LocalDate to, GroupAbsenceType groupAbsenceType, boolean switchGroup, //group
      AbsenceType absenceType, JustifiedType justifiedType,                 //confGroup 
      Integer hours, Integer minutes, boolean forceInsert) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);

    rules.checkIfPermitted(person);

    AbsenceForm absenceForm =
        absenceService.buildAbsenceForm(person, from, categoryTab,
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
   * @param personId persona
   * @param groupAbsenceTypeId gruppo
   * @param from data
   */
  public static void groupStatus(Long personId, Long groupAbsenceTypeId, LocalDate from) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    GroupAbsenceType groupAbsenceType = GroupAbsenceType.findById(groupAbsenceTypeId);
    notFoundIfNull(from);
    
    rules.checkIfPermitted(person);

    groupAbsenceType = groupAbsenceType.firstOfChain();
    
    AbsenceForm categorySwitcher = absenceService
        .buildForCategorySwitch(person, from, groupAbsenceType);
    
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
   * @param personId persona
   * @param groupAbsenceTypeId gruppo
   * @param date data
   */
  public static void initialization(Long personId, Long groupAbsenceTypeId, LocalDate date) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    if (date == null) {
      date = LocalDate.now();
    }
    
    //Categorie inizializzabili e gruppo selezionato
    List<CategoryGroupAbsenceType> initializableCategories = 
        absenceComponentDao.initializablesCategory();
    Verify.verify(!initializableCategories.isEmpty());
    GroupAbsenceType groupAbsenceType = absenceComponentDao.firstGroupInitializable(
        initializableCategories.iterator().next());
    
    if (groupAbsenceTypeId != null) {
      groupAbsenceType = GroupAbsenceType.findById(groupAbsenceTypeId);
      notFoundIfNull(groupAbsenceType);
      Verify.verify(groupAbsenceType.initializable);
    }
    
    InitializationGroup initializationGroup = 
        new InitializationGroup(person, groupAbsenceType, date);
    
    //Tempo a lavoro medio
    Optional<WorkingTimeType> wtt = workingTimeTypeDao.getWorkingTimeType(date, person);
    if (!wtt.isPresent()) {
      wtt = workingTimeTypeDao.getWorkingTimeType(date.plusDays(1), person);
    }
    if (!wtt.isPresent()) {
      Validation.addError("date", "Deve essere una data attiva, "
          + "o immediatamente precedente l'inizio di un contratto.");
      render(initializableCategories, initializationGroup);
    }
    int averageWeekWorkingTime = wtt.get().weekAverageWorkingTime();
    
    //Stato del gruppo
    PeriodChain periodChain = absenceService.residual(person, groupAbsenceType, date);
    if (periodChain.periods.isEmpty()) {
      render(initializableCategories, initializationGroup, periodChain);
    }
    AbsencePeriod absencePeriod = periodChain.periods.iterator().next();
  
    if (absencePeriod.initialization != null) {
      initializationGroup = absencePeriod.initialization;
      if (absencePeriod.initialization.averageWeekTime != averageWeekWorkingTime) {
        //TODO: un warning? Se non è stato fatto andrebbe fatto l'update dei derivati
      }
    } else {
      initializationGroup.averageWeekTime = averageWeekWorkingTime;
    }
    
    
    render(initializableCategories, date, initializationGroup, periodChain, absencePeriod);

  }
  
  /**
   * Persiste una nuova inizializzazione.
   * @param initializationGroup initializationGroup
   */
  public static void saveInitialization(@Valid InitializationGroup initializationGroup) {
    
    // Lo stato della richiesta
    
    notFoundIfNull(initializationGroup.person);
    notFoundIfNull(initializationGroup.groupAbsenceType);
    notFoundIfNull(initializationGroup.date);
    PeriodChain periodChain = absenceService.residual(initializationGroup.person, 
        initializationGroup.groupAbsenceType, initializationGroup.date);
    Preconditions.checkState(!periodChain.periods.isEmpty());
    AbsencePeriod absencePeriod = periodChain.periods.iterator().next();
    

    //Tempo a lavoro medio
    Optional<WorkingTimeType> wtt = workingTimeTypeDao
        .getWorkingTimeType(initializationGroup.date, initializationGroup.person);
    if (!wtt.isPresent()) {
      wtt = workingTimeTypeDao
          .getWorkingTimeType(initializationGroup.date.plusDays(1), initializationGroup.person);
    }
    if (!wtt.isPresent()) {
      Validation.addError("date", "Deve essere una data attiva, "
          + "o immediatamente precedente l'inizio di un contratto.");
    }
    
    // Gli errori della richiesta inserimento inizializzazione
    if (Validation.hasErrors()) {
      flash.error("Correggere gli errori indicati");
      render("@initialization", absencePeriod, periodChain, initializationGroup);
    }
    
    // se esiste già una inizilizzazione devo sovrascrivere quella.
    if (absencePeriod.initialization != null) {
      Verify.verify(absencePeriod.initialization.id.equals(initializationGroup.id));
    }
    
    //new average
    initializationGroup.averageWeekTime = wtt.get().weekAverageWorkingTime();
    
    initializationGroup.save();
    
    //TODO: check del gruppo per i ricalcoli
    
    flash.success("Inizializzazione salvata con successo.");
    
    initialization(initializationGroup.person.id, initializationGroup.groupAbsenceType.id, 
        initializationGroup.date);
  }
  
  /**
   * Elimina l'inizializzazione.
   * @param initializationGroup inizializzazione
   */
  public static void deleteInitialization(InitializationGroup initializationGroup) {
    
    notFoundIfNull(initializationGroup);
    
    initializationGroup.delete();
    
    flash.success("Inizializzazione rimossa con successo.");
    
    //TODO: check del gruppo per i ricalcoli
    
    initialization(initializationGroup.person.id, initializationGroup.groupAbsenceType.id, 
        initializationGroup.date);
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
  
  /**
   * 1.
   */
  public static void absenceAnalyzer() {
    List<Absence> absences = Absence.findAll();
    List<Absence> notPermitted = Lists.newArrayList();
    List<Absence> outOfDate = Lists.newArrayList();
    for (Absence absence : absences) {
      if (!absence.absenceType.justifiedTypesPermitted.contains(absence.justifiedType)) {
        notPermitted.add(absence);
        log.info("{} is: {}, permitted: {}", absence.toString(), 
            absence.justifiedType, absence.absenceType.justifiedTypesPermitted);
      }
      if (absence.absenceType.isExpired()) {
        if (!DateUtility.isDateIntoInterval(absence.getAbsenceDate(), 
            new DateInterval(absence.absenceType.validFrom, absence.absenceType.validTo))) {
          outOfDate.add(absence);
        }
      }
    }
    render(notPermitted, outOfDate);
  }
  
  
  /**
   * Valori altrimenti non modificabili.
   */
  public static void editAbsenceCriticalValue(Long absenceId) {
    
    if (absenceId != null) {
      Absence absence = Absence.findById(absenceId);
      render(absence);
    }
    render();
    
  }
  
  /**
   * Salva i valori altrimenti non modificabili.
   * @param absenceId tipo assenza
   * @param justifiedType tipo giustificativo
   * @param justifiedMinutes minuti specificati
   */
  public static void saveAbsenceCriticalValue(Long absenceId, 
      JustifiedType justifiedType, Integer justifiedMinutes) {
    
    Absence absence = Absence.findById(absenceId);
    notFoundIfNull(absence);
    notFoundIfNull(justifiedType);
    
    //TODO: deve appartenere a absenceType.justifiedTypePermitted
    absence.justifiedType = justifiedType;
    absence.justifiedMinutes = justifiedMinutes;
    
    absence.save();

    flash.success("Operazione eseguita.");
    editAbsenceCriticalValue(absence.id);
    
  }
  
}
