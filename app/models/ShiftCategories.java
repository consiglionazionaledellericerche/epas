package models;

import com.google.common.collect.Lists;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import play.data.validation.Required;
import play.data.validation.Unique;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;


@Entity
@Audited
@Table(name = "shift_categories")
public class ShiftCategories extends BaseModel {

  private static final long serialVersionUID = 3156856871540530483L;

  @Required
  @Unique
  public String description;

  /**
   * responsabile della categoria turno.
   */
  @ManyToOne(optional = false)
  @JoinColumn(name = "supervisor")
  @Required
  public Person supervisor;
  
  public boolean disabled;
  
  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "office_id")
  @NotNull
  public Office office; 
  
  @NotAudited
  @OneToMany(mappedBy = "shiftCategories")
  public List<ShiftType> shiftTypes = new ArrayList<ShiftType>();
  
  @ManyToMany
  public List<Person> manager = Lists.newArrayList();
  
  @Override
  public String toString() {
    return String.format("ShiftCategory[%d] - description = %s", id, description);
  }
}

