package models.absences;

import com.google.common.base.Optional;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.absences.CategoryTab.DefaultTab;
import models.base.BaseModel;

import org.assertj.core.util.Lists;
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
   * Se esiste fra gli enumerati un corrispondente e se è correttamente modellato.
   * @return absent se la categoria non è presente in enum
   */
  public Optional<Boolean> matchEnum() {
    
    for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
      if (defaultCategory.name().equals(this.name)) {
        if (defaultCategory.description.equals(this.description)
            && defaultCategory.priority == this.priority
            && defaultCategory.categoryTab.name().equals(this.tab.name)) {
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
   * Le categorie di default.
   * 
   * @author alessandro
   *
   */
  public enum DefaultCategoryType {

    MISSIONE_CNR("Missioni CNR", 1, DefaultTab.MISSIONE), 
    FERIE_CNR("Ferie CNR", 2, DefaultTab.FERIE),
    RIPOSI_COMPENSATIVI_CNR("Riposi compensativi CNR", 3, DefaultTab.RIPOSO_COMPENSATIVO),
    PERMESSI_VARI("Permessi vari", 4, DefaultTab.ALTRE_TIPOLOGIE),
    CONGEDI_PARENTALI("Congedi parentali", 5, DefaultTab.ALTRE_TIPOLOGIE),
    L_104("Disabilità legge 104/92", 6, DefaultTab.ALTRE_TIPOLOGIE),
    MALATTIA_DIPENDENTE("Malattia dipendente", 8, DefaultTab.ALTRE_TIPOLOGIE),
    MALATTIA_FIGLIO_1("Malattia primo figlio", 9, DefaultTab.ALTRE_TIPOLOGIE),
    MALATTIA_FIGLIO_2("Malattia secondo figlio", 10, DefaultTab.ALTRE_TIPOLOGIE),
    MALATTIA_FIGLIO_3("Malattia terzo figlio", 11, DefaultTab.ALTRE_TIPOLOGIE),
    PERMESSI_SINDACALI("Permessi sindacali", 12, DefaultTab.ALTRE_TIPOLOGIE),
    ALTRI_CODICI("Altri Codici", 13, DefaultTab.ALTRE_TIPOLOGIE),
    CODICI_DIPENDENTI("Codici Dipendenti", 14, DefaultTab.DIPENDENTI),
    CODICI_AUTOMATICI("Codici Automatici", 15, DefaultTab.AUTOMATICI);

    public String description;
    public int priority;
    public DefaultTab categoryTab;

    private DefaultCategoryType(String description, int priority, DefaultTab categoryTab) {
      this.description = description;
      this.priority = priority;
      this.categoryTab = categoryTab;
    }
    
    /**
     * Ricerca le categorie modellate e non presenti fra quelle passate in arg (db). 
     * @return list
     */
    public static List<DefaultCategoryType> missing(List<CategoryGroupAbsenceType> allCategories) {
      List<DefaultCategoryType> missing = Lists.newArrayList();
      for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
        boolean found = false;
        for (CategoryGroupAbsenceType category : allCategories) {
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
     * L'enumerato corrispettivo della categoria (se esiste...) 
     * @return optional dell'enumerato
     */
    public static Optional<DefaultCategoryType> byName(CategoryGroupAbsenceType category) {
      for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
        if (defaultCategory.name().equals(category.name)) {
          return Optional.of(defaultCategory);
        }
      }
      return Optional.absent();
    }
  }


}
