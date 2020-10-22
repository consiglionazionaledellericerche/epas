
package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dao.AbsenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.QualificationDao;
import dao.UserDao;
import dao.WorkingTimeTypeDao;
import dao.absences.AbsenceComponentDao;
import dao.history.AbsenceHistoryDao;
import dao.history.HistoryValue;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.IWrapperPersonDay;
import dao.wrapper.function.WrapperModelFunctionFactory;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.AbsenceManager;
import manager.ConsistencyManager;
import manager.PersonDayManager;
import manager.services.absences.AbsenceCertificationService;
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import manager.services.absences.certifications.CertificationYearSituation;
import manager.services.absences.certifications.CodeComparation;
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.PeriodChain;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Qualification;
import models.Role;
import models.User;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.CategoryTab;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.InitializationGroup;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeAmountAdjustment;
import models.absences.definitions.DefaultAbsenceType;
import models.enumerate.QualificationMapping;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With({Resecure.class, RequestInit.class})
public class AbsenceGroups extends Controller {

  @Inject
  private static PersonDao personDao;
  @Inject
  private static OfficeDao officeDao;
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
  @Inject
  private static AbsenceCertificationService absenceCertificationService;
  @Inject
  private static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  private static AbsenceDao absenceDao;
  @Inject
  private static IWrapperFactory wrapperFactory;

