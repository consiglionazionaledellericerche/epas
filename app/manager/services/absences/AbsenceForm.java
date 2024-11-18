/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package manager.services.absences;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dao.absences.AbsenceComponentDao;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import models.Person;
import models.absences.AbsenceType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.CategoryTab;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import org.joda.time.LocalDate;

/**
 * Contiene le informazioni per generare e controlla la
 * form di inserimento/modifica assenze.
 */
public class AbsenceForm {
  
  public Person person;
  
  //permission check
  public List<GroupAbsenceType> groupsPermitted = Lists.newArrayList();
  public SortedMap<Integer, CategoryTab> tabsVisibile = Maps.newTreeMap();
  boolean permissionDenied = false;
  
  //tab selected
  public CategoryTab categoryTabSelected;
  
  //switch group
  
  private SortedMap<Integer, Set<CategoryGroupAbsenceType>> categoriesByPriority = 
      Maps.newTreeMap();
  private Map<CategoryGroupAbsenceType, List<GroupAbsenceType>> groupsByCategory =
      Maps.newHashMap();    //la lista dei gruppi è ordinata per priorità
  public GroupAbsenceType groupSelected;
  
  // switch date
  public LocalDate from;
  public LocalDate to;
  
  //for those absences who need future recovery of time
  public LocalDate recoveryDate;

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
  
  /**
   * Constructor.
   *
   * @param person person
   * @param from from 
   * @param to to
   * @param groupAbsenceType group
   * @param absenceType absenceType 
   * @param justifiedType justifiedType
   * @param hours hours
   * @param minutes minutes
   * @param absenceComponentDao inj
   * @param absenceEngineUtility inj
   */
  protected AbsenceForm(Person person, LocalDate from, LocalDate to, 
      LocalDate recoveryDate, GroupAbsenceType groupAbsenceType, 
      AbsenceType absenceType, JustifiedType justifiedType, 
      Integer hours, Integer minutes, List<GroupAbsenceType> groupsPermitted,
      AbsenceComponentDao absenceComponentDao, AbsenceEngineUtility absenceEngineUtility) {   
    
    this.person = person;
    this.groupSelected = groupAbsenceType;
    this.from = from;
    if (to != null) {
      this.to = to;
    } else {
      this.to = from;
    }
    
    this.groupsPermitted = groupsPermitted;
    
    //calcolo delle tab visibili
    this.setTabsVisible();

    // generazione della lista dei gruppi della richiesta
    this.categoryTabSelected = groupAbsenceType.getCategory().getTab();

    Set<CategoryGroupAbsenceType> personCategoryGroupsInTab =
        this.categoryTabSelected.getCategoryGroupAbsenceTypes();

    for (CategoryGroupAbsenceType categoryInTab : personCategoryGroupsInTab) {
      
      // aggiungo la categoria alla mappa
      Set<CategoryGroupAbsenceType> categoriesSamePriority = 
          this.categoriesByPriority.get(categoryInTab.getPriority());
      if (categoriesSamePriority == null) {
        categoriesSamePriority = Sets.newHashSet();
        categoriesByPriority.put(categoryInTab.getPriority(), categoriesSamePriority);
      }
      categoriesSamePriority.add(categoryInTab);
      
      groupsByCategory.put(categoryInTab, categoryInTab.orderedGroupsInCategory(true));
    }
    
    // i tipi assenza selezionabili
    GroupAbsenceType current = groupAbsenceType;
    SortedMap<String, AbsenceType> typeConsidered = Maps.newTreeMap();
    while (current != null) {
      if (current.getTakableAbsenceBehaviour() != null) {
        for (AbsenceType takable : current.getTakableAbsenceBehaviour().getTakableCodes()) {
          // TODO: dovrebbre essere anche funzione di to
          if (!takable.isExpired(Optional.ofNullable(from).orElse(LocalDate.now()))) { 
            typeConsidered.put(takable.getCode(), takable);
          }
        }
      }
      current = current.getNextGroupToCheck();
    }
    this.absenceTypes = Lists.newArrayList(typeConsidered.values());
    
    // esistenza gestione automatica: i tipi giustificativi automatici
    List<JustifiedType> automaticJustifiedTypes = absenceComponentDao.justifiedTypes(
        absenceEngineUtility.automaticJustifiedType(groupAbsenceType)); 
    if (!automaticJustifiedTypes.isEmpty()) {
      this.automaticChoiceExists = true;
    }
    
    // scelta del tipo di assenza selezionato e dei tipi giustificativi possibili

    if (absenceType == null && this.automaticChoiceExists) {
      // se non ho specificato il tipo ed esiste una gestione automatica utilizzo quella
      this.automaticChoiceSelected = true;
      this.justifiedTypes = automaticJustifiedTypes;
      
    } else if (absenceType == null && !this.automaticChoiceExists) {
      // se non ho specificato il tipo e non esiste una gestione automatica utilizzo il primo tipo
      this.absenceTypeSelected = this.absenceTypes.iterator().next();
      this.justifiedTypes = Lists.newArrayList(this.absenceTypeSelected
          .getJustifiedTypesPermitted());

    } else if (absenceType != null) {
      // ho specificato il tipo
      Verify.verify(this.absenceTypes.contains(absenceType));
      this.absenceTypeSelected = absenceType;
      this.justifiedTypes = Lists.newArrayList(absenceType.getJustifiedTypesPermitted());
    }
    
    if (justifiedType != null && this.justifiedTypes.contains(justifiedType)) {
      this.justifiedTypeSelected = justifiedType;
    } else {
      this.justifiedTypeSelected = this.justifiedTypes.iterator().next();
    }

    
    if (this.justifiedTypeSelected.getName().equals(JustifiedTypeName.recover_time)) {
      this.recoveryDate = from.plusDays(1);
    } else {
      this.recoveryDate = null;
    }
    if (recoveryDate != null) {
      this.recoveryDate = recoveryDate;
    }
    
    if (minutes != null) {
      this.minutes = minutes;
    }
    if (hours != null) {
      this.hours = hours;
    } 
    if (this.minutes <= 0 && this.hours <= 0) {
      this.hours = 1;
      this.minutes = 0;
    }

    return;
  }
  
