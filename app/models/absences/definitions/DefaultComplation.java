package models.absences.definitions;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;

import models.absences.AmountType;
import models.absences.ComplationAbsenceBehaviour;

import org.assertj.core.util.Lists;

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
  
  C_182(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_182M), 
      ImmutableSet.of(DefaultAbsenceType.A_182H1, 
          DefaultAbsenceType.A_182H2, 
          DefaultAbsenceType.A_182H3, 
          DefaultAbsenceType.A_182H4, 
          DefaultAbsenceType.A_182H5, 
          DefaultAbsenceType.A_182H6, 
          DefaultAbsenceType.A_182H7, 
          DefaultAbsenceType.A_182H8, 
          DefaultAbsenceType.A_182H9)),
  
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
  
  C_25P(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_25PM), 
      ImmutableSet.of(DefaultAbsenceType.A_25PH7)),
  
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
  
  /**
   * Ricerca i completamenti modellati e non presenti fra quelle passate in arg (db). 
   * @return list
   */
  public static List<DefaultComplation> missing(List<ComplationAbsenceBehaviour> allComplations) {
    List<DefaultComplation> missing = Lists.newArrayList();
    for (DefaultComplation defaultComplation : DefaultComplation.values()) {
      boolean found = false;
      for (ComplationAbsenceBehaviour complation : allComplations) {
        if (defaultComplation.name().equals(complation.name)) {
          found = true;
          break;
        }
      }
      if (!found) {
        missing.add(defaultComplation);
      }
    }
    return missing;
  }
  
  /**
   * L'enumerato corrispettivo del takable (se esiste...) 
   * @return optional dell'enumerato
   */
  public static Optional<DefaultComplation> byName(ComplationAbsenceBehaviour complation) {
    for (DefaultComplation defaultComplation : DefaultComplation.values()) {
      if (defaultComplation.name().equals(complation.name)) {
        return Optional.of(defaultComplation);
      }
    }
    return Optional.absent();
  }
}