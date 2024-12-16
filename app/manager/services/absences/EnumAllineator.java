/*
 * Copyright (C) 2024 Consiglio Nazionale delle Ricerche
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

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import dao.absences.AbsenceComponentDao;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.absences.AbsenceType;
import models.absences.AbsenceTypeJustifiedBehaviour;
import models.absences.CategoryGroupAbsenceType;
import models.absences.CategoryTab;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedBehaviour;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultAbsenceType.Behaviour;
import models.absences.definitions.DefaultCategoryType;
import models.absences.definitions.DefaultComplation;
import models.absences.definitions.DefaultGroup;
import models.absences.definitions.DefaultTab;
import models.absences.definitions.DefaultTakable;
import org.joda.time.LocalDate;
import org.testng.collections.Lists;

/**
 * Allinea gli Enum presenti nel codice e che rappresentato la configurazione
 * delle assenze con quelli presenti nel db.
 */
@Slf4j
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
      log.debug("Analizzo il codice: {}", defaultAbsenceType.getCode());
      if (initialization || !absenceComponentDao
          .absenceTypeByCode(defaultAbsenceType.getCode()).isPresent())  {
        //creazione entity a partire dall'enumerato
        buildAbsenceType(defaultAbsenceType);
      }
    }
    
    if (initialization) {
      return;
    }
    
    List<AbsenceType> allAbsenceType = AbsenceType.findAll();
    for (AbsenceType absenceType : allAbsenceType) {
      Optional<DefaultAbsenceType> defaultAbsenceType = DefaultAbsenceType.byCode(absenceType);
      if (!absenceType.isToUpdate()) {
        log.info("Ignorato aggiornamento del codice {}", absenceType.getCode());
        continue;
      }
      if (defaultAbsenceType.isPresent()) {
          //gli absenceType che esistono le allineo all'enum
          absenceType.setCode(defaultAbsenceType.get().getCode());
          absenceType.setCertificateCode(defaultAbsenceType.get().certificationCode);
          absenceType.setDescription(defaultAbsenceType.get().description);
          absenceType.setInternalUse(defaultAbsenceType.get().internalUse);
          updateJustifiedSet(absenceType.getJustifiedTypesPermitted(), 
              defaultAbsenceType.get().justifiedTypeNamesPermitted);
          absenceType.setJustifiedTime(defaultAbsenceType.get().justifiedTime);
          absenceType.setConsideredWeekEnd(defaultAbsenceType.get().consideredWeekEnd);
          absenceType.setMealTicketBehaviour(defaultAbsenceType.get().mealTicketBehaviour);
          absenceType.setReperibilityCompatible(defaultAbsenceType.get().reperibilityCompatible);
          absenceType.setReplacingTime(defaultAbsenceType.get().replacingTime);
          if (defaultAbsenceType.get().replacingType != null) {
            absenceType.setReplacingType(absenceComponentDao
                .getOrBuildJustifiedType(defaultAbsenceType.get().replacingType));
          } else {
            absenceType.setReplacingType(null);
          }
          absenceType.setValidFrom(defaultAbsenceType.get().validFrom);
          absenceType.setValidTo(defaultAbsenceType.get().validTo);
          absenceType.setExternalId(defaultAbsenceType.get().externalId);
          absenceType.setRealAbsence(defaultAbsenceType.get().isRealAbsence);
          absenceType.save();
          updateBehaviourSet(absenceType, absenceType.getJustifiedBehaviours(), 
              defaultAbsenceType.get().behaviour);

          absenceType.save();

      } else {
        //gli absenceType che non sono enumerati li tolgo dai gruppi.
        for (TakableAbsenceBehaviour takable : absenceType.getTakableGroup()) {
          takable.getTakableCodes().remove(absenceType);
          takable.save();
        }
        for (TakableAbsenceBehaviour takable : absenceType.getTakenGroup()) {
          takable.getTakenCodes().remove(absenceType);
          takable.save();
        }
        for (ComplationAbsenceBehaviour complation : absenceType.getComplationGroup()) {
          complation.getComplationCodes().remove(absenceType);
          complation.save();
        }
        //e li disabilito
        absenceType.setValidFrom(new LocalDate(2016, 01, 01));
        absenceType.setValidTo(LocalDate.now().minusDays(1));
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
        complation.setName(defaultComplation.name());
        complation.setAmountType(defaultComplation.amountType);
        for (DefaultAbsenceType defaultType : defaultComplation.complationCodes) {
          complation.getComplationCodes().add(
              absenceComponentDao.absenceTypeByCode(defaultType.getCode()).get());
        }
        for (DefaultAbsenceType defaultType : defaultComplation.replacingCodes) {
          complation.getReplacingCodes().add(
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
        complation.setAmountType(defaultComplation.get().amountType);
        updateSet(complation.getComplationCodes(), defaultComplation.get().complationCodes);
        updateSet(complation.getReplacingCodes(), defaultComplation.get().replacingCodes);
        complation.save();
      } else {
        //le complation che non sono enumerate le elimino
        for (GroupAbsenceType group : complation.getGroupAbsenceTypes()) {
          group.setComplationAbsenceBehaviour(null);
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
        takable.setName(defaultTakable.name());
        takable.setAmountType(defaultTakable.amountType);
        takable.setFixedLimit(defaultTakable.fixedLimit);
        takable.setTakableAmountAdjustment(defaultTakable.takableAmountAdjustment);
        for (DefaultAbsenceType defaultType : defaultTakable.takenCodes) {
          takable.getTakenCodes().add(
              absenceComponentDao.absenceTypeByCode(defaultType.getCode()).get());
        }
        for (DefaultAbsenceType defaultType : defaultTakable.takableCodes) {
          takable.getTakableCodes().add(
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
        takable.setAmountType(defaultTakable.get().amountType);
        takable.setFixedLimit(defaultTakable.get().fixedLimit);
        takable.setTakableAmountAdjustment(defaultTakable.get().takableAmountAdjustment);
        updateSet(takable.getTakenCodes(), defaultTakable.get().takenCodes);
        updateSet(takable.getTakableCodes(), defaultTakable.get().takableCodes);
        takable.save();
      } else {
        //i takable che non sono enumerate  le elimino
        for (GroupAbsenceType group : takable.getGroupAbsenceTypes()) {
          group.setTakableAbsenceBehaviour(null);
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
        group.setName(defaultGroup.name());
        group.setDescription(defaultGroup.description);
        group.setChainDescription(defaultGroup.chainDescription);
        group.setPattern(defaultGroup.pattern);
        group.setCategory(absenceComponentDao.categoryByName(defaultGroup.category.name()).get());
        group.setPriority(defaultGroup.priority);
        group.setPeriodType(defaultGroup.periodType);
        group.setTakableAbsenceBehaviour(absenceComponentDao
            .takableAbsenceBehaviourByName(defaultGroup.takable.name()).get());
        if (defaultGroup.complation != null) {
          group.setComplationAbsenceBehaviour(absenceComponentDao
              .complationAbsenceBehaviourByName(defaultGroup.complation.name()).get());
        } else {
          group.setComplationAbsenceBehaviour(null);
        }
        if (defaultGroup.nextGroupToCheck != null) {
          //N.B. le chain vanno enumerate in ordine inverso! es 24 -> 25 -> 23 in modo da
          // trovare le dipendenze a questo punto già create.
          group.setNextGroupToCheck(absenceComponentDao
              .groupAbsenceTypeByName(defaultGroup.nextGroupToCheck.name()).get());
        }
        group.setAutomatic(defaultGroup.automatic);
        group.setInitializable(defaultGroup.initializable);
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
        group.setDescription(defaultGroup.get().description);
        group.setChainDescription(defaultGroup.get().chainDescription);
        group.setCategory(absenceComponentDao
            .categoryByName(defaultGroup.get().category.name()).get());
        group.setPriority(defaultGroup.get().priority);
        //OSS: capire la politica di aggiornamento... dovrei essere bravo a modificare l'enumerato
        //in modo da evitare effetti collaterali (spostando i codici da takable a taken) e per
        //correggere errori. Questi cambiamenti possono avvenire automaticamente.
        group.setPattern(defaultGroup.get().pattern);
        group.setPeriodType(defaultGroup.get().periodType);
        group.setAutomatic(defaultGroup.get().automatic);
        group.setInitializable(defaultGroup.get().initializable);
        if (defaultGroup.get().nextGroupToCheck != null) {
          group.setNextGroupToCheck(absenceComponentDao
              .groupAbsenceTypeByName(defaultGroup.get().nextGroupToCheck.name()).get());
        }
        group.setTakableAbsenceBehaviour(absenceComponentDao
            .takableAbsenceBehaviourByName(defaultGroup.get().takable.name()).get());
        if (defaultGroup.get().complation != null) {
          group.setComplationAbsenceBehaviour(absenceComponentDao
              .complationAbsenceBehaviourByName(defaultGroup.get().complation.name()).get());
        } else {
          group.setComplationAbsenceBehaviour(null);
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
        category.setName(defaultCategory.name());
        category.setDescription(defaultCategory.description);
        category.setPriority(defaultCategory.priority);
        category.setTab(absenceComponentDao.tabByName(defaultCategory.categoryTab.name()).get());
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
        categoryTab.setDescription(defaultCategory.get().description);
        categoryTab.setPriority(defaultCategory.get().priority);
        categoryTab.setTab(absenceComponentDao
            .tabByName(defaultCategory.get().categoryTab.name()).get());
        categoryTab.save();
      } else {
        //le category che non sono enumerate e non sono associate ad alcun gruppo le elimino
        if (categoryTab.getGroupAbsenceTypes().isEmpty()) {
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
        categoryTab.setName(defaultTab.name());
        categoryTab.setDescription(defaultTab.description);
        categoryTab.setPriority(defaultTab.priority);
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
        categoryTab.setDescription(defaultTab.get().description);
        categoryTab.setPriority(defaultTab.get().priority);
        categoryTab.save();
      } else {
        //le tab che non sono enumerate e non sono associate ad alcuna categoria le elimino
        if (categoryTab.getCategoryGroupAbsenceTypes().isEmpty()) {
          categoryTab.delete();
        }
      }
    }
  }
  
  /**
   * Allinea le liste di codici.
   *
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
    for (AbsenceType absenceType : entitySet.stream().filter(a -> a.isToUpdate()).collect(Collectors.toList())) {
      if (!newStringSet.contains(absenceType.getCode())) {
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
   *
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
      if (!newEnumSet.contains(justifiedType.getName())) {
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
  
  /**
   * Allinea le liste di behaviour.
   *
   * @param oldEntitySet entity set da aggiornare
   * @param newEnumSet set di enumerati
   * @return se l'entity set è stato modificato
   */
  private boolean updateBehaviourSet(AbsenceType absenceType, 
      Set<AbsenceTypeJustifiedBehaviour> entitySet, Set<Behaviour> newEnumSet) {
    
    boolean edited = false;

    //Eliminare quelli non più contenuti
    List<AbsenceTypeJustifiedBehaviour> toRemove = Lists.newArrayList();
    for (AbsenceTypeJustifiedBehaviour behaviour : entitySet) {
      boolean equal = false;
      for (Behaviour defaultBehaviour : newEnumSet) { 
        if (defaultBehaviour.name.equals(behaviour.getJustifiedBehaviour().getName()) 
            && AbsenceType.safeEqual(defaultBehaviour.data, behaviour.getData())) {
          equal = true;
        }
      }
      if (!equal) {
        toRemove.add(behaviour);
      }
    }
    for (AbsenceTypeJustifiedBehaviour behaviour : toRemove) {
      entitySet.remove(behaviour);
      behaviour.delete();
      edited = true;
    }
    
    //Aggiungere quelli non presenti
    for (Behaviour enumBehaviour : newEnumSet) {
      JustifiedBehaviour justifiedBehaviour = absenceComponentDao
          .getOrBuildJustifiedBehaviour(enumBehaviour.name);
      
      boolean equal = false;
      for (AbsenceTypeJustifiedBehaviour behaviour : entitySet) { 
        if (enumBehaviour.name.equals(behaviour.getJustifiedBehaviour().getName()) 
            && AbsenceType.safeEqual(enumBehaviour.data, behaviour.getData())) {
          equal = true;
        }
      }
      if (!equal) {
        AbsenceTypeJustifiedBehaviour b = new AbsenceTypeJustifiedBehaviour();
        b.setAbsenceType(absenceType);
        b.setJustifiedBehaviour(justifiedBehaviour);
        b.setData(enumBehaviour.data);
        b.save();
        entitySet.add(b);
        edited = true;
      }
    }
    
    return edited;
  }
 
  /**
   * Costruisce un'absenceType a partire dall'enumerato.
   *
   * @return entity costruita
   */
  public AbsenceType buildAbsenceType(DefaultAbsenceType defaultAbsenceType) {

    AbsenceType absenceType = new AbsenceType();
    absenceType.setCode(defaultAbsenceType.getCode());
    absenceType.setDescription(defaultAbsenceType.description);
    absenceType.setCertificateCode(defaultAbsenceType.certificationCode);
    absenceType.setInternalUse(defaultAbsenceType.internalUse);
    for (JustifiedTypeName justifiedName : defaultAbsenceType.justifiedTypeNamesPermitted) {
      absenceType.getJustifiedTypesPermitted().add(absenceComponentDao
          .getOrBuildJustifiedType(justifiedName));
    }
    absenceType.setJustifiedTime(defaultAbsenceType.justifiedTime);
    absenceType.setConsideredWeekEnd(defaultAbsenceType.consideredWeekEnd);
    absenceType.setMealTicketBehaviour(defaultAbsenceType.mealTicketBehaviour);
    absenceType.setReperibilityCompatible(defaultAbsenceType.reperibilityCompatible);
    absenceType.setReplacingTime(defaultAbsenceType.replacingTime);
    if (defaultAbsenceType.replacingType != null) {
      absenceType.setReplacingType(absenceComponentDao
          .getOrBuildJustifiedType(defaultAbsenceType.replacingType));
    } else {
      absenceType.setReplacingType(null);
    }
    absenceType.setValidFrom(defaultAbsenceType.validFrom);
    absenceType.setValidTo(defaultAbsenceType.validTo);
    absenceType.save();
    updateBehaviourSet(absenceType, absenceType.getJustifiedBehaviours(), 
        defaultAbsenceType.behaviour);
    return absenceType;
  }

}
