package manager.services.absences.web;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dao.absences.AbsenceComponentDao;

import lombok.extern.slf4j.Slf4j;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.web.AbsenceRequestForm.AbsenceInsertTab;

import models.Person;
import models.absences.AbsenceType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;

import org.joda.time.LocalDate;
import org.testng.collections.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

@Slf4j
public class AbsenceForm {
  
  public Person person;
  
  //tab
  public AbsenceInsertTab absenceInsertTab;
  
  //switch group
  
  private SortedMap<Integer, Set<CategoryGroupAbsenceType>> categoriesByPriority = Maps.newTreeMap();
  private Map<CategoryGroupAbsenceType, Set<GroupAbsenceType>> groupsByCategory =
      Maps.newHashMap();
  public GroupAbsenceType groupSelected;
  
  // switch date
  public LocalDate from;
  public LocalDate to;

  //automatic choice
  public boolean automaticChoiceExists;
  public boolean automaticChoiceSelected;
  
  //switch absenceType
  public List<AbsenceType> absenceTypes = Lists.newArrayList();
  public AbsenceType absenceTypeSelected;
  
  //switch justifiedType
  public List<JustifiedType> justifiedTypes = Lists.newArrayList();
  public JustifiedType justifiedTypeSelected;
  
  //quantity
  public Integer minutes = 0;
  public Integer hours = 0;
  
  public List<CategoryGroupAbsenceType> categories() {
    List<CategoryGroupAbsenceType> categories = Lists.newArrayList();
    for (Set<CategoryGroupAbsenceType> set : categoriesByPriority.values()) {
      categories.addAll(set);
    }
    return categories;
  }
  
  public Set<GroupAbsenceType> groupsForCategory(CategoryGroupAbsenceType category) {
    return groupsByCategory.get(category);
  }
  
  public List<GroupAbsenceType> groups() {
    List<GroupAbsenceType> groups = Lists.newArrayList();
    for (Set<GroupAbsenceType> set : this.groupsByCategory.values()) {
      groups.addAll(set);
    }
    return groups;
  }
  
  public boolean hasGroupChoice() {
    return groups().size() > 1;
  }
  
  public boolean hasAbsenceTypeChoice() {
    int choices = absenceTypes.size();
    if (automaticChoiceExists) {
      choices++;
    }
    return choices > 1;
  }
  
  public boolean hasJustifiedTypeChoice() {
    return justifiedTypes.size() > 1;
  }
  
  public boolean hasHourMinutesChoice() {
    return justifiedTypeSelected.name.equals(JustifiedTypeName.specified_minutes);
  }
  
  public boolean hasToChoice() {
    return justifiedTypeSelected.name.equals(JustifiedTypeName.all_day);
  }

  
  public AbsenceForm(Person person, LocalDate from, LocalDate to, 
      GroupAbsenceType groupAbsenceType, 
      AbsenceType absenceType, JustifiedType justifiedType, 
      Integer hours, Integer minutes, 
      AbsenceComponentDao absenceComponentDao, AbsenceEngineUtility absenceEngineUtility) {   
    
    this.person = person;
    this.groupSelected = groupAbsenceType;
    this.from = from;
    if (to != null) {
      this.to = to;
    } else {
      this.to = from;
    }
    
    this.absenceInsertTab = AbsenceInsertTab.fromGroup(groupAbsenceType);
    
    // generazione della lista dei gruppi 
    
    List<GroupAbsenceType> personGroupsInTab = absenceComponentDao
        .groupsAbsenceTypeByName(this.absenceInsertTab.groupNames);
    
    for (GroupAbsenceType groupInTab : personGroupsInTab) {
      if (!groupInTab.previousGroupChecked.isEmpty()) {
        continue;
      }
      
      log.info("Aggiungo ai gruppi {}", groupInTab.description);
      
      // aggiungo la categoria alla mappa
      Set<CategoryGroupAbsenceType> categoriesSamePriority = 
          this.categoriesByPriority.get(groupInTab.category.priority);
      if (categoriesSamePriority == null) {
        categoriesSamePriority = Sets.newHashSet();
        categoriesByPriority.put(groupInTab.category.priority, categoriesSamePriority);
      }
      categoriesSamePriority.add(groupInTab.category);
      
      // aggiungo il gruppo alla lista della categoria 
      Set<GroupAbsenceType> categoryGroups = groupsByCategory.get(groupInTab.category);
      if (categoryGroups == null) {
        categoryGroups = Sets.newHashSet();
        groupsByCategory.put(groupInTab.category, categoryGroups);
      }
      categoryGroups.add(groupInTab);
    }
    
    // i tipi assenza selezionabili
    GroupAbsenceType current = groupAbsenceType;
    SortedMap<String, AbsenceType> typeConsidered = Maps.newTreeMap();
    while (current != null) {
      if (current.takableAbsenceBehaviour != null) {
        for (AbsenceType takable : current.takableAbsenceBehaviour.takableCodes) {
          if (!takable.isExpired()) { //TODO: deve essere function di from e (to)
            typeConsidered.put(takable.code, takable);
          }
        }
      }
      current = current.nextGroupToCheck;
    }
    this.absenceTypes = Lists.newArrayList(typeConsidered.values());
    
    // esistenza gestione automatica: i tipi giustificativi automatici
    List<JustifiedType> automaticJustifiedTypes = 
        absenceEngineUtility.automaticJustifiedType(groupAbsenceType); 
    if (!automaticJustifiedTypes.isEmpty()) {
      this.automaticChoiceExists = true;
    }
    
    // scelta del tipo di assenza selezionato e dei tipi giustificativi possibili

    if (absenceType == null && this.automaticChoiceExists) {
      // se non ho specificato il tipo ed esiste una gestione automatica utilizzo quella
      this.automaticChoiceSelected = true;
      this.justifiedTypes = automaticJustifiedTypes;
      
    } else if (absenceType == null && !this.automaticChoiceExists){
      // se non ho specificato il tipo e non esiste una gestione automatica utilizzo il primo tipo
      this.absenceTypeSelected = this.absenceTypes.iterator().next();
      this.justifiedTypes = Lists.newArrayList(this.absenceTypeSelected.justifiedTypesPermitted);

    } else if (absenceType != null) {
      // ho specificato il tipo
      Verify.verify(this.absenceTypes.contains(absenceType));
      this.absenceTypeSelected = absenceType;
      this.justifiedTypes = Lists.newArrayList(absenceType.justifiedTypesPermitted);
    }
    
    if (justifiedType != null && this.justifiedTypes.contains(justifiedType)) {
      this.justifiedTypeSelected = justifiedType;
    } else {
      this.justifiedTypeSelected = this.justifiedTypes.iterator().next();
    }
    
    if (minutes != null) {
      this.minutes = minutes;
    }
    if (hours != null) {
      this.hours = hours;
    } 
    if (this.minutes <= 0 && this.hours <= 0) {
      this.hours = 1;
    }
    
    return;
  }
  
  
  

}
