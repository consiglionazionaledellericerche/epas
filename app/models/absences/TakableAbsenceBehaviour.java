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
import models.absences.definitions.DefaultTakable;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.testng.collections.Sets;

/**
 * Rappresenta il comportamento (codici presi, codici prendibili, etc) delle assenze prendibili.
 *
 * @author Alessandro Martelli
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "takable_absence_behaviours")
public class TakableAbsenceBehaviour extends BaseModel {

  private static final long serialVersionUID = 486763865630858142L;
  public static final String NAME_PREFIX = "T_";

  @Column(name = "name")
  private String name;
  
  @OneToMany(mappedBy = "takableAbsenceBehaviour", fetch = FetchType.LAZY)
  private Set<GroupAbsenceType> groupAbsenceTypes = Sets.newHashSet();
  
  @Column(name = "amount_type")
  @Enumerated(EnumType.STRING)
  private AmountType amountType;
  
  @ManyToMany
  @JoinTable(name = "taken_codes_group", 
      joinColumns = { @JoinColumn(name = "takable_behaviour_id") }, 
        inverseJoinColumns = { @JoinColumn(name = "absence_types_id") })
  @OrderBy("code")
  private Set<AbsenceType> takenCodes = Sets.newHashSet();
  
  //  @Column(name = "takable_count_behaviour")
  //  @Enumerated(EnumType.STRING)
  //  public TakeCountBehaviour takableCountBehaviour;
  
  @ManyToMany
  @JoinTable(name = "takable_codes_group", 
      joinColumns = { @JoinColumn(name = "takable_behaviour_id") }, 
      inverseJoinColumns = { @JoinColumn(name = "absence_types_id") })
  @OrderBy("code")
  private Set<AbsenceType> takableCodes = Sets.newHashSet();
  
  @Column(name = "fixed_limit")
  private Integer fixedLimit;
  
  @Column(name = "takable_amount_adjust")
  @Enumerated(EnumType.STRING)
  private TakeAmountAdjustment takableAmountAdjustment;
 
  /**
   * Tipologia di comportamento per i codici di assenza presi. 
   *
   */
  public enum TakeCountBehaviour {
    period, sumAllPeriod, sumUntilPeriod; 
  }
  
  /**
   * Tipologie di aggiustamenti per il tempo totale dei codici di assenza.
   */
  public enum TakeAmountAdjustment {
    workingTimePercent(true, false),
    workingPeriodPercent(false, true),
    workingTimeAndWorkingPeriodPercent(true, true);
    
    public boolean workTime;
    public boolean periodTime;

    private TakeAmountAdjustment(boolean workTime, boolean periodTime) {
      this.workTime = workTime;
      this.periodTime = periodTime;
    }
  }
  
  /**
   * Se esiste fra gli enumerati un corrispondente e se è correttamente modellato.
   *
   * @return absent se il completamento non è presente in enum
   */
  public Optional<Boolean> matchEnum() {
    for (DefaultTakable defaultTakable : DefaultTakable.values()) {
      if (defaultTakable.name().equals(this.name)) {
        if (!defaultTakable.amountType.equals(this.amountType)) {
          return Optional.of(false);
        }
        if (defaultTakable.fixedLimit != this.fixedLimit) {
          return Optional.of(false); 
        }
        if (!ComplationAbsenceBehaviour.matchTypes(defaultTakable.takenCodes, this.takenCodes)) {
          return Optional.of(false);
        }
        if (!ComplationAbsenceBehaviour
            .matchTypes(defaultTakable.takableCodes, this.takableCodes)) {
          return Optional.of(false);
        }
        
        //campi nullable adjustment
        if (defaultTakable.takableAmountAdjustment == null) {
          if (this.takableAmountAdjustment != null) {
            return Optional.of(false);
          }
        } else {
          if (!defaultTakable.takableAmountAdjustment.equals(this.takableAmountAdjustment)) {
            return Optional.of(false);
          }
        }

        return Optional.of(true);
      } 
    }
    return Optional.absent();
  }

}
