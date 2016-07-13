package models.absences;

import com.google.common.base.Preconditions;

import it.cnr.iit.epas.DateInterval;

import models.absences.GroupAbsenceType.PeriodType;
import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Audited
@Entity
@Table(name = "group_absence_types")
public class GroupAbsenceType extends BaseModel {

  @Column
  public String name;
  
  @Column
  public String description;
  
  @Column(name = "pattern")
  @Enumerated(EnumType.STRING)
  public GroupAbsenceTypePattern pattern;
  
  @Column(name = "period_type")
  @Enumerated(EnumType.STRING)
  public PeriodType periodType;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "takable_behaviour_id")
  public TakableAbsenceBehaviour takableAbsenceBehaviour;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "complation_behaviour_id")
  public ComplationAbsenceBehaviour complationAbsenceBehaviour;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "next_group_to_check_id")
  public GroupAbsenceType nextGropToCheck;
  

  
  public enum PeriodType {
    
    always(0, null, null), year(0, null, null), month(0, null, null),
    child1_0_3(1, 0, 3), child1_0_6(1, 0, 6), child1_0_12(1, 0, 12), child1_6_12(1, 6, 12),
    child2_0_3(2, 0, 3), child2_0_6(2, 0, 6), child2_0_12(2, 0, 12), child2_6_12(2, 6, 12),
    child3_0_3(3, 0, 3), child3_0_6(3, 0, 6), child3_0_12(3, 0, 12), child3_6_12(3, 6, 12);
    
    public Integer childNumber;
    public Integer fromYear;
    public Integer toYear;
    
    PeriodType(Integer childNumber, Integer fromYear, Integer toYear) {
      this.childNumber = childNumber;
      this.fromYear = fromYear;
      this.toYear = toYear;
    }
    
    public boolean isChildPeriod() {
      return childNumber != null;
    }
    
    public Integer getChildNumber() {
      return childNumber;
    }
    
    public DateInterval getChildInterval(LocalDate birthDate) {
      if (fromYear == null || toYear == null) {
        return null;
      }
      LocalDate from = birthDate.plusYears(fromYear);
      LocalDate to = birthDate.plusYears(toYear).minusDays(1);
      return new DateInterval(from, to);
    }
  }
  
  public enum GroupAbsenceTypePattern {
    programmed, vacationsCnr, compensatoryRestCnr;
  }

  
}
