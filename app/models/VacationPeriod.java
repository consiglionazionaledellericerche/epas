package models;


import it.cnr.iit.epas.DateInterval;

import models.base.BaseModel;

import org.joda.time.LocalDate;

import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;


@Entity
//@Audited
@Table(name = "vacation_periods")
public class VacationPeriod extends BaseModel {

  private static final long serialVersionUID = 7082224747753675170L;

  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vacation_codes_id", nullable = false)
  public VacationCode vacationCode;


  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contract_id", nullable = false, updatable = false)
  public Contract contract;


  @Column(name = "begin_from")
  public LocalDate beginFrom;


  @Column(name = "end_to")
  public LocalDate endTo;
  
  @Transient
  public DateInterval getDateInterval(){
    return new DateInterval(this.beginFrom, this.endTo);
  }
 
}
