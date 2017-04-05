package models.absences;

import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

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
   * Se esiste fra gli enumerati un corrispondente.
   * @return matching result
   */
  public boolean matchEnum() {
    for (DefaultTab defaultTab : DefaultTab.values()) {
      if (defaultTab.name().equals(this.name) 
          && defaultTab.description.equals(this.description)
          && defaultTab.priority == this.priority) {
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
  
  /**
   * Tab di default.
   * 
   * @author alessandro
   *
   */
  public enum DefaultTab {
    ALTRE_TIPOLOGIE("Altre Tipologie", 4),
    MISSIONE("Missione", 1),
    FERIE("Ferie e Festività Soppr.", 2),
    RIPOSO_COMPENSATIVO("Riposo Compensativo", 3),
    AUTOMATICI("Codici Automatici", 6),
    DIPENDENTI("Codici Dipendenti", 5),
    LAVORO_FUORI_SEDE("Lavoro Fuori Sede", 5);
    
    public String description;
    public int priority;
    
    private DefaultTab(String description, int priority) {
      this.description = description;
      this.priority = priority;
    }
    
    /**
     * Se l'enum è presente nell'elenco delle tabs in list.
     * @return present
     */
    public boolean isPresent(List<CategoryTab> list) {
      for (CategoryTab tab : list) {
        if (tab.name.equals(this.name())) {
          return true;
        }
      }
      return false;
    }
  }
  
  
}
