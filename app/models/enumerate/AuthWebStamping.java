package models.enumerate;

/**
 * Enumerato per l'autorizzazione alla timbratura web.
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
