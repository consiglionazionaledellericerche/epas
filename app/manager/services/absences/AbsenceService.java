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

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import manager.AbsenceManager;
import manager.attestati.dto.show.CodiceAssenza;
import manager.attestati.service.CertificationService;
import manager.response.AbsenceInsertReport;
import manager.response.AbsencesResponse;
import manager.services.absences.certifications.CodeComparation;
import manager.services.absences.enums.CategoryEnum;
import manager.services.absences.enums.GroupEnum;
import manager.services.absences.errors.AbsenceError;
import manager.services.absences.errors.CriticalError;
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.DayInPeriod;
import manager.services.absences.model.DayInPeriod.TemplateRow;
import manager.services.absences.model.PeriodChain;
import manager.services.absences.model.Scanner;
import manager.services.absences.model.ServiceFactories;

import models.Contract;
import models.Person;
import models.PersonChildren;
import models.Role;
import models.User;
import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.CategoryTab;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.DefaultGroup;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.InitializationGroup;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;

import org.joda.time.LocalDate;

/**
 * Interfaccia epas per il componente assenze.
 * le richieste via form.
 * @author alessandro
 *
 */
@Slf4j
public class AbsenceService {

  private final AbsenceEngineUtility absenceEngineUtility;
  private final AbsenceComponentDao absenceComponentDao;
  private final PersonChildrenDao personChildrenDao;
  private final ServiceFactories serviceFactories;
  private final CertificationService certificationService;
  
  /**
   * Costruttore injection.
   * @param absenceEngineUtility injected
   * @param serviceFactories injected
   * @param absenceComponentDao injected
   * @param personChildrenDao injected
   */
  @Inject
  public AbsenceService(
      AbsenceEngineUtility absenceEngineUtility,
      ServiceFactories serviceFactories,
      AbsenceComponentDao absenceComponentDao,
      PersonChildrenDao personChildrenDao, CertificationService certificationService) {
    this.absenceEngineUtility = absenceEngineUtility;
    this.serviceFactories = serviceFactories;
    this.absenceComponentDao = absenceComponentDao;
    this.personChildrenDao = personChildrenDao;
    this.certificationService = certificationService;
    
  }
  
