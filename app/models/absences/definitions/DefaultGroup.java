package models.absences.definitions;

import com.google.common.base.Optional;

import java.util.List;

import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.GroupAbsenceType.PeriodType;

import org.assertj.core.util.Lists;

public enum DefaultGroup {
  
  //Dai codici in attestati si evince che il permesso provvisorio si prende solo a giornate
  //Quindi non viene abilitato il comportamento sui completamenti del tipo C_18, C_182, C_19.
  G_18("18 - Permesso assistenza parenti/affini disabili L. 104/92 tre giorni mese", 
      "", 
      DefaultCategoryType.L_104, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.month, 
      DefaultTakable.T_18, DefaultComplation.C_18, null, false, false),
  G_18P("18P - Permesso provv. assistenza parenti/affini disabili L. 104/92 tre giorni mese",
      "", 
      DefaultCategoryType.PERMESSI_PROVVISORI_104, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.month, 
      DefaultTakable.T_18P, null, null, false, false),
  
  G_182("182 - Permesso assistenza secondo parenti/affini disabili L. 104/92 tre gg. mese", 
      "", 
      DefaultCategoryType.L_104, 1,
      GroupAbsenceTypePattern.programmed, PeriodType.month, 
      DefaultTakable.T_182, DefaultComplation.C_182, null, false, false),
  G_182P("182P - Permesso provv. assist. secondo parenti/affini dis. L. 104/92 tre gg. mese", 
      "", 
      DefaultCategoryType.PERMESSI_PROVVISORI_104, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.month, 
      DefaultTakable.T_182P, null, null, false, false),

  G_19("19 - Permesso per dipendente disabile L. 104/92 tre giorni mese", 
      "", 
      DefaultCategoryType.L_104, 2,
      GroupAbsenceTypePattern.programmed, PeriodType.month, 
      DefaultTakable.T_19, DefaultComplation.C_19, null, false, false),
  G_19P("19P - Permesso provv. per dipendente disabile L. 104/92 tre giorni mese", 
      "", 
      DefaultCategoryType.PERMESSI_PROVVISORI_104, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.month, 
      DefaultTakable.T_19P, null, null, false, false), 
  
  G_26("26 - Permesso per dipendente disabile L. 104/92 due ore giornaliere", 
      "", 
      DefaultCategoryType.ALTRI_104, 0,
      GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
      DefaultTakable.T_26, null, null, false, false),
  
  G_C1718("C17/C18 - Altri congedi L. 104/92", 
      "", 
      DefaultCategoryType.ALTRI_104, 0,
      GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
      DefaultTakable.T_C1718, null, null, false, false),
  
  G_661("661 - Permesso orario per motivi personali 18 ore anno", 
      "", 
      DefaultCategoryType.PERMESSI_PERSONALI, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.year, 
      DefaultTakable.T_661, DefaultComplation.C_661, null, false, false),
  G_89("89 - Permesso diritto allo studio 150 ore anno", 
      "", 
      DefaultCategoryType.PERMESSI_PERSONALI, 1,
      GroupAbsenceTypePattern.programmed, PeriodType.year, 
      DefaultTakable.T_89, DefaultComplation.C_89, null, false, false),
  
  G_09("09 - Permesso orario visita medica", 
      "", 
      DefaultCategoryType.VISITA_MEDICA, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.always, 
      DefaultTakable.T_09, DefaultComplation.C_09, null, false, false),
  G_631("631 - Permesso giornaliero visita medica", 
      "", 
      DefaultCategoryType.VISITA_MEDICA, 1,
      GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
      DefaultTakable.T_631, null, null, false, false),
  MISSIONE("Missione", 
      "", 
      DefaultCategoryType.MISSIONE_CNR, 0,
      GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
      DefaultTakable.T_MISSIONE, null, null, false, false),
  FERIE_CNR("31/94/32 - Ferie e permessi legge", 
      "", 
      DefaultCategoryType.FERIE_CNR, 0,
      GroupAbsenceTypePattern.vacationsCnr, PeriodType.always, 
      DefaultTakable.T_FERIE_CNR, null, null, false, false),
  FERIE_CNR_DIPENDENTI("Ferie e permessi legge", 
      "", 
      DefaultCategoryType.FERIE_DIPENDENTI, 2,         //must be greater than FERIE_CNR
      GroupAbsenceTypePattern.vacationsCnr, PeriodType.always, 
      DefaultTakable.T_FERIE_CNR, null, null, false, false),
  FERIE_CNR_PROROGA("37 - Ferie dopo 31/08", 
      "", 
      DefaultCategoryType.FERIE_CNR, 1,
      GroupAbsenceTypePattern.vacationsCnr, PeriodType.always, 
      DefaultTakable.T_FERIE_CNR_PROROGA, null, null, false, false),
  RIDUCE_FERIE_CNR("Riduce ferie e permessi", 
      "", 
      DefaultCategoryType.CODICI_AUTOMATICI, 0,
      GroupAbsenceTypePattern.vacationsCnr, PeriodType.always, 
      DefaultTakable.T_RIDUCE_FERIE_CNR, null, null, true, false),
  
