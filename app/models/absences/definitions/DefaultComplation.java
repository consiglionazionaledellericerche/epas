/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
 * @author Alessandro Martelli
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
  
  C_18P(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_18PM), 
      ImmutableSet.of(DefaultAbsenceType.A_18PH7)),
  
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
  
  C_183(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_183M), 
      ImmutableSet.of(DefaultAbsenceType.A_183H1, 
          DefaultAbsenceType.A_183H2, 
          DefaultAbsenceType.A_183H3, 
          DefaultAbsenceType.A_183H4, 
          DefaultAbsenceType.A_183H5, 
          DefaultAbsenceType.A_183H6, 
          DefaultAbsenceType.A_183H7, 
          DefaultAbsenceType.A_183H8, 
          DefaultAbsenceType.A_183H9)),

  C_184(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_184M), 
      ImmutableSet.of(DefaultAbsenceType.A_184H1, 
          DefaultAbsenceType.A_184H2, 
          DefaultAbsenceType.A_184H3, 
          DefaultAbsenceType.A_184H4, 
          DefaultAbsenceType.A_184H5, 
          DefaultAbsenceType.A_184H6, 
          DefaultAbsenceType.A_184H7, 
          DefaultAbsenceType.A_184H8, 
          DefaultAbsenceType.A_184H9)),
  
  C_182P(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_182PM), 
      ImmutableSet.of(DefaultAbsenceType.A_182PH7)),  
  
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
  
  C_19P(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_19PM), 
      ImmutableSet.of(DefaultAbsenceType.A_19PH7)),
  
  C_7(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_7M), 
      ImmutableSet.of(DefaultAbsenceType.A_71, 
          DefaultAbsenceType.A_72, 
          DefaultAbsenceType.A_73, 
          DefaultAbsenceType.A_74, 
          DefaultAbsenceType.A_75, 
          DefaultAbsenceType.A_76, 
          DefaultAbsenceType.A_77)),
  
  C_7D(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_7DM), 
      ImmutableSet.of(DefaultAbsenceType.A_71D, 
          DefaultAbsenceType.A_72D, 
          DefaultAbsenceType.A_73D, 
          DefaultAbsenceType.A_74D, 
          DefaultAbsenceType.A_75D, 
          DefaultAbsenceType.A_76D, 
          DefaultAbsenceType.A_77D)),
  
  C_7R(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_7RM), 
      ImmutableSet.of(DefaultAbsenceType.A_71R, 
          DefaultAbsenceType.A_72R, 
          DefaultAbsenceType.A_73R, 
          DefaultAbsenceType.A_74R, 
          DefaultAbsenceType.A_75R, 
          DefaultAbsenceType.A_76R, 
          DefaultAbsenceType.A_77R)),
  

  C_661(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_661MO, DefaultAbsenceType.A_661M), 
      ImmutableSet.of(DefaultAbsenceType.A_661H1, 
          DefaultAbsenceType.A_661H2, 
          DefaultAbsenceType.A_661H3, 
          DefaultAbsenceType.A_661H4, 
          DefaultAbsenceType.A_661H5, 
          DefaultAbsenceType.A_661H6, 
          DefaultAbsenceType.A_661H7, 
          DefaultAbsenceType.A_661H8, 
          DefaultAbsenceType.A_661H9)),
  C_92(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_92M), 
      ImmutableSet.of(DefaultAbsenceType.A_92H1, 
          DefaultAbsenceType.A_92H2, 
          DefaultAbsenceType.A_92H3, 
          DefaultAbsenceType.A_92H4, 
          DefaultAbsenceType.A_92H5, 
          DefaultAbsenceType.A_92H6, 
          DefaultAbsenceType.A_92H7)),
  
  C_20(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_20M), 
      ImmutableSet.of(DefaultAbsenceType.A_20H1, 
          DefaultAbsenceType.A_20H2, 
          DefaultAbsenceType.A_20H3, 
          DefaultAbsenceType.A_20H4, 
          DefaultAbsenceType.A_20H5, 
          DefaultAbsenceType.A_20H6, 
          DefaultAbsenceType.A_20H7)),
  
 
  C_92E(AmountType.units,
      ImmutableSet.of(DefaultAbsenceType.A_92E),
      ImmutableSet.of(DefaultAbsenceType.A_92E)),  
  
  
  C_631(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_631M), 
      ImmutableSet.of(DefaultAbsenceType.A_631H1, 
          DefaultAbsenceType.A_631H2, 
          DefaultAbsenceType.A_631H3, 
          DefaultAbsenceType.A_631H4, 
          DefaultAbsenceType.A_631H5, 
          DefaultAbsenceType.A_631H6)),
  
  C_632(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_632M), 
      ImmutableSet.of(DefaultAbsenceType.A_632H1, 
          DefaultAbsenceType.A_632H2, 
          DefaultAbsenceType.A_632H3, 
          DefaultAbsenceType.A_632H4, 
          DefaultAbsenceType.A_632H5, 
          DefaultAbsenceType.A_632H6,
          DefaultAbsenceType.A_632H7)),
  
  C_633(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_633M), 
      ImmutableSet.of(DefaultAbsenceType.A_633H1, 
          DefaultAbsenceType.A_633H2, 
          DefaultAbsenceType.A_633H3, 
          DefaultAbsenceType.A_633H4, 
          DefaultAbsenceType.A_633H5, 
          DefaultAbsenceType.A_633H6,
          DefaultAbsenceType.A_633H7)),

  C_STUDIO(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_89M), 
      ImmutableSet.of(DefaultAbsenceType.A_89)),

  C_0(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_0M),
      ImmutableSet.of(DefaultAbsenceType.A_01, DefaultAbsenceType.A_02, 
          DefaultAbsenceType.A_03, 
          DefaultAbsenceType.A_04, DefaultAbsenceType.A_05, DefaultAbsenceType.A_06,
          DefaultAbsenceType.A_07, DefaultAbsenceType.A_08)),
  
  C_OA(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_OAM), 
      ImmutableSet.of(DefaultAbsenceType.A_OA1, 
          DefaultAbsenceType.A_OA2, 
          DefaultAbsenceType.A_OA3, 
          DefaultAbsenceType.A_OA4, 
          DefaultAbsenceType.A_OA5, 
          DefaultAbsenceType.A_OA6, 
          DefaultAbsenceType.A_OA7)),

  C_43(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_43), 
      ImmutableSet.of(DefaultAbsenceType.A_43)),
  
  C_45(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_45), 
      ImmutableSet.of(DefaultAbsenceType.A_45)),
  
  //  C_COV50(AmountType.units, 
  //      ImmutableSet.of(DefaultAbsenceType.A_COV50M), 
  //      ImmutableSet.of(DefaultAbsenceType.A_COV50H)),
  //  
  //  C_COV00(AmountType.units, 
  //      ImmutableSet.of(DefaultAbsenceType.A_COV00M), 
  //      ImmutableSet.of(DefaultAbsenceType.A_COV00H)),

  C_23(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_23M), 
      ImmutableSet.of(DefaultAbsenceType.A_23H7)),
  C_25O(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_25OM), 
      ImmutableSet.of(DefaultAbsenceType.A_25OH7)),
  C_25(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_25M), 
      ImmutableSet.of(DefaultAbsenceType.A_25H7)),
  C_25A(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_25AM), 
      ImmutableSet.of(DefaultAbsenceType.A_25AH7)),
  C_25S(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_25SM), 
      ImmutableSet.of(DefaultAbsenceType.A_25SH7)),
  C_24(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_24O), 
      ImmutableSet.of(DefaultAbsenceType.A_24OH7)),
  C_24PROV(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_24MPROV), 
      ImmutableSet.of(DefaultAbsenceType.A_24H7PROV)),
  C_242PROV(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_242MPROV), 
      ImmutableSet.of(DefaultAbsenceType.A_242H7PROV)),
  C_243PROV(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_243MPROV), 
      ImmutableSet.of(DefaultAbsenceType.A_243H7PROV)),
 
  
  C_25P(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_25PM), 
      ImmutableSet.of(DefaultAbsenceType.A_25PH7)),
  
  C_232(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_232M), 
      ImmutableSet.of(DefaultAbsenceType.A_232H7)),
  C_252O(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_252OM), 
      ImmutableSet.of(DefaultAbsenceType.A_252OH7)),
  C_252(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_252M), 
      ImmutableSet.of(DefaultAbsenceType.A_252H7)),
  
  C_252S(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_252SM), 
      ImmutableSet.of(DefaultAbsenceType.A_252SH7)),
  
  C_252A(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_252AM), 
      ImmutableSet.of(DefaultAbsenceType.A_252AH7)),
  C_242(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_242M), 
      ImmutableSet.of(DefaultAbsenceType.A_242H7)),
  
  C_233(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_233M), 
      ImmutableSet.of(DefaultAbsenceType.A_233H7)),
  C_253O(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_253OM), 
      ImmutableSet.of(DefaultAbsenceType.A_253OH7)),
  C_253(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_253M), 
      ImmutableSet.of(DefaultAbsenceType.A_253H7)),
  C_253A(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_253AM), 
      ImmutableSet.of(DefaultAbsenceType.A_253AH7)),
  C_243(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_243M), 
      ImmutableSet.of(DefaultAbsenceType.A_243H7)),
  
  C_234(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_234M), 
      ImmutableSet.of(DefaultAbsenceType.A_234H7)),
  C_254O(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_254OM), 
      ImmutableSet.of(DefaultAbsenceType.A_254OH7)),
  C_254(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_254M), 
      ImmutableSet.of(DefaultAbsenceType.A_254H7)),
  C_254A(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_254AM), 
      ImmutableSet.of(DefaultAbsenceType.A_254AH7)),
  C_244(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_244M), 
      ImmutableSet.of(DefaultAbsenceType.A_244H7));
  
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
   *
   * @return list
   */
  public static List<DefaultComplation> missing(List<ComplationAbsenceBehaviour> allComplations) {
    List<DefaultComplation> missing = Lists.newArrayList();
    for (DefaultComplation defaultComplation : DefaultComplation.values()) {
      boolean found = false;
      for (ComplationAbsenceBehaviour complation : allComplations) {
        if (defaultComplation.name().equals(complation.getName())) {
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
   * L'enumerato corrispettivo del takable (se esiste...).
   *
   * @return optional dell'enumerato
   */
  public static Optional<DefaultComplation> byName(ComplationAbsenceBehaviour complation) {
    for (DefaultComplation defaultComplation : DefaultComplation.values()) {
      if (defaultComplation.name().equals(complation.getName())) {
        return Optional.of(defaultComplation);
      }
    }
    return Optional.absent();
  }
}