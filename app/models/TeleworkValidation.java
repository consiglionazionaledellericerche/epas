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

package models;

import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;


/**
 * Informazioni sull'approvazione mensile delle attività in telelavoro.
 *
 * @author Dario Tagliaferri
 */
@Getter
@Setter
@Entity
@Table(name = "telework_validations")
@Audited
public class TeleworkValidation extends BaseModel {

  private static final long serialVersionUID = -4472102414284745470L;
  
  @ManyToOne(fetch = FetchType.LAZY)
  private Person person;
  
  private int year;
  
  private int month;
  
  private boolean approved;
  
  private LocalDate approvationDate;
  
  /**
   * Verifica che il telelavoro sia stato validato.
   *
   * @return se la validazione del telelavoro è presente o meno.
   */
  @Transient
  public boolean isValidated() {
    if (this.approved && this.approvationDate != null) {
      return true;
    }
    return false;
  }
}