  /**
   * La lista delle categorie definite.
   */
  public static void showCategories() {

    List<CategoryGroupAbsenceType> categories = CategoryGroupAbsenceType.findAll();
    List<CategoryTab> categoryTabs = CategoryTab.findAll();
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
   *
   * @param categoryTabId id
   */
  public static void editCategoryTab(Long categoryTabId) {
    CategoryTab categoryTab = CategoryTab.findById(categoryTabId);
    notFoundIfNull(categoryTab);
    render(categoryTab);
  }

  /**
   * Save tab.
   *
   * @param categoryTab tab
   */
  public static void saveCategoryTab(@Valid CategoryTab categoryTab) {

    if (Validation.hasErrors()) {
      flash.error("Correggere gli errori indicati");
      render("@editCategoryType", categoryTab);
    }
    categoryTab.save();
    flash.success("Operazione eseguita.");
    editCategoryTab(categoryTab.id);
  }

  /**
   * Rimuove la tab.
   *
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
   *
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
   *
   * @param categoryGroupAbsenceType tab
   */
  public static void saveCategoryGroupAbsenceType(
      @Valid CategoryGroupAbsenceType categoryGroupAbsenceType) {

    if (Validation.hasErrors()) {
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
   *
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
   *
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
   *
   * @param groupAbsenceTypeId gruppo
   */
  public static void editGroup(Long groupAbsenceTypeId) {
    GroupAbsenceType groupAbsenceType = GroupAbsenceType.findById(groupAbsenceTypeId);
    notFoundIfNull(groupAbsenceType);
    List<CategoryGroupAbsenceType> allCategories = CategoryGroupAbsenceType.findAll();
    List<AbsenceType> allAbsenceTypes = AbsenceType.findAll();
    List<GroupAbsenceType> allGroups = GroupAbsenceType.findAll();

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
        allCategories, allAbsenceTypes, allGroups);
  }

  /**
   * End point per il salvataggio del gruppo.
   *
   * @param groupAbsenceType gruppo
   */
  public static void saveGroup(@Valid GroupAbsenceType groupAbsenceType,
      List<Long> takableCodesIds,
      @Required AmountType takeAmountType,
      @Required Integer fixedLimit,
      TakeAmountAdjustment takableAmountAdjustment,

      AmountType complationAmountType,
      List<Long> complationCodesIds, List<Long> replacingCodesIds) {

    //Fetch absenceTypes
    List<AbsenceType> takableCodes = absenceComponentDao.absenceTypesByIds(takableCodesIds);
    List<AbsenceType> complationCodes = absenceComponentDao.absenceTypesByIds(complationCodesIds);
    List<AbsenceType> replacingCodes = absenceComponentDao.absenceTypesByIds(replacingCodesIds);
    if (takableCodes == null || complationCodes == null || replacingCodes == null) {
      //se null significa dimensione diversa fra input e fetch.
      badRequest();
    }

    if (takableCodes.isEmpty()) {
      Validation.addError("takableCodesIds", "Deve contenere almeno un codice.");
    }
    if (complationAmountType != null) {
      if (complationCodes.isEmpty()) {
        Validation.addError("complationCodesIds", "Deve contenere almeno un codice.");
      }
      if (replacingCodes.isEmpty()) {
        Validation.addError("replacingCodesIds", "Deve contenere almeno un codice.");
      }
    }

    if (Validation.hasErrors()) {
      List<GroupAbsenceType> allGroups = GroupAbsenceType.findAll();
      List<CategoryGroupAbsenceType> allCategories = CategoryGroupAbsenceType.findAll();
      List<AbsenceType> allAbsenceTypes = AbsenceType.findAll();
      render("@editGroup", groupAbsenceType,
          takableCodes, takeAmountType, fixedLimit, takableAmountAdjustment,
          complationAmountType, complationCodes, replacingCodes,
          allCategories, allAbsenceTypes, allGroups);
    }

    //Save take
    TakableAbsenceBehaviour takableBehaviour;
    if (!groupAbsenceType.isPersistent()) {
      takableBehaviour = new TakableAbsenceBehaviour();
      takableBehaviour.groupAbsenceTypes.add(groupAbsenceType);
      takableBehaviour.name = TakableAbsenceBehaviour.NAME_PREFIX + groupAbsenceType.name;
    } else {
      takableBehaviour = groupAbsenceType.takableAbsenceBehaviour;
    }

    takableBehaviour.amountType = takeAmountType;
    takableBehaviour.takableCodes.clear();
    takableBehaviour.takenCodes.clear();
    takableBehaviour.takableCodes = Sets.newHashSet(takableCodes);
    takableBehaviour.takenCodes = Sets.newHashSet(takableCodes);
    takableBehaviour.fixedLimit = fixedLimit;
    takableBehaviour.takableAmountAdjustment = takableAmountAdjustment;
    takableBehaviour.save();
    groupAbsenceType.takableAbsenceBehaviour = takableBehaviour;

    //Save complatio
    ComplationAbsenceBehaviour complationBehaviour;
    if (!groupAbsenceType.isPersistent() || groupAbsenceType.complationAbsenceBehaviour == null) {
      complationBehaviour = new ComplationAbsenceBehaviour();
      complationBehaviour.name = ComplationAbsenceBehaviour.NAME_PREFIX + groupAbsenceType.name;
      complationBehaviour.groupAbsenceTypes.add(groupAbsenceType);
    } else {
      complationBehaviour = groupAbsenceType.complationAbsenceBehaviour;
    }

    if (complationAmountType == null) {
      //distruzione
      if (groupAbsenceType.complationAbsenceBehaviour != null) {
        ComplationAbsenceBehaviour complationToRemove = groupAbsenceType.complationAbsenceBehaviour;
        groupAbsenceType.complationAbsenceBehaviour = null;
        groupAbsenceType.save();
        complationToRemove.groupAbsenceTypes.remove(groupAbsenceType);
        complationToRemove.complationCodes.clear();
        complationToRemove.replacingCodes.clear();
        complationToRemove.delete();
      }
    } else {
      //creazione / modifica
      complationBehaviour.complationCodes.clear();
      complationBehaviour.replacingCodes.clear();
      complationBehaviour.complationCodes = Sets.newHashSet(complationCodes);
      complationBehaviour.replacingCodes = Sets.newHashSet(replacingCodes);
      complationBehaviour.amountType = complationAmountType;
      complationBehaviour.save();
      groupAbsenceType.complationAbsenceBehaviour = complationBehaviour;
    }

    groupAbsenceType.save();

    flash.success("Operazione eseguita con successo");
    editGroup(groupAbsenceType.id);
  }


  /**
   * End point lista codici assenza.
   */
  public static void showAbsenceTypes() {
    List<AbsenceType> absenceTypes = AbsenceType.findAll();
    render(absenceTypes);
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
   * @param tecnologi se il codice di assenza è valido per i tecnologi
   * @param tecnici se il codice di assenza è valido per i tecnici
   */
  public static void saveAbsenceType(@Valid AbsenceType absenceType,
      boolean tecnologi, boolean tecnici) {

    List<JustifiedType> allJustifiedType = JustifiedType.findAll();

    if (Validation.hasErrors()) {
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
   * Tab per la consultazione di tutti i codici e link per l'inserimento.
   *
   * @param personId persona selezionata
   * @param from data selezionata
   */
  public static void findCode(Long personId, LocalDate from) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);

    AbsenceForm absenceForm = absenceService.buildAbsenceForm(person, from, null,
        null, null, null, true, null, null, null, null, false, false);

    //La lista di tutti i codici takable... con associato il gruppo con maggiore priorità.
    Set<AbsenceType> allTakable = Sets.newHashSet();
    for (GroupAbsenceType group : absenceComponentDao.allGroupAbsenceType(false)) {
      for (AbsenceType abt : group.takableAbsenceBehaviour.takableCodes) {
        if (abt.defaultTakableGroup() == null) {
          log.debug("Il defaultTakable è null per {}", abt.code);
          abt.defaultTakableGroup();
        }
      }
      //TODO eventualmente controllo prendibilità della persona alla data (figli, l 104 etc.)
      allTakable.addAll(group.takableAbsenceBehaviour.takableCodes);
    }

    render(absenceForm, allTakable);

  }

  /**
   * End point per la simulazione di inserimento assenze.s
   *
   * @param personId persona
   * @param from data inizio
   * @param categoryTab tab
   * @param to data fine
   * @param groupAbsenceType gruppo assenze
   * @param switchGroup se cambio gruppo di assenze
   * @param absenceType tipo assenza
   * @param justifiedType tipo giustificativo
   * @param hours ore
   * @param minutes minuti
   */
  public static void insert(
      Long personId, LocalDate from, CategoryTab categoryTab,                      //tab
      LocalDate to, LocalDate recoveryDate, GroupAbsenceType groupAbsenceType,
      boolean switchGroup, AbsenceType absenceType, JustifiedType justifiedType,   //confGroup 
      Integer hours, Integer minutes, boolean forceInsert) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);

    rules.checkIfPermitted(person);

    AbsenceForm absenceForm =
        absenceService.buildAbsenceForm(person, from, categoryTab,
            to, recoveryDate, groupAbsenceType, switchGroup, absenceType,
            justifiedType, hours, minutes, false, false);

    InsertReport insertReport = absenceService.insert(person,
        absenceForm.groupSelected,
        absenceForm.from, absenceForm.to,
        absenceForm.absenceTypeSelected, absenceForm.justifiedTypeSelected,
        absenceForm.hours, absenceForm.minutes, forceInsert, absenceManager);
    if (recoveryDate != null && !recoveryDate.isAfter(to)) {
      Validation.addError("recoveryDate", "Deve essere successiva alla data di fine");
    }
    render(absenceForm, insertReport, forceInsert, recoveryDate);

  }

  /**
   * Inserimento assistito partendo dalla ricerca.
   *
   * @param personId persona
   * @param from data inizio
   * @param absenceType tipo
   */
  public static void insertAssisted(Long personId, LocalDate from, AbsenceType absenceType) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
    notFoundIfNull(absenceType);

    rules.checkIfPermitted(person);

    GroupAbsenceType groupAbsenceType = absenceType.defaultTakableGroup();
    if (groupAbsenceType.firstOfChain() != null) {
      groupAbsenceType = groupAbsenceType.firstOfChain();
    }
    if (groupAbsenceType.pattern != GroupAbsenceTypePattern.simpleGrouping) {
      absenceType = null;
    }

    AbsenceForm absenceForm =
        absenceService.buildAbsenceForm(person, from, null,
            null, null, groupAbsenceType, false, absenceType, null, null, null, false, false);

    InsertReport insertReport = absenceService.insert(person,
        absenceForm.groupSelected,
        absenceForm.from, absenceForm.to,
        absenceForm.absenceTypeSelected, absenceForm.justifiedTypeSelected,
        absenceForm.hours, absenceForm.minutes, false, absenceManager);

    render("@insert", absenceForm, insertReport);
  }