  RIPOSI_CNR("91 - Riposo compensativo", 
      "", 
      DefaultCategoryType.RIPOSI_COMPENSATIVI_CNR, 0, 
      GroupAbsenceTypePattern.compensatoryRestCnr, PeriodType.always, 
      DefaultTakable.T_RIPOSI_CNR, null, null, false, false),
  RIPOSI_CNR_DIPENDENTI("91 - Riposo compensativo", 
      "", 
      DefaultCategoryType.RIPOSI_COMPENSATIVI_DIPENDENTI, 1, //must be greater than RIPOSI_CNR
      GroupAbsenceTypePattern.compensatoryRestCnr, PeriodType.always, 
      DefaultTakable.T_RIPOSI_CNR, null, null, false, false),
  
  MALATTIA("Codici malattia dipendente", 
      "", 
      DefaultCategoryType.MALATTIA_DIPENDENTE, 0,
      GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
      DefaultTakable.T_MALATTIA, null, null, false, false),
  MALATTIA_FIGLIO_1_12("12 - Malattia primo figlio <= 3 anni retribuita 100%", 
      "", 
      DefaultCategoryType.MALATTIA_FIGLIO_1, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child1_0_3, 
      DefaultTakable.T_MALATTIA_FIGLIO_1_12, null, null, false, false),
  MALATTIA_FIGLIO_1_13("13 - Malattia primo figlio oltre 3 anni non retribuita", 
      "", 
      DefaultCategoryType.MALATTIA_FIGLIO_1, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child1_3_12, 
      DefaultTakable.T_MALATTIA_FIGLIO_1_13, null, null, false, false),
  MALATTIA_FIGLIO_1_14("14 - Malattia primo figlio <= 3 anni non retribuita", 
      "", 
      DefaultCategoryType.MALATTIA_FIGLIO_1, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child1_0_3, 
      DefaultTakable.T_MALATTIA_FIGLIO_1_14, null, null, false, false),
  MALATTIA_FIGLIO_2_12("122 - Malattia secondo figlio <= 3 anni retribuita 100%", 
      "", 
      DefaultCategoryType.MALATTIA_FIGLIO_2, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child2_0_3, 
      DefaultTakable.T_MALATTIA_FIGLIO_2_12, null, null, false, false),
  MALATTIA_FIGLIO_2_13("132 - Malattia secondo figlio oltre 3 anni non retribuita", 
      "", 
      DefaultCategoryType.MALATTIA_FIGLIO_2, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child2_3_12, 
      DefaultTakable.T_MALATTIA_FIGLIO_2_13, null, null, false, false),
  
  G_24("24 - Astensione facoltativa post partum non retrib. primo figlio 0-12 anni 600 giorni", 
      "", 
      DefaultCategoryType.ASTENSIONE_POSTPARTUM, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child1_0_12, 
      DefaultTakable.T_24, DefaultComplation.C_24, null, false, true),
  G_25("25 - Astensione facoltativa post partum 30% primo figlio 0-6 anni 150 giorni", 
      "", 
      DefaultCategoryType.ASTENSIONE_POSTPARTUM, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child1_0_6, 
      DefaultTakable.T_25, DefaultComplation.C_25, DefaultGroup.G_24, false, true),
  G_23("23 - Astensione facoltativa post partum 100% primo figlio 0-12 anni 30 giorni", 
      "23/25/24 - Astensione facoltativa post partum primo figlio", 
      DefaultCategoryType.ASTENSIONE_POSTPARTUM, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child1_0_12, 
      DefaultTakable.T_23, DefaultComplation.C_23, DefaultGroup.G_25, false, true),
  
