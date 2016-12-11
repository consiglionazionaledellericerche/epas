package manager.services.absences.enums;

public enum CategoryEnum {

  MISSIONE_CNR("Missioni CNR", 1, CategoryTabEnum.MISSIONE),
  FERIE_CNR("Ferie CNR", 2, CategoryTabEnum.FERIE),
  RIPOSI_COMPENSATIVI_CNR("Riposi compensativi CNR", 3, CategoryTabEnum.RIPOSO_COMPENSATIVO),
  PERMESSI_VARI("Permessi vari", 4, CategoryTabEnum.ALTRE_TIPOLOGIE),
  CONGEDI_PARENTALI("Congedi parentali", 5, CategoryTabEnum.ALTRE_TIPOLOGIE),
  L_104("Disabilità legge 104/92", 6, CategoryTabEnum.ALTRE_TIPOLOGIE),
  PUBBLICA_FUNZIONE("Pubblica funzione", 7, CategoryTabEnum.ALTRE_TIPOLOGIE),
  MALATTIA_DIPENDENTE("Malattia dipendente", 8, CategoryTabEnum.ALTRE_TIPOLOGIE),
  MALATTIA_FIGLIO_1("Malattia primo figlio", 9, CategoryTabEnum.ALTRE_TIPOLOGIE),
  MALATTIA_FIGLIO_2("Malattia secondo figlio", 10, CategoryTabEnum.ALTRE_TIPOLOGIE),
  MALATTIA_FIGLIO_3("Malattia terzo figlio", 11, CategoryTabEnum.ALTRE_TIPOLOGIE),
  ALTRI_CODICI("Altri Codici", 12, CategoryTabEnum.ALTRE_TIPOLOGIE),
  CODICI_DIPENDENTI( "Codici Dipendenti", 13, CategoryTabEnum.DIPENDENTI),
  CODICI_AUTOMATICI("Codici Automatici", 14, CategoryTabEnum.AUTOMATICI);
  
    
  public String label;
  public CategoryTabEnum categoryTab;
  public Integer priority;

  private CategoryEnum(String label, Integer priority, CategoryTabEnum categoryTab) {
    this.label = label;
    this.priority = priority;
    this.categoryTab = categoryTab;
  }
  
}
