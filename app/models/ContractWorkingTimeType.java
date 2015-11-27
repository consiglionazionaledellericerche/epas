package models;

import models.base.IPeriodTarget;
import models.base.PeriodModel;

import play.data.validation.Required;

import java.util.Collection;

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
public class ContractWorkingTimeType extends PeriodModel {

  private static final long serialVersionUID = 3730183716240278997L;

  @Required
  @ManyToOne(fetch = FetchType.LAZY)
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
  public Collection<ContractWorkingTimeType> periods() {
    return this.contract.contractWorkingTimeType;
  }
  
  @Override
  public void setValue(Object value) {
    this.workingTimeType = (WorkingTimeType)value;
  }
  
  @Override
  public IPeriodTarget getTarget() {
    return this.contract;
  }

  @Override
  public void setTarget(IPeriodTarget target) {
    this.contract = (Contract)target;
  }

  @Override
  public ContractWorkingTimeType getPeriod() {
    return this;
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof ContractWorkingTimeType) {
      return this.workingTimeType.id == (((ContractWorkingTimeType) otherValue).workingTimeType.id); 
    }
    return false;
  }


}
