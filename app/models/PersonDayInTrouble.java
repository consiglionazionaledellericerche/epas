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

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import models.enumerate.Troubles;
import org.hibernate.envers.Audited;


/**
 * Giornata lavorativa con qualche problema.
 */
@Getter
@Setter
@Audited
@Entity
public class PersonDayInTrouble extends BaseModel {

  private static final long serialVersionUID = 4802468368796902865L;

  @Enumerated(EnumType.STRING)
  private Troubles cause;

  private boolean emailSent;

  @NotNull
  @ManyToOne
  @JoinColumn(updatable = false)
  private PersonDay personDay;

  /**
   * Costruttore.
   *
   * @param pd il personday
   * @param cause la causa del trouble
   */
  public PersonDayInTrouble(PersonDay pd, Troubles cause) {
    this.personDay = pd;
    this.cause = cause;
    this.emailSent = false;
  }
}
