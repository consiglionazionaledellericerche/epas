package models.dto;

import models.absences.Absence;

import org.joda.time.LocalDate;

public class AbsenceToRecoverDto {

  public Absence absence;
  public LocalDate absenceDate;
  public LocalDate recoverDate;
  public int quantityRecovered;
  public int quantityToRecover;
  public float percentage;
  
  /**
   * Costruttore.
   * @param absence l'assenza
   * @param absenceDate la data dell'assenza
   * @param recoverDate la data di recupero dell'assenza
   * @param quantityRecovered la quantità recuperata
   * @param quantityToRecover la quantità da recuperare
   * @param percentage la percentuale recuperata
   */
  public AbsenceToRecoverDto(Absence absence, LocalDate absenceDate, LocalDate recoverDate, 
      int quantityRecovered, int quantityToRecover, float percentage) {
    this.absence = absence;
    this.absenceDate = absenceDate;
    this.recoverDate = recoverDate;
    this.quantityRecovered = quantityRecovered;
    this.quantityToRecover = quantityToRecover;
    this.percentage = percentage;
  }

}
