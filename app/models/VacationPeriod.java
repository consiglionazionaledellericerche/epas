package models;


import it.cnr.iit.epas.DateInterval;

import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PropertyInPeriod;

import play.data.validation.Required;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Un periodo piani ferie.
 * 
 * @author alessandro
 */
@Entity
@Table(name = "vacation_periods")
public class VacationPeriod extends PropertyInPeriod implements IPropertyInPeriod {

  private static final long serialVersionUID = 7082224747753675170L;

  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vacation_codes_id", nullable = false)
  public VacationCode vacationCode;


  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contract_id", nullable = false, updatable = false)
  public Contract contract;
  
  @Transient
  @Deprecated
  public DateInterval getDateInterval(){
    return new DateInterval(this.beginDate, this.endDate);
  }

  @Override
  public IPropertiesInPeriodOwner getOwner() {
    return this.contract;
  }

  @Override
  public void setOwner(IPropertiesInPeriodOwner target) {
    this.contract = (Contract)target;
    
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
    this.vacationCode = (VacationCode)value;
    
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof VacationCode) {
      return this.getValue().equals(((VacationCode)otherValue));
    }
    return false;
  }
  
  @Override
  public String getLabel() {
    return this.vacationCode.description;
  }
 
}
