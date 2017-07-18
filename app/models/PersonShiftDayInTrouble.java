package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import models.base.BaseModel;
import models.enumerate.ShiftTroubles;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "person_shift_day_in_trouble", uniqueConstraints = @UniqueConstraint(columnNames = {
    "person_shift_day_id", "cause"}))
@EqualsAndHashCode(callSuper = false, of = {"personShiftDay", "cause"})
public class PersonShiftDayInTrouble extends BaseModel {

  private static final long serialVersionUID = -5497453685568298051L;

  @ManyToOne
  @JoinColumn(name = "person_shift_day_id", nullable = false, updatable = false)
  public PersonShiftDay personShiftDay;

  @Enumerated(EnumType.STRING)
  public ShiftTroubles cause;

  @Column(name = "email_sent")
  public boolean emailSent;

  public PersonShiftDayInTrouble(PersonShiftDay pd, ShiftTroubles cause) {
    personShiftDay = pd;
    this.cause = cause;
  }
}
