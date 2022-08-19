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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Getter;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import play.data.validation.Required;

/**
 * Entità di check green pass.
 *
 * @author dario
 *
 */
@Entity
@Audited
public class CheckGreenPass extends BaseModel {
  
  private static final long serialVersionUID = 4909012051833782360L;

  public LocalDate checkDate;
  
  @Getter
  @Required
  @ManyToOne(optional = false)
  @JoinColumn(name = "person_id", nullable = false)
  public Person person;
  
  public boolean checked;
}
