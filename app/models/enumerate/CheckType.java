package models.enumerate;

public enum CheckType {

  DANGER("danger"),
  WARNING("warning"),
  SUCCESS("success");

  private String description;

  CheckType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
