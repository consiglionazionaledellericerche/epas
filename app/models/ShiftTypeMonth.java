package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import models.base.MutableModel;
import org.hibernate.envers.Audited;
import org.joda.time.YearMonth;
import play.data.validation.Required;

/**
 * @author daniele
 * @since 09/06/17.
 */
@Entity
@Audited
@Table(name = "shift_type_month", uniqueConstraints = @UniqueConstraint(columnNames = {
    "shift_type_id", "year_month"}))
public class ShiftTypeMonth extends MutableModel {

  private static final long serialVersionUID = 4745667554574561506L;

  @Required
  @Column(name = "year_month", nullable = false)
  public YearMonth yearMonth;

  @Required
  @ManyToOne
  @JoinColumn(name = "shift_type_id", nullable = false)
  public ShiftType shiftType;

  public boolean approved;
}
