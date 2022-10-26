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
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import models.enumerate.CalculationType;
import org.joda.time.LocalTime;


/**
 * Tabella orario di un turno.
 */
@Getter
@Setter
@Entity
@Table(name = "shift_time_table")
public class ShiftTimeTable extends BaseModel {

  private static final long serialVersionUID = -7869931573320174606L;

  @OneToMany(mappedBy = "shiftTimeTable")
  private List<ShiftType> shiftTypes = new ArrayList<>();

  // start time of morning shift
  @Column(name = "start_morning", columnDefinition = "VARCHAR")
  private LocalTime startMorning;

  // end time of morning shift
  @Column(name = "end_morning", columnDefinition = "VARCHAR")
  private LocalTime endMorning;

  // start time of afternoon shift
  @Column(name = "start_afternoon", columnDefinition = "VARCHAR")
  private LocalTime startAfternoon;

  // end time of afternoon shift
  @Column(name = "end_afternoon", columnDefinition = "VARCHAR")
  private LocalTime endAfternoon;

  @Column(name = "start_evening", columnDefinition = "VARCHAR")
  private LocalTime startEvening;

  @Column(name = "end_evening", columnDefinition = "VARCHAR")
  private LocalTime endEvening;

  // start time for morning lunch break
  @Column(name = "start_morning_lunch_time", columnDefinition = "VARCHAR")
  private LocalTime startMorningLunchTime;

  // end time for the morning lunch break
  @Column(name = "end_morning_lunch_time", columnDefinition = "VARCHAR")
  private LocalTime endMorningLunchTime;

  // start time for the lunch break
  @Column(name = "start_afternoon_lunch_time", columnDefinition = "VARCHAR")
  private LocalTime startAfternoonLunchTime;

  // end time for the lunch break
  @Column(name = "end_afternoon_lunch_time", columnDefinition = "VARCHAR")
  private LocalTime endAfternoonLunchTime;

  // start time for the lunch break
  @Column(name = "start_evening_lunch_time", columnDefinition = "VARCHAR")
  private LocalTime startEveningLunchTime;

  // end time for the lunch break
  @Column(name = "end_evening_lunch_time", columnDefinition = "VARCHAR")
  private LocalTime endEveningLunchTime;

  // total amount of working minutes
  @Column(name = "total_working_minutes")
  private Integer totalWorkMinutes;

  // Paid minuts per shift
  @Column(name = "paid_minutes")
  private Integer paidMinutes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id")
  private Office office;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "calculation_type")
  private CalculationType calculationType;

  /**
   * Quanti slot ci sono nella timetable.
   *
   * @return la quantità di slot presenti nella timetable.
   */
  @Transient
  public int slotCount() {
    int slots = 0;
    if (startMorning != null && endMorning != null) {
      slots++;
    }
    if (startAfternoon != null && endAfternoon != null) {
      slots++;
    }
    if (startEvening != null && endEvening != null) {
      slots++;
    }
    return slots;
  }
}
