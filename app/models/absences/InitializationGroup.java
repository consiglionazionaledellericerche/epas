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

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import models.Person;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import org.testng.collections.Lists;

/**
 * Dati per l'inizializzazione di un gruppo di assenza di un dipendente.
 */
@Audited
@Entity
@Table(name = "initialization_groups")
public class InitializationGroup extends BaseModel {

  private static final long serialVersionUID = -1963061850354314327L;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id", nullable = false)
  public Person person;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "group_absence_type_id", nullable = false)
  public GroupAbsenceType groupAbsenceType;

  @Column(name = "date")
  public LocalDate date;

  @Column(name = "forced_begin")
  public LocalDate forcedBegin;

  @Column(name = "forced_end")
  public LocalDate forcedEnd;

  // if (groupAbsenceType.pattern == programmed)

  @Column(name = "units_input")
  public Integer unitsInput = 0;

  @Column(name = "hours_input")
  public Integer hoursInput = 0;

  @Column(name = "minutes_input")
  public Integer minutesInput = 0;

  @Column(name = "average_week_time")
  public Integer averageWeekTime;

  @Column(name = "takable_total")
  public Integer takableTotal;

  // if (groupAbsenceType.pattern == vacationsCnr)

  @Column(name = "vacation_year")
  public Integer vacationYear;

  //if (groupAbsenceType.pattern == compensatoryRestCnr)

  @Column(name = "residual_minutes_last_year")
  public Integer residualMinutesLastYear;

  @Column(name = "residual_minutes_current_year")
  public Integer residualMinutesCurrentYear;

  /**
   * Constructor.
   *
   * @param person persona
   * @param groupAbsenceType gruppo
   * @param date data
   */
  public InitializationGroup(Person person, GroupAbsenceType groupAbsenceType, LocalDate date) {
    this.person = person;
    this.groupAbsenceType = groupAbsenceType;
    this.date = date;
  }
  
  /**
   * I minuti in input.
   *
   * @return i minuti
   */
  public int inputMinutes() {
    return this.hoursInput * 60 + this.minutesInput;
  }
  
  /**
   * I minuti inseribili.
   *
   * @return list
   */
  public List<Integer> selectableMinutes() {
    List<Integer> hours = Lists.newArrayList();
    for (int i = 0; i <= 59; i++) {
      hours.add(i);
    }
    return hours;
  }

}
