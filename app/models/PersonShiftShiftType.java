package models;

import com.google.common.collect.Range;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import models.base.BaseModel;
import org.joda.time.LocalDate;
import play.data.validation.Required;


/**
 * Associazione tra persone e tipi di turno (con date di inizio e fine).
 * 
 * @author cristian
 * @author arianna
 */
@Entity
@Table(name = "person_shift_shift_type")
public class PersonShiftShiftType extends BaseModel {

  private static final long serialVersionUID = -4476838239881674080L;

  @Required
  @ManyToOne
  @JoinColumn(name = "personshifts_id")
  public PersonShift personShift;

  @Required
  @ManyToOne
  @JoinColumn(name = "shifttypes_id")
  public ShiftType shiftType;


  @Column(name = "begin_date")
  public LocalDate beginDate;


  @Column(name = "end_date")
  public LocalDate endDate;

  public boolean jolly;

  /**
   * Il range di date di appartenenza della persona all'attività.
   * @return il range di date di appartenenza della persona all'attività.
   */
  @Transient
  public Range<LocalDate> dateRange() {
    if (beginDate == null && endDate == null) {
      return Range.all();
    }
    if (beginDate == null) {
      return Range.atMost(endDate);
    }
    if (endDate == null) {
      return Range.atLeast(beginDate);
    }
    return Range.closed(beginDate, endDate);
  }
}
