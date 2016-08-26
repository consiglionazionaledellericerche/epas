package manager.services.absences.web;

import com.google.common.base.Verify;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.web.AbsenceRequestForm.AbsenceGroupFormItem;
import manager.services.absences.web.AbsenceRequestForm.AbsenceRequestCategory;
import manager.services.absences.web.AbsenceRequestForm.SubAbsenceGroupFormItem;

import models.Person;
import models.absences.AbsenceType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;

public class AbsenceRequestFormFactory {
  
  private final AbsenceComponentDao absenceComponentDao;
  private final AbsenceEngineUtility absenceEngineUtility;

  @Inject
  public AbsenceRequestFormFactory(AbsenceComponentDao absenceComponentDao, 
      AbsenceEngineUtility absenceEngineUtility) {
    this.absenceComponentDao = absenceComponentDao;
    this.absenceEngineUtility = absenceEngineUtility;
  }
  
  /**
   * Costruisce la richiesta. 
   * Operazioni critiche: selezionare la prima opzione quando non è specificato niente.
   * @param person
   * @param from
   * @param to
   * @param selectedGroupAbsenceType
   * @param selectedAbsenceType
   * @param selectedJustifiedType
   * @param selectedSpecifiedMinutes
   */
  public AbsenceRequestForm buildAbsenceRequestForm(Person person, LocalDate from, LocalDate to, 
      GroupAbsenceType selectedGroupAbsenceType, AbsenceType selectedAbsenceType, 
      JustifiedType selectedJustifiedType, Integer selectedSpecifiedMinutes) {   

    AbsenceRequestForm absenceRequestForm = new AbsenceRequestForm();
    absenceRequestForm.person = person;      
    absenceRequestForm.from = from;
    if (to != null) {
      absenceRequestForm.to = to;
    } else {
      absenceRequestForm.to = from;
    }
    
    //TODO: filtrare i gruppi sulla base della persona e della sede.
    List<GroupAbsenceType> allAbsenceTypeGroupPersonEnabled = 
        absenceComponentDao.allGroupAbsenceType();
    
    for (GroupAbsenceType groupAbsenceType : allAbsenceTypeGroupPersonEnabled) {

      if (!groupAbsenceType.previousGroupChecked.isEmpty()) {
        //i gruppi annidati non sono item.
        continue;
      }
      
      AbsenceGroupFormItem absenceGroupFormItem = buildAbsenceGroupItemForm(groupAbsenceType, 
          selectedGroupAbsenceType, selectedAbsenceType, selectedJustifiedType, 
          selectedSpecifiedMinutes);
      if (absenceGroupFormItem.selected) {
        absenceRequestForm.selectedAbsenceGroupFormItem = absenceGroupFormItem;
      }
      addFormItemInCategory(absenceRequestForm, absenceGroupFormItem);
    }
    return absenceRequestForm;
  }
  
  /**
   * Inserisce l'AbsenceGroupFormItem nell'AbsenceRequestForm posizionandolo nella categoria giusta.
   * Le categorie sono inoltre ordinate per priorità.
   * @param absenceRequestForm
   * @param absenceGroupFormItem
   */
  private void addFormItemInCategory(AbsenceRequestForm absenceRequestForm, 
      AbsenceGroupFormItem absenceGroupFormItem) {

    CategoryGroupAbsenceType categoryToInsert = absenceGroupFormItem.groupAbsenceType.category;
    int priority = absenceGroupFormItem.groupAbsenceType.category.priority;
    
    // 1) prendo la lista di AbsenceRequestCategory con la priorità della categoria da inserire
    List<AbsenceRequestCategory> samePriorityCategoryList = 
        absenceRequestForm.categoriesWithSamePriority.get(priority);
    
    if (samePriorityCategoryList == null) {
      // 2) se non esiste la creo e la inserisco in categories con la priorità da inserire
      samePriorityCategoryList = Lists.newArrayList();
      absenceRequestForm.categoriesWithSamePriority.put(priority, samePriorityCategoryList);
    }
    
    // 3) da questa lista vedo se esiste un item con la categoria da inserire.
    AbsenceRequestCategory absenceRequestCategory = null;
    for (AbsenceRequestCategory existentAbsenceRequestCateogry : samePriorityCategoryList) {
      if (existentAbsenceRequestCateogry.categoryType.equals(categoryToInsert)) {
        absenceRequestCategory = existentAbsenceRequestCateogry;
      }
    }
    
    // 4) se non esiste creo la AbsenceRequestCategory con quella categoria e la inserirsco alla lista
    if (absenceRequestCategory == null) {
      absenceRequestCategory = new AbsenceRequestCategory();
      absenceRequestCategory.categoryType = categoryToInsert;
      samePriorityCategoryList.add(absenceRequestCategory);
    }
    
    // 5) inserisco l'item nella categoria 
    absenceRequestCategory.items.add(absenceGroupFormItem);
    
  }
  
