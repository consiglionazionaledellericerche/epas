package models.enumerate;

/**
 * @author dario
 */
public enum AuthWebStamping {
  yesToAll("si a tutti"),
  no("no"),
  perPerson("a persona");

  public String description;

  AuthWebStamping(String description) {
    this.description = description;
  }
}
