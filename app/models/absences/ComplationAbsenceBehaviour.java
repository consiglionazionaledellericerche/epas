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
import com.google.common.collect.Sets;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import models.absences.definitions.DefaultAbsenceType;
import models.absences.definitions.DefaultComplation;
import models.base.BaseModel;
import org.hibernate.envers.Audited;

/**
 * Modella il comportamente delle assenze con completamento dell'orario di lavoro.
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "complation_absence_behaviours")
public class ComplationAbsenceBehaviour extends BaseModel {

  private static final long serialVersionUID = 3990946316183363917L;
  public static final String NAME_PREFIX = "C_";

  @Column(name = "name")
  private String name;
  
  @OneToMany(mappedBy = "complationAbsenceBehaviour", fetch = FetchType.LAZY)
  private Set<GroupAbsenceType> groupAbsenceTypes = Sets.newHashSet();
  
  @Column(name = "amount_type")
  @Enumerated(EnumType.STRING)
  private AmountType amountType;

  @ManyToMany
  @JoinTable(name = "complation_codes_group", 
        joinColumns = { @JoinColumn(name = "complation_behaviour_id") }, 
        inverseJoinColumns = { @JoinColumn(name = "absence_types_id") })
  @OrderBy("code")
  private Set<AbsenceType> complationCodes = Sets.newHashSet();

  @ManyToMany
  @JoinTable(name = "replacing_codes_group", 
        joinColumns = { @JoinColumn(name = "complation_behaviour_id") }, 
        inverseJoinColumns = { @JoinColumn(name = "absence_types_id") })
  @OrderBy("code")
  private Set<AbsenceType> replacingCodes = Sets.newHashSet();

  /**
   * Se esiste fra gli enumerati un corrispondente e se è correttamente modellato.
   *
   * @return absent se il completamento non è presente in enum
   */
  public Optional<Boolean> matchEnum() {
    for (DefaultComplation defaultComplation : DefaultComplation.values()) {
      if (defaultComplation.name().equals(this.name)) {
        if (!defaultComplation.amountType.equals(this.amountType)) {
          return Optional.of(false);
        }
        if (!matchTypes(defaultComplation.replacingCodes, this.replacingCodes)) {
          return Optional.of(false);
        }
        if (!matchTypes(defaultComplation.complationCodes, this.complationCodes)) {
          return Optional.of(false);
        }

        return Optional.of(true);
      } 
    }
    return Optional.absent();
  }

  /**
   * Confronta le due liste.
   *
   * @return se le due liste contengono gli stessi codici
   */
  public static boolean matchTypes(Set<DefaultAbsenceType> enumSet, Set<AbsenceType> set) {
    
    if (enumSet.size() != set.size()) {
      return false;
    }
    Set<String> codes1 = Sets.newHashSet();
    for (DefaultAbsenceType defaultType : enumSet) {
      codes1.add(defaultType.getCode());
    }
    Set<String> codes2 = Sets.newHashSet();
    for (AbsenceType type : set) {
      codes2.add(type.getCode());
    }
    for (String code : codes1) {
      if (!codes2.contains(code)) {
        return false;
      }
    }
    return true;
  }

}
