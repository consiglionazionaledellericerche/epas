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
    return computeChainDescription();
  }
  
  /**
   * La stringa che rappresenta la catena cui appartiene il gruppo.
   * @return chainDescription
   */
  public String computeChainDescription() {
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
    
    G_18("18 - Permesso assistenza parenti/affini disabili L. 104/92 tre giorni mese", 
        "", 
        DefaultCategoryType.L_104, 
        GroupAbsenceTypePattern.programmed, PeriodType.month, 
        DefaultTakable.T_18, DefaultComplation.C_18, null, false, false),
    
    G_19("19 - Permesso per dipendente disabile L. 104/92 tre giorni mese", 
        "", 
        DefaultCategoryType.L_104, 
        GroupAbsenceTypePattern.programmed, PeriodType.month, 
        DefaultTakable.T_19, DefaultComplation.C_19, null, false, false),
    G_661("661 - Permesso orario per motivi personali 18 ore anno", 
        "", 
        DefaultCategoryType.PERMESSI_VARI, 
        GroupAbsenceTypePattern.programmed, PeriodType.year, 
        DefaultTakable.T_661, DefaultComplation.C_661, null, false, false),
    G_89("89 - Permesso diritto allo studio 150 ore anno", 
        "", 
        DefaultCategoryType.PERMESSI_VARI, 
        GroupAbsenceTypePattern.programmed, PeriodType.year, 
        DefaultTakable.T_89, DefaultComplation.C_89, null, false, false),
    
    G_09("09 - Permesso visita medica", 
        "", 
        DefaultCategoryType.PERMESSI_VARI, 
        GroupAbsenceTypePattern.programmed, PeriodType.always, 
        DefaultTakable.T_09, DefaultComplation.C_09, null, false, false),
    MISSIONE("Missione", 
        "", 
        DefaultCategoryType.MISSIONE_CNR, 
        GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
        DefaultTakable.T_MISSIONE, null, null, false, false),
    FERIE_CNR("Ferie e permessi legge CNR", 
        "", 
        DefaultCategoryType.FERIE_CNR, 
        GroupAbsenceTypePattern.vacationsCnr, PeriodType.always, 
        DefaultTakable.T_FERIE_CNR, null, null, false, false),
    RIPOSI_CNR("Riposi compensativi CNR", 
        "", 
        DefaultCategoryType.RIPOSI_COMPENSATIVI_CNR, 
        GroupAbsenceTypePattern.compensatoryRestCnr, PeriodType.always, 
        DefaultTakable.T_RIPOSI_CNR, null, null, false, false),
    MALATTIA("111 - Malattia", 
        "", 
        DefaultCategoryType.MALATTIA_DIPENDENTE, 
        GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
        DefaultTakable.T_MALATTIA, null, null, false, false),
    MALATTIA_FIGLIO_1_12("12 - Malattia primo figlio <= 3 anni retribuita 100%", 
        "", 
        DefaultCategoryType.MALATTIA_FIGLIO_1, 
        GroupAbsenceTypePattern.programmed, PeriodType.child1_0_3, 
        DefaultTakable.T_MALATTIA_FIGLIO_1_12, null, null, false, false),
    MALATTIA_FIGLIO_1_13("13 - Malattia primo figlio oltre 3 anni non retribuita", 
        "", 
        DefaultCategoryType.MALATTIA_FIGLIO_1, 
        GroupAbsenceTypePattern.programmed, PeriodType.child1_3_12, 
        DefaultTakable.T_MALATTIA_FIGLIO_1_13, null, null, false, false),
    MALATTIA_FIGLIO_1_14("14 - Malattia primo figlio <= 3 anni non retribuita", 
        "", 
        DefaultCategoryType.MALATTIA_FIGLIO_1, 
        GroupAbsenceTypePattern.programmed, PeriodType.child1_0_3, 
        DefaultTakable.T_MALATTIA_FIGLIO_1_14, null, null, false, false),
    MALATTIA_FIGLIO_2_12("122 - Malattia secondo figlio <= 3 anni retribuita 100%", 
        "", 
        DefaultCategoryType.MALATTIA_FIGLIO_2, 
        GroupAbsenceTypePattern.programmed, PeriodType.child2_0_3, 
        DefaultTakable.T_MALATTIA_FIGLIO_2_12, null, null, false, false),
    MALATTIA_FIGLIO_2_13("132 - Malattia secondo figlio oltre 3 anni non retribuita", 
        "", 
        DefaultCategoryType.MALATTIA_FIGLIO_2, 
        GroupAbsenceTypePattern.programmed, PeriodType.child2_3_12, 
        DefaultTakable.T_MALATTIA_FIGLIO_2_13, null, null, false, false),
    G_24("24 - Astensione facoltativa post partum non retrib. primo figlio 0-12 anni 600 giorni", 
        "", 
        DefaultCategoryType.CONGEDI_PARENTALI, 
        GroupAbsenceTypePattern.programmed, PeriodType.child1_0_12, 
        DefaultTakable.T_24, DefaultComplation.C_24, null, false, true),
    G_25("25 - Astensione facoltativa post partum 30% primo figlio 0-6 anni 150 giorni", 
        "", 
        DefaultCategoryType.CONGEDI_PARENTALI, 
        GroupAbsenceTypePattern.programmed, PeriodType.child1_0_6, 
        DefaultTakable.T_25, DefaultComplation.C_25, DefaultGroup.G_24, false, true),
    G_23("23 - Astensione facoltativa post partum 100% primo figlio 0-12 anni 30 giorni", 
        "23/25/24 - Astensione facoltativa post partum primo figlio", 
        DefaultCategoryType.CONGEDI_PARENTALI, 
        GroupAbsenceTypePattern.programmed, PeriodType.child1_0_12, 
        DefaultTakable.T_23, DefaultComplation.C_23, DefaultGroup.G_25, false, true),
    
    G_242("242 - Astensione facoltativa post partum non retrib. secondo figlio 0-12 anni 600 giorni", 
        "", 
        DefaultCategoryType.CONGEDI_PARENTALI, 
        GroupAbsenceTypePattern.programmed, PeriodType.child2_0_12, 
        DefaultTakable.T_242, DefaultComplation.C_242, null, false, true),
    G_252("252 - Astensione facoltativa post partum 30% secondo figlio 0-6 anni 150 giorni", 
        "", 
        DefaultCategoryType.CONGEDI_PARENTALI, 
        GroupAbsenceTypePattern.programmed, PeriodType.child2_0_6, 
        DefaultTakable.T_252, DefaultComplation.C_252, DefaultGroup.G_242, false, true),
    G_232("232 - Astensione facoltativa post partum 100% secondo figlio 0-12 anni 30 giorni", 
        "232/252/242 - Astensione facoltativa post partum secondo figlio", 
        DefaultCategoryType.CONGEDI_PARENTALI, 
        GroupAbsenceTypePattern.programmed, PeriodType.child2_0_12, 
        DefaultTakable.T_232, DefaultComplation.C_232, DefaultGroup.G_252, false, true),
   
    G_243("243 - Astensione facoltativa post partum non retrib. terzo figlio 0-12 anni 600 giorni", 
        "", DefaultCategoryType.CONGEDI_PARENTALI, 
        GroupAbsenceTypePattern.programmed, PeriodType.child3_0_12, 
        DefaultTakable.T_243, DefaultComplation.C_243, null, false, true),
    G_253("253 - Astensione facoltativa post partum 30% terzo figlio 0-6 anni 150 giorni", 
        "", DefaultCategoryType.CONGEDI_PARENTALI, 
        GroupAbsenceTypePattern.programmed, PeriodType.child3_0_6, 
        DefaultTakable.T_253, DefaultComplation.C_253, DefaultGroup.G_243, false, true),
    G_233("233 - Astensione facoltativa post partum 100% terzo figlio 0-12 anni 30 giorni", 
        "233/253/243 - Astensione facoltativa post partum terzo figlio", 
        DefaultCategoryType.CONGEDI_PARENTALI, 
        GroupAbsenceTypePattern.programmed, PeriodType.child3_0_12, 
        DefaultTakable.T_233, DefaultComplation.C_233, DefaultGroup.G_253, false, true),
    
    MALATTIA_FIGLIO_2_14("142 - Malattia secondo figlio <= 3 anni non retribuita", 
        "", 
        DefaultCategoryType.MALATTIA_FIGLIO_2, 
        GroupAbsenceTypePattern.programmed, PeriodType.child2_0_3, 
        DefaultTakable.T_MALATTIA_FIGLIO_2_14, null, null, false, false),
    MALATTIA_FIGLIO_3_12("123 - Malattia terzo figlio <= 3 anni retribuita 100%", 
        "", 
        DefaultCategoryType.MALATTIA_FIGLIO_3, 
        GroupAbsenceTypePattern.programmed, PeriodType.child3_0_3, 
        DefaultTakable.T_MALATTIA_FIGLIO_3_12, null, null, false, false),
    MALATTIA_FIGLIO_3_13("133 - Malattia terzo figlio oltre 3 anni non retribuita", 
        "", 
        DefaultCategoryType.MALATTIA_FIGLIO_3, 
        GroupAbsenceTypePattern.programmed, PeriodType.child3_3_12, 
        DefaultTakable.T_MALATTIA_FIGLIO_3_13, null, null, false, false),
    MALATTIA_FIGLIO_3_14("143 - Malattia terzo figlio <= 3 anni non retribuita", 
        "", 
        DefaultCategoryType.MALATTIA_FIGLIO_3, 
        GroupAbsenceTypePattern.programmed, PeriodType.child3_0_3, 
        DefaultTakable.T_MALATTIA_FIGLIO_3_14, null, null, false, false),
    PB("PB - Permesso breve 36 ore anno", 
        "", 
        DefaultCategoryType.CODICI_AUTOMATICI, 
        GroupAbsenceTypePattern.programmed, PeriodType.year, 
        DefaultTakable.T_PB, null, null, false, false),
    EMPLOYEE("Codici inseribili dai dipendenti", 
        "", 
        DefaultCategoryType.CODICI_DIPENDENTI, 
        GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
        DefaultTakable.T_EMPLOYEE, null, null, false, false);
    
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
    
    private DefaultGroup(String description, 
        String chainDescription, 
        DefaultCategoryType category, 
        GroupAbsenceTypePattern pattern, PeriodType periodType, 
        DefaultTakable takable, DefaultComplation complation, DefaultGroup nextGroupToCheck, 
        boolean automatic, boolean initializable) {

      this.description = description;
      this.chainDescription = chainDescription;
      this.category = category;
      this.pattern = pattern;
      this.periodType = periodType;
      this.takable = takable;
      this.complation = complation;
      this.nextGroupToCheck = nextGroupToCheck;
      this.automatic = automatic;
      this.initializable = initializable;

    }
    
    
    
    
  }
  
}
