package models.enumerate;

public enum AccumulationType {

  no("no"),
  yearly("annuale"),
  monthly("mensile"),
  always("sempre");

  public String description;

  AccumulationType(String description) {
    this.description = description;
  }

  public static AccumulationType getByDescription(String description) {
    if (description.equals("no")) {
      return AccumulationType.no;
    }
    if (description.equals("annuale")) {
      return AccumulationType.yearly;
    }
    if (description.equals("mensile")) {
      return AccumulationType.monthly;
    }
    if (description.equals("sempre")) {
      return AccumulationType.always;
    }
    return null;
  }
}
