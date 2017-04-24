package models.absences.definitions;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

import models.absences.AmountType;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeAmountAdjustment;

import org.assertj.core.util.Lists;

/**
 * Comportamenti di prendibilità di default.
 * 
 * @author alessandro
 *
 */
public enum DefaultTakable {
  
  T_18(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_18, 
          DefaultAbsenceType.A_18M,
          DefaultAbsenceType.A_18P), 
      ImmutableSet.of(DefaultAbsenceType.A_18, DefaultAbsenceType.A_18M), 
      3, null),
  T_18P(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_18, 
          DefaultAbsenceType.A_18M,
          DefaultAbsenceType.A_18P), 
      ImmutableSet.of(DefaultAbsenceType.A_18P), 
      3, null),
  
  T_182(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_182, 
          DefaultAbsenceType.A_182M,
          DefaultAbsenceType.A_182P), 
      ImmutableSet.of(DefaultAbsenceType.A_182, DefaultAbsenceType.A_182M), 
      3, null),
  T_182P(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_182, 
          DefaultAbsenceType.A_182M,
          DefaultAbsenceType.A_182P), 
      ImmutableSet.of(DefaultAbsenceType.A_182P), 
      3, null),

  T_19(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_19, 
          DefaultAbsenceType.A_19M,
          DefaultAbsenceType.A_19P), 
      ImmutableSet.of(DefaultAbsenceType.A_19, DefaultAbsenceType.A_19M), 
      3, null),
  T_19P(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_19, 
          DefaultAbsenceType.A_19M, 
          DefaultAbsenceType.A_19P), 
      ImmutableSet.of(DefaultAbsenceType.A_19P), 
      3, null),
  
  T_26(AmountType.units,
      ImmutableSet.of(DefaultAbsenceType.A_26), 
      ImmutableSet.of(DefaultAbsenceType.A_26), 
      -1, null),
  
  T_C1718(AmountType.units,
      ImmutableSet.of(DefaultAbsenceType.A_C17, DefaultAbsenceType.A_C18), 
      ImmutableSet.of(DefaultAbsenceType.A_C17, DefaultAbsenceType.A_C18), 
      -1, null),

  T_661(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_661M), 
      ImmutableSet.of(DefaultAbsenceType.A_661M), 
      1080, TakeAmountAdjustment.workingTimeAndWorkingPeriodPercent),

  T_89(AmountType.minutes, 
      ImmutableSet.of(DefaultAbsenceType.A_89M), 
      ImmutableSet.of(DefaultAbsenceType.A_89M), 
      9000, TakeAmountAdjustment.workingTimeAndWorkingPeriodPercent),

  T_09(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_09M), 
      ImmutableSet.of(DefaultAbsenceType.A_09M), 
      -1, null),
  T_631(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_631), 
      ImmutableSet.of(DefaultAbsenceType.A_631), 
      -1, null),

  T_MISSIONE(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_92, 
          DefaultAbsenceType.A_92H1, 
          DefaultAbsenceType.A_92H2, 
          DefaultAbsenceType.A_92H3, 
          DefaultAbsenceType.A_92H4, 
          DefaultAbsenceType.A_92H5, 
          DefaultAbsenceType.A_92H6, 
          DefaultAbsenceType.A_92H7), 
      ImmutableSet.of(DefaultAbsenceType.A_92, 
          DefaultAbsenceType.A_92H1, 
          DefaultAbsenceType.A_92H2, 
          DefaultAbsenceType.A_92H3, 
          DefaultAbsenceType.A_92H4, 
          DefaultAbsenceType.A_92H5, 
          DefaultAbsenceType.A_92H6, 
          DefaultAbsenceType.A_92H7), 
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
  
  T_FERIE_CNR_PROROGA(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_31, 
          DefaultAbsenceType.A_32, 
          DefaultAbsenceType.A_37, 
          DefaultAbsenceType.A_94),
      ImmutableSet.of(DefaultAbsenceType.A_37), 
      -1, null),
  
  T_RIPOSI_CNR(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_91), 
      ImmutableSet.of(DefaultAbsenceType.A_91), 
      -1, null),
  
  T_LAVORO_FUORI_SEDE(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_105BP), 
      ImmutableSet.of(DefaultAbsenceType.A_105BP), 
      -1, null),

  T_23(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_23, 
          DefaultAbsenceType.A_23M, 
          DefaultAbsenceType.A_23U), 
      ImmutableSet.of(DefaultAbsenceType.A_23, 
          DefaultAbsenceType.A_23M, 
          DefaultAbsenceType.A_23U), 30, null),

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
      600, null),

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
      600, null),

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
      600, null),
  
  T_25P(AmountType.units,                             //se fosse primo figlio mettere: 
      ImmutableSet.of(DefaultAbsenceType.A_25P,       //23 23M 25 25M 25P 25PM
          DefaultAbsenceType.A_25PM), 
      ImmutableSet.of(DefaultAbsenceType.A_25P,       //23 23M 25 25M 25P 25PM
          DefaultAbsenceType.A_25PM),     
      -1, null),                                      //150

  T_MALATTIA(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_111, 
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
          DefaultAbsenceType.A_11S), 
      ImmutableSet.of(DefaultAbsenceType.A_111, 
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
          DefaultAbsenceType.A_11S), 
      -1, null),

  T_MALATTIA_FIGLIO_1_12(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_12), 
      ImmutableSet.of(DefaultAbsenceType.A_12), 
      -1, null),

  T_MALATTIA_FIGLIO_1_13(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_13), 
      ImmutableSet.of(DefaultAbsenceType.A_13), 
      -1, null),

  T_MALATTIA_FIGLIO_1_14(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_14), 
      ImmutableSet.of(DefaultAbsenceType.A_14), 
      -1, null),

  T_MALATTIA_FIGLIO_2_12(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_122), 
      ImmutableSet.of(DefaultAbsenceType.A_122), 
      -1, null),

  T_MALATTIA_FIGLIO_2_13(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_132), 
      ImmutableSet.of(DefaultAbsenceType.A_132), 
      -1, null),

  T_MALATTIA_FIGLIO_2_14(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_142), 
      ImmutableSet.of(DefaultAbsenceType.A_142), 
      -1, null),

  T_MALATTIA_FIGLIO_3_12(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_123), 
      ImmutableSet.of(DefaultAbsenceType.A_123), 
      -1, null),

  T_MALATTIA_FIGLIO_3_13(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_133), 
      ImmutableSet.of(DefaultAbsenceType.A_133), 
      -1, null),

  T_MALATTIA_FIGLIO_3_14(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_143), 
      ImmutableSet.of(DefaultAbsenceType.A_143), 
      -1, null),

  
  T_ALTRI(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_103, DefaultAbsenceType.A_103BP,
          
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
          
          DefaultAbsenceType.A_01, DefaultAbsenceType.A_02, 
          DefaultAbsenceType.A_03, DefaultAbsenceType.A_04, DefaultAbsenceType.A_05, 
          DefaultAbsenceType.A_06, DefaultAbsenceType.A_07, DefaultAbsenceType.A_08,
          
          DefaultAbsenceType.A_FA1, DefaultAbsenceType.A_FA2, 
          DefaultAbsenceType.A_FA3, DefaultAbsenceType.A_FA4, DefaultAbsenceType.A_FA5, 
          DefaultAbsenceType.A_FA6, DefaultAbsenceType.A_FA7,
          
          DefaultAbsenceType.A_OA1, DefaultAbsenceType.A_OA2, 
          DefaultAbsenceType.A_OA3, DefaultAbsenceType.A_OA4, DefaultAbsenceType.A_OA5, 
          DefaultAbsenceType.A_OA6, DefaultAbsenceType.A_OA7,
          
          DefaultAbsenceType.A_33, DefaultAbsenceType.A_33B, DefaultAbsenceType.A_33C,
          DefaultAbsenceType.A_34, DefaultAbsenceType.A_38, DefaultAbsenceType.A_39,
          
          DefaultAbsenceType.A_681, DefaultAbsenceType.A_691, 
          DefaultAbsenceType.A_441, DefaultAbsenceType.A_6N,
          DefaultAbsenceType.A_67, DefaultAbsenceType.A_80, DefaultAbsenceType.A_662,
          DefaultAbsenceType.A_62S50V, DefaultAbsenceType.A_79, DefaultAbsenceType.A_ES_L133,
          DefaultAbsenceType.A_99, DefaultAbsenceType.A_65, DefaultAbsenceType.A_61,
          DefaultAbsenceType.A_16, DefaultAbsenceType.A_42, DefaultAbsenceType.A_93),
      ImmutableSet.of(DefaultAbsenceType.A_103, DefaultAbsenceType.A_103BP, 
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
          DefaultAbsenceType.A_01, DefaultAbsenceType.A_02, 
          DefaultAbsenceType.A_03, DefaultAbsenceType.A_04, DefaultAbsenceType.A_05, 
          DefaultAbsenceType.A_06, DefaultAbsenceType.A_07, DefaultAbsenceType.A_08,
          DefaultAbsenceType.A_FA1, DefaultAbsenceType.A_FA2, 
          DefaultAbsenceType.A_FA3, DefaultAbsenceType.A_FA4, DefaultAbsenceType.A_FA5, 
          DefaultAbsenceType.A_FA6, DefaultAbsenceType.A_FA7,
          DefaultAbsenceType.A_OA1, DefaultAbsenceType.A_OA2, 
          DefaultAbsenceType.A_OA3, DefaultAbsenceType.A_OA4, DefaultAbsenceType.A_OA5, 
          DefaultAbsenceType.A_OA6, DefaultAbsenceType.A_OA7,
          DefaultAbsenceType.A_33, DefaultAbsenceType.A_33B, DefaultAbsenceType.A_33C,
          DefaultAbsenceType.A_34, DefaultAbsenceType.A_38, DefaultAbsenceType.A_39,
          DefaultAbsenceType.A_681, DefaultAbsenceType.A_691, 
          DefaultAbsenceType.A_441, DefaultAbsenceType.A_6N,
          DefaultAbsenceType.A_67, DefaultAbsenceType.A_80, DefaultAbsenceType.A_662,
          DefaultAbsenceType.A_62S50V, DefaultAbsenceType.A_79, DefaultAbsenceType.A_ES_L133,
          DefaultAbsenceType.A_99, DefaultAbsenceType.A_65, DefaultAbsenceType.A_61,
          DefaultAbsenceType.A_16, DefaultAbsenceType.A_42, DefaultAbsenceType.A_93),
      -1, null),
  
  T_CONGEDO_MATRIMONIO(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_45), 
      ImmutableSet.of(DefaultAbsenceType.A_45), 
      -1, null),
  
  T_CONGEDI_PRENATALI(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_20, DefaultAbsenceType.A_21), 
      ImmutableSet.of(DefaultAbsenceType.A_20, DefaultAbsenceType.A_21), 
      -1, null),

  T_ASPETTATIVA(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_54, DefaultAbsenceType.A_54C, DefaultAbsenceType.A_54D,
          DefaultAbsenceType.A_54E, DefaultAbsenceType.A_54F, DefaultAbsenceType.A_54P,
          DefaultAbsenceType.A_54A17, DefaultAbsenceType.A_54CNR, DefaultAbsenceType.A_54ORGPP,
          DefaultAbsenceType.A_54L230, DefaultAbsenceType.A_54R, DefaultAbsenceType.A_54VV,
          DefaultAbsenceType.A_54VVH7),
      ImmutableSet.of(DefaultAbsenceType.A_54, DefaultAbsenceType.A_54C, DefaultAbsenceType.A_54D,
          DefaultAbsenceType.A_54E, DefaultAbsenceType.A_54F, DefaultAbsenceType.A_54P,
          DefaultAbsenceType.A_54A17, DefaultAbsenceType.A_54CNR, DefaultAbsenceType.A_54ORGPP,
          DefaultAbsenceType.A_54L230, DefaultAbsenceType.A_54R, DefaultAbsenceType.A_54VV,
          DefaultAbsenceType.A_54VVH7), 
      -1, null),

  T_PUBBLICA_FUNZIONE(AmountType.units, 
      ImmutableSet.of(DefaultAbsenceType.A_9599B, 
          DefaultAbsenceType.A_9591, DefaultAbsenceType.A_9599, DefaultAbsenceType.A_95041E,
          DefaultAbsenceType.A_9505, DefaultAbsenceType.A_95042E, DefaultAbsenceType.A_9504,
          DefaultAbsenceType.A_95053), 
      ImmutableSet.of(DefaultAbsenceType.A_9599B,
          DefaultAbsenceType.A_9591, DefaultAbsenceType.A_9599, DefaultAbsenceType.A_95041E,
          DefaultAbsenceType.A_9505, DefaultAbsenceType.A_95042E, DefaultAbsenceType.A_9504,
          DefaultAbsenceType.A_95053), 
      -1, null),
  
  T_MALATTIA_3_ANNI(AmountType.units,
      ImmutableSet.of(
          DefaultAbsenceType.A_09B, DefaultAbsenceType.A_111,
          DefaultAbsenceType.A_115, DefaultAbsenceType.A_116,
          DefaultAbsenceType.A_119, DefaultAbsenceType.A_11C,
          DefaultAbsenceType.A_11C5, DefaultAbsenceType.A_11C9,
          DefaultAbsenceType.A_11R, DefaultAbsenceType.A_11R5,
          DefaultAbsenceType.A_11R9, DefaultAbsenceType.A_11S,
          DefaultAbsenceType.A_631), 
      ImmutableSet.of(
          DefaultAbsenceType.A_09B, DefaultAbsenceType.A_111,
          DefaultAbsenceType.A_115, DefaultAbsenceType.A_116,
          DefaultAbsenceType.A_119, DefaultAbsenceType.A_11C,
          DefaultAbsenceType.A_11C5, DefaultAbsenceType.A_11C9,
          DefaultAbsenceType.A_11R, DefaultAbsenceType.A_11R5,
          DefaultAbsenceType.A_11R9, DefaultAbsenceType.A_11S,
          DefaultAbsenceType.A_631), 
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
          DefaultAbsenceType.A_54L230,
          //DefaultAbsenceType.A_34,
          //DefaultAbsenceType.A_17C,
          DefaultAbsenceType.A_C17, DefaultAbsenceType.A_C17), 
      ImmutableSet.of(
          DefaultAbsenceType.A_24, DefaultAbsenceType.A_24H7,
          DefaultAbsenceType.A_25, DefaultAbsenceType.A_25H7,
          DefaultAbsenceType.A_242, DefaultAbsenceType.A_242H7,
          DefaultAbsenceType.A_252, DefaultAbsenceType.A_252H7,
          DefaultAbsenceType.A_243, DefaultAbsenceType.A_243H7,
          DefaultAbsenceType.A_253, DefaultAbsenceType.A_253H7,
          DefaultAbsenceType.A_54L230,
          //DefaultAbsenceType.A_34,
          //DefaultAbsenceType.A_17C,
          DefaultAbsenceType.A_C17, DefaultAbsenceType.A_C17), 
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
   * Tutti i codici takable e taken.
   */
  public Set<String> allTakableTakenCodes() {
    Set<String> allCodes = Sets.newHashSet();
    for (DefaultAbsenceType type : this.takableCodes) {
      allCodes.add(type.getCode());
    }
    for (DefaultAbsenceType type : this.takableCodes) {
      allCodes.add(type.getCode());
    }
    return allCodes;
  }
  
  /**
   * Ricerca i comportamenti prendibilità modellati e non presenti fra quelle passate in arg (db).
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
   * L'enumerato corrispettivo del takable (se esiste...) 
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