package models;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;


@Entity
@Audited
@Table(name = "shift_categories")
public class ShiftCategories extends BaseModel {

  private static final long serialVersionUID = 3156856871540530483L;

  public String description;

  /**
   * responsabile della categoria turno.
   */
  @ManyToOne(optional = false)
  @JoinColumn(name = "supervisor")
  public Person supervisor;
  
  public boolean disabled;
  
  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "office_id")
  @NotNull
  public Office office; 
  
  @NotAudited
  @OneToMany(mappedBy = "shiftCategories")
  public List<ShiftType> shiftTypes = new ArrayList<ShiftType>();
  
  @Override
  public String toString() {
    return String.format("ShiftCategory[%d] - description = %s", id, description);
  }
}

