package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Sets;

import dao.PersonDao;
import dao.PersonDayDao;
import dao.absences.AbsenceComponentDao;

import manager.ConsistencyManager;
import manager.services.absences.AbsenceEngine;
import manager.services.absences.AbsenceEngine.AbsenceRequestType;
import manager.services.absences.AbsenceEngine.ResponseItem;
import manager.services.absences.AbsenceEngineInstance;
import manager.services.absences.AbsenceMigration;
import manager.services.absences.AbsenceRequestInterface;
import manager.services.absences.AbsenceRequestInterface.AbsenceRequestForm;
import manager.services.absences.AbsenceRequestInterface.AbsenceGroupFormItem;

import models.AbsenceTypeGroup;
import models.Office;
import models.Person;
import models.PersonDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.enumerate.AccumulationBehaviour;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import play.mvc.Controller;
import play.mvc.With;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

//@Slf4j
@With({Resecure.class, RequestInit.class})
public class AbsenceGroups extends Controller {
  
  @Inject
  private static PersonDao personDao;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  @Inject
  private static AbsenceRequestInterface absenceRequestInterface;
  @Inject
  private static AbsenceEngine absenceEngine;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static AbsenceMigration absenceMigration;
  
  public static void migrate() {
    
    absenceMigration.buildDefaultGroups();
    
    renderText("ok");
    
  }
  
  public static void show() {
    
    List<GroupAbsenceType> groups = GroupAbsenceType.findAll();
    render(groups);
    
  }
  
  public static void index(Office office) {
    
    
    List<AbsenceTypeGroup> groups = AbsenceTypeGroup.findAll();
    
    List<AbsenceTypeGroup> noMoreAbsencesAccepted = Lists.newArrayList();
    List<AbsenceTypeGroup> replaceCodeAndDecreaseAccumulation = Lists.newArrayList();
    List<AbsenceTypeGroup> otherGroups = Lists.newArrayList();
    
    for (AbsenceTypeGroup group : groups) {
      if (group.accumulationBehaviour.equals(AccumulationBehaviour.noMoreAbsencesAccepted)) {
        noMoreAbsencesAccepted.add(group);
      } else if (group.accumulationBehaviour
          .equals(AccumulationBehaviour.replaceCodeAndDecreaseAccumulation)) {
        replaceCodeAndDecreaseAccumulation.add(group);
      } else {
        otherGroups.add(group);
      }
      
    }
    
    render(noMoreAbsencesAccepted, replaceCodeAndDecreaseAccumulation, otherGroups);
  }
  
  public static void insert(Long personId, LocalDate from, LocalDate to, 
      GroupAbsenceType groupAbsenceType) {
    
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
   
    //Fix entity not null
    if (!groupAbsenceType.isPersistent()) {
      groupAbsenceType = null;
    }
        
    AbsenceRequestForm absenceRequestForm = absenceRequestInterface
        .buildInsertForm(person, from, to, groupAbsenceType);
    
    render(absenceRequestForm);
  }
  
  public static void configureInsert(Long personId, LocalDate from, LocalDate to, 
      GroupAbsenceType groupAbsenceType, AbsenceType absenceType, 
      JustifiedType justifiedType, Integer specifiedMinutes) {
  
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
    notFoundIfNull(groupAbsenceType);
    notFoundIfNull(justifiedType);
    
    if (!absenceType.isPersistent()) {
      absenceType = null;
    }
    
    AbsenceRequestForm absenceRequestForm = absenceRequestInterface.configureInsertForm(person, from, to,
        groupAbsenceType, absenceType, justifiedType, specifiedMinutes);
    
    render("@insert", absenceRequestForm);
    
  }
  
  
  public static void save(Long personId, LocalDate from, LocalDate to, 
      GroupAbsenceType groupAbsenceType, AbsenceType absenceType, 
      JustifiedType justifiedType, Integer specifiedMinutes) {
    
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
    notFoundIfNull(groupAbsenceType);
    notFoundIfNull(absenceType);
    notFoundIfNull(justifiedType);
    
    AbsenceRequestForm absenceRequest = absenceRequestInterface.configureInsertForm(person, from, to,
        groupAbsenceType, absenceType, justifiedType, specifiedMinutes);
    
    AbsenceEngineInstance engineInstance = absenceEngine
        .buildAbsenceEngineInstance(person, groupAbsenceType, from);
    Absence absence = new Absence();
    
    absenceEngine.doRequest(engineInstance, AbsenceRequestType.insert, absenceType, justifiedType, 
        Optional.fromNullable(specifiedMinutes));
    
    if (!engineInstance.absenceEngineProblem.isPresent()) {
      for (ResponseItem responseItem : engineInstance.responseItems) {
        // TODO: almeno un errore!!!
        if (responseItem.absenceProblem == null) {
          PersonDay personDay = personDayDao.getOrBuildPersonDay(person, responseItem.date);
          responseItem.absence.personDay = personDay;
          personDay.absences.add(absence);
          //personDay.save();
          //consistencyManager.updatePersonSituation(person.id, date);
        }
      }
    } else {

    }

    render("@insert", absenceRequest, engineInstance);
  }
  

  
  
}
