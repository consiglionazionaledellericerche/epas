package manager.services.absences;

import com.google.common.base.Verify;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;

import lombok.Getter;

import models.Person;
import models.absences.AbsenceType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;

import org.joda.time.LocalDate;
import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.testng.collections.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * Interfaccia web per l'amministratore per navigare i gruppi assenze della persona e per effettuare
 * le richieste via form.
 * @author alessandro
 *
 */
public class AbsenceRequestInterface {

  private final AbsenceComponentDao absenceComponentDao;

  @Inject
  public AbsenceRequestInterface(AbsenceComponentDao absenceComponentDao) {
    this.absenceComponentDao = absenceComponentDao;
  }
  
  /**
   * Genera la form di inserimento assenza.
   * @param person
   * @param from
   * @param to
   * @param groupAbsenceType
   * @return
   */
  public AbsenceRequestForm buildInsertForm(Person person, LocalDate from, LocalDate to, 
      GroupAbsenceType groupAbsenceType) {
    
    //TODO: filtrare i gruppi sulla base della persona e della sede.
    List<GroupAbsenceType> allAbsenceTypeGroupPersonEnabled = 
        absenceComponentDao.allGroupAbsenceType();
    
    //TODO: Preconditions se groupAbsenceType presente verificare che permesso per la persona
    
    return buildAbsenceRequestForm(person, from, to, allAbsenceTypeGroupPersonEnabled, 
        groupAbsenceType, null, null, null);
  }
  
  /**
   * Riconfigura la form di inserimento assenza con i nuovi parametri forniti.
   * @param person
   * @param from
   * @param to
   * @param groupAbsenceType
   * @param absenceType
   * @param justifiedType
   * @param specifiedMinutes
   * @return
   */
  public AbsenceRequestForm configureInsertForm(Person person, LocalDate from, LocalDate to, 
      GroupAbsenceType groupAbsenceType, AbsenceType absenceType, 
      JustifiedType justifiedType, Integer specifiedMinutes) {
    
    //TODO: filtrare i gruppi sulla base della persona e della sede.
    List<GroupAbsenceType> allAbsenceTypeGroupPersonEnabled = 
        absenceComponentDao.allGroupAbsenceType();
    
    //TODO: Preconditions absenceType appartiene ad groupAbsenceType
    
    return buildAbsenceRequestForm(person, from, to, allAbsenceTypeGroupPersonEnabled, 
        groupAbsenceType, absenceType, justifiedType, specifiedMinutes);
  }
   