  /**
   * End Point per il salvataggio di assenze.
   *
   * @param personId persona
   * @param from data inizio
   * @param to data fine
   * @param groupAbsenceType gruppo assenze
   * @param absenceType tipo assenza
   * @param justifiedType giustificativo
   * @param hours ore
   * @param minutes minuti
   * @param forceInsert forza inserimento
   */
  public static void save(Long personId, LocalDate from, LocalDate to, LocalDate recoveryDate,
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

    log.info("Richiesto inserimento assenze per {}. "
        + "Codice/Tipo {}, dal {} al {} (ore:{}, minuti:{})", 
        person.getFullname(), absenceType != null ? absenceType.code : groupAbsenceType,
        from, to, hours, minutes);
    
    absenceManager.saveAbsences(insertReport, person, from, recoveryDate, 
        justifiedType, groupAbsenceType);
    
    log.info("Effettuato inserimento assenze per {}. "
        + "Codice/Tipo {}, dal {} al {} (ore:{}, minuti:{})", 
        person.getFullname(), absenceType != null ? absenceType.code : groupAbsenceType, 
        from, to, hours, minutes);

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

    PeriodChain periodChain = absenceService.residual(person, groupAbsenceType, from);

    boolean isAdmin = false;
    //se l'user è amministratore visualizzo lo switcher del gruppo
    final User currentUser = Security.getUser().get();
    if (currentUser.isSystemUser()
        || userDao.getUsersWithRoles(person.office, Role.PERSONNEL_ADMIN, Role.PERSONNEL_ADMIN_MINI)
        .contains(currentUser)) {
      isAdmin = true;
    }

    if (groupAbsenceType.automatic) {
      render(from, groupAbsenceType, periodChain, isAdmin);  //no switcher
    }

    AbsenceForm categorySwitcher = absenceService
        .buildForCategorySwitch(person, from, groupAbsenceType);

    render(from, categorySwitcher, groupAbsenceType, periodChain, isAdmin);

  }

