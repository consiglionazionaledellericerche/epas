package models;

import com.google.common.collect.Sets;
import events.EntityEvents;
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
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
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
  @Required(message = "calendar.slot")
  @Column(name = "shift_slot", nullable = false)
  @Enumerated(EnumType.STRING)
  public ShiftSlot shiftSlot;

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

  @Transient
  public LocalTime slotBegin() {
    switch (shiftSlot) {
      case MORNING:
        return shiftType.shiftTimeTable.startMorning;
      case AFTERNOON:
        return shiftType.shiftTimeTable.startAfternoon;
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

  @PostUpdate
  @PostPersist
  @PostRemove
  public void changed() {
    EntityEvents.changed(this);
  }

}
