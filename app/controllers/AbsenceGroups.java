package controllers;

import dao.PersonDao;
import dao.absences.AbsenceComponentDao;

import manager.ConsistencyManager;
import manager.PersonDayManager;
import manager.services.absences.AbsenceMigration;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.AbsenceRequestType;
import manager.services.absences.AbsencesReport;
import manager.services.absences.InsertResultItem;
import manager.services.absences.InsertResultItem.Operation;
import manager.services.absences.model.AbsenceEngine;
import manager.services.absences.web.AbsenceRequestForm;
import manager.services.absences.web.AbsenceRequestForm.SubAbsenceGroupFormItem;

import models.Person;
import models.PersonDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;

import java.util.List;

import javax.inject.Inject;

//@Slf4j
@With({Resecure.class, RequestInit.class})
public class AbsenceGroups extends Controller {
  
  @Inject
  private static PersonDao personDao;
  @Inject
  private static PersonDayManager personDayManager;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static AbsenceMigration absenceMigration;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  
  public static void migrate() {
    
    absenceMigration.buildDefaultGroups();
    renderText("ok");
    
  }
  
  public static void show() {
    
    List<GroupAbsenceType> groups = GroupAbsenceType.findAll();
    render(groups);
    
  }
  
  public static void insert(Long personId, LocalDate from, LocalDate to, 
      GroupAbsenceType groupAbsenceType) {
    
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
   
    //Fix entity not null
    if (!groupAbsenceType.isPersistent()) {
      groupAbsenceType = GroupAbsenceType.find("byName", "MISSIONE").first();
    } 
      
        
    AbsenceRequestForm absenceRequestForm = absenceService
        .buildInsertForm(person, from, to, groupAbsenceType);
    
    AbsencesReport report = null;
    if (absenceRequestForm.selectedAbsenceGroupFormItem != null) {
      SubAbsenceGroupFormItem selected = absenceRequestForm
          .selectedAbsenceGroupFormItem.selectedSubAbsenceGroupFormItems;
      
      report = absenceService.insert(person, groupAbsenceType, from, to, 
          AbsenceRequestType.insert, selected.absenceType, 
          selected.selectedJustified, selected.getHours(), selected.getMinutes());  
    }
    
    render(absenceRequestForm, report);
  }
  
  public static void configureInsert(Long personId, LocalDate from, LocalDate to, 
      GroupAbsenceType groupAbsenceType, AbsenceType absenceType, 
      JustifiedType justifiedType, Integer hours, Integer minutes) {
  
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
    notFoundIfNull(groupAbsenceType);
    notFoundIfNull(justifiedType);
    
    if (!absenceType.isPersistent()) {
      absenceType = null;
    }
    
    AbsenceRequestForm absenceRequestForm = absenceService.configureInsertForm(person, from, to,
        groupAbsenceType, absenceType, justifiedType, hours, minutes);
    
    AbsencesReport report = null;
    if (absenceRequestForm.selectedAbsenceGroupFormItem != null) {
      SubAbsenceGroupFormItem selected = absenceRequestForm
          .selectedAbsenceGroupFormItem.selectedSubAbsenceGroupFormItems;
    
      
      report = absenceService.insert(person, groupAbsenceType, from, to, 
          AbsenceRequestType.insert, selected.absenceType, 
          selected.selectedJustified, selected.getHours(), selected.getMinutes());  
    }
    
    render("@insert", absenceRequestForm, report);
  }
  
  
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
    
    AbsenceRequestForm absenceRequestForm = absenceService.configureInsertForm(person, from, to,
        groupAbsenceType, absenceType, justifiedType, hours, minutes);
    
    AbsencesReport report;
    if (forceInsert) {
      report = absenceService.forceInsert(person, groupAbsenceType, from, to, 
          AbsenceRequestType.insert, absenceType, justifiedType, hours, minutes);
    } else {
      report = absenceService.insert(person, groupAbsenceType, from, to, 
          AbsenceRequestType.insert, absenceType, justifiedType, hours, minutes);
      //Se ci sono degli errori non salvo niente
      if (report.containsProblems()) {
        render("@insert", absenceRequestForm, report);
      }
    }

    for (InsertResultItem absenceResultItem : report.insertResultItems) {
      if (absenceResultItem.getOperation().equals(Operation.insert) 
          || absenceResultItem.getOperation().equals(Operation.insertReplacing)) {
        
        PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(person, 
            absenceResultItem.getAbsence().getAbsenceDate());
        Absence absence = absenceResultItem.getAbsence();
        personDay.absences.add(absence);
        absence.personDay = personDay;
        absence.save(); 
        personDay.save();
        JPA.em().flush();
      }
    }
    
    consistencyManager.updatePersonSituation(person.id, from);
    
    flash.success("Codici di assenza inseriti.");
    Stampings.personStamping(person.id, from.getYear(), from.getMonthOfYear());
    
    
  }
  
  public static void groupStatus(Person person, GroupAbsenceType groupAbsenceType, LocalDate date) {
    
    notFoundIfNull(person);
    notFoundIfNull(date);
    notFoundIfNull(groupAbsenceType);
    
    AbsenceEngine absenceEngine = absenceService.residual(person, groupAbsenceType, date);
    
    render(absenceEngine);
  }
  
  public static void scan(Long personId, LocalDate from) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
    
    absenceService.scanner(person, from);
    renderText("ok");
  }
  
  public static void initialization(Long personId, GroupAbsenceType groupAbsenceType, LocalDate date) {

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
  
}
