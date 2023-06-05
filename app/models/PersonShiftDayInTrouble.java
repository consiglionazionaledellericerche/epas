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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import models.enumerate.ShiftTroubles;
import org.hibernate.envers.Audited;

/**
 * Problemi su una giornata di turno di un dipendente.
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "person_shift_day_in_trouble", uniqueConstraints = @UniqueConstraint(columnNames = {
    "person_shift_day_id", "cause"}))
@EqualsAndHashCode(callSuper = false, of = {"personShiftDay", "cause"})
public class PersonShiftDayInTrouble extends BaseModel {

  private static final long serialVersionUID = -5497453685568298051L;

  @ManyToOne
  @JoinColumn(name = "person_shift_day_id", nullable = false, updatable = false)
  private PersonShiftDay personShiftDay;

  @Enumerated(EnumType.STRING)
  private ShiftTroubles cause;

  @Column(name = "email_sent")
  private boolean emailSent;

  public PersonShiftDayInTrouble(PersonShiftDay pd, ShiftTroubles cause) {
    personShiftDay = pd;
    this.cause = cause;
  }
}
