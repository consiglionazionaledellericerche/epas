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

import com.google.common.collect.Range;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.joda.time.LocalDate;
import play.data.validation.Required;


/**
 * Associazione tra persone e tipi di turno (con date di inizio e fine).
 *
 * @author Cristian Lucchesi
 * @author Arianna Del Soldato
 */
@Getter
@Setter
@Entity
@Table(name = "person_shift_shift_type")
public class PersonShiftShiftType extends BaseModel {

  private static final long serialVersionUID = -4476838239881674080L;

  @Required
  @ManyToOne
  @JoinColumn(name = "personshifts_id")
  private PersonShift personShift;

  @Required
  @ManyToOne
  @JoinColumn(name = "shifttypes_id")
  private ShiftType shiftType;


  @Column(name = "begin_date")
  private LocalDate beginDate;


  @Column(name = "end_date")
  private LocalDate endDate;

  private boolean jolly;

  /**
   * Il range di date di appartenenza della persona all'attività.
   *
   * @return il range di date di appartenenza della persona all'attività.
   */
  @Transient
  public Range<LocalDate> dateRange() {
    if (beginDate == null && endDate == null) {
      return Range.all();
    }
    if (beginDate == null) {
      return Range.atMost(endDate);
    }
    if (endDate == null) {
      return Range.atLeast(beginDate);
    }
    return Range.closed(beginDate, endDate);
  }
}
