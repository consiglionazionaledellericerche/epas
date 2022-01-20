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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.util.Strings;
import lombok.Getter;
import lombok.ToString;
import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PropertyInPeriod;
import play.data.validation.Required;

@ToString
@Entity
@Table(name = "personal_working_times")
public class PersonalWorkingTime extends PropertyInPeriod implements IPropertyInPeriod {
  
  public String description;
  
  public String workingTime;
  
  @Getter
  @Required
  @ManyToOne
  @JoinColumn(name = "contract_id")
  public Contract contract;
  
    
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
    
    return this.workingTime;
  }

  @Override
  public void setValue(Object value) {
    this.workingTime = (String) value;
    
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof String) {
      return this.getValue().equals(((String) otherValue));
    }
    return false;
  }
  
  @Override
  public String getLabel() {
    return Strings.isNullOrEmpty(description) ? String.format("%s", workingTime) 
        : 
      String.format("%s (%s)", description, workingTime);
  }

}
