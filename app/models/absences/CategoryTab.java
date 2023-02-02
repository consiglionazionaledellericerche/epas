/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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
import com.google.common.collect.Sets;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import models.absences.definitions.DefaultTab;
import models.base.BaseModel;
import org.hibernate.envers.Audited;

/**
 * Categorie di tab da mostrare nel menu per la gestione delle assenze.
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "category_tabs")
public class CategoryTab extends BaseModel implements Comparable<CategoryTab> {

  private static final long serialVersionUID = 4580659910825885894L;

  @Column
  private String name;

  @Column
  private String description;
  
  @Column
  private int priority;
  
  @Column(name = "is_default")
  private boolean isDefault = false;
  
  @OneToMany(mappedBy = "tab", fetch = FetchType.LAZY)
  private Set<CategoryGroupAbsenceType> categoryGroupAbsenceTypes = Sets.newHashSet();

  @Override
  public int compareTo(CategoryTab obj) {
    return name.compareTo(obj.name);
  }
  
  /**
   * Categoria con miglior priorità.
   *
   * @return categoria
   */
  public CategoryGroupAbsenceType firstByPriority() {
    CategoryGroupAbsenceType candidate = categoryGroupAbsenceTypes.iterator().next();
    for (CategoryGroupAbsenceType category : categoryGroupAbsenceTypes) {
      if (candidate.getPriority() > category.getPriority()) {
        candidate = category;
      }
    }
    return candidate;
  }
  
  /**
   * Se esiste fra gli enumerati un corrispondente e se è correttamente modellato.
   *
   * @return absent se la tab non è presente in enum
   */
  public Optional<Boolean> matchEnum() {
    for (DefaultTab defaultTab : DefaultTab.values()) {
      if (defaultTab.name().equals(this.name)) {
        if (defaultTab.description.equals(this.description)
            && defaultTab.priority == this.priority) {
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
