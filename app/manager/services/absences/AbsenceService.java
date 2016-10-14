package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import lombok.extern.slf4j.Slf4j;

import manager.AbsenceManager;
import manager.response.AbsenceInsertReport;
import manager.response.AbsencesResponse;
import manager.services.absences.AbsenceService.InsertReport;
import manager.services.absences.errors.AbsenceError;
import manager.services.absences.errors.CriticalError;
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.DayInPeriod;
import manager.services.absences.model.DayInPeriod.TemplateRow;
import manager.services.absences.model.PeriodChain;
import manager.services.absences.model.Scanner;
import manager.services.absences.model.ServiceFactories;
import manager.services.absences.web.AbsenceRequestForm;
import manager.services.absences.web.AbsenceRequestForm.AbsenceInsertTab;
import manager.services.absences.web.AbsenceRequestForm.AbsenceRequestCategory;
import manager.services.absences.web.AbsenceRequestFormFactory;

import models.Contract;
import models.Person;
import models.PersonChildren;
import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.SortedMap;

/**
 * Interfaccia epas per il componente assenze.
 * le richieste via form.
 * @author alessandro
 *
 */
@Slf4j
public class AbsenceService {

  private final AbsenceRequestFormFactory absenceRequestFormFactory;
  private final AbsenceEngineUtility absenceEngineUtility;
  private final AbsenceComponentDao absenceComponentDao;
  private final PersonChildrenDao personChildrenDao;
  private final ServiceFactories serviceFactories;
  
  @Inject
  public AbsenceService(AbsenceRequestFormFactory absenceRequestFormFactory, 
      AbsenceEngineUtility absenceEngineUtility,
      ServiceFactories serviceFactories,
      AbsenceComponentDao absenceComponentDao,
      PersonChildrenDao personChildrenDao) {
    this.absenceRequestFormFactory = absenceRequestFormFactory;
    this.absenceEngineUtility = absenceEngineUtility;
    this.serviceFactories = serviceFactories;
    this.absenceComponentDao = absenceComponentDao;
    this.personChildrenDao = personChildrenDao;
  }
  
  public List<AbsenceRequestCategory> orderedCategories(Person person, LocalDate date, 
      GroupAbsenceType groupAbsenceType) {
    
    if (groupAbsenceType == null || !groupAbsenceType.isPersistent()) {
      groupAbsenceType = absenceComponentDao
          .groupAbsenceTypeByName(AbsenceInsertTab.defaultTab().groupNames.get(0)).get();
    }
    
    AbsenceRequestForm form = buildInsertForm(person, date, null, null, 
        groupAbsenceType, true, null, null, null, null);
    List<AbsenceRequestCategory> categories = form.orderedCategories();
    
    return categories;
  }
  
