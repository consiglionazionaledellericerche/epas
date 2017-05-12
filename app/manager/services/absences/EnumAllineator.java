package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.absences.AbsenceComponentDao;

import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import models.absences.AbsenceType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.CategoryTab;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultCategoryType;
import models.absences.definitions.DefaultComplation;
import models.absences.definitions.DefaultGroup;
import models.absences.definitions.DefaultTab;
import models.absences.definitions.DefaultTakable;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import play.db.jpa.JPA;

public class EnumAllineator {
  
  private final AbsenceComponentDao absenceComponentDao;

  /**
   * Constructor for injection.
   */
  @Inject
  public EnumAllineator(AbsenceComponentDao absenceComponentDao) {
    this.absenceComponentDao = absenceComponentDao;
  }
 
  /**
   * Allinea i tipi assenza.
   */
  public void handleAbsenceTypes(boolean initialization) {
    //i codici che non esistono li creo
    for (DefaultAbsenceType defaultAbsenceType : DefaultAbsenceType.values()) {
      if (initialization || !absenceComponentDao
          .absenceTypeByCode(defaultAbsenceType.getCode()).isPresent()) {
        //creazione entity a partire dall'enumerato
        AbsenceType absenceType = new AbsenceType();
        absenceType.code = defaultAbsenceType.getCode();
        absenceType.description = defaultAbsenceType.description;
        absenceType.certificateCode = defaultAbsenceType.certificationCode;
        absenceType.internalUse = defaultAbsenceType.internalUse;
        for (JustifiedTypeName justifiedName : defaultAbsenceType.justifiedTypeNamesPermitted) {
          absenceType.justifiedTypesPermitted.add(absenceComponentDao
              .getOrBuildJustifiedType(justifiedName));
        }
        absenceType.justifiedTime = defaultAbsenceType.justifiedTime;
        absenceType.consideredWeekEnd = defaultAbsenceType.consideredWeekEnd;
        absenceType.timeForMealTicket = defaultAbsenceType.timeForMealTicket;
        absenceType.replacingTime = defaultAbsenceType.replacingTime;
        if (defaultAbsenceType.replacingType != null) {
          absenceType.replacingType = absenceComponentDao
              .getOrBuildJustifiedType(defaultAbsenceType.replacingType);
        } else {
          absenceType.replacingType = null;
        }
        absenceType.validFrom = defaultAbsenceType.validFrom;
        absenceType.validTo = defaultAbsenceType.validTo;
        absenceType.save();
      }
    }
    
    if (initialization) {
      return;
    }
    
    List<AbsenceType> allAbsenceType = AbsenceType.findAll();
    for (AbsenceType absenceType : allAbsenceType) {
      Optional<DefaultAbsenceType> defaultAbsenceType = DefaultAbsenceType.byCode(absenceType); 
      if (defaultAbsenceType.isPresent()) {
        //gli absenceType che esistono le allineo all'enum
        absenceType.code = defaultAbsenceType.get().getCode();
        absenceType.certificateCode = defaultAbsenceType.get().certificationCode;
        absenceType.description = defaultAbsenceType.get().description;
        absenceType.internalUse = defaultAbsenceType.get().internalUse;
        updateJustifiedSet(absenceType.justifiedTypesPermitted, 
            defaultAbsenceType.get().justifiedTypeNamesPermitted);
        absenceType.justifiedTime = defaultAbsenceType.get().justifiedTime;
        absenceType.consideredWeekEnd = defaultAbsenceType.get().consideredWeekEnd;
        absenceType.timeForMealTicket = defaultAbsenceType.get().timeForMealTicket;
        absenceType.replacingTime = defaultAbsenceType.get().replacingTime;
        if (defaultAbsenceType.get().replacingType != null) {
          absenceType.replacingType = absenceComponentDao
              .getOrBuildJustifiedType(defaultAbsenceType.get().replacingType);
        } else {
          absenceType.replacingType = null;
        }
        absenceType.validFrom = defaultAbsenceType.get().validFrom;
        absenceType.validTo = defaultAbsenceType.get().validTo;
        absenceType.save();
      } else {
        //gli absenceType che non sono enumerati li tolgo dai gruppi.
        for (TakableAbsenceBehaviour takable : absenceType.takableGroup) {
          takable.takableCodes.remove(absenceType);
          takable.save();
        }
        for (TakableAbsenceBehaviour takable : absenceType.takenGroup) {
          takable.takenCodes.remove(absenceType);
          takable.save();
        }
        for (ComplationAbsenceBehaviour complation : absenceType.complationGroup) {
          complation.complationCodes.remove(absenceType);
          complation.save();
        }
        //e li disabilito
        absenceType.validFrom = new LocalDate(2016, 01, 01);
        absenceType.validTo = new LocalDate(2016, 01, 01);
        absenceType.save();
      }
    }
  }

