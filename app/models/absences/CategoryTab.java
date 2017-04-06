package models.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.base.BaseModel;

import org.assertj.core.util.Lists;
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
   * Se esiste fra gli enumerati un corrispondente e se è correttamente modellato.
   * @return absent se la tab non è presente in enum
   */
  public Optional<Boolean> matchEnum() {
    for (DefaultTab defaultTab : DefaultTab.values()) {
      if (defaultTab.name().equals(this.name)) {
        if (defaultTab.description.equals(this.description)
            && defaultTab.priority == this.priority) {
          return Optional.of(true);
        } else {
          return Optional.of(false);
        }
      } 
    }
    return Optional.absent();
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
    LAVORO_FUORI_SEDE("Lavoro Fuori Sede", 5),
    FERIE_DIPENDENTE("Ferie Dipendenti", 7);
    
    public String description;
    public int priority;
    
    private DefaultTab(String description, int priority) {
      this.description = description;
      this.priority = priority;
    }
    
    /**
     * Ricerca le categorie modellate e non presenti fra quelle passate in arg (db). 
     * @return list
     */
    public static List<DefaultTab> missing(List<CategoryTab> allTabs) {
      List<DefaultTab> missing = Lists.newArrayList();
      for (DefaultTab defaultCategory : DefaultTab.values()) {
        boolean found = false;
        for (CategoryTab category : allTabs) {
          if (defaultCategory.name().equals(category.name)) {
            found = true;
            break;
          }
        }
        if (!found) {
          missing.add(defaultCategory);
        }
      }
      return missing;
    }
    
    /**
     * L'enumerato corrispettivo della tab (se esiste...) 
     * @return optional dell'enumerato
     */
    public static Optional<DefaultTab> byName(CategoryTab tab) {
      for (DefaultTab defaultTab : DefaultTab.values()) {
        if (defaultTab.name().equals(tab.name)) {
          return Optional.of(defaultTab);
        }
      }
      return Optional.absent();
    }
  }
  
  
}
