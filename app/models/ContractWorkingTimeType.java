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

import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import lombok.Getter;
import lombok.ToString;
import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PropertyInPeriod;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import play.data.validation.Required;


/**
 * Un periodo contrattuale.
 *
 * @author Alessandro Martelli
 */
@ToString
@Audited
@Entity
@Table(name = "contracts_working_time_types")
public class ContractWorkingTimeType extends PropertyInPeriod implements IPropertyInPeriod {

  private static final long serialVersionUID = 3730183716240278997L;

  @Getter
  @Required
  @ManyToOne
  @JoinColumn(name = "contract_id")
  public Contract contract;

  @Getter
  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "working_time_type_id")
  public WorkingTimeType workingTimeType;

  @NotAudited
  public LocalDateTime updatedAt;

  public String externalId;

  @PreUpdate
  @PrePersist
  private void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  @Override
  public Object getValue() {
    return this.workingTimeType;
  }

  @Override
  public void setValue(Object value) {
    this.workingTimeType = (WorkingTimeType) value;
  }

  public IPropertiesInPeriodOwner getOwner() {
    return this.contract;
  }

  public void setOwner(IPropertiesInPeriodOwner target) {
    this.contract = (Contract) target;
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof WorkingTimeType) {
      return this.getValue().equals(((WorkingTimeType) otherValue));
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
    return this.workingTimeType.description;
  }


}
