package models.enumerate;

/**
 * Enumerato per la definizione del tipo di pagamento del turno.
 * @author dario
 *      enumerato che serve a stabilire se pagare tutto il turno con una certa tipologia o con un'altra
 *      se questo ricade in parte in una fascia e in parte in un'altra.
 */
public enum PaymentType {

  T1("T1", "Calcolo tutto con turno diurno"),
  T2("T2", "Calcolo tutto con turno notturno");
  
  public String name;
  public String description;
  
  private PaymentType(String name, String description) {
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