  /**
   * Genera la form di inserimento assenza.
   * @return
   */
  public AbsenceRequestForm buildInsertForm(Person person, LocalDate from, 
      AbsenceInsertTab absenceInsertTab,                                                      //tab 
      LocalDate to, GroupAbsenceType groupAbsenceType,  boolean switchGroup,                  //group
      AbsenceType absenceType, JustifiedType justifiedType, Integer hours, Integer minutes) { //reconf
    
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
    
    if (groupAbsenceType == null) {
      if (absenceInsertTab == null) {
        absenceInsertTab = AbsenceInsertTab.defaultTab();
      } 
      groupAbsenceType = absenceComponentDao
          .groupAbsenceTypeByName(absenceInsertTab.groupNames.get(0)).get();
      to = null;
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
    
    return absenceRequestFormFactory.buildAbsenceRequestForm(person, from, to, 
        groupAbsenceType, null, null, null);
  }
  
  /**
   * Esegue la richiesta
   * @param person
   * @param groupAbsenceType
   * @param from
   * @param to
   * @param insert
   * @param absenceTypeRequest
   * @param justifiedType
   * @param specifiedMinutes
   * @return
   */
  public InsertReport insert(Person person, GroupAbsenceType groupAbsenceType, 
      LocalDate from, LocalDate to, 
      AbsenceType absenceType, JustifiedType justifiedType, 
      Integer hours, Integer minutes, AbsenceManager absenceManager) {
    
    if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.vacationsCnr)) {
      InsertReport insertReport = temporaryInsertVacation(person, 
          groupAbsenceType, from, to, null, absenceManager);
      return insertReport;
    } else if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.compensatoryRestCnr)) {
      InsertReport insertReport = temporaryInsertCompensatoryRest(person, 
          groupAbsenceType, from, to, null, absenceManager);
      return insertReport;
    } 
    
    Integer specifiedMinutes = absenceEngineUtility.getMinutes(hours, minutes);
    LocalDate currentDate = from;
    
    List<PeriodChain> chains = Lists.newArrayList();
    List<Absence> previousInserts = Lists.newArrayList();
    List<CriticalError> criticalErrors = Lists.newArrayList();
    
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
      
      PeriodChain periodChain = serviceFactories
          .buildPeriodChain(person, groupAbsenceType, currentDate, 
              previousInserts, absenceToInsert, orderedChildren, fetchedContracts);

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
    
    InsertReport insertReport = new InsertReport();
    
    //Se una catena contiene errori esco
    if (!criticalErrors.isEmpty()) {
      insertReport.criticalErrors = criticalErrors;
      return insertReport;
    }

    //Gli esiti sotto forma di template rows
    List<TemplateRow> insertTemplateRows = Lists.newArrayList();
    for (PeriodChain periodChain : chains) {
      if (periodChain.childIsMissing()) {
        TemplateRow templateRow = new TemplateRow();
        templateRow.date = periodChain.date;
        templateRow.absenceErrors.add(AbsenceError.builder()
            .absenceProblem(AbsenceProblem.NoChildExist).build());
        insertTemplateRows.add(templateRow);
      }
      for (AbsencePeriod absencePeriod : periodChain.periods) {
        for (DayInPeriod dayInPeriod : absencePeriod.daysInPeriod.values()) {
          insertTemplateRows.addAll(dayInPeriod.templateRowsForInsert());
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
   * @param person
   * @param from
   */
  public Scanner scanner(Person person, LocalDate from) {
    
    log.debug("");
    log.debug("Lanciata procedura scan assenze person={}, from={}", person.fullName(), from);

    List<Absence> absencesToScan = absenceComponentDao.orderedAbsences(person, from, 
        null, Lists.newArrayList());
    List<PersonChildren> orderedChildren = personChildrenDao.getAllPersonChildren(person);    
    List<Contract> fetchedContracts = person.contracts; //TODO: fetch
    
    Scanner absenceScan = serviceFactories.buildScanInstance(person, from, absencesToScan, 
        orderedChildren, fetchedContracts);
        
    // scan dei gruppi
    absenceScan.scan();

    log.debug("");

    return absenceScan;

  }

  public PeriodChain residual(Person person, GroupAbsenceType groupAbsenceType, LocalDate date) {

    if (date == null) {
      date = LocalDate.now();
    }
    
    List<PersonChildren> orderedChildren = personChildrenDao.getAllPersonChildren(person);   
    List<Contract> fetchedContracts = person.contracts; //TODO: fetch
    
    PeriodChain periodChain = serviceFactories.buildPeriodChain(person, groupAbsenceType, date, 
        Lists.newArrayList(), null,
        orderedChildren, fetchedContracts);

    return periodChain;

  }
  
  public SortedMap<Integer, List<AbsenceRequestCategory>> formCategories(Person person, LocalDate date, 
      GroupAbsenceType groupAbsenceType) {
    return absenceRequestFormFactory
        .buildAbsenceRequestForm(person, date, date, groupAbsenceType, null, null, null)
        .categoriesWithSamePriority;
  }
  
  @Deprecated
  public InsertReport temporaryInsertCompensatoryRest(Person person, 
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
  public InsertReport temporaryInsertVacation(Person person, GroupAbsenceType groupAbsenceType, 
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
    
    public int howManyReplacing() {
      int result = 0;
      for (TemplateRow templateRow : insertTemplateRows) {
        if (templateRow.isReplacingRow) {
          result++;
        }
      }
      return result;
    }
    
    public int howManyIgnored() {
      int result = 0;
      for (TemplateRow templateRow : insertTemplateRows) {
        if (templateRow.onlyNotOnHoliday()) {
          result++;
        }
      }
      return result;
    }
    
    public int howManyError() {
      int result = 0;
      for (TemplateRow templateRow : insertTemplateRows) {
        if (!templateRow.absenceErrors.isEmpty() && !templateRow.onlyNotOnHoliday()) {
          result++;
        }
      }
      return result;
    }
    
    public int howManyWarning() {
      int result = 0;
      for (TemplateRow templateRow : insertTemplateRows) {
        if (!templateRow.absenceWarnings.isEmpty()) {
          result++;
        }
      }
      return result;
    }

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
