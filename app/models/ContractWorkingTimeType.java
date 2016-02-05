package models;

import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PropertyInPeriod;

import play.data.validation.Required;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Un periodo contrattuale.
 *
 * @author alessandro
 */
@Entity
@Table(name = "contracts_working_time_types")
public class ContractWorkingTimeType extends PropertyInPeriod implements IPropertyInPeriod {

  private static final long serialVersionUID = 3730183716240278997L;

  @Required
  @ManyToOne
  @JoinColumn(name = "contract_id")
  public Contract contract;

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

  public void setOwner(IPropertiesInPeriodOwner owner) {
    this.contract = (Contract)owner;
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof ContractWorkingTimeType) {
      return this.workingTimeType.id == (((ContractWorkingTimeType) otherValue).workingTimeType.id);
    }
    return false;
  }


  /* (non-Javadoc)
   * @see models.base.IPropertyInPeriod#getType()
   */
  @Override
  public Object getType() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see models.base.IPropertyInPeriod#setType(java.lang.Object)
   */
  @Override
  public void setType(Object value) {
    // TODO Auto-generated method stub
  }
  
  @Override
  public String getLabel() {
    return this.workingTimeType.description;
  }


}
