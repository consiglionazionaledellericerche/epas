package models.enumerate;

public enum LimitUnit {

  minutes("minuti"),
  hours("ore"),
  days("giorni");

  public String description;

  LimitUnit(String description) {
    this.description = description;
  }

  public static LimitUnit getByDescription(String description) {
    if (description.equals("ore")) {
      return LimitUnit.hours;
    }
    if (description.equals("giorni")) {
      return LimitUnit.days;
    }
    if (description.equals("minuti")) {
      return LimitUnit.minutes;
    }

    return null;
  }

}
