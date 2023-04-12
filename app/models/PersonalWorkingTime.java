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
 * Entità di associazione tra persona e orario di lavoro particolare.
 *
 * @author dario
 *
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "personal_working_times")
public class PersonalWorkingTime extends PropertyInPeriod implements IPropertyInPeriod {
  
  private static final long serialVersionUID = 98286754527639967L;
  
  @Getter
  @Required
  @ManyToOne
  @JoinColumn(name = "time_slot_id")
  private TimeSlot timeSlot;
  
  @Getter
  @Required
  @ManyToOne
  @JoinColumn(name = "contract_id")
  private Contract contract;
  
    
  @Override
  public IPropertiesInPeriodOwner getOwner() {    
    return this.contract;
  }

  @Override
  public void setOwner(IPropertiesInPeriodOwner target) {
    this.contract = (Contract) target;    
  }

  @Override
  public Object getType() {
    return this.getClass();
  }

  @Override
  public void setType(Object value) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Object getValue() {
    
    return this.timeSlot;
  }

  @Override
  public void setValue(Object value) {
    this.timeSlot = (TimeSlot) value;
    
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof TimeSlot) {
      return this.getValue().equals(((TimeSlot) otherValue));
    }
    return false;
  }
  
  @Override
  public String getLabel() {
    return this.timeSlot.getLabel();
  }
  
  
}
