package controllers;

import dao.PersonDao;

import manager.PersonDayManager;
import manager.services.absences.AbsenceMigration;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.AbsenceRequestType;
import manager.services.absences.model.AbsenceEngine;
import manager.services.absences.web.AbsenceRequestForm;
import manager.services.absences.web.AbsenceRequestForm.SubAbsenceGroupFormItem;

import models.AbsenceTypeGroup;
import models.Office;
import models.Person;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
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
  private static PersonDayManager personDayManager;
  @Inject
  private static AbsenceService absenceService;
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
        
    AbsenceRequestForm absenceRequestForm = absenceService
        .buildInsertForm(person, from, to, groupAbsenceType);
    
    AbsenceEngine absenceEngine = null;
    if (absenceRequestForm.selectedAbsenceGroupFormItem != null) {
      SubAbsenceGroupFormItem selected = absenceRequestForm
          .selectedAbsenceGroupFormItem.selectedSubAbsenceGroupFormItems;
      
      absenceEngine = absenceService.doRequest(person, groupAbsenceType, from, to, 
          AbsenceRequestType.insert, selected.absenceType, 
          selected.selectedJustified, selected.getHours(), selected.getMinutes());  
    }
    
    render(absenceRequestForm, absenceEngine);
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
    
    AbsenceEngine absenceEngine = null;
    if (absenceRequestForm.selectedAbsenceGroupFormItem != null) {
      SubAbsenceGroupFormItem selected = absenceRequestForm
          .selectedAbsenceGroupFormItem.selectedSubAbsenceGroupFormItems;
      
      absenceEngine = absenceService.doRequest(person, groupAbsenceType, from, to, 
          AbsenceRequestType.insert, selected.absenceType, 
          selected.selectedJustified, selected.getHours(), selected.getMinutes());  
    }
    
    render("@insert", absenceRequestForm, absenceEngine);
    
  }
  
  
  public static void save(Long personId, LocalDate from, LocalDate to, 
      GroupAbsenceType groupAbsenceType, AbsenceType absenceType, 
      JustifiedType justifiedType, Integer hours, Integer minutes) {
    
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
    notFoundIfNull(groupAbsenceType);
    notFoundIfNull(absenceType);
    notFoundIfNull(justifiedType);
    
    if (!absenceType.isPersistent()) {
      absenceType = null;
    }
    
    AbsenceEngine absenceEngine = absenceService.doRequest(person, groupAbsenceType, from, to, 
        AbsenceRequestType.insert, absenceType, justifiedType, hours, minutes);
    
    AbsenceRequestForm absenceRequestForm = absenceService.configureInsertForm(person, from, to,
        groupAbsenceType, absenceType, justifiedType, hours, minutes);
    
    render("@insert", absenceRequestForm, absenceEngine);
  }
  

  
  
}
