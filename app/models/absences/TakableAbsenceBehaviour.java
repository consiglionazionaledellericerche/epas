package models.absences;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import lombok.Getter;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.testng.collections.Sets;

@Audited
@Entity
@Table(name = "takable_absence_behaviours")
public class TakableAbsenceBehaviour extends BaseModel {

  private static final long serialVersionUID = 486763865630858142L;
  public static final String NAME_PREFIX = "T_";

  @Column(name = "name")
  public String name;
  
  @OneToMany(mappedBy = "takableAbsenceBehaviour", fetch = FetchType.LAZY)
  public Set<GroupAbsenceType> groupAbsenceTypes = Sets.newHashSet();
  
  @Getter
  @Column(name = "amount_type")
  @Enumerated(EnumType.STRING)
  public AmountType amountType;
  
  @Getter
  @ManyToMany
  @JoinTable(name = "taken_codes_group", 
      joinColumns = { @JoinColumn(name = "takable_behaviour_id") }, 
        inverseJoinColumns = { @JoinColumn(name = "absence_types_id") })
  @OrderBy("code")
  public Set<AbsenceType> takenCodes = Sets.newHashSet();
  
  //  @Column(name = "takable_count_behaviour")
  //  @Enumerated(EnumType.STRING)
  //  public TakeCountBehaviour takableCountBehaviour;
  
  @Getter
  @ManyToMany
  @JoinTable(name = "takable_codes_group", 
      joinColumns = { @JoinColumn(name = "takable_behaviour_id") }, 
      inverseJoinColumns = { @JoinColumn(name = "absence_types_id") })
  @OrderBy("code")
  public Set<AbsenceType> takableCodes = Sets.newHashSet();
  
  @Getter
  @Column(name = "fixed_limit")
  public Integer fixedLimit;
  
  @Getter
  @Column(name = "takable_amount_adjust")
  @Enumerated(EnumType.STRING)
  public TakeAmountAdjustment takableAmountAdjustment;
 
  public enum TakeCountBehaviour {
    period, sumAllPeriod, sumUntilPeriod; 
  }
  
  public enum TakeAmountAdjustment {
    workingPeriodPercent, workingTimeAndWorkingPeriodPercent;
  }

}
