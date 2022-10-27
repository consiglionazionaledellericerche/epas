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

import it.cnr.iit.epas.DateInterval;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PropertyInPeriod;
import models.enumerate.VacationCode;
import org.hibernate.envers.Audited;
import play.data.validation.Required;

/**
 * Un periodo piani ferie.
 *
 * @author Alessandro Martelli
 */
@Getter
@Setter
@Entity
@Table(name = "vacation_periods")
@Audited
public class VacationPeriod extends PropertyInPeriod implements IPropertyInPeriod {

  private static final long serialVersionUID = 7082224747753675170L;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "vacation_code")
  @Required
  private VacationCode vacationCode;

  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contract_id", nullable = false, updatable = false)
  private Contract contract;
  
  @Transient
  @Deprecated
  public DateInterval getDateInterval() {
    return new DateInterval(this.getBeginDate(), this.getEndDate());
  }

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
    // questo metodo in questo caso non serve, perchè i periods sono tutti dello stesso tipo.
  }

  @Override
  public Object getValue() {
    return this.vacationCode;
  }

  @Override
  public void setValue(Object value) {
    this.vacationCode = (VacationCode) value;
    
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof VacationCode) {
      return this.getValue().equals(((VacationCode) otherValue));
    }
    return false;
  }
  
  @Override
  public String getLabel() {
    return this.vacationCode.name;
  }
  
  public String toString() {
    return "[" + getBeginDate() + "," + getEndDate() + "] " + vacationCode.name;
  }
 
}