  /**
   * La absenceForm utile alla lista delle categorie abilitate per la persona alla data, 
   * ordinate per priorità.
   * Se groupAbsenceType è presente imposta il gruppo come selezionato.
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
    
    AbsenceForm form = buildAbsenceForm(person, date, null, null, 
        groupAbsenceType, true, null, null, null, null, true);
    
    return form;
  }

  
  /**
   * Genera la form di inserimento assenza.
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
      LocalDate to, GroupAbsenceType groupAbsenceType,  boolean switchGroup,             //group
      AbsenceType absenceType, JustifiedType justifiedType,                              //reconf 
      Integer hours, Integer minutes, boolean readOnly) {
    
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
    
    List<GroupAbsenceType> groupsPermitted = groupsPermitted(person, readOnly);
    
    if (groupAbsenceType != null) {
      Verify.verify(groupsPermitted.contains(groupAbsenceType));
      categoryTab = groupAbsenceType.category.tab;
    } else {
      if (categoryTab != null) {
        groupAbsenceType = categoryTab.firstByPriority()
            .groupAbsenceTypes.iterator().next();
        Verify.verify(groupsPermitted.contains(groupAbsenceType));
      } else {
        //selezionare missione?
        for (GroupAbsenceType group : groupsPermitted) {
          if (group.name.equals(DefaultGroup.MISSIONE.name())) {
            groupAbsenceType = group;
            break;
          }
        }
        if (groupAbsenceType == null) {
          groupAbsenceType = groupsPermitted.get(0);  
        }
        categoryTab = absenceComponentDao.categoriesByPriority().iterator().next().tab;  
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
    
    return new AbsenceForm(person, from, to, groupAbsenceType, absenceType, 
        justifiedType, hours, minutes, groupsPermitted,
        absenceComponentDao, absenceEngineUtility);
  }
  
  /**
   * Effettua la simuzione dell'inserimento. Genera il report di inserimento.
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
      return forceInsert(person, groupAbsenceType, from, to, 
          absenceType, justifiedType, hours, minutes);
    }
    
    if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.vacationsCnr)) {
      InsertReport insertReport = temporaryInsertVacation(person, 
          groupAbsenceType, from, to, absenceType, absenceManager);
      return insertReport;
    } else if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.compensatoryRestCnr)) {
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
  
  
  private InsertReport forceInsert(Person person, GroupAbsenceType groupAbsenceType, 
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
   * Esegue lo scanning delle assenze della persona a partire dalla data
   * passata come parametro per verificarne la correttezza.
   * Gli errori riscontrati vengono persistiti all'assenza.
   * 
   * <p>
   * Microservices
   * Questo metodo dovrebbe avere una person dove sono fetchate tutte le 
   * informazioni per i calcoli non mantenute del componente assenze:
   * </p>
   * I Contratti / Tempi a lavoro / Piani ferie
   * I Figli
   * Le Altre Tutele
   * 
   * @param person persona 
   * @param from data inizio
   */
  public Scanner scanner(Person person, LocalDate from) {
    
    log.debug("");
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
   * I gruppi su cui l'utente collegato ha i diritti per la persona passata. 
   * A seconda che la richista avvenga in lettura.
   * o in scrittura.
   * @param person persona
   * @param readOnly sola lettura
   * @return list
   */
  public List<GroupAbsenceType> groupsPermitted(Person person, boolean readOnly) {
    List<GroupAbsenceType> groupsPermitted = absenceComponentDao.allGroupAbsenceType();
    if (readOnly) {
      return groupsPermitted;
    }
    final User currentUser = Security.getUser().get();
    final GroupAbsenceType employee = absenceComponentDao
        .groupAbsenceTypeByName(DefaultGroup.EMPLOYEE.name()).get();
    if (!currentUser.isSystemUser()) {
      if (currentUser.person.id.equals(person.id)
          && !currentUser.hasRoles(Role.PERSONNEL_ADMIN)) {
        //verificare... perchè non controllo has personnel admin role on person.office?
        groupsPermitted = absenceComponentDao.groupsAbsenceTypeByName(
            Lists.newArrayList(DefaultGroup.EMPLOYEE.name()));
      }
    } else {
      groupsPermitted.remove(employee);
    }
    return groupsPermitted;
  }
  
  @Deprecated
  private InsertReport temporaryInsertCompensatoryRest(Person person, 
      GroupAbsenceType groupAbsenceType, LocalDate from, LocalDate to, AbsenceType absenceType, 
      AbsenceManager absenceManager) {
    
    if (absenceType == null || !absenceType.isPersistent()) {
      absenceType = absenceComponentDao.absenceTypeByCode("91").get();
    }

    return insertReportFromOldReport(
        absenceManager.insertAbsenceSimulation(person, from, Optional.fromNullable(to), 
            absenceType, Optional.absent(), Optional.absent(), Optional.absent()), 
        groupAbsenceType);
    
  }
  
  @Deprecated
  private InsertReport temporaryInsertVacation(Person person, GroupAbsenceType groupAbsenceType, 
      LocalDate from, LocalDate to, AbsenceType absenceType, 
      AbsenceManager absenceManager) {

    if (absenceType == null || !absenceType.isPersistent()) {
      absenceType = absenceComponentDao.absenceTypeByCode("FER").get();
    }

    return insertReportFromOldReport(
        absenceManager.insertAbsenceSimulation(person, from, Optional.fromNullable(to), 
            absenceType, Optional.absent(), Optional.absent(), Optional.absent()), 
        groupAbsenceType);

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
   * Calcola la comparazione con i codici in attestati.
   */
  public CodeComparation computeCodeComparation() {
    

    CodeComparation codeComparation = new CodeComparation();

    try {
      //Codici di assenza in attestati
      Map<String, CodiceAssenza> attestatiAbsenceCodes = certificationService.absenceCodes();
      if (attestatiAbsenceCodes.isEmpty()) {
        log.info("Impossibile accedere ai codici in attestati");
        return null;
      }
      //Tasformazione in superCodes
      for (CodiceAssenza codiceAssenza : attestatiAbsenceCodes.values()) {
        codeComparation.putCodiceAssenza(codiceAssenza);
      }
    } catch (Exception ex) {
      return null;
    }

    //Codici di assenza epas
    List<AbsenceType> absenceTypes = AbsenceType.findAll();
    //Tasformazione in superCodes
    for (AbsenceType absenceType : absenceTypes) {
      codeComparation.putAbsenceType(absenceType);
    }
    
    //Tutte le assenze epas
    List<Absence> absences = Absence.findAll();
    //Inserimento in superCodes
    for (Absence absence : absences) {
      codeComparation.putAbsence(absence);
    }
   
    
    codeComparation.setOnlyAttestati();
    codeComparation.setOnlyEpas();
    codeComparation.setBoth();
    
    return codeComparation;
  }
  
  /**
   * Completa i post partum coi gruppi 24*.
   * Metodo utile quando e se occorrerà creare i congedi quarto figlio.
   */
  public void fixPostPartumGroups() {
    prepare24();
    prepare242();
    prepare243();
    
    log.info("Controllo gruppi congedi parentali completato.");
  }
  
  /**
   * Prepara il gruppo 24.
   */
  private void prepare24() {

    //Code 24
    AbsenceType code24 = new AbsenceType();
    code24.code = "24";
    if (absenceComponentDao.absenceTypeByCode("24").isPresent()) {
      code24 = absenceComponentDao.absenceTypeByCode("24").get();
    }
    code24.description = 
        "Astensione facoltativa post partum non retrib. primo figlio intera giornata";
    code24.certificateCode = "24";
    code24.internalUse = false;
    code24.justifiedTypesPermitted.clear();
    code24.justifiedTypesPermitted.add(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day));
    code24.justifiedTime = 0;
    code24.replacingTime = 0;
    code24.timeForMealTicket = false;
    code24.save();

    //
    AbsenceType code24M = new AbsenceType();
    code24M.code = "24M";
    if (absenceComponentDao.absenceTypeByCode("24M").isPresent()) {
      code24M = absenceComponentDao.absenceTypeByCode("24M").get();
    }
    code24M.description = 
        "Astensione facoltativa post partum non retrib. primo figlio in ore e minuti";
    code24M.certificateCode = "24M";
    code24M.internalUse = true;
    code24M.justifiedTypesPermitted.clear();
    code24M.justifiedTypesPermitted.add(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes));
    code24M.justifiedTime = 0;
    code24M.replacingTime = 0;
    code24M.timeForMealTicket = false;
    code24M.save();

    //
    AbsenceType code24U = new AbsenceType();
    code24U.code = "24U";
    if (absenceComponentDao.absenceTypeByCode("24U").isPresent()) {
      code24U = absenceComponentDao.absenceTypeByCode("24U").get();
    }
    code24U.description = 
      "Astensione facoltativa post partum non retrib. primo figlio intera giornata altro genitore";
    code24U.certificateCode = "24U";
    code24U.internalUse = true;
    code24U.justifiedTypesPermitted.clear();
    code24U.justifiedTypesPermitted.add(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day_limit));
    code24U.justifiedTime = 0;
    code24U.replacingTime = 0;
    code24U.timeForMealTicket = false;
    code24U.save();

