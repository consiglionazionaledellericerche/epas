package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.absences.Absence;
import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;



@Entity
@Audited
@Table(name = "time_variations")
public class TimeVariation extends BaseModel {
  
  private static final long serialVersionUID = -6067037671772984710L;

  @Column(name = "date_variation")
  public LocalDate dateVariation;
  
  @Column(name = "time_variation")
  public int timeVariation;

  @ManyToOne//(fetch = FetchType.LAZY)
  @JoinColumn(name = "absence_id", nullable = false, updatable = false)
  public Absence absence;
}
