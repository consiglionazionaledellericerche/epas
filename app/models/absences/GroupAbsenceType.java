package models.absences;

import com.google.common.base.Strings;

import it.cnr.iit.epas.DateInterval;

import lombok.Getter;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Audited
@Entity
@Table(name = "group_absence_types")
public class GroupAbsenceType extends BaseModel {

  private static final long serialVersionUID = 3290760775533091791L;

  @Required
  @Column
  public String name;
  
  //Astensione facoltativa post partum 100% primo figlio 0-12 anni 30 giorni 
  @Column
  public String description;

  //Se i gruppi sono concatenati e si vuole una unica etichetta (da assegnare alla radice)
  // Esempio Congedi primo figlio 100%, Congedi primo figlio 30% hanno una unica chainDescription
  @Column(name = "chain_description")
  public String chainDescription;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_type_id")
  public CategoryGroupAbsenceType category;
  
  @Required
  @Getter
  @Column(name = "pattern")
  @Enumerated(EnumType.STRING)
  public GroupAbsenceTypePattern pattern;
  
  @Getter
  @Column(name = "period_type")
  @Enumerated(EnumType.STRING)
  public PeriodType periodType;
  
  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "takable_behaviour_id")
  public TakableAbsenceBehaviour takableAbsenceBehaviour;
  
  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "complation_behaviour_id")
  public ComplationAbsenceBehaviour complationAbsenceBehaviour;
  
  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "next_group_to_check_id")
  public GroupAbsenceType nextGroupToCheck;

  @OneToMany(mappedBy = "nextGroupToCheck", fetch = FetchType.LAZY)
  public Set<GroupAbsenceType> previousGroupChecked;

  @Column
  public boolean automatic = false;
  
  @Column
  public boolean initializable = false;
  
  /**
   * Label.
   * @return label
   */
  public String getLabel() {
    return getChainDescription();
  }
  
  /**
   * La stringa che rappresenta la catena cui appartiene il gruppo.
   * @return chainDescription
   */
  public String getChainDescription() {
    if (!Strings.isNullOrEmpty(this.chainDescription)) {
      return this.chainDescription;
    } else {
      return this.description;
    }
  }
  
  /**
   * Il primo gruppo della catena (quando ho un modo univoco di raggiungerlo).
   * @return primo gruppo
   */
  public GroupAbsenceType firstOfChain() {
    if (this.previousGroupChecked.isEmpty()) {
      return this; 
    }
    if (this.previousGroupChecked.size() == 1) {
      return this.previousGroupChecked.iterator().next().firstOfChain();
    }
    return this;
  }
  
  public enum PeriodType {
    
    always(0, null, null), year(0, null, null), month(0, null, null),
    child1_0_3(1, 0, 3), child1_0_6(1, 0, 6), child1_0_12(1, 0, 12), 
    child1_6_12(1, 6, 12), child1_3_12(1, 3, 12),
    
    child2_0_3(2, 0, 3), child2_0_6(2, 0, 6), child2_0_12(2, 0, 12), child2_6_12(2, 6, 12), 
    child2_3_12(2, 3, 12),
    
    child3_0_3(3, 0, 3), child3_0_6(3, 0, 6), child3_0_12(3, 0, 12), child3_6_12(3, 6, 12), 
    child3_3_12(3, 3, 12);
    
    
    public Integer childNumber;
    public Integer fromYear;
    public Integer toYear;
    
    PeriodType(Integer childNumber, Integer fromYear, Integer toYear) {
      this.childNumber = childNumber;
      this.fromYear = fromYear;
      this.toYear = toYear;
    }
    
    public boolean isChildPeriod() {
      return childNumber > 0;
    }
    
    public Integer getChildNumber() {
      return childNumber;
    }
    
    /**
     * L'intervallo figlio.
     * @param birthDate data di nascita
     * @return intervallo
     */
    public DateInterval getChildInterval(LocalDate birthDate) {
      if (fromYear == null || toYear == null) {
        return null;
      }
      LocalDate from = birthDate.plusYears(fromYear);
      LocalDate to = birthDate.plusYears(toYear).minusDays(1);
      return new DateInterval(from, to);
    }
  }
  
  public enum GroupAbsenceTypePattern {
    simpleGrouping,              // semplice raggruppamento senza controlli o automatismi
    programmed,                  
    vacationsCnr,                // custom ferie cnr
    compensatoryRestCnr;         // custom riposi compensativi cnr
    
  }
  
  public String toString() {
    return description;
  }
  
  public enum DefaultCategoryType {

    GENERAL("Assenze generali cnr", 1),
    PERMISSION("Permessi vari", 2),
    POST_PARTUM("Congedi parentali", 3),
    LAW_104_92("Disabilità legge 104/92", 5), 
    MALATTIA("Malattia Dipendente", 6),
    MALATTIA_FIGLIO_1("Malattia primo figlio", 7),
    MALATTIA_FIGLIO_2("Malattia secondo figlio", 8),
    MALATTIA_FIGLIO_3("Malattia terzo figlio", 9),
    PUBLIC_FUNCTION("Pubblica Funzione", 10),
    OTHER_CODES("Altri Codici", 11),
    AUTOMATIC_CODES("Codici Automatici", 12),
    EMPLOYEE_CODES("Codici Dipendenti", 13)
    ;

    public String name;
    public int priority;

    private DefaultCategoryType(String name, int priority) {
      this.name = name;
      this.priority = priority;
    }

  }

  public enum DefaultComplation {
    C_18, C_19, C_661, 
    C_23, C_25, C_232, C_252, C_233, C_253, 
    C_89, C_09;
  }

  public enum DefaultTakable {
    T_18, T_19, T_661, 
    T_23, T_25, T_232, T_252, T_233, T_253, 
    T_89, T_09, T_FERIE_CNR, T_RIPOSI_CNR, T_MISSIONE, T_95, T_ALTRI,
    T_MALATTIA,
    T_MALATTIA_FIGLIO_1_12,
    T_MALATTIA_FIGLIO_1_13,
    T_MALATTIA_FIGLIO_1_14,
    T_MALATTIA_FIGLIO_2_12,
    T_MALATTIA_FIGLIO_2_13,
    T_MALATTIA_FIGLIO_2_14,
    T_MALATTIA_FIGLIO_3_12,
    T_MALATTIA_FIGLIO_3_13,
    T_MALATTIA_FIGLIO_3_14,
    T_PB,
    T_EMPLOYEE,
    ;
  }

  public enum DefaultGroup {
    G_18, G_19, G_661, 
    G_23, G_25, G_232, G_252, G_233, G_253,

    G_89, G_09, MISSIONE, ALTRI, FERIE_CNR, RIPOSI_CNR, G_95,
    MALATTIA, 
    MALATTIA_FIGLIO_1_12,
    MALATTIA_FIGLIO_1_13,
    MALATTIA_FIGLIO_1_14,
    MALATTIA_FIGLIO_2_12,
    MALATTIA_FIGLIO_2_13,
    MALATTIA_FIGLIO_2_14,
    MALATTIA_FIGLIO_3_12,
    MALATTIA_FIGLIO_3_13,
    MALATTIA_FIGLIO_3_14,
    PB,
    EMPLOYEE,
    ;
  }

  
}
