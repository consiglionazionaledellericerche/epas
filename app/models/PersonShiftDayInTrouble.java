package models;

import models.enumerate.ShiftTroubles;


import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

public class PersonShiftDayInTrouble {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "personshiftday_id", nullable = false, updatable = false)
  public PersonShiftDay personShiftDay;
  
  @Enumerated(EnumType.STRING)
  public ShiftTroubles cause;
  
  public boolean emailSent;
}
