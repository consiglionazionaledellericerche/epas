package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.ToString;
import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PropertyInPeriod;
import play.data.validation.Required;

@ToString
@Entity
@Table(name = "personal_working_times")
public class PersonalWorkingTime extends PropertyInPeriod implements IPropertyInPeriod {
  
  public String description;
  
  public String workingTime;
  
  @Getter
  @Required
  @ManyToOne
  @JoinColumn(name = "contract_id")
  public Contract contract;
  
    
  @Override
  public IPropertiesInPeriodOwner getOwner() {    
    return this.contract;
  }

  @Override
  public void setOwner(IPropertiesInPeriodOwner target) {
    this.contract = (Contract) target;    
  }

  @Override
  public Object getType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setType(Object value) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Object getValue() {
    
    return this.workingTime;
  }

  @Override
  public void setValue(Object value) {
    this.workingTime = (String) value;
    
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  public String getLabel() {
    return this.description + ": " + this.workingTime;
  }

}
