/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

package models.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.absences.definitions.DefaultCategoryType;
import models.base.BaseModel;
import models.contractual.ContractualClause;
import org.assertj.core.util.Lists;
import org.hibernate.envers.Audited;

/**
 * Associazione tra tipologie di gruppi di assenze e le tab in cui mostrarle
 * nell'interfaccia di gestione delle assenze.
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "category_group_absence_types")
public class CategoryGroupAbsenceType extends BaseModel 
    implements Comparable<CategoryGroupAbsenceType> {

  private static final long serialVersionUID = 4580659910825885894L;

  @Column
  private String name;

  @Column
  private String description;
  
  @Column
  private int priority;
  
  @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
  private Set<GroupAbsenceType> groupAbsenceTypes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_tab_id")
  private CategoryTab tab;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contractual_clause_id")
  private ContractualClause contractualClause;

  @Override
  public int compareTo(CategoryGroupAbsenceType obj) {
    return name.compareTo(obj.name);
  }
  
  /**
   * Un modo (semplificabile) per ordinare i gruppi della categoria per priorità, prendendoli 
   * (se ci sono) dallo heap.
   *
   * @param onlyFirstOfChain se voglio solo i primi della catena.
   */
  @Transient
  public List<GroupAbsenceType> orderedGroupsInCategory(boolean onlyFirstOfChain) {
    
    SortedMap<Integer, Set<GroupAbsenceType>> setByPriority = 
        Maps.newTreeMap();
    
    //ogni gruppo lo inserisco con quelli della stessa priorità
    List<GroupAbsenceType> list = this.groupAbsenceTypes.stream().sorted(Comparator.comparing(GroupAbsenceType::getId)).collect(Collectors.toList());
        
    for (GroupAbsenceType group : list) {
      if (onlyFirstOfChain && !group.getPreviousGroupChecked().isEmpty()) {
        continue;
      }
      Set<GroupAbsenceType> prioritySet = setByPriority.get(group.getPriority());
      if (prioritySet == null) {
        prioritySet = Sets.newHashSet();
        setByPriority.put(group.getPriority(), prioritySet);
      }
      prioritySet.add(group);
    }
    //lista unica ordinata 
    List<GroupAbsenceType> orderedGroupInCateogory = Lists.newArrayList();
    for (Set<GroupAbsenceType> set : setByPriority.values()) {
      orderedGroupInCateogory.addAll(set);
    }
    orderedGroupInCateogory = orderedGroupInCateogory.stream().sorted(Comparator.comparing(GroupAbsenceType::getId)).collect(Collectors.toList());
    return orderedGroupInCateogory;
  }
  
  /**
   * Se esiste fra gli enumerati un corrispondente e se è correttamente modellato.
   *
   * @return absent se la categoria non è presente in enum
   */
  public Optional<Boolean> matchEnum() {
    
    for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
      if (defaultCategory.name().equals(this.name)) {
        if (defaultCategory.description.equals(this.description)
            && defaultCategory.priority == this.priority
            && defaultCategory.categoryTab.name().equals(this.tab.getName())) {
          return Optional.of(true);
        } else {
          return Optional.of(false);
        }
      } 
    }
    
    return Optional.absent();
  }
  
  /**
   * Calcola la lista di tutti i codici prendibili in ogni groupAbsenceType di questa
   * categoria.
   *
   * @return la lista di tutti i codici prendibili in questa categoria.
   */
  @Transient
  public Set<AbsenceType> getAbsenceTypes() {
    return groupAbsenceTypes.stream()
        .flatMap(gat -> gat.getTakableAbsenceBehaviour().getTakableCodes().stream())
        .collect(Collectors.toSet());
  }
  
  /**
   * To String.
   */
  public String toString() {
    return this.description;
  }


}