  /**
   * Allinea i comportamenti di completamento.
   */
  public void handleComplations(boolean initialization) {
    //i complation che non esistono li creo
    for (DefaultComplation defaultComplation : DefaultComplation.values()) {
      if (initialization || !absenceComponentDao
          .complationAbsenceBehaviourByName(defaultComplation.name()).isPresent()) {
        //creazione entity a partire dall'enumerato
        ComplationAbsenceBehaviour complation = new ComplationAbsenceBehaviour();
        complation.name = defaultComplation.name();
        complation.amountType = defaultComplation.amountType;
        for (DefaultAbsenceType defaultType : defaultComplation.complationCodes) {
          complation.complationCodes.add(
              absenceComponentDao.absenceTypeByCode(defaultType.getCode()).get());
        }
        for (DefaultAbsenceType defaultType : defaultComplation.replacingCodes) {
          complation.replacingCodes.add(
              absenceComponentDao.absenceTypeByCode(defaultType.getCode()).get());
        }
        complation.save();
      }
    }
    
    if (initialization) {
      return;
    }
    
    List<ComplationAbsenceBehaviour> allComplation = ComplationAbsenceBehaviour.findAll();
    for (ComplationAbsenceBehaviour complation : allComplation) {
      Optional<DefaultComplation> defaultComplation = DefaultComplation.byName(complation); 
      if (defaultComplation.isPresent()) {
        //i complation che esistono le allineo all'enum
        complation.amountType = defaultComplation.get().amountType;
        updateSet(complation.complationCodes, defaultComplation.get().complationCodes);
        updateSet(complation.replacingCodes, defaultComplation.get().replacingCodes);
        complation.save();
      } else {
        //le complation che non sono enumerate le elimino
        for (GroupAbsenceType group : complation.groupAbsenceTypes) {
          group.complationAbsenceBehaviour = null;
          group.save();
        }
        complation.delete();
      }
    }
    
  }
  
  /**
   * Allinea i comportamenti di prendibilità.
   */
  public void handleTakables(boolean initialization) {
    //i takable che non esistono li creo
    for (DefaultTakable defaultTakable : DefaultTakable.values()) {
      if (initialization || !absenceComponentDao
          .takableAbsenceBehaviourByName(defaultTakable.name()).isPresent()) {
        //creazione entity a partire dall'enumerato
        TakableAbsenceBehaviour takable = new TakableAbsenceBehaviour();
        takable.name = defaultTakable.name();
        takable.amountType = defaultTakable.amountType;
        takable.fixedLimit = defaultTakable.fixedLimit;
        takable.takableAmountAdjustment = defaultTakable.takableAmountAdjustment;
        for (DefaultAbsenceType defaultType : defaultTakable.takenCodes) {
          takable.takenCodes.add(
              absenceComponentDao.absenceTypeByCode(defaultType.getCode()).get());
        }
        for (DefaultAbsenceType defaultType : defaultTakable.takableCodes) {
          takable.takableCodes.add(
              absenceComponentDao.absenceTypeByCode(defaultType.getCode()).get());
        }
        takable.save();
      }
    }
    
    if (initialization) {
      return;
    }
    
    List<TakableAbsenceBehaviour> allTakable = TakableAbsenceBehaviour.findAll();
    for (TakableAbsenceBehaviour takable : allTakable) {
      Optional<DefaultTakable> defaultTakable = DefaultTakable.byName(takable); 
      if (defaultTakable.isPresent()) {
        //i takable che esistono le allineo all'enum
        takable.amountType = defaultTakable.get().amountType;
        takable.fixedLimit = defaultTakable.get().fixedLimit;
        takable.takableAmountAdjustment = defaultTakable.get().takableAmountAdjustment;
        updateSet(takable.takenCodes, defaultTakable.get().takenCodes);
        updateSet(takable.takableCodes, defaultTakable.get().takableCodes);
        takable.save();
      } else {
        //i takable che non sono enumerate  le elimino
        for (GroupAbsenceType group : takable.groupAbsenceTypes) {
          group.takableAbsenceBehaviour = null;
          group.save();
        }
        takable.delete();
      }
    }
  }
  
