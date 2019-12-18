package models.enumerate;

/**
 * Enumerato per la definizione del tipo di calcolo.
 * @author dario
 *      enumerato per definire la tipologia di calcolo della quantità oraria in turno.
 */
public enum CalculationType {

  standard_CNR("Calcolo CNR Standard", "Calcola il quantitativo orario in turno sulla base "
      + "del rispetto orario della fascia."),
  percentage("Calcolo percentuale", "In base a quanto tempo si trascorre all'interno della "
      + "fascia di turno, il turno viene pagato proporzionalmente.");
  
  public String name;
  public String description;
  
  private CalculationType(String name, String description) {
    this.name = name;
    this.description = description;
  }
  
  public String getDescription() {
    return description;
  }
  
  public String getName() {
    return name;
  }
  
}
