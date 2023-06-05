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

package manager.response;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import java.util.List;
import org.joda.time.LocalDate;

/**
 * Report dell'inserimento di un'assenza.
 */
public class AbsenceInsertReport {

  private List<AbsencesResponse> absences = Lists.newArrayList();
  private List<String> warnings = Lists.newArrayList();
  private List<LocalDate> datesInTrouble = Lists.newArrayList();
  private int totalAbsenceInsert = 0;
  private int absenceInReperibilityOrShift = 0;

  /**
   * Aggiunge il riepilogo dell'inserimento assenza e incrementa i contatori corretti.
   *
   * @param response Il riepilogo dell'inserimento assenza.
   */
  public void add(AbsencesResponse response) {
    absences.add(response);
    if (response.isInsertSucceeded()) {
      totalAbsenceInsert++;
    }
    if (response.isDayInReperibilityOrShift()) {
      absenceInReperibilityOrShift++;
    }
  }

  /**
   * Verifica se sono presenti warning o date con problemi.
   */
  public boolean hasWarningOrDaysInTrouble() {
    return !(warnings.isEmpty() && datesInTrouble.isEmpty());
  }

  /**
   * Lista della risposte relative alle assenze.
   */
  public List<AbsencesResponse> getAbsences() {
    return absences;
  }

  /**
   * Imposta la lista della risposte sulle assenze.
   */
  public void setAbsences(List<AbsencesResponse> absences) {
    this.absences = absences;
  }

  /**
   * La lista della risposte sulle assenze.
   */
  public List<String> getWarnings() {
    return warnings;
  }


  /**
   * Imposta la lista dei warning.
   */
  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }

  /**
   * La lista della date con problemi.
   */
  public List<LocalDate> getDatesInTrouble() {
    return datesInTrouble;
  }

  /**
   * Imposta la date con problemi.
   */
  public void setDatesInTrouble(List<LocalDate> datesInTrouble) {
    this.datesInTrouble = datesInTrouble;
  }

  /**
   * Il numero totale delle assenze inserite.
   */
  public int getTotalAbsenceInsert() {
    return totalAbsenceInsert;
  }

  /**
   * Imposta il numero totale delle assenze inserite.
   */
  public void setTotalAbsenceInsert(int totalAbsenceInsert) {
    this.totalAbsenceInsert = totalAbsenceInsert;
  }

  /**
   * Il numero delle assenza in reperibilità o turno.
   */
  public int getAbsenceInReperibilityOrShift() {
    return absenceInReperibilityOrShift;
  }

  /**
   * Setter del campo absenceInReperibilityOrShift.
   *
   * @param absenceInReperibilityOrShift numero di assenze in giorni di reperibilità o turno.
   */
  public void setAbsenceInReperibilityOrShift(int absenceInReperibilityOrShift) {
    this.absenceInReperibilityOrShift = absenceInReperibilityOrShift;
  }

  /**
   * Metodo che ritorna la lista dei giorni di reperibilità o turno.
   *
   * @return la lista dei giorni in cui si è in reperibilità o turno.
   */
  public List<LocalDate> datesInReperibilityOrShift() {

    return FluentIterable.from(absences).filter(
        new Predicate<AbsencesResponse>() {
          @Override
          public boolean apply(AbsencesResponse air) {
            return air.isDayInReperibilityOrShift();
          }
        }).transform(AbsencesResponse.ToDate.INSTANCE).toList();
  }

}
