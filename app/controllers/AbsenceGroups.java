package controllers;

import dao.PersonDao;

import lombok.extern.slf4j.Slf4j;

import manager.AbsenceManager;
import manager.ConsistencyManager;
import manager.PersonDayManager;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import manager.services.absences.model.PeriodChain;
import manager.services.absences.web.AbsenceForm;
import manager.services.absences.web.AbsenceForm.AbsenceInsertTab;

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
  private static ConsistencyManager consistencyManager;
  @Inject
  private static AbsenceManager absenceManager;
  
  /**
   * End point per la visualizzazione dei gruppi assenze definiti.
   */
  public static void show() {
    
    List<GroupAbsenceType> groups = GroupAbsenceType.findAll();
    render(groups);
    
  }
  
  
  /**
   * End point per la simulazione di inserimento assenze.s
   * @param personId persona
   * @param from data inizio
   * @param absenceInsertTab web tab    
   * @param to data fine
   * @param groupAbsenceType gruppo assenze
   * @param switchGroup se cambio gruppo di assenze
   * @param absenceType tipo assenza
   * @param justifiedType tipo giustificativo
   * @param hours ore
   * @param minutes minuti
   */
  public static void insert(
      Long personId, LocalDate from, AbsenceInsertTab absenceInsertTab,      //tab
      LocalDate to, GroupAbsenceType groupAbsenceType,  boolean switchGroup, //group
      AbsenceType absenceType, JustifiedType justifiedType,                  //confGroup 
      Integer hours, Integer minutes) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
   
    AbsenceForm absenceForm = 
        absenceService.buildAbsenceForm(person, from, absenceInsertTab, 
        to, groupAbsenceType, switchGroup, absenceType, justifiedType, hours, minutes);

    InsertReport insertReport  = absenceService.insert(person, 
        absenceForm.groupSelected, 
        absenceForm.from, absenceForm.to, 
        absenceForm.absenceTypeSelected, absenceForm.justifiedTypeSelected, 
        absenceForm.hours, absenceForm.minutes, absenceManager);
    render(absenceForm, insertReport);

  }
  
  /**
   * End Point per il salvataggio di assenze. 
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

  /**
   * End point per visualizzare lo stato di un gruppo assenze alla data.
   * 
   * @param personId persona
   * @param groupAbsenceType gruppo
   * @param from data
   */
  public static void groupStatus(Long personId, GroupAbsenceType groupAbsenceType, LocalDate from) {
    
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
    
    AbsenceForm categorySwitcher = absenceService
        .buildForCateogorySwitch(person, from, groupAbsenceType);
    
    PeriodChain periodChain = absenceService.residual(person, categorySwitcher.groupSelected, from);
    
    render(from, categorySwitcher, groupAbsenceType, periodChain);
  }
  
  /**
   * End point per definire l'inizializzazione di un gruppo.
   * 
   * @param personId persona
   * @param groupAbsenceType gruppo
   * @param date data
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
  
 
  
}