  /**
   * Elenco delle inizializzazioni assenze.
   *
   * @param officeId sede
   * @param groupAbsenceTypeId gruppo
   */
  public static void absenceInitializations(Long officeId, Long groupAbsenceTypeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    //Categorie inizializzabili e gruppo selezionato
    List<CategoryGroupAbsenceType> initializableCategories =
        absenceComponentDao.initializablesCategory();

    GroupAbsenceType groupAbsenceType = null;
    if (groupAbsenceTypeId != null) {
      groupAbsenceType = absenceComponentDao.groupAbsenceTypeById(groupAbsenceTypeId);
    }

    List<InitializationGroup> initializations = Lists.newArrayList();
    List<Person> withoutInitialization = Lists.newArrayList();

    //Tutte le persone attive e tutte le inizializzazioni.
    for (Person person : personDao.listFetched(Optional.absent(),
        ImmutableSet.of(office), false, LocalDate.now(), LocalDate.now(), false).list()) {
      boolean existInitialization = false;
      for (InitializationGroup initializationGroup : person.initializationGroups) {

        if (groupAbsenceType == null
            || groupAbsenceType.equals(initializationGroup.groupAbsenceType)) {
          existInitialization = true;
          initializations.add(initializationGroup);
        }
      }

      if (!existInitialization) {
        withoutInitialization.add(person);
      }
    }

    render(initializableCategories, groupAbsenceType, initializations,
        withoutInitialization, office);

  }

