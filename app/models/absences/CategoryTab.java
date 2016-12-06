package models.absences;

import com.google.common.collect.Sets;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Audited
@Entity
@Table(name = "category_tabs")
public class CategoryTab extends BaseModel implements Comparable<CategoryTab> {

  private static final long serialVersionUID = 4580659910825885894L;

  @Column
  public String name;

  @Column
  public String description;
  
  @Column
  public int priority;
  
  @Column(name = "is_default")
  public boolean isDefault = false;
  
  @OneToMany(mappedBy = "tab", fetch = FetchType.LAZY)
  public Set<CategoryGroupAbsenceType> categoryGroupAbsenceTypes = Sets.newHashSet();

  @Override
  public int compareTo(CategoryTab obj) {
    return name.compareTo(obj.name);
  }
  
  /**
   * Categoria con miglior priorità.
   * @return categoria
   */
  public CategoryGroupAbsenceType firstByPriority() {
    CategoryGroupAbsenceType candidate = categoryGroupAbsenceTypes.iterator().next();
    for (CategoryGroupAbsenceType category : categoryGroupAbsenceTypes) {
      if (candidate.priority > category.priority) {
        candidate = category;
      }
    }
    return candidate;
  }
  
  /**
   * To String.
   */
  public String toString() {
    return this.name;
  }
}