  G_242("242 - Astensione facoltativa post partum non retrib. "
      + "secondo figlio 0-12 anni 600 giorni", 
      "", 
      DefaultCategoryType.ASTENSIONE_POSTPARTUM, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child2_0_12, 
      DefaultTakable.T_242, DefaultComplation.C_242, null, false, true),
  G_252("252 - Astensione facoltativa post partum 30% secondo figlio 0-6 anni 150 giorni", 
      "", 
      DefaultCategoryType.ASTENSIONE_POSTPARTUM, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child2_0_6, 
      DefaultTakable.T_252, DefaultComplation.C_252, DefaultGroup.G_242, false, true),
  G_232("232 - Astensione facoltativa post partum 100% secondo figlio 0-12 anni 30 giorni", 
      "232/252/242 - Astensione facoltativa post partum secondo figlio", 
      DefaultCategoryType.ASTENSIONE_POSTPARTUM, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child2_0_12, 
      DefaultTakable.T_232, DefaultComplation.C_232, DefaultGroup.G_252, false, true),

  G_243("243 - Astensione facoltativa post partum non retrib. terzo figlio 0-12 anni 600 giorni", 
      "", DefaultCategoryType.ASTENSIONE_POSTPARTUM, 0, 
      GroupAbsenceTypePattern.programmed, PeriodType.child3_0_12, 
      DefaultTakable.T_243, DefaultComplation.C_243, null, false, true),
  G_253("253 - Astensione facoltativa post partum 30% terzo figlio 0-6 anni 150 giorni", 
      "", DefaultCategoryType.ASTENSIONE_POSTPARTUM, 0, 
      GroupAbsenceTypePattern.programmed, PeriodType.child3_0_6, 
      DefaultTakable.T_253, DefaultComplation.C_253, DefaultGroup.G_243, false, true),
  G_233("233 - Astensione facoltativa post partum 100% terzo figlio 0-12 anni 30 giorni", 
      "233/253/243 - Astensione facoltativa post partum terzo figlio", 
      DefaultCategoryType.ASTENSIONE_POSTPARTUM, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child3_0_12, 
      DefaultTakable.T_233, DefaultComplation.C_233, DefaultGroup.G_253, false, true),
  
  G_25P("25P - Prolungamento astensione facoltativa post partum 30% (no limiti)",
      "", 
      DefaultCategoryType.ASTENSIONE_POSTPARTUM, 1,                //se fosse primo figlio:
      GroupAbsenceTypePattern.programmed, PeriodType.always,              //child1_6_12
      DefaultTakable.T_25P, DefaultComplation.C_25P, null, false, false),
  
  G_CONGEDI_PRENATALI("20/21 - Congedi Prenatali", 
      "", 
      DefaultCategoryType.CONGEDI_PRENATALI, 0,
      GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
      DefaultTakable.T_CONGEDI_PRENATALI, null, null, false, false),
  
  G_CONGEDO_MATRIMONIO("45 - Congedo straordinario per matrimonio", 
      "", 
      DefaultCategoryType.CONGEDO_MATRIMONIO, 0,
      GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
      DefaultTakable.T_CONGEDO_MATRIMONIO, null, null, false, false),
  
  G_MALATTIA_FIGLIO_2_14("142 - Malattia secondo figlio <= 3 anni non retribuita", 
      "", 
      DefaultCategoryType.MALATTIA_FIGLIO_2, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child2_0_3, 
      DefaultTakable.T_MALATTIA_FIGLIO_2_14, null, null, false, false),
  G_MALATTIA_FIGLIO_3_12("123 - Malattia terzo figlio <= 3 anni retribuita 100%", 
      "", 
      DefaultCategoryType.MALATTIA_FIGLIO_3, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child3_0_3, 
      DefaultTakable.T_MALATTIA_FIGLIO_3_12, null, null, false, false),
  G_MALATTIA_FIGLIO_3_13("133 - Malattia terzo figlio oltre 3 anni non retribuita", 
      "", 
      DefaultCategoryType.MALATTIA_FIGLIO_3, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child3_3_12, 
      DefaultTakable.T_MALATTIA_FIGLIO_3_13, null, null, false, false),
  G_MALATTIA_FIGLIO_3_14("143 - Malattia terzo figlio <= 3 anni non retribuita", 
      "", 
      DefaultCategoryType.MALATTIA_FIGLIO_3, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.child3_0_3, 
      DefaultTakable.T_MALATTIA_FIGLIO_3_14, null, null, false, false),
  PB("PB - Permesso breve 36 ore anno", 
      "", 
      DefaultCategoryType.CODICI_AUTOMATICI, 0,
      GroupAbsenceTypePattern.programmed, PeriodType.year, 
      DefaultTakable.T_PB, null, null, true, false),
  