  /**
   * Costruisce la richiesta. 
   * Operazioni critiche: selezionare la prima opzione quando non è specificato niente.
   * @param person
   * @param from
   * @param to
   * @param allAbsenceTypeGroupPersonEnabled
   * @param selectedGroupAbsenceType
   * @param selectedAbsenceType
   * @param selectedJustifiedType
   * @param selectedSpecifiedMinutes
   */
  private AbsenceRequestForm buildAbsenceRequestForm(Person person, LocalDate from, LocalDate to, 
      List<GroupAbsenceType> allAbsenceTypeGroupPersonEnabled, 
      GroupAbsenceType selectedGroupAbsenceType, AbsenceType selectedAbsenceType, 
      JustifiedType selectedJustifiedType, Integer selectedSpecifiedMinutes) {   

    AbsenceRequestForm absenceRequestForm = new AbsenceRequestForm();
    absenceRequestForm.person = person;      
    absenceRequestForm.from = from;
    absenceRequestForm.to = to;
    
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
    
    // TODO: capire se conviene costruire i subAbsenceGroupForm per tutti i gruppi o solo di quello
    // selezionato... 
    if (absenceGroupFormItem.selected) {
      
      //Se non non ho preselezionato alcun absenceType allora selezionerò la prima opzione 
      // (automatica se esiste o il primo codice della lista)
      boolean selectNextSubGroup = selectedAbsenceType == null ? true : false;

      if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.programmed)) {
        
        List<JustifiedType> automaticJustifiedTypes = automaticJustifiedType(groupAbsenceType);
        
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
      
          if (selectNextSubGroup) {
            selectNextSubGroup = false; // è true solo la prima volta
            automaticSubFormItem.selected = true;
            absenceGroupFormItem.subAbsenceGroupFormItems.add(automaticSubFormItem);
            absenceGroupFormItem.selectedSubAbsenceGroupFormItems = automaticSubFormItem;
          }
        }

      } else if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.vacationsCnr)) {
        
        //TODO: Gruppo automatico per ferie.
      }

      // Sub Group Each Code takable e complation (anche dei gruppi annidati)
      Set<AbsenceType> typeConsidered = Sets.newHashSet();
      
      GroupAbsenceType currentGroupAbsenceType = groupAbsenceType; 
      while (currentGroupAbsenceType != null) {
        
        if (currentGroupAbsenceType.takableAbsenceBehaviour != null) {
          for (AbsenceType takable : currentGroupAbsenceType.takableAbsenceBehaviour.takableCodes) {
            typeConsidered.add(takable);
          }
        }
        if (currentGroupAbsenceType.complationAbsenceBehaviour != null) {
          for (AbsenceType complation : currentGroupAbsenceType.complationAbsenceBehaviour.complationCodes) {
            typeConsidered.add(complation);
          }
        }
        for (AbsenceType absenceType : typeConsidered) {
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
   * Le operazioni univocamente identificabili dal justifiedType. Devo riuscire a derivare
   * l'assenza da inserire attraverso il justifiedType.
   *  Lista con priorità:
   *  - se esiste un solo codice allDay  -> lo metto tra le opzioni
   *  - se esiste un solo codice halfDay -> lo metto tra le opzioni
   *  - se esiste: un solo codice absence_type_minutes con Xminute
   *               un solo codice absence_type_minutes con Yminute
   *               ...
   *               un solo codice absence_type_minutes con Zminute
   *               un solo codice specifiedMinutes 
   *               -> metto specifiedMinutes tra le opzioni
   *  
   *  TODO: decidere come gestire il quanto manca               
   *                
   * @param groupAbsenceType
   * @return
   */
  private List<JustifiedType> automaticJustifiedType(GroupAbsenceType groupAbsenceType) {
    
    List<JustifiedType> justifiedTypes = Lists.newArrayList();
    
    //TODO: Copia che mi metto da parte... ma andrebbe cachata!!
    final JustifiedType specifiedMinutesVar = 
        absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.specified_minutes);
    JustifiedType allDayVar = null;
    JustifiedType halfDayVar = null;

    Map<Integer, Integer> specificMinutesFinded = Maps.newHashMap(); //(minute, count)
    boolean specificMinutesDenied = false;
    Integer allDayFinded = 0;
    Integer halfDayFinded = 0;
    Integer specifiedMinutesFinded = 0;

    if (groupAbsenceType.takableAbsenceBehaviour == null) {
      return justifiedTypes;
    }
    
    for (AbsenceType absenceType : groupAbsenceType.takableAbsenceBehaviour.takableCodes) {
      for (JustifiedType justifiedType : absenceType.justifiedTypesPermitted) { 
        if (justifiedType.name.equals(JustifiedTypeName.all_day)) {
          allDayFinded++;
          allDayVar = justifiedType;
        }
        if (justifiedType.name.equals(JustifiedTypeName.half_day)) {
          halfDayFinded++;
          halfDayVar = justifiedType;
        }
        if (justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
          specifiedMinutesFinded++;
        }
        if (justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
          Integer minuteKey = absenceType.justifiedTime;
          Integer count = specificMinutesFinded.get(minuteKey);
          if (count == null) {
            specificMinutesFinded.put(minuteKey, 1);
          } else {
            count++;
            if (count > 1) {
              specificMinutesDenied = true;
            }
            specificMinutesFinded.put(minuteKey, count);
          }
        }
      }
    }
    
    if (allDayFinded == 1) {
      justifiedTypes.add(allDayVar);
    }
    if (halfDayFinded == 1) {
      justifiedTypes.add(halfDayVar);
    }
    if (specifiedMinutesFinded == 1 && specificMinutesDenied == false) {
      justifiedTypes.add(specifiedMinutesVar);
    }
    
    return justifiedTypes;
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
  
  /**
   * AbsenceRequestForm: Rappresenta la web form di inserimento assenza.
   * Le modalità di inserimento sono suddivise per categoria. Le categorie sono mappate con priorità.

   * @author alessandro
   *
   */
  public static class AbsenceRequestForm {

    public Person person;
    public LocalDate from;
    public LocalDate to;
    
    public SortedMap<Integer, List<AbsenceRequestCategory>> categoriesWithSamePriority = Maps.newTreeMap();
    // Note: keySet() which returns a set of the keys in ascending order
    //       values() which returns a collection of all values in the ascending 
    //                order of the corresponding keys

    public AbsenceGroupFormItem selectedAbsenceGroupFormItem = null;
   
  }


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

    protected CategoryGroupAbsenceType categoryType;
    protected List<AbsenceGroupFormItem> items = Lists.newArrayList();
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
    
    protected GroupAbsenceType groupAbsenceType;
    
    protected boolean selected = false;
    
    protected List<SubAbsenceGroupFormItem> subAbsenceGroupFormItems = Lists.newArrayList();
    protected SubAbsenceGroupFormItem selectedSubAbsenceGroupFormItems = null;
       
    protected AbsenceGroupFormItem(GroupAbsenceType groupAbsenceType) {
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
    
    // campi per modalità automatica
    protected String name;
    protected List<JustifiedType> justifiedTypes;
    
    // campi per modalità assenza
    protected boolean selected = false;
    protected AbsenceType absenceType;            
    
    protected JustifiedType selectedJustified;
    protected Integer specifiedMinutes;

    protected SubAbsenceGroupFormItem(AbsenceType absenceType) {
      this.absenceType = absenceType;
      this.name = absenceType.description;
    }
    
    protected SubAbsenceGroupFormItem(String name) {
      this.name = name;
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
  }
}
