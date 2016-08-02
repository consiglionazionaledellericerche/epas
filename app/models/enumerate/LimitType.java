package models.enumerate;

public enum LimitType {

  monthly("mensile"),
  yearly("annuale"),
  onMonthlyPresence("su presenza mensile"),
  noLimit("senza limite");
  
  public String description;
  
  LimitType(String description) {
    this.description = description;
  }
  
  public static LimitType getByDescription(String description) {
    if (description.equals("senza limite")) {
      return LimitType.noLimit;
    }
    if (description.equals("annuale")) {
      return LimitType.yearly;
    }
    if (description.equals("mensile")) {
      return LimitType.monthly;
    }

    return null;
  }
}
