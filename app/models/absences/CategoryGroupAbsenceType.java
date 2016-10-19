package models.absences;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Audited
@Entity
@Table(name = "category_group_absence_types")
public class CategoryGroupAbsenceType extends BaseModel implements Comparable<CategoryGroupAbsenceType> {

  @Column
  public String name;
  
  @Column
  public String description;
  
  @Column
  public int priority;

  @Override
  public int compareTo(CategoryGroupAbsenceType o) {
    return name.compareTo(o.name);
  }
  
}
