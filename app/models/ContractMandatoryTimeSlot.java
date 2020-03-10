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


/**
 * Un periodo di fascia oraria di presenza obbligatoria.
 *
 * @author cristian
 */
@ToString
@Entity
@Table(name = "contract_mandatory_time_slots")
public class ContractMandatoryTimeSlot extends PropertyInPeriod implements IPropertyInPeriod {

  private static final long serialVersionUID = 98286759517639967L;

  @Getter
  @Required
  @ManyToOne
  @JoinColumn(name = "contract_id")
  public Contract contract;

  @Getter
  @Required
  @ManyToOne
  @JoinColumn(name = "time_slot_id")
  public TimeSlot timeSlot;

  @Override
  public Object getValue() {
    return this.timeSlot;
  }

  @Override
  public void setValue(Object value) {
    this.timeSlot = (TimeSlot)value;
  }

  public IPropertiesInPeriodOwner getOwner() {
    return this.contract;
  }

  public void setOwner(IPropertiesInPeriodOwner target) {
    this.contract = (Contract)target;
  }

  @Override
  public boolean periodValueEquals(Object otherValue) {
    if (otherValue instanceof TimeSlot) {
      return this.getValue().equals(((TimeSlot)otherValue));
    }
    return false;
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
  public String getLabel() {
    return this.timeSlot.getLabel();
  }

}