  G_ALTRI_CODICI("Altri Codici", 
      "", 
      DefaultCategoryType.ALTRI_CODICI, 0,
      GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
      DefaultTakable.T_ALTRI, null, null, false, false),
  
  LAVORO_FUORI_SEDE("Lavoro fuori sede", 
      "", 
      DefaultCategoryType.LAVORO_FUORI_SEDE, 0,
      GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
      DefaultTakable.T_LAVORO_FUORI_SEDE, null, null, false, false),
  
  G_PUBBLICA_FUNZIONE("Codici Pubblica Funzione", 
      "", 
      DefaultCategoryType.PUBBLICA_FUNZIOINE, 2,
      GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
      DefaultTakable.T_PUBBLICA_FUNZIONE, null, null, false, false),
  
  G_ASPETTATIVA("Codici Aspettativa", 
      "", 
      DefaultCategoryType.ASPETTATIVA, 1,
      GroupAbsenceTypePattern.simpleGrouping, PeriodType.always, 
      DefaultTakable.T_ASPETTATIVA, null, null, false, false);
  


  public String description;
  public String chainDescription;
  public DefaultCategoryType category;
  public int priority;
  public GroupAbsenceTypePattern pattern;
  public PeriodType periodType;
  public DefaultTakable takable;
  public DefaultComplation complation;    //nullable
  public DefaultGroup nextGroupToCheck;   //nullable
  public boolean automatic;
  public boolean initializable;
  
  
  private DefaultGroup(String description, 
      String chainDescription, 
      DefaultCategoryType category, int priority,
      GroupAbsenceTypePattern pattern, PeriodType periodType, 
      DefaultTakable takable, DefaultComplation complation, DefaultGroup nextGroupToCheck, 
      boolean automatic, boolean initializable) {

    this.description = description;
    this.chainDescription = chainDescription;
    this.category = category;
    this.priority = priority;
    this.pattern = pattern;
    this.periodType = periodType;
    this.takable = takable;
    this.complation = complation;
    this.nextGroupToCheck = nextGroupToCheck;
    this.automatic = automatic;
    this.initializable = initializable;

  }
  
  /**
   * Ricerca i gruppi modellati e non presenti fra quelle passate in arg (db). 
   * @return list
   */
  public static List<DefaultGroup> missing(List<GroupAbsenceType> allGroup) {
    List<DefaultGroup> missing = Lists.newArrayList();
    for (DefaultGroup defaultGroup : DefaultGroup.values()) {
      boolean found = false;
      for (GroupAbsenceType group : allGroup) {
        if (defaultGroup.name().equals(group.name)) {
          found = true;
          break;
        }
      }
      if (!found) {
        missing.add(defaultGroup);
      }
    }
    return missing;
  }
  
  /**
   * L'enumerato corrispettivo del group (se esiste...) 
   * @return optional dell'enumerato
   */
  public static Optional<DefaultGroup> byName(GroupAbsenceType group) {
    for (DefaultGroup defaultGroup : DefaultGroup.values()) {
      if (defaultGroup.name().equals(group.name)) {
        return Optional.of(defaultGroup);
      }
    }
    return Optional.absent();
  }
  
  /**
   * Codici ferie prendibili dal gruppo ferie per dipendenti.
   * @return list
   */
  public static List<String> employeeVacationCodes() {
    List<String> codes = Lists.newArrayList();
    for (DefaultAbsenceType takable : DefaultGroup.FERIE_CNR_DIPENDENTI.takable.takableCodes) {
      codes.add(takable.getCode());
    }
    return codes;
  }
  
  /**
   * Codici ferie prendibili dal gruppo riposi compensativi per dipendenti.
   * @return list
   */
  public static List<String> employeeCompensatoryCodes() {
    List<String> codes = Lists.newArrayList();
    for (DefaultAbsenceType takable : DefaultGroup.RIPOSI_CNR_DIPENDENTI.takable.takableCodes) {
      codes.add(takable.getCode());
    }
    return codes;
  }
  
  /**
   * Codici ferie prendibili dal gruppo lavoro fuori sede per dipendenti. (105BP)
   * @return list
   */
  public static List<String> employeeOffSeatCodes() {
    List<String> codes = Lists.newArrayList();
    for (DefaultAbsenceType takable : DefaultGroup.LAVORO_FUORI_SEDE.takable.takableCodes) {
      codes.add(takable.getCode());
    }
    return codes;
  }

}