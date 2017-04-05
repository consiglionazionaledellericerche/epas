package models.absences;

import com.google.common.base.Strings;

import it.cnr.iit.epas.DateInterval;

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

import lombok.Getter;

import models.absences.CategoryGroupAbsenceType.DefaultCategoryType;
import models.absences.ComplationAbsenceBehaviour.DefaultComplation;
import models.absences.TakableAbsenceBehaviour.DefaultTakable;
import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

@Audited
@Entity
@Table(name = "group_absence_types")
public class GroupAbsenceType extends BaseModel {

  private static final long serialVersionUID = 3290760775533091791L;
  
  public static final String EMPLOYEE_NAME = "EMPLOYEE";
  public static final String REDUCING_VACATIONS_NAME = "REDUCING_VACATIONS";

  @Required
  @Column
  public String name;
  
  //Astensione facoltativa post partum 100% primo figlio 0-12 anni 30 giorni 
  @Required
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
  
  @Required
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
  
  public enum DefaultGroup {
    G_18, 
    G_19, 
    G_661, 
    
    G_23, 
    G_25, 
    G_232, 
    G_252, 
    G_233, 
    G_253,

    G_89, 
    G_09, 
    MISSIONE, 
    ALTRI, 
    FERIE_CNR, 
    RIPOSI_CNR, 
    G_95,
    
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
    FUORI_SEDE_105BP;
    
    public String description;
    public String chainDescription;
    public DefaultCategoryType category;
    public GroupAbsenceTypePattern pattern;
    public PeriodType periodType;
    public DefaultTakable takable;
    public DefaultComplation complation;
    public DefaultGroup nextGroupToCheck;
    public boolean automatic;
    public boolean initializable;
    
    
    
    
  }
  
}