  /**
   * Le categorie (ordinate per priorità).
   *
   * @return list
   */
  public List<CategoryGroupAbsenceType> categories() {
    List<CategoryGroupAbsenceType> categories = Lists.newArrayList();
    for (Set<CategoryGroupAbsenceType> set : categoriesByPriority.values()) {
      categories.addAll(set);
    }
    return categories;
  }
  
  /**
   * I gruppi della categoria (già ordinati per priorità).
   *
   * @param category categoria
   */
  public List<GroupAbsenceType> groupsForCategory(CategoryGroupAbsenceType category) {
    return groupsByCategory.get(category);
  }
  
  /**
   * I gruppi.
   */
  public List<GroupAbsenceType> groups() {
    List<GroupAbsenceType> groups = Lists.newArrayList();
    for (List<GroupAbsenceType> set : this.groupsByCategory.values()) {
      groups.addAll(set);
    }
    return groups;
  }
  
  public boolean hasGroupChoice() {
    return groups().size() > 1;
  }
  
  /**
   * Se la form ha una scelta sul tipo assenza.
   *
   * @return esito
   */
  public boolean hasAbsenceTypeChoice() {
    int choices = absenceTypes.size();
    return choices > 1;
  }
  
  /**
   * Verifica se ha un unico tipo di assenza impostato.
   */
  public AbsenceType theOnlyAbsenceType() {
    Verify.verify(!hasAbsenceTypeChoice());
    return this.absenceTypes.get(0);
  }
  
  /**
   * Verifica se ci sono più tipologie di giustificazione dell'orario.
   */
  public boolean hasJustifiedTypeChoice() {
    return justifiedTypes.size() > 1;
  }
  
  /**
   * Verifica se la giustificazione oraria selezionata è con minuti e ore.
   */
  public boolean hasHourMinutesChoice() {
    return justifiedTypeSelected.getName().equals(JustifiedTypeName.specified_minutes)
        || justifiedTypeSelected.getName().equals(JustifiedTypeName.specified_minutes_limit);
  }
  
  /**
   * Verifica se la giustificazione selezionata è quella che assegna tutto il giorno.
   */
  public boolean hasToChoice() {
    return justifiedTypeSelected.getName().equals(JustifiedTypeName.all_day);
  }
  
  /**
   * Le ore inseribili per questa richiesta.
   */
  public List<Integer> selectableHours() {
    List<Integer> hours = Lists.newArrayList();
    for (int i = 0; i <= 7; i++) {
      hours.add(i);
    }
    return hours;
  }
  
  /**
   * I minuti inseribili per questa richiesta.
   */
  public List<Integer> selectableMinutes() {
    List<Integer> hours = Lists.newArrayList();
    for (int i = 0; i <= 59; i++) {
      hours.add(i);
    }
    return hours;
  }
  
  /**
   * Setter per le tab visibili a partire dai groupsPermitted.
   */
  private void setTabsVisible() {
    for (GroupAbsenceType group : this.groupsPermitted) {
      if (group.isAutomatic()) { 
        continue;
      }
      this.tabsVisibile.put(group.getCategory().getTab().getPriority(), 
          group.getCategory().getTab());
    }
  }

}
