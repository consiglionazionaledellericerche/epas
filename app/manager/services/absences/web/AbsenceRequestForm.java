package manager.services.absences.web;

import com.google.common.collect.Maps;

import lombok.Getter;

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

  public SortedMap<Integer, List<AbsenceRequestCategory>> categoriesWithSamePriority = Maps.newTreeMap();
  // Note: keySet() which returns a set of the keys in ascending order
  //       values() which returns a collection of all values in the ascending 
  //                order of the corresponding keys

  public AbsenceGroupFormItem selectedAbsenceGroupFormItem = null;

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
}
