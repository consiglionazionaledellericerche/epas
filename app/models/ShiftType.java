package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

@Entity
@Audited
@Table(name = "shift_type")
public class ShiftType extends BaseModel {

  private static final long serialVersionUID = 3156856871540530483L;


  public String type;

  public String description;

  @Column(name = "entrance_tolerance")
  public int entranceTolerance;

  @Column(name = "exit_tolerance")
  public int exitTolerance;

  @Column(name = "hour_tolerance")
  public int hourTolerance;

  @Column(name = "break_in_shift_enabled")
  public boolean breakInShiftEnabled;

  @Column(name = "break_in_shift")
  public int breakInShift;

  @NotAudited
  @OneToMany(mappedBy = "shiftType")
  public List<PersonShiftShiftType> personShiftShiftTypes = new ArrayList<>();

  @NotAudited
  @OneToMany(mappedBy = "shiftType")
  public List<PersonShiftDay> personShiftDays = new ArrayList<>();

  @NotAudited
  @OneToMany(mappedBy = "type")
  public List<ShiftCancelled> shiftCancelled = new ArrayList<>();

  @NotAudited
  @ManyToOne
  @JoinColumn(name = "shift_time_table_id")
  public ShiftTimeTable shiftTimeTable;

  //@Required
  @ManyToOne(optional = false)
  @JoinColumn(name = "shift_categories_id")
  public ShiftCategories shiftCategories;

  @OneToMany(mappedBy = "shiftType", cascade = CascadeType.REMOVE)
  public Set<ShiftTypeMonth> monthsStatus = new HashSet<>();

  @Override
  public String toString() {
    return shiftCategories.description + " - " + type;
  }

  public enum ToleranceType {
    entrance("entrance"),
    exit("exit"),
    both("both");

    public String description;

    ToleranceType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @Transient
  public Optional<ShiftTypeMonth> monthStatusByDate(LocalDate date) {
    final YearMonth requestedMonth = new YearMonth(date);
    return monthsStatus.stream()
        .filter(shiftTypeMonth -> shiftTypeMonth.yearMonth.equals(requestedMonth)).findFirst();
  }

  @Transient
  public boolean approvedOn(LocalDate date) {
    Optional<ShiftTypeMonth> monthStatus = monthStatusByDate(date);
    if (monthStatus.isPresent()) {
      return monthStatus.get().approved;
    } else {
      return false;
    }
  }

}
