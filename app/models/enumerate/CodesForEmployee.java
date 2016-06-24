package models.enumerate;

/**
 * enumerato contenente i codici giustificativi che possono essere presi dai dipendenti
 * che hanno abilitata la timbratura per lavoro fuori sede
 * @author dario
 *
 */
public enum CodesForEmployee {

  BP("105BP");

  private String description;

  CodesForEmployee(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
