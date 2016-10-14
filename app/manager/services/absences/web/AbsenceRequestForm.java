package manager.services.absences.web;

import com.google.common.collect.Maps;

import lombok.Getter;

import manager.services.absences.AbsenceMigration.DefaultGroup;
import manager.services.absences.web.AbsenceRequestForm.AbsenceRequestCategory;

import models.Person;
import models.absences.AbsenceType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import java.util.List;
import java.util.SortedMap;

/**
 * AbsenceRequestForm: Rappresenta la web form di inserimento assenza.
 * Le modalità di inserimento sono suddivise per categoria. Le categorie sono mappate con priorità.

 * @author alessandro
 *
 */

public class AbsenceRequestForm {

  public Person person;
  public LocalDate from;
  public LocalDate to;
  public AbsenceInsertTab absenceInsertTab;

  public SortedMap<Integer, List<AbsenceRequestCategory>> categoriesWithSamePriority = Maps.newTreeMap();
  // Note: keySet() which returns a set of the keys in ascending order
  //       values() which returns a collection of all values in the ascending 
  //                order of the corresponding keys

  public AbsenceGroupFormItem selectedAbsenceGroupFormItem = null;
  
  public boolean formHasNoGroupChoice() {
    List<AbsenceGroupFormItem> items = Lists.newArrayList();
    for (List<AbsenceRequestCategory> categoryList : categoriesWithSamePriority.values()) {
      for (AbsenceRequestCategory category : categoryList) {
        items.addAll(category.items);  
      }
    }
    return items.size() == 0 || items.size() == 1;
  }
  
  public List<AbsenceRequestCategory> orderedCategories() {
    List<AbsenceRequestCategory> categories = Lists.newArrayList();
    for (List<AbsenceRequestCategory> categoriesSamePriority : 
      this.categoriesWithSamePriority.values()) {
      categories.addAll(categoriesSamePriority);
    }
    return categories;
  }
  
  
  /// STATIC CLASS

  /**
   * AbsenceRequestCategory: Raccoglie i GroupAbsenceTypeItem che hanno groupAbsenceType 
   * con la stessa categoria
   * Esempio AbsenceRequestCategory: Permessi Vari
   *          GroupAbsenceTypeItem: Permesso visita medica
   *          GroupAbsenceTypeItem: Permesso diritto studio
   *          GroupAbsenceTypeItem: Permesso personale
   * 
   * @author alessandro
   *
   */
  @Getter
  public static class AbsenceRequestCategory {

    public CategoryGroupAbsenceType categoryType;
    public List<AbsenceGroupFormItem> items = Lists.newArrayList();
  }

  /**
   * AbsenceGroupFormItem: La scelta del groupAbsenceType.
   * Esempio:  - Congedo Ordinario Primo Figlio
   *           - Ferie e Permessi
   *           - Permesso visita medica
   *           - Permesso personale
   */
  @Getter
  public static class AbsenceGroupFormItem {

    public GroupAbsenceType groupAbsenceType;

    public boolean selected = false;
    public boolean containsAutomatic = false;

    public List<SubAbsenceGroupFormItem> subAbsenceGroupFormItems = Lists.newArrayList();
    public SubAbsenceGroupFormItem selectedSubAbsenceGroupFormItems = null;

    public AbsenceGroupFormItem(GroupAbsenceType groupAbsenceType) {
      this.groupAbsenceType = groupAbsenceType;
    }

    public String getLabel() {
      if (groupAbsenceType.chainDescription != null) {
        return groupAbsenceType.chainDescription;
      } else {
        return groupAbsenceType.description;
      }
    }
  }  

  /**
   * SubAbsenceGroupItemForm: la scelta all'interno di un AbsenceGroupItemForm.
   * E' quasi sempre associato ad un absenceType (tranne che nella modalità automatica).
   * Esempio: AbsenceGroupItemForm: Congedo Ordinario Primo figlio
   *          - Congedo Ordinario Primo Figlio (Automatico)
   *          - 23
   *          - 23M
   *          - 23U
   *          - 23H7
   */
  @Getter
  public static class SubAbsenceGroupFormItem {

