package models.absences;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.absences.GroupAbsenceType.DefaultCategoryType;
import models.absences.GroupAbsenceType.DefaultTab;
import models.base.BaseModel;

import org.hibernate.envers.Audited;

@Audited
@Entity
@Table(name = "category_group_absence_types")
public class CategoryGroupAbsenceType extends BaseModel 
    implements Comparable<CategoryGroupAbsenceType> {

  private static final long serialVersionUID = 4580659910825885894L;

  @Column
  public String name;

  @Column
  public String description;
  
  @Column
  public int priority;
  
  @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
  public Set<GroupAbsenceType> groupAbsenceTypes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_tab_id")
  public CategoryTab tab;
  
  @Override
  public int compareTo(CategoryGroupAbsenceType obj) {
    return name.compareTo(obj.name);
  }
  
  /**
   * Se esiste fra gli enumerati un corrispondente.
   * @return matching result
   */
  public boolean matchEnum() {
    for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
      if (defaultCategory.name().equals(this.name) 
          && defaultCategory.description.equals(this.description)
          && defaultCategory.priority == this.priority
          && defaultCategory.categoryTab.name().equals(this.tab.name)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * To String.
   */
  public String toString() {
    return this.description;
  }

}
