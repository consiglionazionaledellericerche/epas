package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;

import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PropertyInPeriod;

import play.data.validation.Required;


/**
 * Un periodo contrattuale.
 *
 * @author alessandro
 */
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

  @Override
  public Object getValue() {
    return this.workingTimeType;
  }

  @Override
  public void setValue(Object value) {
    this.workingTimeType = (WorkingTimeType)value;
  }

  public IPropertiesInPeriodOwner getOwner() {
    return this.contract;
  }

  public void setOwner(IPropertiesInPeriodOwner target) {
    this.contract = (Contract)target;
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof WorkingTimeType) {
      return this.getValue().equals(((WorkingTimeType)otherValue));
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
