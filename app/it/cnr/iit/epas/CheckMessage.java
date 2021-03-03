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

package it.cnr.iit.epas;

import models.absences.AbsenceType;

/**
 * Questa classe mi occorre per ritornare un oggetto nel quale sintetizzare il
 * risultato della chiamata della funzione di assegnamento di un codice di assenza oraria
 * con gruppo di riferimento. In modo da stabilire se un certo codice possa essere preso o
 * meno e, nel caso possa essere preso, se necessita anche del corrispondente codice di
 * rimpiazzamento determinato dal raggiungimento del limite per esso previsto.
 */
public class CheckMessage {
  public boolean check;
  public String message;
  public AbsenceType absenceType = null;

  /**
   * Costruttore.
   *
   * @param check controllo
   * @param message messaggio
   * @param absenceType tipo di assenza
   */
  public CheckMessage(boolean check, String message, AbsenceType absenceType) {
    this.check = check;
    this.message = message;
    this.absenceType = absenceType;
  }
}
