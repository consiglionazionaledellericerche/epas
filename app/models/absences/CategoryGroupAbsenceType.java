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

import models.absences.definitions.DefaultCategoryType;
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


}