    //
    AbsenceType code24H7 = new AbsenceType();
    code24H7.code = "24H7";
    if (absenceComponentDao.absenceTypeByCode("24H7").isPresent()) {
      code24H7 = absenceComponentDao.absenceTypeByCode("24H7").get();
    }
    code24H7.description = 
        "Astensione facoltativa post partum non retrib. primo figlio completamento giornata";
    code24H7.certificateCode = "24H7";
    code24H7.internalUse = false;
    code24H7.justifiedTypesPermitted.clear();
    code24H7.justifiedTypesPermitted.add(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.nothing));
    code24H7.justifiedTime = 0;
    code24H7.replacingType = absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.all_day);
    code24H7.replacingTime = 0;
    code24H7.timeForMealTicket = false;
    code24H7.save();


    //Group 24

    GroupAbsenceType group24 = new GroupAbsenceType();
    group24.name = "G_24";
    group24.takableAbsenceBehaviour = new TakableAbsenceBehaviour();
    group24.takableAbsenceBehaviour.name = "T_24";
    group24.complationAbsenceBehaviour = new ComplationAbsenceBehaviour();
    group24.complationAbsenceBehaviour.name = "C_24";

    //but if it already exists take it
    if (absenceComponentDao.groupAbsenceTypeByName(GroupEnum.G_24.name()).isPresent()) {
      group24 = absenceComponentDao.groupAbsenceTypeByName(GroupEnum.G_24.name()).get();
    }

    group24.description = 
        "24 - Astensione facoltativa post partum non retrib. primo figlio 0-12 anni 600 giorni";
    group24.category = absenceComponentDao.categoryByName(CategoryEnum.CONGEDI_PARENTALI.name());
    group24.pattern = GroupAbsenceTypePattern.programmed;
    group24.periodType = PeriodType.child1_0_12;
    group24.automatic = false;
    group24.initializable = true;

    group24.takableAbsenceBehaviour.amountType = AmountType.units;
    group24.takableAbsenceBehaviour.fixedLimit = 600;
    group24.takableAbsenceBehaviour.takableCodes.clear();
    group24.takableAbsenceBehaviour.takableCodes.add(code24);
    group24.takableAbsenceBehaviour.takableCodes.add(code24M);
    group24.takableAbsenceBehaviour.takableCodes.add(code24U);
    group24.takableAbsenceBehaviour.takenCodes.clear();
    group24.takableAbsenceBehaviour.takenCodes.add(code24);
    group24.takableAbsenceBehaviour.takenCodes.add(code24M);
    group24.takableAbsenceBehaviour.takenCodes.add(code24U);

    group24.complationAbsenceBehaviour.amountType = AmountType.units;
    group24.complationAbsenceBehaviour.complationCodes.clear();
    group24.complationAbsenceBehaviour.complationCodes.add(code24M);
    group24.complationAbsenceBehaviour.replacingCodes.clear();
    group24.complationAbsenceBehaviour.replacingCodes.add(code24H7);


    group24.takableAbsenceBehaviour.save();
    group24.complationAbsenceBehaviour.save();
    group24.save();


    GroupAbsenceType group25 = absenceComponentDao
        .groupAbsenceTypeByName(GroupEnum.G_25.name()).get();
    group25.nextGroupToCheck = group24;
    group25.save();

    //definire le etichette dei gruppi e della catena
    GroupAbsenceType group23 = absenceComponentDao
        .groupAbsenceTypeByName(GroupEnum.G_23.name()).get();
    group23.description = 
        "23 - Astensione facoltativa post partum 100% primo figlio 0-12 anni 30 giorni"; 
    group23.chainDescription = "23/25/24 - Astensione facoltativa post partum primo figlio";
    group23.initializable = true;
    group23.save();

    group25.description = 
        "25 - Astensione facoltativa post partum 30% primo figlio 0-6 anni 150 giorni";
    group25.chainDescription = "";
    group25.initializable = true;
    group25.save();

  }

  /**
   * Prepara il gruppo 242.
   */
  private void prepare242() {

    //Code 242
    AbsenceType code242 = new AbsenceType();
    code242.code = "242";
    if (absenceComponentDao.absenceTypeByCode("242").isPresent()) {
      code242 = absenceComponentDao.absenceTypeByCode("242").get();
    }
    code242.description = 
        "Astensione facoltativa post partum non retrib. secondo figlio intera giornata";
    code242.certificateCode = "242";
    code242.internalUse = false;
    code242.justifiedTypesPermitted.clear();
    code242.justifiedTypesPermitted.add(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day));
    code242.justifiedTime = 0;
    code242.replacingTime = 0;
    code242.timeForMealTicket = false;
    code242.save();

    //
    AbsenceType code242M = new AbsenceType();
    code242M.code = "242M";
    if (absenceComponentDao.absenceTypeByCode("242M").isPresent()) {
      code242M = absenceComponentDao.absenceTypeByCode("242M").get();
    }
    code242M.description = 
        "Astensione facoltativa post partum non retrib. secondo figlio in ore e minuti";
    code242M.certificateCode = "242M";
    code242M.internalUse = true;
    code242M.justifiedTypesPermitted.clear();
    code242M.justifiedTypesPermitted.add(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes));
    code242M.justifiedTime = 0;
    code242M.replacingTime = 0;
    code242M.timeForMealTicket = false;
    code242M.save();

    //
    AbsenceType code242U = new AbsenceType();
    code242U.code = "242U";
    if (absenceComponentDao.absenceTypeByCode("242U").isPresent()) {
      code242U = absenceComponentDao.absenceTypeByCode("242U").get();
    }
    code242U.description = 
        "Astensione facoltativa post partum non retrib. secondo figlio intera giornata "
        + "altro genitore";
    code242U.certificateCode = "242U";
    code242U.internalUse = true;
    code242U.justifiedTypesPermitted.clear();
    code242U.justifiedTypesPermitted.add(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day_limit));
    code242U.justifiedTime = 0;
    code242U.replacingTime = 0;
    code242U.timeForMealTicket = false;
    code242U.save();

    //
    AbsenceType code242H7 = new AbsenceType();
    code242H7.code = "242H7";
    if (absenceComponentDao.absenceTypeByCode("242H7").isPresent()) {
      code242H7 = absenceComponentDao.absenceTypeByCode("242H7").get();
    }
    code242H7.description = 
        "Astensione facoltativa post partum non retrib. secondo figlio completamento giornata";
    code242H7.certificateCode = "242H7";
    code242H7.internalUse = false;
    code242H7.justifiedTypesPermitted.clear();
    code242H7.justifiedTypesPermitted.add(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.nothing));
    code242H7.justifiedTime = 0;
    code242H7.replacingType = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day);
    code242H7.replacingTime = 0;
    code242H7.timeForMealTicket = false;
    code242H7.save();


    //Group 242

    GroupAbsenceType group242 = new GroupAbsenceType();
    group242.name = "G_242";
    group242.takableAbsenceBehaviour = new TakableAbsenceBehaviour();
    group242.takableAbsenceBehaviour.name = "T_242";
    group242.complationAbsenceBehaviour = new ComplationAbsenceBehaviour();
    group242.complationAbsenceBehaviour.name = "C_242";

    //but if it already exists take it
    if (absenceComponentDao.groupAbsenceTypeByName(GroupEnum.G_242.name()).isPresent()) {
      group242 = absenceComponentDao.groupAbsenceTypeByName(GroupEnum.G_242.name()).get();
    }

    group242.description = 
        "242 - Astensione facoltativa post partum non retrib. secondo figlio 0-12 anni 600 giorni";
    group242.category = absenceComponentDao.categoryByName(CategoryEnum.CONGEDI_PARENTALI.name());
    group242.pattern = GroupAbsenceTypePattern.programmed;
    group242.periodType = PeriodType.child2_0_12;
    group242.automatic = false;
    group242.initializable = true;

    group242.takableAbsenceBehaviour.amountType = AmountType.units;
    group242.takableAbsenceBehaviour.fixedLimit = 600;
    group242.takableAbsenceBehaviour.takableCodes.clear();
    group242.takableAbsenceBehaviour.takableCodes.add(code242);
    group242.takableAbsenceBehaviour.takableCodes.add(code242M);
    group242.takableAbsenceBehaviour.takableCodes.add(code242U);
    group242.takableAbsenceBehaviour.takenCodes.clear();
    group242.takableAbsenceBehaviour.takenCodes.add(code242);
    group242.takableAbsenceBehaviour.takenCodes.add(code242M);
    group242.takableAbsenceBehaviour.takenCodes.add(code242U);

    group242.complationAbsenceBehaviour.amountType = AmountType.units;
    group242.complationAbsenceBehaviour.complationCodes.clear();
    group242.complationAbsenceBehaviour.complationCodes.add(code242M);
    group242.complationAbsenceBehaviour.replacingCodes.clear();
    group242.complationAbsenceBehaviour.replacingCodes.add(code242H7);


    group242.takableAbsenceBehaviour.save();
    group242.complationAbsenceBehaviour.save();
    group242.save();


    GroupAbsenceType group252 = absenceComponentDao
        .groupAbsenceTypeByName(GroupEnum.G_252.name()).get();
    group252.nextGroupToCheck = group242;
    group252.save();

    //definire le etichette dei gruppi e della catena
    GroupAbsenceType group232 = absenceComponentDao
        .groupAbsenceTypeByName(GroupEnum.G_232.name()).get();
    group232.description = 
        "232 - Astensione facoltativa post partum 100% secondo figlio 0-12 anni 30 giorni"; 
    group232.chainDescription = "232/252/242 - Astensione facoltativa post partum secondo figlio";
    group232.initializable = true;
    group232.save();

    group252.description = 
        "252 - Astensione facoltativa post partum 30% secondo figlio 0-6 anni 150 giorni";
    group252.chainDescription = "";
    group252.initializable = true;
    group252.save();

  }

  /**
   * Prepara il gruppo 243.
   */
  private void prepare243() {

    //Code 243
    AbsenceType code243 = new AbsenceType();
    code243.code = "243";
    if (absenceComponentDao.absenceTypeByCode("243").isPresent()) {
      code243 = absenceComponentDao.absenceTypeByCode("243").get();
    }
    code243.description = 
        "Astensione facoltativa post partum non retrib. terzo figlio intera giornata";
    code243.certificateCode = "243";
    code243.internalUse = false;
    code243.justifiedTypesPermitted.clear();
    code243.justifiedTypesPermitted.add(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day));
    code243.justifiedTime = 0;
    code243.replacingTime = 0;
    code243.timeForMealTicket = false;
    code243.save();

    //
    AbsenceType code243M = new AbsenceType();
    code243M.code = "243M";
    if (absenceComponentDao.absenceTypeByCode("243M").isPresent()) {
      code243M = absenceComponentDao.absenceTypeByCode("243M").get();
    }
    code243M.description = 
        "Astensione facoltativa post partum non retrib. terzo figlio in ore e minuti";
    code243M.certificateCode = "243M";
    code243M.internalUse = true;
    code243M.justifiedTypesPermitted.clear();
    code243M.justifiedTypesPermitted.add(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.specified_minutes));
    code243M.justifiedTime = 0;
    code243M.replacingTime = 0;
    code243M.timeForMealTicket = false;
    code243M.save();

    //
    AbsenceType code243U = new AbsenceType();
    code243U.code = "243U";
    if (absenceComponentDao.absenceTypeByCode("243U").isPresent()) {
      code243U = absenceComponentDao.absenceTypeByCode("243U").get();
    }
    code243U.description = 
        "Astensione facoltativa post partum non retrib. terzo figlio intera giornata "
        + "altro genitore";
    code243U.certificateCode = "243U";
    code243U.internalUse = true;
    code243U.justifiedTypesPermitted.clear();
    code243U.justifiedTypesPermitted.add(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day_limit));
    code243U.justifiedTime = 0;
    code243U.replacingTime = 0;
    code243U.timeForMealTicket = false;
    code243U.save();

    //
    AbsenceType code243H7 = new AbsenceType();
    code243H7.code = "243H7";
    if (absenceComponentDao.absenceTypeByCode("243H7").isPresent()) {
      code243H7 = absenceComponentDao.absenceTypeByCode("243H7").get();
    }
    code243H7.description = 
        "Astensione facoltativa post partum non retrib. terzo figlio completamento giornata";
    code243H7.certificateCode = "243H7";
    code243H7.internalUse = false;
    code243H7.justifiedTypesPermitted.clear();
    code243H7.justifiedTypesPermitted.add(absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.nothing));
    code243H7.justifiedTime = 0;
    code243H7.replacingType = absenceComponentDao
        .getOrBuildJustifiedType(JustifiedTypeName.all_day);
    code243H7.replacingTime = 0;
    code243H7.timeForMealTicket = false;
    code243H7.save();


    //Group 243

    GroupAbsenceType group243 = new GroupAbsenceType();
    group243.name = "G_243";
    group243.takableAbsenceBehaviour = new TakableAbsenceBehaviour();
    group243.takableAbsenceBehaviour.name = "T_243";
    group243.complationAbsenceBehaviour = new ComplationAbsenceBehaviour();
    group243.complationAbsenceBehaviour.name = "C_243";

    //but if it already exists take it
    if (absenceComponentDao.groupAbsenceTypeByName(GroupEnum.G_243.name()).isPresent()) {
      group243 = absenceComponentDao.groupAbsenceTypeByName(GroupEnum.G_243.name()).get();
    }

    group243.description = 
        "243 - Astensione facoltativa post partum non retrib. terzo figlio 0-12 anni 600 giorni";
    group243.category = absenceComponentDao.categoryByName(CategoryEnum.CONGEDI_PARENTALI.name());
    group243.pattern = GroupAbsenceTypePattern.programmed;
    group243.periodType = PeriodType.child3_0_12;
    group243.automatic = false;
    group243.initializable = true;

    group243.takableAbsenceBehaviour.amountType = AmountType.units;
    group243.takableAbsenceBehaviour.fixedLimit = 600;
    group243.takableAbsenceBehaviour.takableCodes.clear();
    group243.takableAbsenceBehaviour.takableCodes.add(code243);
    group243.takableAbsenceBehaviour.takableCodes.add(code243M);
    group243.takableAbsenceBehaviour.takableCodes.add(code243U);
    group243.takableAbsenceBehaviour.takenCodes.clear();
    group243.takableAbsenceBehaviour.takenCodes.add(code243);
    group243.takableAbsenceBehaviour.takenCodes.add(code243M);
    group243.takableAbsenceBehaviour.takenCodes.add(code243U);

    group243.complationAbsenceBehaviour.amountType = AmountType.units;
    group243.complationAbsenceBehaviour.complationCodes.clear();
    group243.complationAbsenceBehaviour.complationCodes.add(code243M);
    group243.complationAbsenceBehaviour.replacingCodes.clear();
    group243.complationAbsenceBehaviour.replacingCodes.add(code243H7);


    group243.takableAbsenceBehaviour.save();
    group243.complationAbsenceBehaviour.save();
    group243.save();


    GroupAbsenceType group253 = absenceComponentDao
        .groupAbsenceTypeByName(GroupEnum.G_253.name()).get();
    group253.nextGroupToCheck = group243;
    group253.save();

    //definire le etichette dei gruppi e della catena
    GroupAbsenceType group233 = absenceComponentDao
        .groupAbsenceTypeByName(GroupEnum.G_233.name()).get();
    group233.description = 
        "233 - Astensione facoltativa post partum 100% terzo figlio 0-12 anni 30 giorni"; 
    group233.chainDescription = 
        "233/253/243 - Astensione facoltativa post partum terzo figlio";
    group233.initializable = true;
    group233.save();

    group253.description = 
        "253 - Astensione facoltativa post partum 30% terzo figlio 0-6 anni 150 giorni";
    group253.chainDescription = "";
    group253.initializable = true;
    group253.save();

  }

  
}
