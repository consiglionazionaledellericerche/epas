package models.absences;

import manager.services.absences.AbsenceEngine.ComputeAmountRestriction;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Audited
@Entity
@Table(name = "takable_absence_behaviours")
public class TakableAbsenceBehaviour extends BaseModel {

  @Column(name = "name")
  public String name;
  
  @Column(name = "amount_type")
  @Enumerated(EnumType.STRING)
  public AmountType amountType;
  
//  @Column(name = "taken_count_behaviour")
//  @Enumerated(EnumType.STRING)
//  public TakeCountBehaviour takenCountBehaviour;
  
  @ManyToMany
  @JoinTable(name = "taken_codes_group", 
  joinColumns = { @JoinColumn(name = "takable_behaviour_id") }, 
  inverseJoinColumns = { @JoinColumn(name = "absence_types_id") })
  @OrderBy("code")
  public Set<AbsenceType> takenCodes;
  
//  @Column(name = "takable_count_behaviour")
//  @Enumerated(EnumType.STRING)
//  public TakeCountBehaviour takableCountBehaviour;
  
  @ManyToMany
  @JoinTable(name = "takable_codes_group", 
  joinColumns = { @JoinColumn(name = "takable_behaviour_id") }, 
  inverseJoinColumns = { @JoinColumn(name = "absence_types_id") })
  @OrderBy("code")
  public Set<AbsenceType> takableCodes;
  
  @Column(name = "fixed_limit")
  public Integer fixedLimit;
  
  @Column(name = "takable_amount_adjust")
  @Enumerated(EnumType.STRING)
  public TakeAmountAdjustment takableAmountAdjustment;
 
  public enum TakeCountBehaviour {
    period, sumAllPeriod, sumUntilPeriod; 
  }
  
  public enum TakeAmountAdjustment {
    workingTimePercent, workingPeriodPercent, workingTimeAndWorkingPeriodPercent;
  }

}
