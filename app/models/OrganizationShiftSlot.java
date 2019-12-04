package models;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.hibernate.envers.Audited;
import org.joda.time.LocalTime;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import models.base.BaseModel;
import models.enumerate.CalculationType;
import models.enumerate.PaymentType;
import play.data.validation.Required;

/**
 * 
 * @author dario
 * Nuova gestione degli slot dei turni associati.
 */
@Audited
@Entity
public class OrganizationShiftSlot extends BaseModel{

  private static final long serialVersionUID = 2019_10_28_1039L;
  
  
  public String name;
  
  @Transient
  public String getName() {
    if (Strings.isNullOrEmpty(this.name)) {
      return String.format("%s - %s", this.beginSlot, this.endSlot);
    } else {
      return name;
    }
  }
  
  @NotNull
  @Column(columnDefinition = "VARCHAR")
  public LocalTime beginSlot;
  
  @NotNull
  @Column(columnDefinition = "VARCHAR")
  public LocalTime endSlot;
  
  @Column(columnDefinition = "VARCHAR")
  @Nullable
  public LocalTime beginMealSlot;
  
  @Column(columnDefinition = "VARCHAR")
  @Nullable
  public LocalTime endMealSlot;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "payment_type")
  public PaymentType paymentType;;
 
  public Integer minutesPaid;
  
  @ManyToOne
  @JoinColumn(name = "shift_time_table_id")
  public OrganizationShiftTimeTable shiftTimeTable;
}
