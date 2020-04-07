package models.enumerate;

/**
 * Enumerato per la gestione dei riposi compensativi IV-VIII.
 * @author dario
 */
public enum CapacityCompensatoryRestFourEight {

  onDayResidual("residuo del giorno"),
  onEndOfMonthResidual("residuo a fine mese"),
  onEndPastMonthResidual("residuo a fine mese precedente"),
  onEndPastQuarterResidual("residuo a trimestre precedente");

  public String description;

  private CapacityCompensatoryRestFourEight(String description) {
    this.description = description;
  }
}
