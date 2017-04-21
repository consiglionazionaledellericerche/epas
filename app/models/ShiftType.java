package models;

import lombok.ToString;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;


@ToString(of= {"type", "description"})
@Entity
@Audited
@Table(name = "shift_type")
public class ShiftType extends BaseModel {

  private static final long serialVersionUID = 3156856871540530483L;

  
  public String type;
  
  public String description;
  
  @Column(name="entrance_tolerance")
  public int entranceTolerance;
  
  @Column(name="exit_tolerance")
  public int exitTolerance;
  
  @Column(name="hour_tolerance")
  public int hourTolerance;
  
  @Column(name="break_in_shift_enabled")
  public boolean breakInShiftEnabled;
  
  @Column(name="break_in_shift")
  public int breakInShift;

  @NotAudited
  @OneToMany(mappedBy = "shiftType")
  public List<PersonShiftShiftType> personShiftShiftTypes = new ArrayList<PersonShiftShiftType>();

  @NotAudited
  @OneToMany(mappedBy = "shiftType")
  public List<PersonShiftDay> personShiftDays = new ArrayList<PersonShiftDay>();

  @NotAudited
  @OneToMany(mappedBy = "type")
  public List<ShiftCancelled> shiftCancelled = new ArrayList<ShiftCancelled>();

  @NotAudited
  @ManyToOne
  @JoinColumn(name = "shift_time_table_id")
  public ShiftTimeTable shiftTimeTable;

  //@Required
  @ManyToOne(optional = false)
  @JoinColumn(name = "shift_categories_id")
  public ShiftCategories shiftCategories;
  
  
  public enum ToleranceType {
    entrance("entrance"),
    exit("exit"),
    both("both");
    
    public String description;

    ToleranceType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return this.description;
    }
  }
}
