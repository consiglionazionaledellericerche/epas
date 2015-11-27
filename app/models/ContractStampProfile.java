package models;

import com.google.common.collect.Range;

import models.base.BaseModel;
import models.base.IPeriodTarget;
import models.base.PeriodModel;

import org.joda.time.LocalDate;

import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "contract_stamp_profiles")
public class ContractStampProfile extends PeriodModel {

  private static final long serialVersionUID = 3503562995113282540L;

  @Column(name = "fixed_working_time")
  public boolean fixedworkingtime;

  @ManyToOne
  @JoinColumn(name = "contract_id", nullable = false)
  public Contract contract;

  public boolean includeDate(LocalDate date) {
    if (beginDate == null && endDate == null) {
//			TODO decidere se considerare l'intervallo infinito, oppure nullo
      return false;
    }
    if (beginDate == null) {
      return !endDate.isAfter(date);
    }
    if (endDate == null) {
      return !beginDate.isBefore(date);
    }
    return !beginDate.isBefore(date) && !endDate.isAfter(date);
  }

  public Range<LocalDate> dateRange() {
    if (beginDate == null && endDate == null) {
      return Range.all();
    }
    if (beginDate == null) {
      return Range.atMost(endDate);
    }
    if (endDate == null) {
      return Range.atLeast(beginDate);
    }
    return Range.closed(beginDate, endDate);
  }

  @Override
  public PeriodModel getPeriod() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IPeriodTarget getTarget() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setTarget(IPeriodTarget target) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Object getValue() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setValue(Object value) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Collection periods() {
    // TODO Auto-generated method stub
    return null;
  }

}
