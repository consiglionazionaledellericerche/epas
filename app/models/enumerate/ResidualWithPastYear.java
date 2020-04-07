package models.enumerate;

/**
 * Enumerato per la gestione dei residui dell'anno passato.
 * @author dario
 */
public enum ResidualWithPastYear {

  atMonth("al mese"),
  atDay("al giorno"),
  atMonthInWhichCanUse("nel mese in cui posso usarla");

  public String description;

  private ResidualWithPastYear(String description) {
    this.description = description;
  }

}
