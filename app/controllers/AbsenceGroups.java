package controllers;

import dao.PersonDao;
import dao.PersonDayDao;

import lombok.extern.slf4j.Slf4j;

import manager.ConsistencyManager;
import manager.services.absences.AbsenceEngine;

import models.AbsenceTypeGroup;
import models.Office;
import models.Person;
import models.enumerate.AccumulationBehaviour;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

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
  private static PersonDayDao personDayDao;
  @Inject
  private static AbsenceEngine absenceEngine;
  @Inject
  private static ConsistencyManager consistencyManager;
  
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
  
  public static void insert(Long personId, LocalDate date, String group) {
    
    Person person = personDao.getPersonById(personId);
    
//    AbsencePeriod absencePeriod = absenceEngine.buildAbsencePeriod(person, 
//        GroupAbsenceType.group661, date);
//    
//    AbsenceType absenceType = absencePeriod.takableComponent.get().takableCodes.iterator().next(); 
//    
//    boolean result = absenceEngine.requestForAbsenceInPeriod(absencePeriod, 
//        AbsenceRequestType.insertTakable, absenceType, date);
//    
//    if (result) {
//      PersonDay personDay = personDayDao.getOrBuildPersonDay(person, date);
//      Absence absence = new Absence();
//      absence.absenceType = absenceType;
//      absence.personDay = personDay;
//      personDay.absences.add(absence);
//      personDay.save();
//      
//      consistencyManager.updatePersonSituation(person.id, date);
//      
//      log.info("Inserita assenza {}.", absenceType.code);
//    } else {
//      log.info("Rifiutata assenza {}.", absenceType.code);
//    }
//    
//    renderText(result);
    
  }
}
