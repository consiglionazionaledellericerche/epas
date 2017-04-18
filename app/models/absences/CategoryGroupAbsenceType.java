package models.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.absences.CategoryTab.DefaultTab;
import models.base.BaseModel;

import org.assertj.core.util.Lists;
import org.hibernate.envers.Audited;

@Audited
@Entity
@Table(name = "category_group_absence_types")
public class CategoryGroupAbsenceType extends BaseModel 
    implements Comparable<CategoryGroupAbsenceType> {

  private static final long serialVersionUID = 4580659910825885894L;

  @Column
  public String name;

  @Column
  public String description;
  
  @Column
  public int priority;
  
  @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
  public Set<GroupAbsenceType> groupAbsenceTypes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_tab_id")
  public CategoryTab tab;
  
  @Override
  public int compareTo(CategoryGroupAbsenceType obj) {
    return name.compareTo(obj.name);
  }
  
  /**
   * Un modo (semplificabile) per ordinare i gruppi della categoria per priorità, prendendoli 
   * (se ci sono) dallo heap.
   * @param onlyFirstOfChain se voglio solo i primi della catena.
   */
  @Transient
  public List<GroupAbsenceType> orderedGroupsInCategory(boolean onlyFirstOfChain) {
    
    SortedMap<Integer, Set<GroupAbsenceType>> setByPriority = 
        Maps.newTreeMap();
    
    //ogni gruppo lo inserisco con quelli della stessa priorità
    for (GroupAbsenceType group : this.groupAbsenceTypes) {
      if (onlyFirstOfChain && !group.previousGroupChecked.isEmpty()) {
        continue;
      }
      Set<GroupAbsenceType> prioritySet = setByPriority.get(group.priority);
      if (prioritySet == null) {
        prioritySet = Sets.newHashSet();
        setByPriority.put(group.priority, prioritySet);
      }
      prioritySet.add(group);
    }
    //lista unica ordinata 
    List<GroupAbsenceType> orderedGroupInCateogory = Lists.newArrayList();
    for (Set<GroupAbsenceType> set : setByPriority.values()) {
      orderedGroupInCateogory.addAll(set);
    }
    return orderedGroupInCateogory;
  }
  
  /**
   * Se esiste fra gli enumerati un corrispondente e se è correttamente modellato.
   * @return absent se la categoria non è presente in enum
   */
  public Optional<Boolean> matchEnum() {
    
    for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
      if (defaultCategory.name().equals(this.name)) {
        if (defaultCategory.description.equals(this.description)
            && defaultCategory.priority == this.priority
            && defaultCategory.categoryTab.name().equals(this.tab.name)) {
          return Optional.of(true);
        } else {
          return Optional.of(false);
        }
      } 
    }
    
    return Optional.absent();
  }
  
  /**
   * To String.
   */
  public String toString() {
    return this.description;
  }
  
  /**
   * Le categorie di default.
   * 
   * @author alessandro
   *
   */
  public enum DefaultCategoryType {

    MISSIONE_CNR("Missioni CNR", 1, DefaultTab.MISSIONE), 
    FERIE_CNR("Ferie e permessi legge", 2, DefaultTab.FERIE_PERMESSI_RIPOSI),
    RIPOSI_COMPENSATIVI_CNR("Riposi compensativi", 3, DefaultTab.FERIE_PERMESSI_RIPOSI),
    
    ASTENSIONE_POSTPARTUM("Astensione post partum", 5, DefaultTab.CONGEDI_PARENTALI),
    CONGEDI_PRENATALI("Congedi prenatali", 7, DefaultTab.CONGEDI_PARENTALI),
    
    L_104("Disabilità legge 104/92 - Tre giorni mensili", 6, DefaultTab.LEGGE_104),
    PERMESSI_PROVVISORI_104("Permessi Provvisori legge 104/92", 7, DefaultTab.LEGGE_104),
    ALTRI_104("Altri congedi legge 104/92", 8, DefaultTab.LEGGE_104),
    
    MALATTIA_DIPENDENTE("Malattia dipendente", 8, DefaultTab.MALATTIA),
    VISITA_MEDICA("Visita medica", 9, DefaultTab.MALATTIA),
    
    MALATTIA_FIGLIO_1("Malattia primo figlio", 9, DefaultTab.CONGEDI_PARENTALI),
    MALATTIA_FIGLIO_2("Malattia secondo figlio", 10, DefaultTab.CONGEDI_PARENTALI),
    MALATTIA_FIGLIO_3("Malattia terzo figlio", 11, DefaultTab.CONGEDI_PARENTALI),
    
    CONGEDO_MATRIMONIO("Congedo per matrimonio", 12, 
        DefaultTab.CONGEDI_PARENTALI),


    
    PERMESSI_PERSONALI("Permessi Personali", 12, DefaultTab.FERIE_PERMESSI_RIPOSI),
    //DIRITTO_STUDIO("Diritto allo studio", 13, DefaultTab.ALTRE_TIPOLOGIE),
    //TELELAVORO("Telelavoro", 14, DefaultTab.ALTRI_GRUPPI),
    //PERMESSI_SINDACALI("Permessi Sindacali", 15, DefaultTab.ALTRI_GRUPPI),
    ALTRI_CODICI("Altri Codici", 16, DefaultTab.ALTRI_CODICI),
    //TUTTI_I_CODICI("Tutti i codici", 16, DefaultTab.ALTRE_TIPOLOGIE),

    LAVORO_FUORI_SEDE("Lavoro fuori sede", 17, DefaultTab.LAVORO_FUORI_SEDE),
    FERIE_DIPENDENTI("Ferie e permessi legge", 18, DefaultTab.FERIE_DIPENDENTI),
    
    CODICI_AUTOMATICI("Codici Automatici", 18, DefaultTab.AUTOMATICI);

    public String description;
    public int priority;
    public DefaultTab categoryTab;

    private DefaultCategoryType(String description, int priority, DefaultTab categoryTab) {
      this.description = description;
      this.priority = priority;
      this.categoryTab = categoryTab;
    }
    
    /**
     * Ricerca le categorie modellate e non presenti fra quelle passate in arg (db). 
     * @return list
     */
    public static List<DefaultCategoryType> missing(List<CategoryGroupAbsenceType> allCategories) {
      List<DefaultCategoryType> missing = Lists.newArrayList();
      for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
        boolean found = false;
        for (CategoryGroupAbsenceType category : allCategories) {
          if (defaultCategory.name().equals(category.name)) {
            found = true;
            break;
          }
        }
        if (!found) {
          missing.add(defaultCategory);
        }
      }
      return missing;
    }
    
    /**
     * L'enumerato corrispettivo della categoria (se esiste...) 
     * @return optional dell'enumerato
     */
    public static Optional<DefaultCategoryType> byName(CategoryGroupAbsenceType category) {
      for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
        if (defaultCategory.name().equals(category.name)) {
          return Optional.of(defaultCategory);
        }
      }
      return Optional.absent();
    }
  }


}
