package models.enumerate;

public enum LimitUnit {

  minutes("minuti"),
  hours("ore"),
  days("giorni"),
  month("mese");
  
  private String description;
  
  LimitUnit(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
