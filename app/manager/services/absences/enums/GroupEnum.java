package manager.services.absences.enums;

public enum GroupEnum {

  G_18(CategoryEnum.L_104),
  G_19(CategoryEnum.L_104),
  G_661(CategoryEnum.PERMESSI_VARI),
  G_25(CategoryEnum.CONGEDI_PARENTALI),
  G_23(CategoryEnum.CONGEDI_PARENTALI),
  G_24(CategoryEnum.CONGEDI_PARENTALI),
  G_252(CategoryEnum.CONGEDI_PARENTALI),
  G_232(CategoryEnum.CONGEDI_PARENTALI),
  G_242(CategoryEnum.CONGEDI_PARENTALI),
  G_253(CategoryEnum.CONGEDI_PARENTALI),
  G_233(CategoryEnum.CONGEDI_PARENTALI),
  G_243(CategoryEnum.CONGEDI_PARENTALI),
  G_89(CategoryEnum.PERMESSI_VARI),
  G_09(CategoryEnum.PERMESSI_VARI),
  MISSIONE(CategoryEnum.MISSIONE_CNR),
  FERIE_CNR(CategoryEnum.FERIE_CNR),
  RIPOSI_CNR(CategoryEnum.RIPOSI_COMPENSATIVI_CNR),
  G_95(CategoryEnum.ALTRI_CODICI),
  MALATTIA(CategoryEnum.MALATTIA_DIPENDENTE),
  MALATTIA_FIGLIO_1_12(CategoryEnum.MALATTIA_FIGLIO_1),
  MALATTIA_FIGLIO_1_13(CategoryEnum.MALATTIA_FIGLIO_1),
  MALATTIA_FIGLIO_1_14(CategoryEnum.MALATTIA_FIGLIO_1),
  MALATTIA_FIGLIO_2_12(CategoryEnum.MALATTIA_FIGLIO_2),
  MALATTIA_FIGLIO_2_13(CategoryEnum.MALATTIA_FIGLIO_2),
  MALATTIA_FIGLIO_2_14(CategoryEnum.MALATTIA_FIGLIO_2),
  MALATTIA_FIGLIO_3_12(CategoryEnum.MALATTIA_FIGLIO_3),
  MALATTIA_FIGLIO_3_13(CategoryEnum.MALATTIA_FIGLIO_3),
  MALATTIA_FIGLIO_3_14(CategoryEnum.MALATTIA_FIGLIO_3),
  PB(CategoryEnum.CODICI_AUTOMATICI),
  EMPLOYEE(CategoryEnum.CODICI_DIPENDENTI),
  ALTRI(CategoryEnum.ALTRI_CODICI);
  
  public CategoryEnum category;
  
  private GroupEnum(CategoryEnum category) {
    this.category = category;
  }

  
}
