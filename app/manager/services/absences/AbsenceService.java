package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Inject;

import controllers.Security;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import lombok.extern.slf4j.Slf4j;

import manager.AbsenceManager;
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

import models.Contract;
import models.Person;
import models.PersonChildren;
import models.Role;
import models.User;
import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;
import models.absences.CategoryTab;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.DefaultGroup;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.InitializationGroup;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;

import org.joda.time.LocalDate;

import java.util.List;

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
      PersonChildrenDao personChildrenDao) {
    this.absenceEngineUtility = absenceEngineUtility;
    this.serviceFactories = serviceFactories;
    this.absenceComponentDao = absenceComponentDao;
    this.personChildrenDao = personChildrenDao;
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
        null, Lists.newArrayList());
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
  
}
