package models;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import org.hibernate.envers.Audited;
import org.joda.time.LocalTime;
import models.base.BaseModel;

/**
 * 
 * @author dario
 * Nuova gestione degli slot dei turni associati.
 */
@Audited
public class OrganizationShiftSlot extends BaseModel{

  @NotNull
  public LocalTime beginSlot;
  @NotNull
  public LocalTime endSlot;
  
  public LocalTime beginMealSlot;
  
  public LocalTime endMealSlot;
  
  public Integer minutesSlot;
 
  public Integer minutesPaid;
  
  @ManyToOne
  @JoinColumn(name = "competence_code_group_id")
  public OrganizationShiftTimeTable shiftTimeTable;
}