  /**
   * Allinea i gruppi.
   */
  public void handleGroup(boolean initialization) {
    
    //i gruppi che non esistono li creo
    for (DefaultGroup defaultGroup : DefaultGroup.values()) {
      if (initialization 
          || !absenceComponentDao.groupAbsenceTypeByName(defaultGroup.name()).isPresent()) {
        //creazione entity a partire dall'enumerato
        GroupAbsenceType group = new GroupAbsenceType();
        group.name = defaultGroup.name();
        group.description = defaultGroup.description;
        group.chainDescription = defaultGroup.chainDescription;
        group.pattern = defaultGroup.pattern;
        group.category = absenceComponentDao.categoryByName(defaultGroup.category.name()).get();
        group.priority = defaultGroup.priority;
        group.periodType = defaultGroup.periodType;
        group.takableAbsenceBehaviour = absenceComponentDao
            .takableAbsenceBehaviourByName(defaultGroup.takable.name()).get();
        if (defaultGroup.complation != null) {
          group.complationAbsenceBehaviour = absenceComponentDao
              .complationAbsenceBehaviourByName(defaultGroup.complation.name()).get();
        } else {
          group.complationAbsenceBehaviour = null;
        }
        if (defaultGroup.nextGroupToCheck != null) {
          //N.B. le chain vanno enumerate in ordine inverso! es 24 -> 25 -> 23 in modo da
          // trovare le dipendenze a questo punto già create.
          group.nextGroupToCheck = absenceComponentDao
              .groupAbsenceTypeByName(defaultGroup.nextGroupToCheck.name()).get();
        }
        group.automatic = defaultGroup.automatic;
        group.initializable = defaultGroup.initializable;
        group.save();
      }
    }
    
    if (initialization) {
      return;
    }
    
    for (GroupAbsenceType group : absenceComponentDao.allGroupAbsenceType(true)) {
      Optional<DefaultGroup> defaultGroup = DefaultGroup.byName(group); 
      if (defaultGroup.isPresent()) {
        //i gruppi che esistono li allineo all'enum
        group.description = defaultGroup.get().description;
        group.chainDescription = defaultGroup.get().chainDescription;
        group.category = absenceComponentDao
            .categoryByName(defaultGroup.get().category.name()).get();
        group.priority = defaultGroup.get().priority;
        //OSS: capire la politica di aggiornamento... dovrei essere bravo a modificare l'enumerato
        //in modo da evitare effetti collaterali (spostando i codici da takable a taken) e per
        //correggere errori. Questi cambiamenti possono avvenire automaticamente.
        group.pattern = defaultGroup.get().pattern;
        group.periodType = defaultGroup.get().periodType;
        group.automatic = defaultGroup.get().automatic;
        group.initializable = defaultGroup.get().initializable;
        if (defaultGroup.get().nextGroupToCheck != null) {
          group.nextGroupToCheck = absenceComponentDao
              .groupAbsenceTypeByName(defaultGroup.get().nextGroupToCheck.name()).get();
        }
        group.takableAbsenceBehaviour = absenceComponentDao
            .takableAbsenceBehaviourByName(defaultGroup.get().takable.name()).get();
        if (defaultGroup.get().complation != null) {
          group.complationAbsenceBehaviour = absenceComponentDao
              .complationAbsenceBehaviourByName(defaultGroup.get().complation.name()).get();
        } else {
          group.complationAbsenceBehaviour = null;
        }
        group.save();
      } else {
        //i gruppi non enumerati li elimino
        group.delete();
      }
    }
  }

  /**
   * Allinea le categorie.
   */
  public void handleCategory(boolean initialization) {
    
    //le categorie che non esistono le creo
    for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
      if (initialization 
          || !absenceComponentDao.categoryByName(defaultCategory.name()).isPresent()) {
        //creazione entity a partire dall'enumerato
        CategoryGroupAbsenceType category = new CategoryGroupAbsenceType();
        category.name = defaultCategory.name();
        category.description = defaultCategory.description;
        category.priority = defaultCategory.priority;
        category.tab = absenceComponentDao.tabByName(defaultCategory.categoryTab.name()).get();
        category.save();
      }
    }
    
    if (initialization) {
      return;
    }

