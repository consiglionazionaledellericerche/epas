package models.enumerate;

public enum LimitDescription {

  daysOfMonth("giorni del mese"),
  daysOfYear("giorni dell'anno");
  
  public String description;
  
  LimitDescription(String description) {
    this.description = description;
  }
  
  public static LimitDescription getByDescription(String description) {
    if (description.equals("giorni del mese")) {
      return LimitDescription.daysOfMonth;
    }
    if (description.equals("giorni dell'anno")) {
      return LimitDescription.daysOfYear;
    }


    return null;
  }
  
}