  /**
   * AbsenceGroupFormItem: La scelta del groupAbsenceType.
   * Esempio:  - Congedo Ordinario Primo Figlio
   *           - Ferie e Permessi
   *           - Permesso visita medica
   * Il loro ordine è stabilito dal chiamante.          
   * @param groupAbsenceType
   * @param selectedGroupAbsenceType
   * @param selectedAbsenceType
   * @param selectedJustifiedType
   * @param selectedSpecifiedMinutes
   * @return
   */
  private AbsenceGroupFormItem buildAbsenceGroupItemForm(GroupAbsenceType groupAbsenceType, 
      GroupAbsenceType selectedGroupAbsenceType, AbsenceType selectedAbsenceType, 
      JustifiedType selectedJustifiedType, Integer selectedSpecifiedMinutes) {
    
    AbsenceGroupFormItem absenceGroupFormItem = new AbsenceGroupFormItem(groupAbsenceType);
    if (selectedGroupAbsenceType != null && selectedGroupAbsenceType.equals(groupAbsenceType)) {
      absenceGroupFormItem.selected = true;
    }
    
    // Capire se conviene costruire i subAbsenceGroupForm per tutti i gruppi o solo di quello
    // selezionato... 
    if (absenceGroupFormItem.selected) {
      
      //Se non non ho preselezionato alcun absenceType allora selezionerò la prima opzione 
      // (automatica se esiste o il primo codice della lista)
      boolean selectNextSubGroup = selectedAbsenceType == null ? true : false;

      if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.programmed)) {
        
        List<JustifiedType> automaticJustifiedTypes = 
            absenceEngineUtility.automaticJustifiedType(groupAbsenceType);
        
        if (!automaticJustifiedTypes.isEmpty()) {
          
          SubAbsenceGroupFormItem automaticSubFormItem = 
              new SubAbsenceGroupFormItem(absenceGroupFormItem.getLabel());
          automaticSubFormItem.justifiedTypes = automaticJustifiedTypes;
          if (automaticJustifiedTypes.contains(selectedJustifiedType)) {
            automaticSubFormItem.selectedJustified = selectedJustifiedType; 
          } else {
            automaticSubFormItem.selectedJustified = automaticJustifiedTypes.iterator().next();
          }
          automaticSubFormItem.specifiedMinutes = selectedSpecifiedMinutes;

          absenceGroupFormItem.subAbsenceGroupFormItems.add(automaticSubFormItem);
          absenceGroupFormItem.containsAutomatic = true;
          
          if (selectNextSubGroup) {
            selectNextSubGroup = false; // è true solo la prima volta
            automaticSubFormItem.selected = true;
            absenceGroupFormItem.selectedSubAbsenceGroupFormItems = automaticSubFormItem;
          }
        }

      } else if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.vacationsCnr)) {
        
        //TODO: Gruppo automatico per ferie.
      }

      // Sub Group Each Code takable e complation (anche dei gruppi annidati)
      SortedMap<String, AbsenceType> typeConsidered = Maps.newTreeMap();
      
      GroupAbsenceType currentGroupAbsenceType = groupAbsenceType; 
      while (currentGroupAbsenceType != null) {
        
        if (currentGroupAbsenceType.takableAbsenceBehaviour != null) {
          for (AbsenceType takable : currentGroupAbsenceType.takableAbsenceBehaviour.takableCodes) {
            typeConsidered.put(takable.code, takable);
          }
        }
        if (currentGroupAbsenceType.complationAbsenceBehaviour != null) {
          for (AbsenceType complation : currentGroupAbsenceType.complationAbsenceBehaviour.complationCodes) {
            typeConsidered.put(complation.code, complation);
          }
        }
        for (AbsenceType absenceType : typeConsidered.values()) {
          if (absenceType.justifiedTypesPermitted.isEmpty()) {
            // TODO: questo evento andrebbe segnalato.
            continue;
          }
          SubAbsenceGroupFormItem  subAbsenceGroupFormItem = buildSubAbsenceGroupItemForm(absenceType, 
              selectedAbsenceType, selectedJustifiedType, selectedSpecifiedMinutes, selectNextSubGroup);
          selectNextSubGroup = false; // è true solo la prima volta
          absenceGroupFormItem.subAbsenceGroupFormItems.add(subAbsenceGroupFormItem);
          if (subAbsenceGroupFormItem.selected) {
            absenceGroupFormItem.selectedSubAbsenceGroupFormItems = subAbsenceGroupFormItem;
          }
        }
        
        currentGroupAbsenceType = currentGroupAbsenceType.nextGroupToCheck;
      }
    }
    return absenceGroupFormItem;
  }

  
  /**
   * SubAbsenceGroupItemForm: la scelta all'interno di un AbsenceGroupItemForm.
   * E' quasi sempre assiciato ad un absenceType (tranne che nella modalità automatica).
   * 
   * Esempio: AbsenceGroupItemForm: Congedo Ordinario Primo figlio
   *          
   *          - Congedo Ordinario Primo Figlio (Automatico)
   *          - 23
   *          - 23M
   *          - 23U
   *          - 23H7
   * @param absenceType
   * @param selectedAbsenceType
   * @param selectedJustifiedType
   * @param selectedSpecifiedMinutes
   * @param toSelect
   * @return
   */
  private SubAbsenceGroupFormItem buildSubAbsenceGroupItemForm(AbsenceType absenceType, 
      AbsenceType selectedAbsenceType, JustifiedType selectedJustifiedType, 
      Integer selectedSpecifiedMinutes, boolean toSelect) {
    
    SubAbsenceGroupFormItem subAbsenceGroupFormItem = new SubAbsenceGroupFormItem(absenceType);

    // 1) Decido se è il sotto gruppo selezionato 
    if (toSelect || (selectedAbsenceType != null 
        && selectedAbsenceType.equals(absenceType)) ) {
      // Se è il sottogruppo dell'assenza selezionata allora lo imposto a selected.
      subAbsenceGroupFormItem.selected = true;
    }

    // 2) Decido il justifiedType selezionato
    if (selectedJustifiedType != null && absenceType.justifiedTypesPermitted.contains(selectedJustifiedType)) {
      // Se il justifiedType è popolato e compatibile con l'absenceType lo imposto a selected
      subAbsenceGroupFormItem.selectedJustified = selectedJustifiedType;
    } else {
      // Altrimenti imposto come selezionato il primo della lista (che deve esistere)
      Verify.verify(!absenceType.justifiedTypesPermitted.isEmpty());
      subAbsenceGroupFormItem.selectedJustified = absenceType.justifiedTypesPermitted.iterator().next();

    }
    if (subAbsenceGroupFormItem.selectedJustified.name.equals(JustifiedType.JustifiedTypeName.specified_minutes)) {
      subAbsenceGroupFormItem.specifiedMinutes = selectedSpecifiedMinutes;  
      if (selectedSpecifiedMinutes == null) {
        subAbsenceGroupFormItem.specifiedMinutes = 0;
      }
    }
    return subAbsenceGroupFormItem;
  }
}
