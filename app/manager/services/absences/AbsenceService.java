package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Inject;
import controllers.Security;
import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import manager.AbsenceManager;
import manager.SecureManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.response.AbsenceInsertReport;
import manager.response.AbsencesResponse;
import manager.services.absences.errors.AbsenceError;
import manager.services.absences.errors.CriticalError;
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.DayInPeriod;
import manager.services.absences.model.DayInPeriod.TemplateRow;
import manager.services.absences.model.PeriodChain;
import manager.services.absences.model.Scanner;
import manager.services.absences.model.ServiceFactories;
import manager.services.absences.model.VacationSituation;
import manager.services.absences.model.VacationSituation.VacationSummary;
import manager.services.absences.model.VacationSituation.VacationSummary.TypeSummary;
import manager.services.absences.model.VacationSituation.VacationSummaryCached;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonChildren;
import models.User;
import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;
import models.absences.CategoryTab;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.InitializationGroup;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultGroup;
import org.joda.time.LocalDate;
import org.joda.time.MonthDay;
import play.cache.Cache;

/**
 * Interfaccia epas per il componente assenze. le richieste via form.
 *
 * @author alessandro
 */
@Slf4j
public class AbsenceService {

  private final AbsenceEngineUtility absenceEngineUtility;
  private final AbsenceComponentDao absenceComponentDao;
  private final PersonChildrenDao personChildrenDao;
  private final ServiceFactories serviceFactories;
  private final EnumAllineator enumAllineator;
  private final ConfigurationManager confManager;
  private final SecureManager secureManager;
  private final ConfigurationManager configurationManager;

  /**
   * Costruttore injection.
   *
   * @param absenceEngineUtility injected
   * @param serviceFactories injected
   * @param absenceComponentDao injected
   * @param personChildrenDao injected
   */
  @Inject
  public AbsenceService(
      ConfigurationManager configurationManager,
      AbsenceEngineUtility absenceEngineUtility,
      ServiceFactories serviceFactories,
      AbsenceComponentDao absenceComponentDao,
      PersonChildrenDao personChildrenDao,
      ConfigurationManager confManager,
      SecureManager secureManager,
      EnumAllineator enumAllineator) {
    this.configurationManager = configurationManager;
    this.absenceEngineUtility = absenceEngineUtility;
    this.serviceFactories = serviceFactories;
    this.absenceComponentDao = absenceComponentDao;
    this.personChildrenDao = personChildrenDao;
    this.confManager = confManager;
    this.secureManager = secureManager;
    this.enumAllineator = enumAllineator;
  }

  /**
   * La absenceForm utile alla lista delle categorie abilitate per la persona alla data, ordinate
   * per priorità. Se groupAbsenceType è presente imposta il gruppo come selezionato.
   *
   * @param person persona
   * @param date data
   * @param groupAbsenceType gruppo selezionato
   * @return absenceForm.
   */
  public AbsenceForm buildForCategorySwitch(Person person, LocalDate date,
      GroupAbsenceType groupAbsenceType) {

    if (groupAbsenceType == null || !groupAbsenceType.isPersistent()) {
      groupAbsenceType = absenceComponentDao
          .categoriesByPriority().get(0)
          .groupAbsenceTypes.iterator().next();
    }

    AbsenceForm form = buildAbsenceForm(person, date, null, null, null,
        groupAbsenceType, true, null, null, null, null, true, false);

    return form;
  }


