package controllers;

import dao.PersonDao;
import dao.absences.AbsenceComponentDao;

import lombok.extern.slf4j.Slf4j;

import manager.AbsenceManager;
import manager.ConsistencyManager;
import manager.PersonDayManager;
import manager.services.absences.AbsenceMigration;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import manager.services.absences.model.PeriodChain;
import manager.services.absences.web.AbsenceRequestForm;
import manager.services.absences.web.AbsenceRequestForm.AbsenceInsertTab;
import manager.services.absences.web.AbsenceRequestForm.AbsenceRequestCategory;
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
  private static AbsenceMigration absenceMigration;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  @Inject
  private static AbsenceManager absenceManager;
  
  public static void migrate() {
    
    absenceMigration.buildDefaultGroups();
    renderText("ok");
    
  }
  
  public static void show() {
    
    List<GroupAbsenceType> groups = GroupAbsenceType.findAll();
    render(groups);
    
  }
  
  
  public static void insert(Long personId, LocalDate from, AbsenceInsertTab absenceInsertTab, //tab
      LocalDate to, GroupAbsenceType groupAbsenceType,  boolean switchGroup,                  //group
      AbsenceType absenceType, JustifiedType justifiedType, Integer hours, Integer minutes) { //confGroup

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
   
    AbsenceRequestForm absenceRequestForm = absenceService.buildInsertForm(person, from, absenceInsertTab, 
        to, groupAbsenceType, switchGroup, absenceType, justifiedType, hours, minutes);

    if (absenceRequestForm.selectedAbsenceGroupFormItem == null) {
      render(absenceRequestForm);
    }

    SubAbsenceGroupFormItem selected = absenceRequestForm
        .selectedAbsenceGroupFormItem.selectedSubAbsenceGroupFormItems;

    InsertReport insertReport  = absenceService.insert(person, 
        absenceRequestForm.selectedAbsenceGroupFormItem.groupAbsenceType, 
        absenceRequestForm.from, absenceRequestForm.to, 
        selected.absenceType, selected.selectedJustified, 
        selected.getHours(), selected.getMinutes(), absenceManager);
    render(absenceRequestForm, insertReport);

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
    
    if (forceInsert) {
      //report = absenceService.forceInsert(person, groupAbsenceType, from, to, 
      //    AbsenceRequestType.insert, absenceType, justifiedType, hours, minutes);
      flash.error("L'inserimento forzato è in fase di implementazione ...");
    } else {
      
      InsertReport insertReport = absenceService.insert(person, groupAbsenceType, from, to, 
            absenceType, justifiedType, hours, minutes, absenceManager);
      
      //Persistenza
      if (!insertReport.absencesToPersist.isEmpty()) {
        for (Absence absence : insertReport.absencesToPersist) {
          PersonDay personDay = personDayManager
              .getOrCreateAndPersistPersonDay(person, absence.getAbsenceDate());
          absence.personDay = personDay;
          personDay.absences.add(absence);
          absence.save(); 
          personDay.save();
        }
        if (!insertReport.reperibilityShiftDate().isEmpty()) {
          //absenceManager.sendReperibilityShiftEmail(person, insertReport.reperibilityShiftDate());
          log.info("Inserite assenze con reperibilità e turni {} {}. Le email sono disabilitate.", 
              person.fullName(), insertReport.reperibilityShiftDate() );
        }
        JPA.em().flush();
        consistencyManager.updatePersonSituation(person.id, from);
        flash.success("Codici di assenza inseriti.");
      }
    }

    Stampings.personStamping(person.id, from.getYear(), from.getMonthOfYear());

  }

  
//  /**
//   * metodo che cancella una certa assenza fino ad un certo periodo.
//   *
//   * @param absence l'assenza
//   * @param dateTo  la data di fine periodo
//   */
//  public static void delete(Absence absence, GroupAbsenceType groupAbsenceType) {
//
//    notFoundIfNull(absence);
//    Person person = absence.personDay.person;
//    LocalDate dateFrom = absence.personDay.date;
//
//    if (absence.absenceFile.exists()) {
//      absence.absenceFile.getFile().delete();
//    }
//    absence.personDay.absences.remove(absence);
//    absence.delete();
//    consistencyManager.updatePersonSituation(person.id, absence.getAbsenceDate());
//    
//    flash.success("Assenza rimossa correttamente.");
//    if (groupAbsenceType != null) {
//      groupStatus(person.id, groupAbsenceType.id, absence.getAbsenceDate());
//    } else {
//      Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());  
//    }
//  }
  
  public static void groupStatus(Person person, GroupAbsenceType groupAbsenceType, LocalDate date) {
    
    notFoundIfNull(person);
    notFoundIfNull(date);
    
    List<AbsenceRequestCategory> categories = absenceService
      .orderedCategories(person, date, groupAbsenceType);
    
    groupAbsenceType = absenceComponentDao.firstGroupOfChain(groupAbsenceType);
        
    PeriodChain periodChain = absenceService.residual(person, groupAbsenceType, date);
    
    render(date, categories, groupAbsenceType, periodChain);
  }
  
  public static void changeGroupStatus(Person person, GroupAbsenceType groupAbsenceType, LocalDate date) {

    notFoundIfNull(person);
    notFoundIfNull(date);
    notFoundIfNull(groupAbsenceType);
    
    groupAbsenceType = absenceComponentDao.firstGroupOfChain(groupAbsenceType);
    
    List<AbsenceRequestCategory> categories = absenceService
        .orderedCategories(person, date, groupAbsenceType);
    
    PeriodChain periodChain = absenceService.residual(person, groupAbsenceType, date);
    
    render("@groupStatus", date, categories, groupAbsenceType, periodChain);
    
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
