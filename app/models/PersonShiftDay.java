package models;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import models.base.BaseModel;
import models.enumerate.ShiftSlot;
import models.enumerate.ShiftTroubles;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import play.data.validation.Required;

@Entity
@Audited
@Table(name = "person_shift_days")
public class PersonShiftDay extends BaseModel {

  private static final long serialVersionUID = -2441219908198684741L;

  // morning or afternoon slot
  @Column(name = "shift_slot")
  @Enumerated(EnumType.STRING)
  public ShiftSlot shiftSlot;
  
//  @Transient
//  public ShiftSlot getShiftSlot() {
//    return ShiftSlot.valueOf(this.organizationShiftSlot.getName());
//  }

  //@Required
  @ManyToOne
  public OrganizationShiftSlot organizationShiftSlot;
  
  // shift date
  @Required
  public LocalDate date;

  @Required
  @ManyToOne
  @JoinColumn(name = "shift_type_id", nullable = false)
  public ShiftType shiftType;

  @Required
  @ManyToOne
  @JoinColumn(name = "person_shift_id", nullable = false)
  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  public PersonShift personShift;

  //  Nuova relazione con gli errori associati ai personShiftDay
  @OneToMany(mappedBy = "personShiftDay", cascade = CascadeType.REMOVE)
  public Set<PersonShiftDayInTrouble> troubles = Sets.newHashSet();
  
  /**
   * numero di soglie (minime) superate.
   */
  @Column(name = "exceeded_thresholds")
  public int exceededThresholds;

  @Transient
  public LocalTime slotBegin() {
    switch (shiftSlot) {
      case MORNING:
        return shiftType.shiftTimeTable.startMorning;
      case AFTERNOON:
        return shiftType.shiftTimeTable.startAfternoon;
      case EVENING:
        return shiftType.shiftTimeTable.startEvening;
      default:
        return null;
    }
  }

  @Transient
  public LocalTime slotEnd() {
    switch (shiftSlot) {
      case MORNING:
        return shiftType.shiftTimeTable.endMorning;
      case AFTERNOON:
        return shiftType.shiftTimeTable.endAfternoon;
      case EVENING:
        return shiftType.shiftTimeTable.endEvening;
      default:
        return null;
    }
  }

  @Transient
  public LocalTime lunchTimeBegin() {
    switch (shiftSlot) {
      case MORNING:
        return shiftType.shiftTimeTable.startMorningLunchTime;
      case AFTERNOON:
        return shiftType.shiftTimeTable.startAfternoonLunchTime;
      case EVENING:
        return shiftType.shiftTimeTable.startEveningLunchTime;
      default:
        return null;
    }
  }

  @Transient
  public LocalTime lunchTimeEnd() {
    switch (shiftSlot) {
      case MORNING:
        return shiftType.shiftTimeTable.endMorningLunchTime;
      case AFTERNOON:
        return shiftType.shiftTimeTable.endAfternoonLunchTime;
      case EVENING:
        return shiftType.shiftTimeTable.endEveningLunchTime;
      default:
        return null;
    }
  }

  @Transient
  public boolean hasError(ShiftTroubles trouble) {
    return troubles.stream().anyMatch(error -> error.cause == trouble);
  }

  @Transient
  public boolean hasOneOfErrors(Collection<ShiftTroubles> shiftTroubles) {
    return troubles.stream().anyMatch(trouble -> {
      return shiftTroubles.contains(trouble.cause);
    });
  }

}
