package models;

import com.google.common.collect.Range;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.IPropertiesInPeriodOwner;
import models.base.PropertyInPeriod;

import org.joda.time.LocalDate;

@Entity
@Table(name = "contract_stamp_profiles")
public class ContractStampProfile extends PropertyInPeriod {

  private static final long serialVersionUID = 3503562995113282540L;

  @Column(name = "fixed_working_time")
  public boolean fixedworkingtime;

  @ManyToOne
  @JoinColumn(name = "contract_id", nullable = false)
  public Contract contract;

  /**
   * TODO: questa implementazione andrebbe spostata nel PeriodModel
   * Se la data è contenuta nel periodo.
   * @param date data da verificare
   * @return esito
   */
  public boolean includeDate(LocalDate date) {
    if (beginDate == null && endDate == null) {
      //TODO decidere se considerare l'intervallo infinito, oppure nullo
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

  /**
   * TODO: questa implementazione andrebbe spostata nel PeriodModel.
   * Il range del periodo.
   * @return range
   */
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
  public IPropertiesInPeriodOwner getOwner() {
    return this.contract;
  }

  @Override
  public void setOwner(IPropertiesInPeriodOwner owner) {
    this.contract = (Contract)owner;
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
    return this.fixedworkingtime;
  }

  @Override
  public void setValue(Object value) {
    this.fixedworkingtime = (Boolean)value;

  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof Boolean) {
      return this.getValue() == (Boolean)otherValue;
    }
    return false;
  }

  @Override
  public String getLabel() {
    return this.fixedworkingtime + "";
  }

}
