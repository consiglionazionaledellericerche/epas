package models;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import models.base.IPeriodTarget;
import models.base.IPeriodValue;
import models.base.PeriodModel;

import org.joda.time.LocalDate;

import play.data.validation.Required;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;


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
  public IPeriodValue getValue() {
    return this.workingTimeType;
  }

  @Override
  public Collection<ContractWorkingTimeType> periods() {
    return this.contract.contractWorkingTimeType;
  }
  
  @Override
  public void setValue(IPeriodValue value) {
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


}