    for (CategoryGroupAbsenceType categoryTab : absenceComponentDao.categoriesByPriority()) {
      Optional<DefaultCategoryType> defaultCategory = DefaultCategoryType.byName(categoryTab); 
      if (defaultCategory.isPresent()) {
        //le category che esistono le allineo all'enum
        categoryTab.description = defaultCategory.get().description;
        categoryTab.priority = defaultCategory.get().priority;
        categoryTab.tab = absenceComponentDao
            .tabByName(defaultCategory.get().categoryTab.name()).get();
        categoryTab.save();
      } else {
        //le category che non sono enumerate e non sono associate ad alcun gruppo le elimino
        if (categoryTab.groupAbsenceTypes.isEmpty()) {
          categoryTab.delete();
        }
      }
    }
  }

  /**
   * Allinea le tab.
   */
  public void handleTab(boolean initialization) {

    //le tab che non esistono le creo
    for (DefaultTab defaultTab : DefaultTab.values()) {
      if (initialization || !absenceComponentDao.tabByName(defaultTab.name()).isPresent()) {
        //creazione entity a partire dall'enumerato
        CategoryTab categoryTab = new CategoryTab();
        categoryTab.name = defaultTab.name();
        categoryTab.description = defaultTab.description;
        categoryTab.priority = defaultTab.priority;
        categoryTab.save();
      }
    }
    
    if (initialization) {
      return;
    }

    for (CategoryTab categoryTab : absenceComponentDao.tabsByPriority()) {
      Optional<DefaultTab> defaultTab = DefaultTab.byName(categoryTab); 
      if (defaultTab.isPresent()) {
        //le tab che esistono le allineo all'enumerato
        categoryTab.description = defaultTab.get().description;
        categoryTab.priority = defaultTab.get().priority;
        categoryTab.save();
      } else {
        //le tab che non sono enumerate e non sono associate ad alcuna categoria le elimino
        if (categoryTab.categoryGroupAbsenceTypes.isEmpty()) {
          categoryTab.delete();
        }
      }
    }
  }
  
  /**
   * Allinea le liste di codici. 
   * @param oldEntitySet entity set da aggiornare
   * @param newEnumSet set di enumerati
   * @return se l'entity set è stato modificato
   */
  private boolean updateSet(Set<AbsenceType> entitySet, Set<DefaultAbsenceType> newEnumSet) {
    
    boolean edited = false;
    
    Set<String> newStringSet = Sets.newHashSet();
    for (DefaultAbsenceType defaultType : newEnumSet) {
      newStringSet.add(defaultType.getCode());
    }
   
    //Eliminare quelli non più contenuti
    List<AbsenceType> toRemove = Lists.newArrayList();
    for (AbsenceType absenceType : entitySet) {
      if (!newStringSet.contains(absenceType.code)) {
        toRemove.add(absenceType);
      }
    }
    for (AbsenceType absenceType : toRemove) {
      entitySet.remove(absenceType);
      edited = true;
    }
    
    //Aggiungere quelli non presenti
    for (String code : newStringSet) {
      AbsenceType absenceType = absenceComponentDao.absenceTypeByCode(code).get();
      if (!entitySet.contains(absenceType)) {
        entitySet.add(absenceType);
        edited = true;
      }
    }
    
    return edited;
  }
  
  /**
   * Allinea le liste di tipi justificazione. 
   * @param oldEntitySet entity set da aggiornare
   * @param newEnumSet set di enumerati
   * @return se l'entity set è stato modificato
   */
  private boolean updateJustifiedSet(Set<JustifiedType> entitySet, 
      Set<JustifiedTypeName> newEnumSet) {
    
    boolean edited = false;
    
    //Eliminare quelli non più contenuti
    List<JustifiedType> toRemove = Lists.newArrayList();
    for (JustifiedType justifiedType : entitySet) {
      if (!newEnumSet.contains(justifiedType.name)) {
        toRemove.add(justifiedType);
      }
    }
    for (JustifiedType justifiedType : toRemove) {
      entitySet.remove(justifiedType);
      edited = true;
    }
    
    //Aggiungere quelli non presenti
    for (JustifiedTypeName name : newEnumSet) {
      JustifiedType justifiedType = absenceComponentDao.getOrBuildJustifiedType(name);
      if (!entitySet.contains(justifiedType)) {
        entitySet.add(justifiedType);
        edited = true;
      }
    }
    
    return edited;
  }
  
      
}
