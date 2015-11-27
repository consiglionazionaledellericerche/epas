package models;

import models.base.BaseModel;
import models.enumerate.ShiftSlot;

import org.joda.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "person_shift_days")
public class PersonShiftDay extends BaseModel {

  private static final long serialVersionUID = -2441219908198684741L;

  // morning or afternoon slot
  @Column(name = "shift_slot")
  @Enumerated(EnumType.STRING)
  public ShiftSlot shiftSlot;

  // shift date

  public LocalDate date;

  @ManyToOne
  @JoinColumn(name = "shift_type_id")
  public ShiftType shiftType;

  @ManyToOne
  @JoinColumn(name = "person_shift_id", nullable = false)
  public PersonShift personShift;

  public ShiftSlot getShiftSlot() {
    return this.shiftSlot;
  }

  public void setShiftSlot(ShiftSlot shiftSlot) {
    this.shiftSlot = shiftSlot;
  }
}
