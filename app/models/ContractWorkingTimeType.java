package models;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import models.base.IPeriodTarget;
import models.base.IPeriodValue;
import models.base.PeriodModel;

import org.joda.time.LocalDate;

import play.data.validation.Required;

import java.util.ArrayList;
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
  @ManyToOne(fetch=FetchType.LAZY)
  @JoinColumn(name="contract_id")
  public Contract contract;

  @Required
  @ManyToOne(fetch=FetchType.LAZY)
  @JoinColumn(name="working_time_type_id")
  public WorkingTimeType workingTimeType;

  @Required
  @Column(name="begin_date")
  public LocalDate beginDate;

  @Column(name="end_date")
  public LocalDate endDate;

  @Override
  public LocalDate getBegin() {
    return this.beginDate;
  }
  
  @Override
  public void setBegin(LocalDate begin) {
    this.beginDate = begin;
    
  }

  @Override
  public Optional<LocalDate> getEnd() {
    return Optional.fromNullable(this.endDate);
  }

  // FIXME: non riesco a impostare il generico Optional<LocalDate>
  @Override
  public void setEnd(Optional end) {
    if (end.isPresent()) {
      this.endDate = (LocalDate)end.get();
    } else {
      this.endDate = null;
    }
  }
  
  @Override
  public IPeriodValue getValue() {
    return this.workingTimeType;
  }

  @Override
  public List<PeriodModel> orderedPeriods() {
    List<PeriodModel> list = Lists.newArrayList();
    for (ContractWorkingTimeType cwtt : this.contract.contractWorkingTimeType) {
      list.add(cwtt);
    }
    Collections.sort(list);
    return list;
  }
  
  @Override
  public void setValue(IPeriodValue value) {
    this.workingTimeType = (WorkingTimeType)value;
  }

  @Override
  public PeriodModel newInstance() {
    return new ContractWorkingTimeType();
  }

  @Override
  public IPeriodTarget getTarget() {
    return this.contract;
  }

  @Override
  public void setTarget(IPeriodTarget target) {
    this.contract = (Contract)target;
    
  }


}
