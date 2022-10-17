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

package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.data.validation.Max;
import play.data.validation.Min;

/**
 * Tipologia di turno.
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "shift_type")
public class ShiftType extends BaseModel {

  private static final long serialVersionUID = 3156856871540530483L;

  
  private String type;
  
  private String description;
  
  @Column(name = "allow_unpair_slots")
  private boolean allowUnpairSlots = false;
  
  @Min(0)
  @Column(name = "entrance_tolerance")
  private int entranceTolerance;
  
  @Min(0)
  @Column(name = "entrance_max_tolerance")
  private int entranceMaxTolerance;

  @Min(0)
  @Column(name = "exit_tolerance")
  private int exitTolerance;
  
  @Min(0)
  @Column(name = "exit_max_tolerance")
  private int exitMaxTolerance;
  
  //quantità massima di tolleranze concesse all'interno dell'attività
  @Max(3)
  @Min(0)
  @Column(name = "max_tolerance_allowed")
  private int maxToleranceAllowed;

  @Min(0)
  @Column(name = "break_in_shift")
  private int breakInShift;
  
  @Min(0)
  @Column(name = "break_max_in_shift")
  private int breakMaxInShift;

  @NotAudited
  @OneToMany(mappedBy = "shiftType")
  private List<PersonShiftShiftType> personShiftShiftTypes = new ArrayList<>();

  @NotAudited
  @OneToMany(mappedBy = "shiftType")
  private List<PersonShiftDay> personShiftDays = new ArrayList<>();

  @NotAudited
  @OneToMany(mappedBy = "type")
  private List<ShiftCancelled> shiftCancelled = new ArrayList<>();

  @NotAudited
  @ManyToOne
  @JoinColumn(name = "shift_time_table_id")
  private ShiftTimeTable shiftTimeTable;

  @NotAudited
  @ManyToOne
  @JoinColumn(name = "organization_shift_time_table_id")
  private OrganizationShiftTimeTable organizaionShiftTimeTable;
  
  //@Required
  @ManyToOne(optional = false)
  @JoinColumn(name = "shift_categories_id")
  private ShiftCategories shiftCategories;

  @OneToMany(mappedBy = "shiftType", cascade = CascadeType.REMOVE)
  @OrderBy("yearMonth DESC")
  private Set<ShiftTypeMonth> monthsStatus = new HashSet<>();

  @Override
  public String toString() {
    return shiftCategories.getDescription() + " - " + type;
  }

  /**
   * Tipologia di tolleranza.
   */
  public enum ToleranceType {
    entrance("entrance"),
    exit("exit"),
    both("both");

    public String description;

    ToleranceType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * RItorna l'oggetto che contiene l'approvazione del turno ad una certa data.
   *
   * @param date la data da considerare
   * @return l'oggetto che contiene l'approvazione del turno ad una certa data.
   */
  @Transient
  public Optional<ShiftTypeMonth> monthStatusByDate(LocalDate date) {
    final YearMonth requestedMonth = new YearMonth(date);
    return monthsStatus.stream()
        .filter(shiftTypeMonth -> shiftTypeMonth.getYearMonth().equals(requestedMonth)).findFirst();
  }

  /**
   * Controlla se il turno è stato approvato alla data passata come parametro.
   *
   * @param date la data da considerare
   * @return true se il turno è stato approvato alla data date, false altrimenti.
   */
  @Transient
  public boolean approvedOn(LocalDate date) {
    Optional<ShiftTypeMonth> monthStatus = monthStatusByDate(date);
    if (monthStatus.isPresent()) {
      return monthStatus.get().isApproved();
    } else {
      return false;
    }
  }

}
