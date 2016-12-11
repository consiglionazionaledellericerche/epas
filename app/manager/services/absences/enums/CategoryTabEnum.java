package manager.services.absences.enums;

public enum CategoryTabEnum {
  
  MISSIONE("Missione", 1),
  FERIE("Ferie e Festività Soppr.", 2),
  RIPOSO_COMPENSATIVO("Riposo Compensativo", 3),
  ALTRE_TIPOLOGIE("Altre Tipologie", 4),
  DIPENDENTI("Codici Dipendenti", 5),
  AUTOMATICI("Codici Automatici", 6);
  
  public String label;
  public Integer priority;
  
  private CategoryTabEnum(String label, Integer priority) {
    this.label = label;
    this.priority = priority;
  }
}