  /**
   * Genera la form di inserimento assenza.
   *
   * @param person person
   * @param from data inizio
   * @param categoryTab tab
   * @param to data fine
   * @param groupAbsenceType gruppo
   * @param switchGroup se passa a nuovo gruppo
   * @param absenceType tipo assenza
   * @param justifiedType giustificativo
   * @param hours ore
   * @param minutes minuti
   * @param readOnly se richiesta sola lettura
   * @return form
   */
  public AbsenceForm buildAbsenceForm(
      Person person, LocalDate from, CategoryTab categoryTab,                            //tab 
      LocalDate to, LocalDate recoveryDate, GroupAbsenceType groupAbsenceType,
      boolean switchGroup, AbsenceType absenceType, JustifiedType justifiedType,         //reconf 
      Integer hours, Integer minutes, boolean readOnly, boolean fromWorkFlow) {

    //clean entities
    if (groupAbsenceType == null || !groupAbsenceType.isPersistent()) {
      groupAbsenceType = null;
      switchGroup = true;
    }
    if (justifiedType == null || !justifiedType.isPersistent()) {
      justifiedType = null;
    }
    if (absenceType == null || !absenceType.isPersistent()) {
      absenceType = null;
    }
    if (categoryTab == null || !categoryTab.isPersistent()) {
      categoryTab = null;
    }

    // TODO: in base al parametro passato al metodo verifico come popolare la lista dei 
    // groupsPermitted
    List<GroupAbsenceType> groupsPermitted = Lists.newArrayList();
    if (fromWorkFlow) {
      //provengo dal flusso di approvazione ferie/permessi
      groupsPermitted = groupsPermittedFlow();
    } else {
      //iter normale di inserimento assenze
      groupsPermitted = groupsPermitted(person, readOnly);
    }

    if (groupAbsenceType != null) {
      Verify.verify(groupsPermitted.contains(groupAbsenceType));
      categoryTab = groupAbsenceType.category.tab;
    } else {
      if (categoryTab != null) {
        groupAbsenceType = categoryTab.firstByPriority()
            .orderedGroupsInCategory(true).iterator().next();
        Verify.verify(groupsPermitted.contains(groupAbsenceType));
      } else {
        //selezionare missione?
        for (GroupAbsenceType group : groupsPermitted) {
          if (group.name.equals(DefaultGroup.MISSIONE_GIORNALIERA.name())) {
            groupAbsenceType = group;
            break;
          }
        }
        if (groupAbsenceType == null) {
          groupAbsenceType = groupsPermitted.get(0);
        }
        categoryTab = groupAbsenceType.category.tab;
      }
    }

    if (switchGroup) {
      absenceType = null;
      justifiedType = null;
      hours = null;
      minutes = null;
    }

    //Errore grave
    Verify.verifyNotNull(groupAbsenceType);

    //TODO: Preconditions se groupAbsenceType presente verificare che permesso per la persona

    return new AbsenceForm(person, from, to, recoveryDate, groupAbsenceType, absenceType,
        justifiedType, hours, minutes, groupsPermitted,
        absenceComponentDao, absenceEngineUtility);
  }

