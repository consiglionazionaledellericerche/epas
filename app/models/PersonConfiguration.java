package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import manager.configurations.EpasParam;

import models.base.IPropertiesInPeriodOwner;
import models.base.PropertyInPeriod;

import org.hibernate.envers.Audited;


@Audited
@Entity
@Table(name = "person_configurations")
public class PersonConfiguration extends PropertyInPeriod {

  private static final long serialVersionUID = 6467506090648831715L;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id")
  public Person person;

  @Enumerated(EnumType.STRING)
  @Column(name = "epas_param")
  public EpasParam epasParam;

  @Column(name = "field_value")
  public String fieldValue;

  @Override
  public IPropertiesInPeriodOwner getOwner() {
    return this.person;
  }

  @Override
  public void setOwner(IPropertiesInPeriodOwner target) {
    this.person = (Person) target;
  }

  @Override
  public Object getType() {
    return this.epasParam;
  }

  @Override
  public void setType(Object value) {
    this.epasParam = (EpasParam) value;
  }

  @Override
  public Object getValue() {
    return this.fieldValue;
  }

  @Override
  public void setValue(Object value) {
    this.fieldValue = (String) value;
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof String) {
      if (this.getValue().equals((String) otherValue)) {
        return true;
      }
    }
    return false;
  }

}
