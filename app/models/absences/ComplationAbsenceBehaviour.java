package models.absences;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import lombok.Getter;

import models.absences.AbsenceType.DefaultAbsenceType;
import models.base.BaseModel;

import org.hibernate.envers.Audited;

@Audited
@Entity
@Table(name = "complation_absence_behaviours")
public class ComplationAbsenceBehaviour extends BaseModel {

  private static final long serialVersionUID = 3990946316183363917L;
  public static final String NAME_PREFIX = "C_";

  @Column(name = "name")
  public String name;
  
  @OneToMany(mappedBy = "complationAbsenceBehaviour", fetch = FetchType.LAZY)
  public Set<GroupAbsenceType> groupAbsenceTypes = Sets.newHashSet();
  
  @Getter
  @Column(name = "amount_type")
  @Enumerated(EnumType.STRING)
  public AmountType amountType;

  @Getter
  @ManyToMany
  @JoinTable(name = "complation_codes_group", 
        joinColumns = { @JoinColumn(name = "complation_behaviour_id") }, 
        inverseJoinColumns = { @JoinColumn(name = "absence_types_id") })
  @OrderBy("code")
  public Set<AbsenceType> complationCodes = Sets.newHashSet();

  @Getter
  @ManyToMany
  @JoinTable(name = "replacing_codes_group", 
        joinColumns = { @JoinColumn(name = "complation_behaviour_id") }, 
        inverseJoinColumns = { @JoinColumn(name = "absence_types_id") })
  @OrderBy("code")
  public Set<AbsenceType> replacingCodes = Sets.newHashSet();

  /**
   * Comportamenti di completamento di default.
   * 
   * @author alessandro
   *
   */
  public enum DefaultComplation {
    
    C_18(AmountType.minutes, 
        ImmutableSet.of(DefaultAbsenceType.A_18M), 
        ImmutableSet.of(DefaultAbsenceType.A_18H1, 
            DefaultAbsenceType.A_18H2, 
            DefaultAbsenceType.A_18H3, 
            DefaultAbsenceType.A_18H4, 
            DefaultAbsenceType.A_18H5, 
            DefaultAbsenceType.A_18H6, 
            DefaultAbsenceType.A_18H7, 
            DefaultAbsenceType.A_18H8, 
            DefaultAbsenceType.A_18H9)),
    
    C_19(AmountType.minutes, 
        ImmutableSet.of(DefaultAbsenceType.A_19M), 
        ImmutableSet.of(DefaultAbsenceType.A_19H1, 
            DefaultAbsenceType.A_19H2, 
            DefaultAbsenceType.A_19H3, 
            DefaultAbsenceType.A_19H4, 
            DefaultAbsenceType.A_19H5, 
            DefaultAbsenceType.A_19H6, 
            DefaultAbsenceType.A_19H7, 
            DefaultAbsenceType.A_19H8, 
            DefaultAbsenceType.A_19H9)),

    C_661(AmountType.minutes, 
        ImmutableSet.of(DefaultAbsenceType.A_661M), 
        ImmutableSet.of(DefaultAbsenceType.A_661H1, 
            DefaultAbsenceType.A_661H2, 
            DefaultAbsenceType.A_661H3, 
            DefaultAbsenceType.A_661H4, 
            DefaultAbsenceType.A_661H5, 
            DefaultAbsenceType.A_661H6, 
            DefaultAbsenceType.A_661H7, 
            DefaultAbsenceType.A_661H8, 
            DefaultAbsenceType.A_661H9)),
    
    C_89(AmountType.units, 
        ImmutableSet.of(DefaultAbsenceType.A_89M), 
        ImmutableSet.of(DefaultAbsenceType.A_89)),
    
    C_09(AmountType.units, 
        ImmutableSet.of(DefaultAbsenceType.A_09M), 
        ImmutableSet.of(DefaultAbsenceType.A_09B)),
    
    C_23(AmountType.units, 
        ImmutableSet.of(DefaultAbsenceType.A_23M), 
        ImmutableSet.of(DefaultAbsenceType.A_23H7)),
    C_25(AmountType.units, 
        ImmutableSet.of(DefaultAbsenceType.A_25M), 
        ImmutableSet.of(DefaultAbsenceType.A_25H7)),
    C_24(AmountType.units, 
        ImmutableSet.of(DefaultAbsenceType.A_24M), 
        ImmutableSet.of(DefaultAbsenceType.A_24H7)),
    
    C_232(AmountType.units, 
        ImmutableSet.of(DefaultAbsenceType.A_232M), 
        ImmutableSet.of(DefaultAbsenceType.A_232H7)),
    C_252(AmountType.units, 
        ImmutableSet.of(DefaultAbsenceType.A_252M), 
        ImmutableSet.of(DefaultAbsenceType.A_252H7)),
    C_242(AmountType.units, 
        ImmutableSet.of(DefaultAbsenceType.A_242M), 
        ImmutableSet.of(DefaultAbsenceType.A_242H7)),
    
    C_233(AmountType.units, 
        ImmutableSet.of(DefaultAbsenceType.A_233M), 
        ImmutableSet.of(DefaultAbsenceType.A_233H7)),
    C_253(AmountType.units, 
        ImmutableSet.of(DefaultAbsenceType.A_253M), 
        ImmutableSet.of(DefaultAbsenceType.A_253H7)),
    C_243(AmountType.units, 
        ImmutableSet.of(DefaultAbsenceType.A_243M), 
        ImmutableSet.of(DefaultAbsenceType.A_243H7));
    
    public AmountType amountType;
    public Set<DefaultAbsenceType> complationCodes;
    public Set<DefaultAbsenceType> replacingCodes;

    private DefaultComplation(AmountType amountType,
        Set<DefaultAbsenceType> complationCodes, Set<DefaultAbsenceType> replacingCodes) {
      this.amountType = amountType;
      this.complationCodes = complationCodes;
      this.replacingCodes = replacingCodes;
    }
  }

}