  /**
   * Effettua la simulazione dell'inserimento. Genera il report di inserimento.
   *
   * @param person person
   * @param groupAbsenceType gruppo
   * @param from data inizio
   * @param to data fine
   * @param absenceType tipo assenza
   * @param justifiedType giustificativo
   * @param hours ore
   * @param minutes minuti
   * @param absenceManager absenceManager inject (circular dependency)
   * @return insert report
   */
  public InsertReport insert(Person person, GroupAbsenceType groupAbsenceType,
      LocalDate from, LocalDate to,
      AbsenceType absenceType, JustifiedType justifiedType,
      Integer hours, Integer minutes, boolean forceInsert, AbsenceManager absenceManager) {

    //Inserimento forzato (nessun controllo)
    if (forceInsert) {
      Preconditions.checkNotNull(absenceType);
      return forceInsert(person, from, to,
          absenceType, justifiedType, hours, minutes);
    }

    if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.compensatoryRestCnr)) {
      InsertReport insertReport = temporaryInsertCompensatoryRest(person,
          groupAbsenceType, from, to, null, absenceManager);
      return insertReport;
    }

    List<PeriodChain> chains = Lists.newArrayList();
    List<Absence> previousInserts = Lists.newArrayList();
    List<CriticalError> criticalErrors = Lists.newArrayList();
    LocalDate currentDate = from;
    Integer specifiedMinutes = absenceEngineUtility.getMinutes(hours, minutes);
    while (true) {

      //Preparare l'assenza da inserire
      Absence absenceToInsert = new Absence();
      absenceToInsert.date = currentDate;
      absenceToInsert.absenceType = absenceType;
      absenceToInsert.justifiedType = justifiedType;
      if (specifiedMinutes != null) {
        absenceToInsert.justifiedMinutes = specifiedMinutes;
      }

      List<PersonChildren> orderedChildren = personChildrenDao.getAllPersonChildren(person);
      List<Contract> fetchedContracts = person.contracts; //TODO: fetch
      List<InitializationGroup> initializationGroups =
          absenceComponentDao.personInitializationGroups(person);

      PeriodChain periodChain = serviceFactories
          .buildPeriodChain(person, groupAbsenceType, currentDate,
              previousInserts, absenceToInsert,
              orderedChildren, fetchedContracts, initializationGroups);

      criticalErrors.addAll(periodChain.criticalErrors());

      chains.add(periodChain);

      if (to == null) {
        break;
      }
      currentDate = currentDate.plusDays(1);
      if (currentDate.isAfter(to)) {
        break;
      }
    }

    return buildInsertReport(chains, criticalErrors);

  }

  /**
   * Costruisce il report per l'inserimento.
   *
   * @param chains catene con gli inserimenti.
   * @param criticalErrors errori critici.
   * @return insert report
   */
  private InsertReport buildInsertReport(List<PeriodChain> chains,
      List<CriticalError> criticalErrors) {

    InsertReport insertReport = new InsertReport();

    //Se una catena contiene errori critici il report è vuoto.
    if (!criticalErrors.isEmpty()) {
      insertReport.criticalErrors = criticalErrors;
      return insertReport;
    }

    //Gli esiti sotto forma di template rows
    List<TemplateRow> insertTemplateRows = Lists.newArrayList();
    for (PeriodChain periodChain : chains) {

      //caso particolare di errore figli.
      if (periodChain.childIsMissing()) {
        TemplateRow templateRow = new TemplateRow();
        templateRow.date = periodChain.date;
        templateRow.absenceErrors.add(AbsenceError.builder()
            .absenceProblem(AbsenceProblem.NoChildExist).build());
        insertTemplateRows.add(templateRow);
      }

      AbsencePeriod lastPeriod = periodChain.lastPeriod();
      for (AbsencePeriod absencePeriod : periodChain.periods) {
        boolean addResult = false;
        //Aggiungo il risultato in caso di fallimento per il solo ultimo periodo
        if (periodChain.successPeriodInsert == null && absencePeriod.equals(lastPeriod)) {
          addResult = true;
        }
        //Aggiungo il risultato in caso di successo per il solo periodo di successo
        if (periodChain.successPeriodInsert != null
            && periodChain.successPeriodInsert.equals(absencePeriod)) {
          addResult = true;
        }
        if (addResult) {
          for (DayInPeriod dayInPeriod : absencePeriod.daysInPeriod.values()) {
            insertTemplateRows.addAll(dayInPeriod.templateRowsForInsert(
                absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.nothing)));
          }
        }
      }
    }
    insertReport.insertTemplateRows = insertTemplateRows;

    for (TemplateRow templateRow : insertReport.insertTemplateRows) {
      if (templateRow.usableColumn) {
        insertReport.usableColumn = true;
      }
      if (templateRow.complationColumn) {
        insertReport.complationColumn = true;
      }
    }

    // le assenze da persistere
    for (PeriodChain periodChain : chains) {
      if (periodChain.successPeriodInsert != null) {
        insertReport.absencesToPersist.add(periodChain
            .successPeriodInsert.attemptedInsertAbsence);
      }
    }

    return insertReport;
  }

  /**
   * Effettua la simulazione dell'inserimento forzato. Genera il report di inserimento.
   *
   * @param person person
   * @param from data inizio
   * @param to data fine
   * @param absenceType tipo assenza
   * @param justifiedType giustificativo
   * @param hours ore
   * @param minutes minuti
   * @return insert report
   */
  public InsertReport forceInsert(Person person,
      LocalDate from, LocalDate to,
      AbsenceType absenceType, JustifiedType justifiedType,
      Integer hours, Integer minutes) {
    InsertReport insertReport = new InsertReport();

    Integer specifiedMinutes = absenceEngineUtility.getMinutes(hours, minutes);
    LocalDate currentDate = from;

    while (true) {

      //Preparare l'assenza da inserire
      Absence absenceToInsert = new Absence();
      absenceToInsert.date = currentDate;
      absenceToInsert.absenceType = absenceType;
      absenceToInsert.justifiedType = justifiedType;
      if (specifiedMinutes != null) {
        absenceToInsert.justifiedMinutes = specifiedMinutes;
      }

      insertReport.absencesToPersist.add(absenceToInsert);

      TemplateRow templateRow = new TemplateRow();
      templateRow.absence = absenceToInsert;
      templateRow.date = currentDate;
      templateRow.absenceWarnings.add(AbsenceError.builder()
          .absence(absenceToInsert)
          .absenceProblem(AbsenceProblem.ForceInsert).build());
      insertReport.insertTemplateRows.add(templateRow);

      if (to == null) {
        break;
      }
      currentDate = currentDate.plusDays(1);
      if (currentDate.isAfter(to)) {
        break;
      }
    }

    return insertReport;
  }


  /**
   * Esegue lo scanning delle assenze della persona a partire dalla data passata come parametro per
   * verificarne la correttezza. Gli errori riscontrati vengono persistiti all'assenza.
   *
   * <p>
   * Microservices Questo metodo dovrebbe avere una person dove sono fetchate tutte le informazioni
   * per i calcoli non mantenute del componente assenze:
   * </p>
   * I Contratti / Tempi a lavoro / Piani ferie I Figli Le Altre Tutele
   *
   * @param person persona
   * @param from data inizio
   */
  public Scanner scanner(Person person, LocalDate from) {

    log.debug("Lanciata procedura scan assenze person={}, from={}", person.fullName(), from);

    List<Absence> absencesToScan = absenceComponentDao.orderedAbsences(person, from,
        null, Sets.newHashSet());
    List<PersonChildren> orderedChildren = personChildrenDao.getAllPersonChildren(person);
    List<Contract> fetchedContracts = person.contracts; //TODO: fetch
    List<InitializationGroup> initializationGroups =
        absenceComponentDao.personInitializationGroups(person);

    Scanner absenceScan = serviceFactories.buildScanInstance(person, from, absencesToScan,
        orderedChildren, fetchedContracts, initializationGroups);

    // scan dei gruppi
    absenceScan.scan();

    log.debug("");

    return absenceScan;

  }

  /**
   * Calcola la situazione residuale per la persona per quel gruppo alla data.
   *
   * @param person persona
   * @param groupAbsenceType gruppo
   * @param date data
   * @return situazione (sotto forma di periodChain)
   */
  public PeriodChain residual(Person person, GroupAbsenceType groupAbsenceType, LocalDate date) {

    if (date == null) {
      date = LocalDate.now();
    }

    List<PersonChildren> orderedChildren = personChildrenDao.getAllPersonChildren(person);
    List<Contract> fetchedContracts = person.contracts; //TODO: fetch
    List<InitializationGroup> initializationGroups =
        absenceComponentDao.personInitializationGroups(person);

    PeriodChain periodChain = serviceFactories.buildPeriodChain(person, groupAbsenceType, date,
        Lists.newArrayList(), null,
        orderedChildren, fetchedContracts, initializationGroups);

    return periodChain;

  }

  /**
   * @return la lista dei gruppi di assenza abilitati per il flusso di approvazione.
   */
  public List<GroupAbsenceType> groupsPermittedFlow() {
    List<GroupAbsenceType> groupsPermitted = Lists.newArrayList();
    final GroupAbsenceType vacation =
        absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.FERIE_CNR.name()).get();
    final GroupAbsenceType compensatory =
        absenceComponentDao.groupAbsenceTypeByName(DefaultGroup.RIPOSI_CNR.name()).get();
    groupsPermitted.add(vacation);
    groupsPermitted.add(compensatory);
    return groupsPermitted;
  }

  /**
   * I gruppi su cui l'utente collegato ha i diritti per la persona passata. A seconda che la
   * richista avvenga in lettura. o in scrittura.
   *
   * @param person persona
   * @param readOnly sola lettura
   * @return list la lista dei gruppi di assenza abilitati per la persona.
   */
  public List<GroupAbsenceType> groupsPermitted(Person person, boolean readOnly) {
    List<GroupAbsenceType> groupsPermitted = absenceComponentDao.allGroupAbsenceType(false);
    log.info("Configurazione groupsPermitted, readOnly = {}, groupsPermitted = {}",
        readOnly, groupsPermitted);
    if (readOnly) {
      return groupsPermitted;
    }

    //Fetch special groups
    final GroupAbsenceType employeeVacation = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.FERIE_CNR_DIPENDENTI.name()).get();
    final GroupAbsenceType employeeCompensatory = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.RIPOSI_CNR_DIPENDENTI.name()).get();
    final GroupAbsenceType employeeOffseat = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.LAVORO_FUORI_SEDE.name()).get();
    final GroupAbsenceType telework = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.TELELAVORO.name()).get();

    final User currentUser = Security.getUser().get();

    final boolean officeWriteAdmin = secureManager
        .officesWriteAllowed(currentUser).contains(person.office);

    log.info("officeWriteAdmin = {}, officeWriteAllowed = {}",
        officeWriteAdmin, secureManager.officesWriteAllowed(currentUser));

    //Utente di sistema o amministratore della persona
    if (currentUser.isSystemUser() || officeWriteAdmin) {
      groupsPermitted.remove(employeeVacation);
      groupsPermitted.remove(employeeOffseat);
      groupsPermitted.remove(employeeCompensatory);
      groupsPermitted.remove(telework);
      return groupsPermitted;
    }

    //Persona stessa non autoamministrata
    if (currentUser.person.equals(person) && !officeWriteAdmin) {

      log.info("configurazione gruppi per persona, officeWriteAdmin = {}", officeWriteAdmin);
      //vedere le configurazioni
      groupsPermitted = Lists.newArrayList();

      if ((Boolean) confManager.configValue(person.office, EpasParam.WORKING_OFF_SITE)
          && (Boolean) confManager.configValue(person, EpasParam.OFF_SITE_ABSENCE_WITH_CONVENTION)) {
        groupsPermitted.add(employeeOffseat);
      }

      if ((Boolean) confManager.configValue(person.office, EpasParam.TR_VACATIONS)
          && person.qualification.qualification <= 3) {
        groupsPermitted.add(employeeVacation);
      }

      if ((Boolean) confManager.configValue(person.office, EpasParam.TR_COMPENSATORY)
          && person.qualification.qualification <= 3) {
        groupsPermitted.add(employeeCompensatory);
      }
      
      if ((Boolean) confManager.configValue(person, EpasParam.TELEWORK)) {
        groupsPermitted.add(telework);
      }

      log.info("groupPermitted = {}", groupsPermitted);
      return groupsPermitted;
    }

    return Lists.newArrayList();
  }

  @Deprecated
  private InsertReport temporaryInsertCompensatoryRest(Person person,
      GroupAbsenceType groupAbsenceType, LocalDate from, LocalDate to,
      AbsenceType absenceType, AbsenceManager absenceManager) {

    if (absenceType == null || !absenceType.isPersistent()) {
      absenceType = absenceComponentDao.absenceTypeByCode("91").get();
    }

    return insertReportFromOldReport(
        absenceManager.insertAbsenceSimulation(person, from, Optional.fromNullable(to),
            absenceType, Optional.absent(),
            Optional.absent(), Optional.absent()), groupAbsenceType);

  }

  @Deprecated
  private InsertReport insertReportFromOldReport(AbsenceInsertReport absenceInsertReport,
      GroupAbsenceType groupAbsenceType) {

    InsertReport insertReport = new InsertReport();

    for (AbsencesResponse absenceResponse : absenceInsertReport.getAbsences()) {

      if (absenceResponse.isInsertSucceeded()) {
        TemplateRow templateRow = new TemplateRow();
        templateRow.date = absenceResponse.getDate();
        templateRow.absence = absenceResponse.getAbsenceAdded();
        templateRow.groupAbsenceType = groupAbsenceType;
        insertReport.insertTemplateRows.add(templateRow);
        insertReport.absencesToPersist.add(templateRow.absence);
        if (absenceResponse.isDayInReperibilityOrShift()) {
          templateRow.absenceWarnings.add(AbsenceError.builder()
              .absence(templateRow.absence)
              .absenceProblem(AbsenceProblem.InReperibilityOrShift).build());
        }
        continue;
      }
      TemplateRow templateRow = new TemplateRow();
      templateRow.date = absenceResponse.getDate();
      templateRow.absence = absenceResponse.getAbsenceInError();
      if (absenceResponse.isHoliday()) {
        templateRow.absenceErrors.add(AbsenceError.builder()
            .absence(absenceResponse.getAbsenceAdded())
            .absenceProblem(AbsenceProblem.NotOnHoliday)
            .build());
      } else {
        templateRow.absenceErrors.add(AbsenceError.builder()
            .absence(absenceResponse.getAbsenceAdded())
            .absenceProblem(AbsenceProblem.LimitExceeded)
            .build());
      }
      insertReport.insertTemplateRows.add(templateRow);
    }

    if (absenceInsertReport.getAbsences().isEmpty()) {
      insertReport.warningsPreviousVersion = absenceInsertReport.getWarnings();
    }

    return insertReport;
  }

  public static class InsertReport {

    public List<CriticalError> criticalErrors = Lists.newArrayList();
    public List<TemplateRow> insertTemplateRows = Lists.newArrayList();
    public boolean usableColumn;
    public boolean complationColumn;

    public List<Absence> absencesToPersist = Lists.newArrayList();

    public List<String> warningsPreviousVersion = Lists.newArrayList();

    public int howManySuccess() {
      return insertTemplateRows.size() - howManyReplacing() - howManyError() - howManyIgnored();
    }

    /**
     * Quanti codici di rimpiazzamento.
     *
     * @return int
     */
    public int howManyReplacing() {
      int result = 0;
      for (TemplateRow templateRow : insertTemplateRows) {
        if (templateRow.isReplacingRow) {
          result++;
        }
      }
      return result;
    }

    /**
     * Quanti inserimenti da ignorare.
     *
     * @return int
     */
    public int howManyIgnored() {
      int result = 0;
      for (TemplateRow templateRow : insertTemplateRows) {
        if (templateRow.onlyNotOnHoliday()) {
          result++;
        }
      }
      return result;
    }

    /**
     * Quanti inserimenti con errori.
     *
     * @return int
     */
    public int howManyError() {
      int result = 0;
      for (TemplateRow templateRow : insertTemplateRows) {
        if (!templateRow.absenceErrors.isEmpty() && !templateRow.onlyNotOnHoliday()) {
          result++;
        }
      }
      return result;
    }

    /**
     * Quanti inserimenti con warning.
     *
     * @return int
     */
    public int howManyWarning() {
      int result = 0;
      for (TemplateRow templateRow : insertTemplateRows) {
        if (!templateRow.absenceWarnings.isEmpty()) {
          result++;
        }
      }
      return result;
    }

    /**
     * Le date in repiribilità o in turno.
     *
     * @return date list
     */
    public List<LocalDate> reperibilityShiftDate() {
      List<LocalDate> dates = Lists.newArrayList();
      for (TemplateRow templateRow : insertTemplateRows) {
        if (templateRow.absence == null) {
          continue;
        }
        if (!absencesToPersist.contains(templateRow.absence)) {
          continue;
        }
        if (templateRow.absenceWarnings.isEmpty()) {
          continue;
        }
        for (AbsenceError absenceWarning : templateRow.absenceWarnings) {
          if (absenceWarning.absenceProblem.equals(AbsenceProblem.InReperibility)
              || absenceWarning.absenceProblem.equals(AbsenceProblem.InShift)
              || absenceWarning.absenceProblem.equals(AbsenceProblem.InReperibilityOrShift)) {
            dates.add(templateRow.absence.getAbsenceDate());
          }
        }
      }
      return dates;
    }
  }

  /**
   * Allinea la modellazione db assenze con quella degli enumerati.
   */
  public void enumAllineator() {

    //enumAllineator.patchGroupsProduction();

    enumAllineator.handleTab(false);
    enumAllineator.handleCategory(false);

    enumAllineator.handleAbsenceTypes(false);
    enumAllineator.handleComplations(false);
    enumAllineator.handleTakables(false);
    enumAllineator.handleGroup(false);
    enumAllineator.handleCategory(false);
    enumAllineator.handleTab(false);

  }

  /**
   * Inizializza il db.
   */
  public void enumInitializator() {

    if (AbsenceType.count() > 0) {
      return;
    }
    enumAllineator.handleTab(true);
    enumAllineator.handleCategory(true);

    enumAllineator.handleAbsenceTypes(true);
    enumAllineator.handleComplations(true);
    enumAllineator.handleTakables(true);
    enumAllineator.handleGroup(true);

  }

  /**
   * Situazione riepilogativa della persona.
   *
   * @param contract contratto
   * @param year anno situation
   * @param vacationGroup injected
   * @param residualDate data per maturazione giorni
   * @param cache se prelevare i dati dalla cache
   * @param vacationsService per costruire il vecchio ripilogo da confrontare col nuovo
   * @return situazione
   */
  public VacationSituation buildVacationSituation(Contract contract, int year,
      GroupAbsenceType vacationGroup, Optional<LocalDate> residualDate, boolean cache) {

    VacationSituation situation = new VacationSituation();
    situation.person = contract.person;
    situation.contract = contract;
    situation.year = year;

    //La data target per il riepilogo contrattuale
    LocalDate date = vacationResidualDate(contract, residualDate, year);
    if (date == null) {
      return situation;
    }
    situation.date = date;

    final String lastYearKey = vacationCacheKey(contract, year - 1, TypeSummary.VACATION);
    final String currentYearKey = vacationCacheKey(contract, year, TypeSummary.VACATION);
    final String permissionsKey = vacationCacheKey(contract, year, TypeSummary.PERMISSION);

    //Provo a prelevare la situazione dalla cache
    if (cache) {
      situation.lastYearCached = (VacationSummaryCached) Cache.get(lastYearKey);
      situation.currentYearCached = (VacationSummaryCached) Cache.get(currentYearKey);
      situation.permissionsCached = (VacationSummaryCached) Cache.get(permissionsKey);
      if (situation.lastYearCached != null //&& situation.lastYearCached.date.isEqual(date)
          && situation.currentYearCached != null //&& situation.currentYearCached.date.isEqual(date)
          && situation.permissionsCached != null //&& situation.permissionsCached.date.isEqual(date)
      ) {
        //Tutto correttamente cachato.
        return situation;
      } else {
        log.info("La situazione di {} non era cachata", contract.person.fullName());
      }
    }
    PeriodChain periodChain = residual(contract.person, vacationGroup, date);
    if (!periodChain.vacationSupportList.get(0).isEmpty()) {
      situation.lastYear = new VacationSummary(contract,
          periodChain.vacationSupportList.get(0).get(0),
          year - 1, date, TypeSummary.VACATION);
    }
    if (!periodChain.vacationSupportList.get(1).isEmpty()) {
      situation.currentYear = new VacationSummary(contract,
          periodChain.vacationSupportList.get(1).get(0), year, date, TypeSummary.VACATION);
    }
    if (!periodChain.vacationSupportList.get(2).isEmpty()) {
      situation.permissions = new VacationSummary(contract,
          periodChain.vacationSupportList.get(2).get(0), year, date, TypeSummary.PERMISSION);
    }

    if (cache) {
      situation.lastYearCached = new VacationSummaryCached(situation.lastYear,
          contract, year - 1, date, TypeSummary.VACATION);
      situation.currentYearCached = new VacationSummaryCached(situation.currentYear,
          contract, year, date, TypeSummary.VACATION);
      situation.permissionsCached = new VacationSummaryCached(situation.permissions,
          contract, year, date, TypeSummary.PERMISSION);

      Cache.set(lastYearKey, situation.lastYearCached);
      Cache.set(currentYearKey, situation.currentYearCached);
      Cache.set(permissionsKey, situation.permissionsCached);
    }

    //    try {
    //      CruscottoDipendente cruscottoDipendente = certService
    //        .getCruscottoDipendente(person, year);
    //      CertificationYearSituation yearSituation = 
    //          new CertificationYearSituation(absenceComponentDao, person, cruscottoDipendente);
    //      comparedVacation.certLastYear = yearSituation
    //          .getAbsenceSituation(AbsenceImportType.FERIE_ANNO_PRECEDENTE);
    //      comparedVacation.certCurrentYear = yearSituation
    //          .getAbsenceSituation(AbsenceImportType.FERIE_ANNO_CORRENTE);
    //      comparedVacation.certPermission = yearSituation
    //          .getAbsenceSituation(AbsenceImportType.PERMESSI_LEGGE);
    //      
    //    } catch (Exception e) {
    //      log.info("Impossibile scaricare l'informazione da attestati di {}", person.fullName());
    //    }
    return situation;

  }

  /**
   * Elimina i periodi ferie in cache per quella persona a partire dalla data from.
   *
   * @param person persona
   * @param from from
   */
  public void emptyVacationCache(Person person, LocalDate from) {
    for (Contract contract : person.contracts) {
      if (DateUtility.isDateIntoInterval(from, contract.periodInterval())) {
        emptyVacationCache(contract);
      }
    }
  }

  /**
   * Elimina i riepiloghi ferie in cache per quel contratto.
   *
   * @param contract cotratto
   */
  public void emptyVacationCache(Contract contract) {
    //per ogni anno fino a quello successivo l'attuale
    int year = contract.beginDate.getYear();
    if (contract.sourceDateVacation != null) {
      year = contract.sourceDateVacation.getYear() - 1;
    }
    while (true) {
      Cache.set(vacationCacheKey(contract, year, TypeSummary.VACATION), null);
      Cache.set(vacationCacheKey(contract, year, TypeSummary.PERMISSION), null);
      year++;
      if (year > LocalDate.now().getYear() + 1) {
        return;
      }
    }
  }

  private String vacationCacheKey(Contract contract, int year, TypeSummary type) {
    return contract.id + "-" + year + "-" + type.name();
  }

  /**
   * La data per cui fornire il residuo. Se non l'ho fornita ritorno un default.
   */
  private LocalDate vacationResidualDate(Contract contract,
      Optional<LocalDate> residualDate, int year) {
    if (!residualDate.isPresent()) {
      LocalDate date = LocalDate.now();
      if (date.getYear() > year) {
        date = new LocalDate(year, 12, 31);
      }
      if (contract.calculatedEnd() != null
          && contract.calculatedEnd().getYear() == year
          && !DateUtility.isDateIntoInterval(date, contract.periodInterval())) {
        date = contract.calculatedEnd();
      }
      return date;
    } else {
      //La data che passo deve essere una data contenuta nell'anno.
      if (residualDate.get().getYear() != year) {
        log.info("VacationSummary: anno={} data={}: la data deve appartenere all'anno.");
        return null;
      }
      return residualDate.get();
    }
  }

  private LocalDate vacationsLastYearExpireDate(int year, Office office) {

    MonthDay dayMonthExpiryVacationPastYear = (MonthDay) configurationManager
        .configValue(office, EpasParam.EXPIRY_VACATION_PAST_YEAR, year);

    LocalDate expireDate = LocalDate.now()
        .withYear(year)
        .withMonthOfYear(dayMonthExpiryVacationPastYear.getMonthOfYear())
        .withDayOfMonth(dayMonthExpiryVacationPastYear.getDayOfMonth());

    return expireDate;
  }

  /**
   * Se sono scadute le ferie per l'anno passato.
   *
   * @param year anno
   * @param office data scadenza
   * @return esito
   */
  public boolean isVacationsLastYearExpired(int year, Office office) {
    LocalDate today = LocalDate.now();

    LocalDate expireDate = vacationsLastYearExpireDate(year, office);
    if (year < today.getYear()) {        //query anni passati
      return true;
    } else if (year == today.getYear() && today.isAfter(expireDate)) {    //query anno attuale
      return true;
    }
    return false;
  }
}
