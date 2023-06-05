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
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PropertyInPeriod;
import play.data.validation.Required;


/**
 * Un periodo di fascia oraria di presenza obbligatoria.
 *
 * @author Cristian Lucchesi
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "contract_mandatory_time_slots")
public class ContractMandatoryTimeSlot extends PropertyInPeriod implements IPropertyInPeriod {

  private static final long serialVersionUID = 98286759517639967L;

  @Getter
  @Required
  @ManyToOne
  @JoinColumn(name = "contract_id")
  private Contract contract;

  @Getter
  @Required
  @ManyToOne
  @JoinColumn(name = "time_slot_id")
  private TimeSlot timeSlot;

  @Override
  public Object getValue() {
    return this.timeSlot;
  }

  @Override
  public void setValue(Object value) {
    this.timeSlot = (TimeSlot) value;
  }

  public IPropertiesInPeriodOwner getOwner() {
    return this.contract;
  }

  public void setOwner(IPropertiesInPeriodOwner target) {
    this.contract = (Contract) target;
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof TimeSlot) {
      return this.getValue().equals(((TimeSlot) otherValue));
    }
    return false;
  }

  @Override
  public Object getType() {
    return this.getClass();
  }

  @Override
  public void setType(Object value) {
    // questo metodo in questo caso non serve, perchè i periods sono tutti dello stesso tipo.
  }
  
  @Override
  public String getLabel() {
    return this.timeSlot.getLabel();
  }

}