package models.absences;

import models.Person;
import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Audited
@Entity
@Table(name = "initialization_groups")
public class InitializationGroup extends BaseModel {
  
  private static final long serialVersionUID = -1963061850354314327L;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id", nullable = false)
  public Person person;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "group_absence_type_id", nullable = false)
  public GroupAbsenceType groupAbsenceType;
  
  @Column(name = "initialization_date")
  public LocalDate initializationDate;

  @Column(name = "forced_begin")
  public LocalDate forcedBegin;
  
  @Column(name = "forced_end")
  public LocalDate forcedEnd;
  
  // if (groupAbsenceType.pattern == programmed)
  
  @Column(name = "takable_total")
  public Integer takableTotal;
  
  @Column(name = "takable_used")
  public Integer takableUsed;
  
  @Column(name = "complation_used")
  public Integer complationUsed;
  
  // if (groupAbsenceType.pattern == vacationsCnr)
  
  @Column(name = "vacation_year")
  public Integer vacationYear;
  
  //TODO: enum ferie o permessi
  
  //if (groupAbsenceType.pattern == compensatoryRestCnr)
  
  @Column(name = "residual_minutes_last_year")
  public Integer residualMinutesLastYear;
  
  @Column(name = "residual_minutes_current_year")
  public Integer residualMinutesCurrentYear;
  

}
