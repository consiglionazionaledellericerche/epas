package models;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
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
@Entity
public class OrganizationShiftSlot extends BaseModel{

  private static final long serialVersionUID = 2019_10_28_1039L;
  
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
  
  public Integer minutesSlot;
 
  public Integer minutesPaid;
  
  @ManyToOne
  @JoinColumn(name = "shift_time_table_id")
  public OrganizationShiftTimeTable shiftTimeTable;
}
