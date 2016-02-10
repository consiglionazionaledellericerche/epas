package models;

import models.base.BaseModel;
import models.base.IPropertiesInPeriodOwner;
import models.base.PropertyInPeriod;
import models.enumerate.EpasParam;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


@Audited
@Entity
@Table(name = "configurations")
public class Configuration extends PropertyInPeriod {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id")
  public Office office;

  @Enumerated(EnumType.STRING)
  @Column(name = "epas_param")
  public EpasParam epasParam;

  @Column(name = "field_value")
  public String fieldValue;
  
  @Override
  public IPropertiesInPeriodOwner getOwner() {
    return this.office;
  }

  @Override
  public void setOwner(IPropertiesInPeriodOwner target) {
    this.office = (Office)target;
  }

  @Override
  public Object getType() {
    return this.epasParam;
  }

  @Override
  public void setType(Object value) {
    this.epasParam = (EpasParam)value;
  }

  @Override
  public Object getValue() {
    return this.fieldValue;
  }

  @Override
  public void setValue(Object value) {
    this.fieldValue = (String)value;
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof String) {
      if (this.getValue().equals((String)otherValue)) {
        return true;
      }
    }
    return false;
  }


}
