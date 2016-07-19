package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import controllers.AbsenceGroups.AbsenceRequest.AbsenceRequestItem.SubAbsenceGroup;

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
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType;
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
      GroupAbsenceType groupAbsenceType, AbsenceType absenceType, 
      JustifiedType justifiedType, Integer specifiedMinutes) {
    
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    notFoundIfNull(from);
    
    //Fix entity not null
    if (!groupAbsenceType.isPersistent()) {
      groupAbsenceType = null;
    }
    if (!absenceType.isPersistent()) {
      absenceType = null;
    }
    if (!justifiedType.isPersistent()) {
      justifiedType = null;
    }
    
    AbsenceRequest absenceRequest = new AbsenceRequest(absenceComponentDao, person, from, to, 
        groupAbsenceType, absenceType, justifiedType, specifiedMinutes);
    
    render(absenceRequest);
  }
  
  public static class AbsenceRequest {

    public Person person;
    public LocalDate from;
    public LocalDate to;
    public List<AbsenceRequestItem> items = Lists.newArrayList();
    
    public AbsenceRequest(AbsenceComponentDao absenceComponentDao, Person person, LocalDate from, 
        LocalDate to, GroupAbsenceType groupAbsenceType, AbsenceType absenceType,  
        JustifiedType justifiedType, Integer specifiedMinutes) {      
      this.person = person;      
      this.from = from;
      this.to = to;
      
      List<GroupAbsenceType> allAbsenceTypeGroup = absenceComponentDao.allGroupAbsenceType();
      
      //TODO: filtrare i gruppi sulla base della persona e della sede.
      
      //TODO: quale ordine?
      
      //////////////////////////////////////////////////////////////////////////////////////////
      //////////////////////////////////////////////////////////////////////////////////////////
      // GroupAbsenceTypeItem 
        
      boolean selectedItem = false;
      
      for (GroupAbsenceType groupAbsenceTypeItem : allAbsenceTypeGroup) {

        AbsenceRequestItem absenceRequestItem = 
            new AbsenceRequestItem(groupAbsenceTypeItem, groupAbsenceType);
        
        this.items.add(absenceRequestItem);
        
        if (!selectedItem && groupAbsenceType == null) { //TODO: oppure non è prendibile dalla persona
          absenceRequestItem.selected = true;
          selectedItem = true;
        }
        
        if (absenceRequestItem.selected) {

          // GroupRequestItem Selected: Sub Group

          if (!groupAbsenceTypeItem.pattern.equals(GroupAbsenceTypePattern.simpleGrouping)) {
            // Gruppo automatico
              

          }

          // Sub Group Each Code takable e complation
          Set<AbsenceType> typeConsidered = Sets.newHashSet();

          if (groupAbsenceTypeItem.takableAbsenceBehaviour != null) {
            for (AbsenceType takable : groupAbsenceTypeItem.takableAbsenceBehaviour.takableCodes) {
              if (!typeConsidered.contains(takable)) {
                typeConsidered.add(takable);
                SubAbsenceGroup subAbsenceGroup = new SubAbsenceGroup(takable, absenceType, 
                    justifiedType, specifiedMinutes);
                absenceRequestItem.subAbsenceGroups.add(subAbsenceGroup);  
              }
            }
          }
          if (groupAbsenceTypeItem.complationAbsenceBehaviour != null) {
            for (AbsenceType complation : groupAbsenceTypeItem.complationAbsenceBehaviour.complationCodes) {
              if (!typeConsidered.contains(complation)) {
                typeConsidered.add(complation);
                SubAbsenceGroup subAbsenceGroup = new SubAbsenceGroup(complation, absenceType, 
                    justifiedType, specifiedMinutes);
                absenceRequestItem.subAbsenceGroups.add(subAbsenceGroup);
              }
            }
          }
        }
        
      }
    }
    
    public static class AbsenceRequestItem {
      
      public GroupAbsenceType groupAbsenceType;
      public boolean selected = false;
      public List<SubAbsenceGroup> subAbsenceGroups = Lists.newArrayList();
         
      public AbsenceRequestItem(GroupAbsenceType groupAbsenceType, 
          GroupAbsenceType selectedGroupAbsenceType) {
        this.groupAbsenceType = groupAbsenceType;
        if (selectedGroupAbsenceType != null && selectedGroupAbsenceType.equals(groupAbsenceType)) {
          this.selected = true;
        }
      }
      
      public static class SubAbsenceGroup {
        public String name;
        public boolean selected = false;
        public AbsenceType absenceType;
        public JustifiedType selectedJustified;
        public Integer specifiedMinutes;
        
        public SubAbsenceGroup(AbsenceType absenceType, AbsenceType selectedAbsenceType, 
            JustifiedType selectedJustifiedType, Integer specifiedMinutes) {
          this.absenceType = absenceType;
          this.name = absenceType.description;
          if (selectedAbsenceType != null && selectedAbsenceType.equals(absenceType)) {
            this.selected = true;
          }
          if (selectedJustifiedType != null) {
            this.selectedJustified = selectedJustifiedType;
          } else {
            if (!absenceType.justifiedTypesPermitted.isEmpty()) {
              this.selectedJustified = absenceType.justifiedTypesPermitted.iterator().next();
            }
          }
          this.specifiedMinutes = specifiedMinutes;
        }
      }
      
    }  
  }
  
  
  public static void save(Long personId, LocalDate date, String group, String code, 
      Integer specifiedMinutes, JustifiedType justifiedType) {
    
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