    public boolean selected = false;
    public String name;

    // campi per modalità assenza
    public AbsenceType absenceType;            
   
    // campo per modalità automatica
    public List<JustifiedType> justifiedTypes;

    public JustifiedType selectedJustified;
    public Integer specifiedMinutes;

    public SubAbsenceGroupFormItem(AbsenceType absenceType) {
      this.absenceType = absenceType;
      this.name = absenceType.description;
    }

    public SubAbsenceGroupFormItem(String name) {
      this.name = name;
    }
    
    public boolean isAutomatic() {
      return this.absenceType == null;
    }

    public String getLabel() {
      if (absenceType != null) {
        return absenceType.code + " - " + absenceType.description;
      }
      return name;
    }

    public List<JustifiedType> getJustifiedTypesPermitted() {
      if (absenceType != null) {
        return Lists.newArrayList(absenceType.justifiedTypesPermitted);
      }
      return justifiedTypes;
    }
    
    public int getHours() {
      if (this.specifiedMinutes == null || this.getSpecifiedMinutes() < 0) {
        return 0;
      } else {
        return this.specifiedMinutes / 60;
      }
    }
    
    public int getMinutes() {
      if (this.specifiedMinutes == null || this.getSpecifiedMinutes() < 0) {
        return 0;
      } else {
        return this.specifiedMinutes % 60;
      }
    }
  }
  
 public static enum AbsenceInsertTab {
    
    mission(Lists.newArrayList(DefaultGroup.MISSIONE.name())),
    vacation(Lists.newArrayList(DefaultGroup.FERIE_CNR.name())),
    compensatory(Lists.newArrayList(DefaultGroup.RIPOSI_CNR.name())),
    automatic(Lists.newArrayList(DefaultGroup.PB.name())),
    other(Lists.newArrayList(
        DefaultGroup.G_661.name(), 
        DefaultGroup.G_89.name(), DefaultGroup.G_09.name(),
        DefaultGroup.G_18.name(), DefaultGroup.G_19.name(),
        DefaultGroup.G_23.name(), DefaultGroup.G_25.name(),
        DefaultGroup.G_232.name(), DefaultGroup.G_252.name(),
        DefaultGroup.G_233.name(), DefaultGroup.G_253.name(),
        DefaultGroup.MALATTIA.name(),
        DefaultGroup.MALATTIA_FIGLIO_1_12.name(),
        DefaultGroup.MALATTIA_FIGLIO_1_13.name(),
        DefaultGroup.MALATTIA_FIGLIO_1_14.name(),
        DefaultGroup.MALATTIA_FIGLIO_2_12.name(),
        DefaultGroup.MALATTIA_FIGLIO_2_13.name(),
        DefaultGroup.MALATTIA_FIGLIO_2_14.name(),
        DefaultGroup.MALATTIA_FIGLIO_3_12.name(),
        DefaultGroup.MALATTIA_FIGLIO_3_13.name(),
        DefaultGroup.MALATTIA_FIGLIO_3_14.name(),
        DefaultGroup.ALTRI.name(), DefaultGroup.G_95.name()
        ));

        public List<String> groupNames;
    
    private AbsenceInsertTab(List<String> groupNames) {
      this.groupNames = groupNames;
    }
    
    public static AbsenceInsertTab fromGroup(GroupAbsenceType groupAbsenceType) {
      for (AbsenceInsertTab absenceInsertTab : AbsenceInsertTab.values()) {
        if (absenceInsertTab.groupNames.contains(groupAbsenceType.name)) {
          return absenceInsertTab;
        }
      }
      return null;
    }
    
    public static AbsenceInsertTab defaultTab() {
      return mission;
    }
    
    public boolean newImplementation() {
      return true; //!this.equals(AbsenceInsertTab.compensatory) && !this.equals(AbsenceInsertTab.vacation);
    }
  }
}