  /**
   * End point per definire l'inizializzazione di un gruppo.
   *
   * @param personId persona
   * @param groupAbsenceTypeId gruppo
   * @param date data
   */
  public static void initialization(Long personId, Long groupAbsenceTypeId, LocalDate date,
      boolean redirectToStatus) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);

    rules.checkIfPermitted(person.office);

    GroupAbsenceType groupAbsenceType = GroupAbsenceType.findById(groupAbsenceTypeId);
    notFoundIfNull(groupAbsenceType);
    if (date == null) {
      date = LocalDate.now();
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
      render(initializationGroup, date, redirectToStatus);
    }
    int averageWeekWorkingTime = wtt.get().weekAverageWorkingTime();

    //Stato del gruppo
    PeriodChain periodChain = absenceService.residual(person, groupAbsenceType, date);
    if (periodChain.periods.isEmpty()) {
      render(initializationGroup, periodChain, date, redirectToStatus);
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

    render(date, initializationGroup, periodChain, absencePeriod, redirectToStatus);

  }

  /**
   * Persiste una nuova inizializzazione.
   *
   * @param initializationGroup initializationGroup
   */
  public static void saveInitialization(@Valid InitializationGroup initializationGroup,
      boolean redirectToStatus) {

    // Lo stato della richiesta

    notFoundIfNull(initializationGroup.person);
    notFoundIfNull(initializationGroup.groupAbsenceType);
    notFoundIfNull(initializationGroup.date);

    rules.checkIfPermitted(initializationGroup.person.office);

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

    if (redirectToStatus) {
      groupStatus(initializationGroup.person.id, initializationGroup.groupAbsenceType.id,
          initializationGroup.date);
    } else {
      absenceInitializations(initializationGroup.person.office.id,
          initializationGroup.groupAbsenceType.id);
    }
  }

  /**
   * Elimina l'inizializzazione.
   *
   * @param initializationGroup inizializzazione
   */
  public static void deleteInitialization(InitializationGroup initializationGroup,
      boolean redirectToStatus) {

    notFoundIfNull(initializationGroup);

    rules.checkIfPermitted(initializationGroup.person.office);

    initializationGroup.delete();

    flash.success("Inizializzazione rimossa con successo.");

    //TODO: check del gruppo per i ricalcoli

    if (redirectToStatus) {
      groupStatus(initializationGroup.person.id, initializationGroup.groupAbsenceType.id,
          initializationGroup.date);
    } else {
      absenceInitializations(initializationGroup.person.office.id,
          initializationGroup.groupAbsenceType.id);
    }

  }


  /**
   * metodo che renderizza la pagina di modifica di una determinata assenza.
   *
   * @param absenceId id dell'assenza
   */
  public static void edit(final long absenceId) {

    final Absence absence = absenceDao.getAbsenceById(absenceId);

    notFoundIfNull(absence);

    rules.checkIfPermitted(absence);

    List<HistoryValue<Absence>> historyAbsence = absenceHistoryDao.absences(absence.id);

    LocalDate dateFrom = absence.personDay.date;
    LocalDate dateTo = absence.personDay.date;
    log.debug("Code: {}", absence.absenceType);

    render(absence, dateFrom, dateTo, historyAbsence);
  }

  /**
   * metodo che cancella una certa assenza fino ad un certo periodo.
   *
   * @param absenceId id dell'assenza
   * @param dateTo la data di fine periodo
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
   * Gli errori sulle assenze di quella sede.
   *
   * @param officeId sede
   */
  public static void absenceTroubles(Long officeId, int year, int month) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    //Tutte le persone attive nel mese attuale?
    List<Person> people = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(office), false,
        new LocalDate(year, month, 1),
        new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(), true).list();

    //Tutti gli absenceErrors di quelle persone...
    Map<Person, List<Absence>> mapTroubles = absenceComponentDao.absenceTroubles(people);

    render(people, mapTroubles, office, year, month);
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
        log.debug("{} is: {}, permitted: {}", absence.toString(),
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
   *
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

  /**
   * I codici assenza in attestati e loro analisi.
   */
  public static void certificationsAbsenceCodes(boolean eraseErasable) throws ExecutionException {

    CodeComparation codeComparation = absenceCertificationService.computeCodeComparation();

    if (eraseErasable) {
      codeComparation.eraseErasable();
    }

    render(codeComparation);
  }

  /**
   * Se lo stato del db è consistente la modellazione via enumerati (che garantisce il corretto
   * funzionamento). Entità controllate: Tab, Categorie, Gruppi, Parte takable, Parte completamento,
   * Codici Assenza
   */
  public static void consistencyGroups() {

    List<CategoryTab> allCategoryTabs = absenceComponentDao.tabsByPriority();
    List<CategoryGroupAbsenceType> allCategories = absenceComponentDao.categoriesByPriority();
    List<GroupAbsenceType> allGroups = absenceComponentDao.allGroupAbsenceType(true);
    List<ComplationAbsenceBehaviour> allComplations = Lists.newArrayList();
    for (GroupAbsenceType group : allGroups) {
      if (group.complationAbsenceBehaviour != null) {
        allComplations.add(group.complationAbsenceBehaviour);
      }
    }
    List<TakableAbsenceBehaviour> allTakables = Lists.newArrayList();
    for (GroupAbsenceType group : allGroups) {
      allTakables.add(group.takableAbsenceBehaviour);
    }
    List<AbsenceType> allAbsenceTypes = AbsenceType.findAll();
    render(allCategoryTabs, allCategories, allGroups, allComplations, allTakables, allAbsenceTypes);

  }

  /**
   * Allinea la modellazione con gli enumerati.
   */
  public static void consistencyCleans() {

    absenceService.enumAllineator();

    //TODO: in questi casi bisogna validare per bene gli enumerati....
    //    List<AbsenceType> allAbsenceTypes = AbsenceType.findAll();
    //    for (AbsenceType absenceType : allAbsenceTypes) {
    //      //justified time 0 anzichè null
    //      if (absenceType.justifiedTime == null) {
    //        absenceType.justifiedTime = 0;
    //        absenceType.save();
    //      }
    //      //replacing time 0 anzichè null
    //      if (absenceType.replacingTime == null) {
    //        absenceType.replacingTime = 0;
    //        absenceType.save();
    //      }
    //      //un codice attivo è sempre stato attivo (null, null)
    //      if (!absenceType.isExpired()) {
    //        absenceType.validFrom = null;
    //        absenceType.validTo = null;
    //        absenceType.save();
    //      }
    //    }

    consistencyGroups();
  }

  /**
   * Migra tutti i 661MO presi nel 2018, in accordo alle nuove normative. Una volta che sarà
   * applicato a Roma sarà possibile cancellarlo ...
   */
  @SuppressWarnings("deprecation")
  public static void migration661M() {
    List<Absence> absences = absenceComponentDao
        .absences(Lists.newArrayList(DefaultAbsenceType.A_661MO.getCode()));

    DateInterval yearInterval = DateUtility.getYearInterval(2018);

    Set<Person> toUpdate = Sets.newHashSet();

    Set<Person> toScan = Sets.newHashSet();

    JustifiedType allDay = absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.all_day);
    Optional<AbsenceType> a661G = absenceComponentDao
        .absenceTypeByCode(DefaultAbsenceType.A_661G.getCode());

    LocalDate beginCalculation = null;

    // tutte le assenze 661MO
    for (Absence absence : absences) {

      // utilizzate dal 1/1/2018 al 31/12/2018
      if (!DateUtility.isDateIntoInterval(absence.personDay.getDate(), yearInterval)) {
        continue;
      }
      IWrapperPerson wrapperPerson = wrapperFactory.create(absence.personDay.person);

      Optional<Contract> contract = wrapperPerson.getCurrentContract();
      if (!contract.isPresent()) {
        continue;
      }

      if (!contract.get().beginDate.isBefore(yearInterval.getBegin())) {
        beginCalculation = contract.get().beginDate;
      } else {
        beginCalculation = yearInterval.getBegin();
      }

      if (!DateUtility.isDateIntoInterval(absence.personDay.getDate(),
          new DateInterval(contract.get().beginDate, contract.get().endDate))) {
        continue;
      }

      IWrapperPersonDay wrapperPersonDay = wrapperFactory.create(absence.personDay);

      Optional<WorkingTimeTypeDay> wttd = wrapperPersonDay.getWorkingTimeTypeDay();

      if (!wttd.isPresent()) {
        continue;
      }

      // di 7:12 quando la persona ha un orario 7:12
      if (wttd.get().workingTime == 432 && absence.justifiedMinutes == 432) {

        // sono convertite in 661G
        absence.justifiedMinutes = null;
        absence.justifiedType = allDay;
        absence.absenceType = a661G.get();
        absence.save();
        toUpdate.add(absence.personDay.person);
        continue;
      }

      // di 7:00 quando la persona ha un orario 7:12
      if (wttd.get().workingTime == 432 && absence.justifiedMinutes == 420) {

        // sono convertite in 661G
        absence.justifiedMinutes = null;
        absence.justifiedType = allDay;
        absence.absenceType = a661G.get();
        absence.save();
        toUpdate.add(absence.personDay.person);
        continue;
      }

      // non di 7:12 ma di un valore uguale o superiore a 6 ore
      if (absence.justifiedMinutes > 300) {

        // saranno inserite in uno stato di warning
        toScan.add(absence.personDay.person);
      }

    }
    JPA.em().flush();
    JPAPlugin.closeTx(false);

    if (!toUpdate.isEmpty()) {
      for (Person person : toUpdate) {
        JPAPlugin.startTx(false);
        IWrapperPerson wrapperPerson = wrapperFactory.create(person);

        Optional<Contract> contract = wrapperPerson.getCurrentContract();
        if (!contract.get().beginDate.isBefore(yearInterval.getBegin())) {
          beginCalculation = contract.get().beginDate;
        } else {
          beginCalculation = yearInterval.getBegin();
        }

        consistencyManager.updatePersonSituation(person.id, beginCalculation);
        JPA.em().flush();
        JPAPlugin.closeTx(false);
      }
      for (Person person : toScan) {
        JPAPlugin.startTx(false);
        IWrapperPerson wrapperPerson = wrapperFactory.create(person);

        Optional<Contract> contract = wrapperPerson.getCurrentContract();
        if (!contract.get().beginDate.isBefore(yearInterval.getBegin())) {
          beginCalculation = contract.get().beginDate;
        } else {
          beginCalculation = yearInterval.getBegin();
        }

        consistencyManager.updatePersonSituation(person.id, beginCalculation);
        JPA.em().flush();
        JPAPlugin.closeTx(false);
      }
    }

    renderText("Migrazione completata.");
  }

  /**
   * Import (e sincronizzazione) assenze da attestati. Le persone devono avere il monte ore
   * correttamente inizializzato.
   *
   * @param officeId sede
   */
  public static void importCertificationsAbsences(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    List<IWrapperPerson> people = Lists.newArrayList();
    Map<String, Optional<CertificationYearSituation>> certificationsSummary = Maps.newHashMap();

    int year = LocalDate.now().getMonthOfYear() == DateTimeConstants.JANUARY 
        ? LocalDate.now().getYear() - 1 : LocalDate.now().getYear();

    for (IWrapperPerson wrPerson : FluentIterable.from(personDao.listFetched(Optional.absent(),
        ImmutableSet.of(office), false, null, null, false).list())
        .transform(wrapperFunctionFactory.person()).toList()) {

      if (!wrPerson.getCurrentContract().isPresent()) {
        continue;
      }
      //if (wrapperFactory.create(wrPerson.getCurrentContract().get()).initializationMissing()) {
      //  continue;
      //}

      people.add(wrPerson);

      //prelevo dalla cache la situazione se è già presente.
      certificationsSummary.put(wrPerson.getValue().number, absenceCertificationService
          .certificationYearSituationCached(wrPerson.getValue(), year));
    }

    render(people, office, year, certificationsSummary);
  }

  /**
   * Le assenze in attestati.
   */
  public static void certificationsAbsences(Long personId, Integer year)
      throws NoSuchFieldException, ExecutionException {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);

    CertificationYearSituation yearSituation = absenceCertificationService
        .buildCertificationYearSituation(person, year, false);

    render(yearSituation, person);
  }

  /**
   * Ricarica lo stato in cache della situazione import assenze.
   *
   * @param personId persona
   */
  public static void reloadCertificationAbsences(Long personId) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);
    int year = LocalDate.now().getYear();
    if (LocalDate.now().getMonthOfYear() == DateTimeConstants.JANUARY) {
      year = year - 1;
    }
    CertificationYearSituation yearSituation = absenceCertificationService
        .buildCertificationYearSituation(person, year, false);

    if (yearSituation == null) {
      flash.error("Impossibile recuperare la situazione del dipendente"
          + " all'interno della sede selezionata");
    }

    importCertificationsAbsences(person.office.id);

  }

  /**
   * Importa le assenze mancanti da attestati.
   */
  public static void syncCertificationsAbsences(Long personId, Integer year)
      throws NoSuchFieldException, ExecutionException {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);
    LocalDate updateFrom = LocalDate.now();
    LocalDate beginYear = updateFrom.monthOfYear().withMinimumValue()
        .dayOfMonth().withMinimumValue();
    List<Absence> absences = absenceCertificationService.absencesToPersist(person, year);
    absences.sort(Comparator.comparing(Absence::getAbsenceDate));

    for (Absence absence : absences) {
      JPA.em().flush(); //potrebbero esserci dei doppioni, per sicurezza flusho a ogni assenza.
      if (!absenceComponentDao
          .findAbsences(person, absence.getAbsenceDate(), absence.absenceType.code).isEmpty()) {
        continue;
      }

      PersonDay personDay = personDayManager
          .getOrCreateAndPersistPersonDay(person, absence.getAbsenceDate());
      absence.personDay = personDay;
      personDay.absences.add(absence);
      absence.save();
      personDay.save();
      if (absence.absenceType.code.equals(DefaultAbsenceType.A_91.certificationCode) 
          && !absence.getAbsenceDate().isBefore(beginYear)) {
        IWrapperPerson wrPerson = wrapperFactory.create(person);
        Optional<Contract> contract = wrPerson.getCurrentContract();
        if (contract.isPresent()) {
          contract.get().sourceDateRecoveryDay = absence.getAbsenceDate();
          contract.get().sourceRecoveryDayUsed++;
          contract.get().save();
        }
      }
      if (personDay.date.isBefore(updateFrom)) {
        updateFrom = personDay.date;
      }
    }

    JPA.em().flush();
    consistencyManager.updatePersonSituation(person.id, updateFrom);

    absenceCertificationService.buildCertificationYearSituation(person, year, false);

    flash.success("Tutti i dati sono stati caricati correttamente.");

    importCertificationsAbsences(person.office.id);
  }

}
