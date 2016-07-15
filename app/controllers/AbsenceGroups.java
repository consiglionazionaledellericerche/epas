package controllers;

import com.google.common.base.Optional;

import dao.PersonDao;
import dao.PersonDayDao;
import dao.absences.AbsenceComponentDao;

import manager.ConsistencyManager;
import manager.services.absences.AbsenceEngine;
import manager.services.absences.AbsenceEngine.AbsenceRequestType;
import manager.services.absences.AbsenceEngine.ResponseItem;
import manager.services.absences.AbsenceEngineInstance;
import manager.services.absences.AbsenceMigration;

import models.AbsenceTypeGroup;
import models.Office;
import models.Person;
import models.PersonDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.enumerate.AccumulationBehaviour;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

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
  private static PersonDayDao personDayDao;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
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
  
  public static void insert(Long personId, LocalDate date, String group, String code) {
    
    Person person = personDao.getPersonById(personId);
    Optional<GroupAbsenceType> groupAbsenceType = absenceComponentDao.groupAbsenceTypeByName(group);
    Optional<AbsenceType> absenceType = absenceComponentDao.absenceTypeByCode(code);
    if (person == null || date == null || !groupAbsenceType.isPresent() 
        || !absenceType.isPresent())  {
      render();
    }
    
    AbsenceEngineInstance engineInstance = absenceEngine
        .buildAbsenceEngineInstance(person, groupAbsenceType.get(), date);
    Absence absence = new Absence();
    
    absenceEngine.doRequest(engineInstance, AbsenceRequestType.insert, absenceType.get());
    
    if (!engineInstance.absenceEngineProblem.isPresent()) {
      for (ResponseItem responseItem : engineInstance.responseItems) {
        PersonDay personDay = personDayDao.getOrBuildPersonDay(person, responseItem.date);
        responseItem.absence.personDay = personDay;
        personDay.absences.add(absence);
        //personDay.save();
        //consistencyManager.updatePersonSituation(person.id, date);  
      }
    } else {
      
    }
    
    render(engineInstance);
  }
}
