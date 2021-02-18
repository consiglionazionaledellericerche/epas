/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package models.dto;

import models.absences.Absence;
import org.joda.time.LocalDate;

/**
 * DTO per le assenza da recuperare (ex. 91CE).
 */
public class AbsenceToRecoverDto {

  public Absence absence;
  public LocalDate absenceDate;
  public LocalDate recoverDate;
  public int quantityRecovered;
  public int quantityToRecover;
  public float percentage;
  
  /**
   * Costruttore.
   *
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
