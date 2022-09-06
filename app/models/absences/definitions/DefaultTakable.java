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
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeAmountAdjustment;
import org.assertj.core.util.Lists;

/**
 * Comportamenti di prendibilità di default.
 *
 * @author Alessandro Martelli
 *
 */
public enum DefaultTakable {

  T_18(AmountType.units,
      ImmutableSet.of(DefaultAbsenceType.A_18, 
          DefaultAbsenceType.A_18M,
          DefaultAbsenceType.A_18P,
          DefaultAbsenceType.A_18PM), 
      ImmutableSet.of(DefaultAbsenceType.A_18, DefaultAbsenceType.A_18M), 
      3, TakeAmountAdjustment.workingTimePercent),
  T_18P(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_18, 
          DefaultAbsenceType.A_18M,
          DefaultAbsenceType.A_18P,
          DefaultAbsenceType.A_18PM), 
      ImmutableSet.of(DefaultAbsenceType.A_18P, DefaultAbsenceType.A_18PM), 
      3, null),

  T_182(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_182, 
          DefaultAbsenceType.A_182M,
          DefaultAbsenceType.A_182P,
          DefaultAbsenceType.A_182PM), 
      ImmutableSet.of(DefaultAbsenceType.A_182, DefaultAbsenceType.A_182M), 
      3, TakeAmountAdjustment.workingTimePercent),
  T_182P(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_182, 
          DefaultAbsenceType.A_182M,
          DefaultAbsenceType.A_182P,
          DefaultAbsenceType.A_182PM), 
      ImmutableSet.of(DefaultAbsenceType.A_182P, DefaultAbsenceType.A_182PM), 
      3, null),

  T_19(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_19, 
          DefaultAbsenceType.A_19M,
          DefaultAbsenceType.A_19P,
          DefaultAbsenceType.A_19PM), 
      ImmutableSet.of(DefaultAbsenceType.A_19, DefaultAbsenceType.A_19M), 
      3, TakeAmountAdjustment.workingTimePercent),
  T_19P(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_19, 
          DefaultAbsenceType.A_19M,
          DefaultAbsenceType.A_19P,
          DefaultAbsenceType.A_19PM), 
      ImmutableSet.of(DefaultAbsenceType.A_19P, DefaultAbsenceType.A_19PM), 
      3, null),

  T_20(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_20M),
      ImmutableSet.of(DefaultAbsenceType.A_20M),
      -1, null),
  
  T_26(AmountType.units,
      ImmutableSet.of(DefaultAbsenceType.A_26, DefaultAbsenceType.A_26BP), 
      ImmutableSet.of(DefaultAbsenceType.A_26, DefaultAbsenceType.A_26BP), 
      -1, null),
  T_43(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_43), 
      ImmutableSet.of(DefaultAbsenceType.A_43), 
      15, null),

  T_C161718(AmountType.units,
      ImmutableSet.of(DefaultAbsenceType.A_C16, DefaultAbsenceType.A_C17, 
          DefaultAbsenceType.A_C18), 
      ImmutableSet.of(DefaultAbsenceType.A_C16, DefaultAbsenceType.A_C17, 
          DefaultAbsenceType.A_C18), 
      -1, null),

  T_661(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_661MO, DefaultAbsenceType.A_661M, 
          DefaultAbsenceType.A_661G), // taken (scala dal limite per lo storico)
      ImmutableSet.of(DefaultAbsenceType.A_661M, DefaultAbsenceType.A_661G),  // takable
      1080, TakeAmountAdjustment.workingTimePercent),

  //  T_89(AmountType.minutes, 
  //      ImmutableSet.of(DefaultAbsenceType.A_89M), 
  //      ImmutableSet.of(DefaultAbsenceType.A_89M), 
  //      9000, TakeAmountAdjustment.workingTimeAndWorkingPeriodPercent),
  
  T_STUDIO(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_89M, DefaultAbsenceType.A_99), 
      ImmutableSet.of(DefaultAbsenceType.A_89M, DefaultAbsenceType.A_99), 
      9000, TakeAmountAdjustment.workingTimePercent),
  
  T_COVID19(AmountType.units, 
    ImmutableSet.of(DefaultAbsenceType.A_COVID19, DefaultAbsenceType.A_COVID19BP), 
    ImmutableSet.of(DefaultAbsenceType.A_COVID19, DefaultAbsenceType.A_COVID19BP), 
    -1, null),
  
  T_LAGILE(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_LAGILE), 
      ImmutableSet.of(DefaultAbsenceType.A_LAGILE), 
      10, null),
  
  T_39LA(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_39LA), 
      ImmutableSet.of(DefaultAbsenceType.A_39LA), 
      -1, null),
  
  T_SMART(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_SMART), 
      ImmutableSet.of(DefaultAbsenceType.A_SMART), 
      -1, null),

  T_0(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_0M), 
      ImmutableSet.of(DefaultAbsenceType.A_0M), 
      600, null),
  
  T_631(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_631, // capire se considerare il vecchio 631 
          DefaultAbsenceType.A_631G, 
          DefaultAbsenceType.A_631M/*,
          DefaultAbsenceType.A_09M*/), 
      ImmutableSet.of(DefaultAbsenceType.A_631G,
          DefaultAbsenceType.A_631M), 
      1080, TakeAmountAdjustment.workingTimePercent), // si riduce solo se partTime
  
  T_ORE_AGGIUNTIVE(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_OAM),
      ImmutableSet.of(DefaultAbsenceType.A_OAM),
      9600, null),

  T_PERMESSI_SINDACALI(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_7M),
      ImmutableSet.of(DefaultAbsenceType.A_7M),
      -1, null),
  
  T_MISSIONE(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_92M),
      ImmutableSet.of(DefaultAbsenceType.A_92M),
      -1, null),
      
  T_MISSIONE_ESTERA(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_92E,
          DefaultAbsenceType.A_92NG),
      ImmutableSet.of(DefaultAbsenceType.A_92E,
          DefaultAbsenceType.A_92NG),
      180, null),
  
  T_MISSIONE_INTERNA(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_92,
          DefaultAbsenceType.A_92NG),
      ImmutableSet.of(DefaultAbsenceType.A_92,
          DefaultAbsenceType.A_92NG),
      240, null),
  
  T_MISSIONE_COMUNE_RESIDENZA(AmountType.units,
      ImmutableSet.of(DefaultAbsenceType.A_92RE),
      ImmutableSet.of(DefaultAbsenceType.A_92RE),
      -1, null),
  
  T_FERIE_CNR(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_31, //taken
          DefaultAbsenceType.A_32,
          DefaultAbsenceType.A_37, 
          DefaultAbsenceType.A_94), 
      ImmutableSet.of(DefaultAbsenceType.A_31, //takable 
          DefaultAbsenceType.A_32, 
          DefaultAbsenceType.A_94), 
      -1, null),
  
  T_FERIE_CNR_PROROGA_2020(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_31_2020), 
      ImmutableSet.of(DefaultAbsenceType.A_31_2020), 
      -1, null),
  
  T_FERIE_CNR_PROROGA(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_31, 
          DefaultAbsenceType.A_32, 
          DefaultAbsenceType.A_37, 
          DefaultAbsenceType.A_94),
      ImmutableSet.of(DefaultAbsenceType.A_37), 
      -1, null),

  T_ESENZ_19(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_ESENZ19), 
      ImmutableSet.of(DefaultAbsenceType.A_ESENZ19), 
      -1, null),
  
  T_RIPOSI_CNR(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_91), 
      ImmutableSet.of(DefaultAbsenceType.A_91), 
      -1, null),

  T_RIPOSI_CNR_FESTIVO(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_91F), 
      ImmutableSet.of(DefaultAbsenceType.A_91F), 
      -1, null),

  //  T_RIPOSI_CHIUSURA_ENTE(AmountType.units,
  //      ImmutableSet.of(DefaultAbsenceType.A_91CE),
  //      ImmutableSet.of(DefaultAbsenceType.A_91CE),
  //      -1, null),

  T_LAVORO_FUORI_SEDE(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_105BP), 
      ImmutableSet.of(DefaultAbsenceType.A_105BP), 
      -1, null),
  
  T_TELELAVORO(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_103), 
      ImmutableSet.of(DefaultAbsenceType.A_103), 
      -1, null),
  
  T_TELELAVORO_RT(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_103RT), 
      ImmutableSet.of(DefaultAbsenceType.A_103RT), 
      -1, null),

  T_23(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_23, 
          DefaultAbsenceType.A_23M, 
          DefaultAbsenceType.A_23U), 
      ImmutableSet.of(DefaultAbsenceType.A_23, 
          DefaultAbsenceType.A_23M, 
          DefaultAbsenceType.A_23U), 30, null),
  
  //  T_COVID50(AmountType.units, 
  //      ImmutableSet.of(DefaultAbsenceType.A_COVID50), 
  //      ImmutableSet.of(DefaultAbsenceType.A_COVID50), -1, null),
  
  T_COV50(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_COV50, 
          DefaultAbsenceType.A_COV50M), 
      ImmutableSet.of(DefaultAbsenceType.A_COV50, 
          DefaultAbsenceType.A_COV50M), -1, null),
  
  T_COV00(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_COV00, 
          DefaultAbsenceType.A_COV00M), 
      ImmutableSet.of(DefaultAbsenceType.A_COV00, 
          DefaultAbsenceType.A_COV00M), -1, null),

  T_25(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_25, 
          DefaultAbsenceType.A_25M, 
          DefaultAbsenceType.A_25U), 
      ImmutableSet.of(DefaultAbsenceType.A_25, 
          DefaultAbsenceType.A_25M, 
          DefaultAbsenceType.A_25U), 
      150, null),

  T_24(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_24, 
          DefaultAbsenceType.A_24M, 
          DefaultAbsenceType.A_24U), 
      ImmutableSet.of(DefaultAbsenceType.A_24, 
          DefaultAbsenceType.A_24M, 
          DefaultAbsenceType.A_24U), 
      150, null),

  T_232(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_232, 
          DefaultAbsenceType.A_232M, 
          DefaultAbsenceType.A_232U), 
      ImmutableSet.of(DefaultAbsenceType.A_232, 
          DefaultAbsenceType.A_232M, 
          DefaultAbsenceType.A_232U), 
      30, null),

  T_252(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_252, 
          DefaultAbsenceType.A_252M, 
          DefaultAbsenceType.A_252U), 
      ImmutableSet.of(DefaultAbsenceType.A_252, 
          DefaultAbsenceType.A_252M, 
          DefaultAbsenceType.A_252U), 
      150, null),

  T_242(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_242, 
          DefaultAbsenceType.A_242M, 
          DefaultAbsenceType.A_242U), 
      ImmutableSet.of(DefaultAbsenceType.A_242, 
          DefaultAbsenceType.A_242M, 
          DefaultAbsenceType.A_242U), 
      150, null),

  T_233(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_233, 
          DefaultAbsenceType.A_233M, 
          DefaultAbsenceType.A_233U), 
      ImmutableSet.of(DefaultAbsenceType.A_233, 
          DefaultAbsenceType.A_233M, 
          DefaultAbsenceType.A_233U), 
      30, null),

  T_253(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_253, 
          DefaultAbsenceType.A_253M, 
          DefaultAbsenceType.A_253U), 
      ImmutableSet.of(DefaultAbsenceType.A_253, 
          DefaultAbsenceType.A_253M, 
          DefaultAbsenceType.A_253U), 
      150, null),

  T_243(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_243, 
          DefaultAbsenceType.A_243M, 
          DefaultAbsenceType.A_243U), 
      ImmutableSet.of(DefaultAbsenceType.A_243, 
          DefaultAbsenceType.A_243M, 
          DefaultAbsenceType.A_243U), 
      150, null),
  
  T_234(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_234, 
          DefaultAbsenceType.A_234M, 
          DefaultAbsenceType.A_234U), 
      ImmutableSet.of(DefaultAbsenceType.A_234, 
          DefaultAbsenceType.A_234M, 
          DefaultAbsenceType.A_234U), 30, null),

  T_254(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_254, 
          DefaultAbsenceType.A_254M, 
          DefaultAbsenceType.A_254U), 
      ImmutableSet.of(DefaultAbsenceType.A_254, 
          DefaultAbsenceType.A_254M, 
          DefaultAbsenceType.A_254U), 
      150, null),

  T_244(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_244, 
          DefaultAbsenceType.A_244M, 
          DefaultAbsenceType.A_244U), 
      ImmutableSet.of(DefaultAbsenceType.A_244, 
          DefaultAbsenceType.A_244M, 
          DefaultAbsenceType.A_244U), 
      150, null),

  T_25P(AmountType.units,                             //se fosse primo figlio mettere: 
      ImmutableSet.of(DefaultAbsenceType.A_25P,       //23 23M 25 25M 25P 25PM
          DefaultAbsenceType.A_25PM), 
      ImmutableSet.of(DefaultAbsenceType.A_25P,       //23 23M 25 25M 25P 25PM
          DefaultAbsenceType.A_25PM),     
      -1, null),                                      //150

  T_MALATTIA(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_111, 
          DefaultAbsenceType.A_111FR,
          DefaultAbsenceType.A_115, 
          DefaultAbsenceType.A_116, 
          DefaultAbsenceType.A_117, 
          DefaultAbsenceType.A_118, 
          DefaultAbsenceType.A_119, 
          DefaultAbsenceType.A_11C,
          DefaultAbsenceType.A_11C5,
          DefaultAbsenceType.A_11C9,
          DefaultAbsenceType.A_11R,
          DefaultAbsenceType.A_11R5,
          DefaultAbsenceType.A_11R9,
          DefaultAbsenceType.A_11S,
          DefaultAbsenceType.A_111SCM), 
      ImmutableSet.of(DefaultAbsenceType.A_111, 
          DefaultAbsenceType.A_111FR,
          DefaultAbsenceType.A_115, 
          DefaultAbsenceType.A_116, 
          DefaultAbsenceType.A_117, 
          DefaultAbsenceType.A_118, 
          DefaultAbsenceType.A_119, 
          DefaultAbsenceType.A_11C, 
          DefaultAbsenceType.A_11C5,
          DefaultAbsenceType.A_11C9,
          DefaultAbsenceType.A_11R, 
          DefaultAbsenceType.A_11R5,
          DefaultAbsenceType.A_11R9,
          DefaultAbsenceType.A_11S,
          DefaultAbsenceType.A_111SCM), 
      -1, null),

  T_VISITE_MEDICHE_TERAPIE(AmountType.units,
      ImmutableSet.of(DefaultAbsenceType.A_111VM, 
          DefaultAbsenceType.A_119VM, 
          DefaultAbsenceType.A_115VM),
      ImmutableSet.of(DefaultAbsenceType.A_111VM, 
          DefaultAbsenceType.A_119VM, 
          DefaultAbsenceType.A_115VM), -1, null),

  T_MALATTIA_FIGLIO_1(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_12, DefaultAbsenceType.A_13, DefaultAbsenceType.A_14, 
          DefaultAbsenceType.A_12U), 
      ImmutableSet.of(DefaultAbsenceType.A_12, DefaultAbsenceType.A_13, DefaultAbsenceType.A_14, 
          DefaultAbsenceType.A_12U), 
      -1, null),

  T_MALATTIA_FIGLIO_2(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_122, DefaultAbsenceType.A_132, DefaultAbsenceType.A_142),
      ImmutableSet.of(DefaultAbsenceType.A_122, DefaultAbsenceType.A_132, DefaultAbsenceType.A_142),
      -1, null),

  T_MALATTIA_FIGLIO_3(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_123, DefaultAbsenceType.A_133, DefaultAbsenceType.A_143),
      ImmutableSet.of(DefaultAbsenceType.A_123, DefaultAbsenceType.A_133, DefaultAbsenceType.A_143),
      -1, null),
  T_MALATTIA_FIGLIO_4(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_124, DefaultAbsenceType.A_134, DefaultAbsenceType.A_144),
      ImmutableSet.of(DefaultAbsenceType.A_124, DefaultAbsenceType.A_134, DefaultAbsenceType.A_144),
      -1, null),
  
  T_441(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_441),
      ImmutableSet.of(DefaultAbsenceType.A_441),
      8, null),

  T_ALTRI(AmountType.units, 
      ImmutableSet.of(
          DefaultAbsenceType.A_102,
          DefaultAbsenceType.A_103, DefaultAbsenceType.A_103BP, 
          DefaultAbsenceType.A_105BP,

          DefaultAbsenceType.A_71, DefaultAbsenceType.A_72, DefaultAbsenceType.A_73,
          DefaultAbsenceType.A_74, DefaultAbsenceType.A_75, DefaultAbsenceType.A_76,
          DefaultAbsenceType.A_77, DefaultAbsenceType.A_78,

          DefaultAbsenceType.A_71A, DefaultAbsenceType.A_72A, 
          DefaultAbsenceType.A_73A, DefaultAbsenceType.A_74A, DefaultAbsenceType.A_75A, 
          DefaultAbsenceType.A_76A, DefaultAbsenceType.A_77A, DefaultAbsenceType.A_78A,

          DefaultAbsenceType.A_71S, DefaultAbsenceType.A_72S, 
          DefaultAbsenceType.A_73S, DefaultAbsenceType.A_74S, DefaultAbsenceType.A_75S, 
          DefaultAbsenceType.A_76S, DefaultAbsenceType.A_77S,

          DefaultAbsenceType.A_71R, DefaultAbsenceType.A_72R, 
          DefaultAbsenceType.A_73R, DefaultAbsenceType.A_74R, DefaultAbsenceType.A_75R, 
          DefaultAbsenceType.A_76R, DefaultAbsenceType.A_77R,

          DefaultAbsenceType.A_71D, DefaultAbsenceType.A_72D, 
          DefaultAbsenceType.A_73D, DefaultAbsenceType.A_74D, DefaultAbsenceType.A_75D, 
          DefaultAbsenceType.A_76D, DefaultAbsenceType.A_77D, DefaultAbsenceType.A_78D,

          DefaultAbsenceType.A_FA1, DefaultAbsenceType.A_FA2, 
          DefaultAbsenceType.A_FA3, DefaultAbsenceType.A_FA4, DefaultAbsenceType.A_FA5, 
          DefaultAbsenceType.A_FA6, DefaultAbsenceType.A_FA7,

          //          DefaultAbsenceType.A_OA1, DefaultAbsenceType.A_OA2, 
          //          DefaultAbsenceType.A_OA3, DefaultAbsenceType.A_OA4, DefaultAbsenceType.A_OA5, 
          //          DefaultAbsenceType.A_OA6, DefaultAbsenceType.A_OA7,

          DefaultAbsenceType.A_33, DefaultAbsenceType.A_33B, DefaultAbsenceType.A_33C,
          DefaultAbsenceType.A_34, DefaultAbsenceType.A_38, DefaultAbsenceType.A_39,


          DefaultAbsenceType.A_681, DefaultAbsenceType.A_682, DefaultAbsenceType.A_683, 
          DefaultAbsenceType.A_441, DefaultAbsenceType.A_6N, DefaultAbsenceType.A_442,

          DefaultAbsenceType.A_67, DefaultAbsenceType.A_80, 
          DefaultAbsenceType.A_81, DefaultAbsenceType.A_82,
          DefaultAbsenceType.A_83, DefaultAbsenceType.A_84, 
          DefaultAbsenceType.A_85, DefaultAbsenceType.A_86,
          DefaultAbsenceType.A_87, DefaultAbsenceType.A_662,
          DefaultAbsenceType.A_62S50V, DefaultAbsenceType.A_ES_L133,
          DefaultAbsenceType.A_99, DefaultAbsenceType.A_65, DefaultAbsenceType.A_61,
          DefaultAbsenceType.A_16, DefaultAbsenceType.A_42, DefaultAbsenceType.A_93,
          DefaultAbsenceType.A_408, DefaultAbsenceType.A_50,
          DefaultAbsenceType.A_NC, DefaultAbsenceType.A_62, DefaultAbsenceType.A_35R,
          DefaultAbsenceType.A_96, DefaultAbsenceType.A_96A, DefaultAbsenceType.A_96B,
          DefaultAbsenceType.A_98, DefaultAbsenceType.A_52, DefaultAbsenceType.A_100,
          DefaultAbsenceType.A_401, DefaultAbsenceType.A_412,
          DefaultAbsenceType.A_402, DefaultAbsenceType.A_62,
          DefaultAbsenceType.A_62A, DefaultAbsenceType.A_62D, DefaultAbsenceType.A_98CV, 
          DefaultAbsenceType.A_39LA, DefaultAbsenceType.A_46,
          DefaultAbsenceType.A_46RA, DefaultAbsenceType.A_VAC19),
          ImmutableSet.of(DefaultAbsenceType.A_102,
          DefaultAbsenceType.A_103, DefaultAbsenceType.A_103BP, 
          DefaultAbsenceType.A_105BP,
          DefaultAbsenceType.A_71, DefaultAbsenceType.A_72, DefaultAbsenceType.A_73,
          DefaultAbsenceType.A_74, DefaultAbsenceType.A_75, DefaultAbsenceType.A_76,
          DefaultAbsenceType.A_77, DefaultAbsenceType.A_78,
          DefaultAbsenceType.A_71A, DefaultAbsenceType.A_72A, 
          DefaultAbsenceType.A_73A, DefaultAbsenceType.A_74A, DefaultAbsenceType.A_75A, 
          DefaultAbsenceType.A_76A, DefaultAbsenceType.A_77A, DefaultAbsenceType.A_78A,
          DefaultAbsenceType.A_71S, DefaultAbsenceType.A_72S, 
          DefaultAbsenceType.A_73S, DefaultAbsenceType.A_74S, DefaultAbsenceType.A_75S, 
          DefaultAbsenceType.A_76S, DefaultAbsenceType.A_77S,
          DefaultAbsenceType.A_71R, DefaultAbsenceType.A_72R, 
          DefaultAbsenceType.A_73R, DefaultAbsenceType.A_74R, DefaultAbsenceType.A_75R, 
          DefaultAbsenceType.A_76R, DefaultAbsenceType.A_77R,
          DefaultAbsenceType.A_71D, DefaultAbsenceType.A_72D, 
          DefaultAbsenceType.A_73D, DefaultAbsenceType.A_74D, DefaultAbsenceType.A_75D, 
          DefaultAbsenceType.A_76D, DefaultAbsenceType.A_77D, DefaultAbsenceType.A_78D,
          DefaultAbsenceType.A_FA1, DefaultAbsenceType.A_FA2, 
          DefaultAbsenceType.A_FA3, DefaultAbsenceType.A_FA4, DefaultAbsenceType.A_FA5, 
          DefaultAbsenceType.A_FA6, DefaultAbsenceType.A_FA7,
          //          DefaultAbsenceType.A_OA1, DefaultAbsenceType.A_OA2, 
          //          DefaultAbsenceType.A_OA3, DefaultAbsenceType.A_OA4, DefaultAbsenceType.A_OA5, 
          //          DefaultAbsenceType.A_OA6, DefaultAbsenceType.A_OA7,
          DefaultAbsenceType.A_33, DefaultAbsenceType.A_33B, DefaultAbsenceType.A_33C,
          DefaultAbsenceType.A_34, DefaultAbsenceType.A_38, DefaultAbsenceType.A_39,
          DefaultAbsenceType.A_681, DefaultAbsenceType.A_682, DefaultAbsenceType.A_683, 
          DefaultAbsenceType.A_441, DefaultAbsenceType.A_6N, DefaultAbsenceType.A_442,
          DefaultAbsenceType.A_67, DefaultAbsenceType.A_80, 
          DefaultAbsenceType.A_81, DefaultAbsenceType.A_82,
          DefaultAbsenceType.A_83, DefaultAbsenceType.A_84, 
          DefaultAbsenceType.A_85, DefaultAbsenceType.A_86,
          DefaultAbsenceType.A_87, DefaultAbsenceType.A_662,
          DefaultAbsenceType.A_62S50V, DefaultAbsenceType.A_ES_L133,
          DefaultAbsenceType.A_99, DefaultAbsenceType.A_65, DefaultAbsenceType.A_61,
          DefaultAbsenceType.A_16, DefaultAbsenceType.A_42, DefaultAbsenceType.A_93,
          DefaultAbsenceType.A_408, DefaultAbsenceType.A_50,
          DefaultAbsenceType.A_NC, DefaultAbsenceType.A_62, DefaultAbsenceType.A_35R,
          DefaultAbsenceType.A_96, DefaultAbsenceType.A_96A, DefaultAbsenceType.A_96B,
          DefaultAbsenceType.A_98, DefaultAbsenceType.A_52, DefaultAbsenceType.A_100,
          DefaultAbsenceType.A_401, DefaultAbsenceType.A_412,
          DefaultAbsenceType.A_402, DefaultAbsenceType.A_62,
          DefaultAbsenceType.A_62A, DefaultAbsenceType.A_62D, DefaultAbsenceType.A_98CV,
          DefaultAbsenceType.A_39LA, DefaultAbsenceType.A_46,
          DefaultAbsenceType.A_46RA, DefaultAbsenceType.A_VAC19),
      -1, null),

  T_CONGEDO_MATRIMONIO(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_45), 
      ImmutableSet.of(DefaultAbsenceType.A_45), 
      -1, null),

  T_681(AmountType.units,
      ImmutableSet.of(DefaultAbsenceType.A_681),
      ImmutableSet.of(DefaultAbsenceType.A_681),
      3, null),
  T_682(AmountType.units,
      ImmutableSet.of(DefaultAbsenceType.A_682),
      ImmutableSet.of(DefaultAbsenceType.A_682),
      3, null),
  T_683(AmountType.units,
      ImmutableSet.of(DefaultAbsenceType.A_683),
      ImmutableSet.of(DefaultAbsenceType.A_683),
      3, null),
  
  T_6N(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_6N),
      ImmutableSet.of(DefaultAbsenceType.A_6N),
      30, null),
  
  T_6NTD(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_6NTD),
      ImmutableSet.of(DefaultAbsenceType.A_6NTD),
      15, null),

  T_CONGEDI_PRENATALI(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_20, DefaultAbsenceType.A_21), 
      ImmutableSet.of(DefaultAbsenceType.A_20, DefaultAbsenceType.A_21), 
      -1, null),

  T_ASPETTATIVA(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_54, DefaultAbsenceType.A_54C, DefaultAbsenceType.A_54D,
          DefaultAbsenceType.A_54DD,
          DefaultAbsenceType.A_54E, DefaultAbsenceType.A_54F, DefaultAbsenceType.A_54P,
          DefaultAbsenceType.A_54A17, DefaultAbsenceType.A_54CNR, DefaultAbsenceType.A_54ORGPP,
          DefaultAbsenceType.A_54L230, DefaultAbsenceType.A_54R, DefaultAbsenceType.A_54VV,
          DefaultAbsenceType.A_54VVH7),
      ImmutableSet.of(DefaultAbsenceType.A_54, DefaultAbsenceType.A_54C, DefaultAbsenceType.A_54D,
          DefaultAbsenceType.A_54DD,
          DefaultAbsenceType.A_54E, DefaultAbsenceType.A_54F, DefaultAbsenceType.A_54P,
          DefaultAbsenceType.A_54A17, DefaultAbsenceType.A_54CNR, DefaultAbsenceType.A_54ORGPP,
          DefaultAbsenceType.A_54L230, DefaultAbsenceType.A_54R, DefaultAbsenceType.A_54VV,
          DefaultAbsenceType.A_54VVH7), 
      -1, null),

  T_PUBBLICA_FUNZIONE(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_95001, DefaultAbsenceType.A_95002,
          DefaultAbsenceType.A_95003, DefaultAbsenceType.A_95004, DefaultAbsenceType.A_95005,
          DefaultAbsenceType.A_95006, DefaultAbsenceType.A_95007, DefaultAbsenceType.A_9599B, 
          DefaultAbsenceType.A_9591,  DefaultAbsenceType.A_9599, DefaultAbsenceType.A_95041E,
          DefaultAbsenceType.A_9505, DefaultAbsenceType.A_95042E, DefaultAbsenceType.A_95043E, 
          DefaultAbsenceType.A_95044E, DefaultAbsenceType.A_95045E, DefaultAbsenceType.A_95046E,
          DefaultAbsenceType.A_95047E, DefaultAbsenceType.A_95E, DefaultAbsenceType.A_95N,
          DefaultAbsenceType.A_9504, DefaultAbsenceType.A_95053, DefaultAbsenceType.A_58,
          DefaultAbsenceType.A_95051S, DefaultAbsenceType.A_95052S, DefaultAbsenceType.A_95053S,
          DefaultAbsenceType.A_95054S, DefaultAbsenceType.A_95055S, DefaultAbsenceType.A_95056S,
          DefaultAbsenceType.A_95057S, DefaultAbsenceType.A_95051E, DefaultAbsenceType.A_95052E,
          DefaultAbsenceType.A_95053E, DefaultAbsenceType.A_95054E, DefaultAbsenceType.A_95055E,
          DefaultAbsenceType.A_95056E, DefaultAbsenceType.A_95057E,
          DefaultAbsenceType.A_55, DefaultAbsenceType.A_58U),       
      ImmutableSet.of(DefaultAbsenceType.A_95001, DefaultAbsenceType.A_95002,
          DefaultAbsenceType.A_95003, DefaultAbsenceType.A_95004, DefaultAbsenceType.A_95005,
          DefaultAbsenceType.A_95006, DefaultAbsenceType.A_95007, DefaultAbsenceType.A_9599B,
          DefaultAbsenceType.A_9591, DefaultAbsenceType.A_9599, DefaultAbsenceType.A_95041E,
          DefaultAbsenceType.A_9505, DefaultAbsenceType.A_95042E, DefaultAbsenceType.A_95043E, 
          DefaultAbsenceType.A_95044E, DefaultAbsenceType.A_95045E, DefaultAbsenceType.A_95046E,
          DefaultAbsenceType.A_95047E, DefaultAbsenceType.A_95E, DefaultAbsenceType.A_95N,
          DefaultAbsenceType.A_9504, DefaultAbsenceType.A_95053, DefaultAbsenceType.A_58,
          DefaultAbsenceType.A_95051S, DefaultAbsenceType.A_95052S, DefaultAbsenceType.A_95053S,
          DefaultAbsenceType.A_95054S, DefaultAbsenceType.A_95055S, DefaultAbsenceType.A_95056S,
          DefaultAbsenceType.A_95057S, DefaultAbsenceType.A_95051E, DefaultAbsenceType.A_95052E,
          DefaultAbsenceType.A_95053E, DefaultAbsenceType.A_95054E, DefaultAbsenceType.A_95055E,
          DefaultAbsenceType.A_95056E, DefaultAbsenceType.A_95057E,
          DefaultAbsenceType.A_55, DefaultAbsenceType.A_58U), 
      -1, null),

  T_MALATTIA_3_ANNI(AmountType.units,
      ImmutableSet.of(
          DefaultAbsenceType.A_09BI, DefaultAbsenceType.A_111,
          DefaultAbsenceType.A_115, DefaultAbsenceType.A_116,
          DefaultAbsenceType.A_119, DefaultAbsenceType.A_11C,
          DefaultAbsenceType.A_11C5, DefaultAbsenceType.A_11C9,
          DefaultAbsenceType.A_11R, DefaultAbsenceType.A_11R5,
          DefaultAbsenceType.A_11R9, DefaultAbsenceType.A_11S,
          DefaultAbsenceType.A_631, DefaultAbsenceType.A_111SCM), 
      ImmutableSet.of(
          DefaultAbsenceType.A_09BI, DefaultAbsenceType.A_111,
          DefaultAbsenceType.A_115, DefaultAbsenceType.A_116,
          DefaultAbsenceType.A_119, DefaultAbsenceType.A_11C,
          DefaultAbsenceType.A_11C5, DefaultAbsenceType.A_11C9,
          DefaultAbsenceType.A_11R, DefaultAbsenceType.A_11R5,
          DefaultAbsenceType.A_11R9, DefaultAbsenceType.A_11S,
          DefaultAbsenceType.A_631, DefaultAbsenceType.A_111SCM), 
      -1, null),

  T_PB(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_PB), 
      ImmutableSet.of(DefaultAbsenceType.A_PB), 
      2160, null),

  T_RIDUCE_FERIE_CNR(AmountType.units, 
      ImmutableSet.of(
          DefaultAbsenceType.A_24, DefaultAbsenceType.A_24H7,
          DefaultAbsenceType.A_25, DefaultAbsenceType.A_25H7,
          DefaultAbsenceType.A_242, DefaultAbsenceType.A_242H7,
          DefaultAbsenceType.A_252, DefaultAbsenceType.A_252H7,
          DefaultAbsenceType.A_243, DefaultAbsenceType.A_243H7,
          DefaultAbsenceType.A_253, DefaultAbsenceType.A_253H7,
          DefaultAbsenceType.A_54L230, DefaultAbsenceType.A_50,
          DefaultAbsenceType.A_54, DefaultAbsenceType.A_54D,
          DefaultAbsenceType.A_54E, DefaultAbsenceType.A_54F, 
          DefaultAbsenceType.A_54P, DefaultAbsenceType.A_54R, 
          DefaultAbsenceType.A_54CNR, DefaultAbsenceType.A_54C, 
          DefaultAbsenceType.A_54L230, DefaultAbsenceType.A_33, 
          DefaultAbsenceType.A_96B, DefaultAbsenceType.A_96, 
          DefaultAbsenceType.A_96A, DefaultAbsenceType.A_13, 
          DefaultAbsenceType.A_132, DefaultAbsenceType.A_133, 
          DefaultAbsenceType.A_14, DefaultAbsenceType.A_142, 
          DefaultAbsenceType.A_143, DefaultAbsenceType.A_54A17,
          //DefaultAbsenceType.A_17C,
          DefaultAbsenceType.A_C17, DefaultAbsenceType.A_C18,
          DefaultAbsenceType.A_C16, DefaultAbsenceType.A_35R), 
      ImmutableSet.of(
          DefaultAbsenceType.A_24, DefaultAbsenceType.A_24H7,
          DefaultAbsenceType.A_25, DefaultAbsenceType.A_25H7,
          DefaultAbsenceType.A_242, DefaultAbsenceType.A_242H7,
          DefaultAbsenceType.A_252, DefaultAbsenceType.A_252H7,
          DefaultAbsenceType.A_243, DefaultAbsenceType.A_243H7,
          DefaultAbsenceType.A_253, DefaultAbsenceType.A_253H7,
          DefaultAbsenceType.A_54L230, DefaultAbsenceType.A_50,
          DefaultAbsenceType.A_54, DefaultAbsenceType.A_54D,
          DefaultAbsenceType.A_54E, DefaultAbsenceType.A_54F, 
          DefaultAbsenceType.A_54P, DefaultAbsenceType.A_54R, 
          DefaultAbsenceType.A_54CNR, DefaultAbsenceType.A_54C, 
          DefaultAbsenceType.A_54L230, DefaultAbsenceType.A_33, 
          DefaultAbsenceType.A_96B, DefaultAbsenceType.A_96, 
          DefaultAbsenceType.A_96A, DefaultAbsenceType.A_13, 
          DefaultAbsenceType.A_132, DefaultAbsenceType.A_133, 
          DefaultAbsenceType.A_14, DefaultAbsenceType.A_142, 
          DefaultAbsenceType.A_143, DefaultAbsenceType.A_54A17,
          //DefaultAbsenceType.A_17C,
          DefaultAbsenceType.A_C17, DefaultAbsenceType.A_C18,
          DefaultAbsenceType.A_C16, DefaultAbsenceType.A_35R), 
      -1, null),

  T_RIPOSI_CNR_ATTESTATI(AmountType.units, 
      ImmutableSet.of(
          DefaultAbsenceType.A_91, DefaultAbsenceType.A_91F/*, DefaultAbsenceType.A_91CE*/), 
      ImmutableSet.of(
          DefaultAbsenceType.A_91, DefaultAbsenceType.A_91F/*, DefaultAbsenceType.A_91CE*/), 
      -1, null);


  public AmountType amountType;
  public Set<DefaultAbsenceType> takenCodes;
  public Set<DefaultAbsenceType> takableCodes;
  public int fixedLimit;
  public TakeAmountAdjustment takableAmountAdjustment;

  private DefaultTakable(AmountType amountType,
      Set<DefaultAbsenceType> takenCodes, Set<DefaultAbsenceType> takableCodes, 
      int fixedLimit, TakeAmountAdjustment takableAmountAdjustment) {
    this.amountType = amountType;
    this.takenCodes = takenCodes;
    this.takenCodes = takenCodes;
    this.takableCodes = takableCodes;
    this.fixedLimit = fixedLimit;
    this.takableAmountAdjustment = takableAmountAdjustment;

  }

  /**
   * Ricerca i comportamenti prendibilità modellati e non presenti fra quelle passate in arg (db).
   *
   * @return list
   */
  public static List<DefaultTakable> missing(List<TakableAbsenceBehaviour> allTakables) {
    List<DefaultTakable> missing = Lists.newArrayList();
    for (DefaultTakable defaultTakable : DefaultTakable.values()) {
      boolean found = false;
      for (TakableAbsenceBehaviour takable : allTakables) {
        if (defaultTakable.name().equals(takable.name)) {
          found = true;
          break;
        }
      }
      if (!found) {
        missing.add(defaultTakable);
      }
    }
    return missing;
  }

  /**
   * L'enumerato corrispettivo del takable (se esiste...).
   *
   * @return optional dell'enumerato
   */
  public static Optional<DefaultTakable> byName(TakableAbsenceBehaviour takable) {
    for (DefaultTakable defaultTakable : DefaultTakable.values()) {
      if (defaultTakable.name().equals(takable.name)) {
        return Optional.of(defaultTakable);
      }
    }
    return Optional.absent();
  }

}