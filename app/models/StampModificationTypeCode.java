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

package models;

/**
 * Enumerato per la gestione degli identificativi applicati alle singole timbrature.
 *
 */
public enum StampModificationTypeCode {

  /*
   id | code |                                        description
  ----+------+-------------------------------------------------------------------------------------
    1 | p    | Tempo calcolato togliendo dal tempo di lavoro la durata dell'intervallo pranzo
    2 | e    | Ora di entrata calcolata perché la durata dell'intervallo pranzo è minore del minimo
    3 | m    | Timbratura modificata dall'amministratore
    4 | x    | Ora inserita automaticamente per considerare il tempo di lavoro a cavallo
             | della mezzanotte
    5 | f    | Tempo di lavoro che si avrebbe uscendo adesso
    6 | d    | Considerato presente se non ci sono codici di assenza (orario di lavoro
             | autodichiarato)
    7 | md   | Timbratura modificata dal dipendente
    8 | tl   | Timbratura inserita dalla form del telelavoro per livelli I-III
   */



  FOR_DAILY_LUNCH_TIME("p"),
  FOR_MIN_LUNCH_TIME("e"),
  MARKED_BY_ADMIN("m"),
  TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT("x"),
  ACTUAL_TIME_AT_WORK("f"),
  FIXED_WORKINGTIME("d"),
  MARKED_BY_EMPLOYEE("md"),
  MARKED_BY_TELEWORK("tl");


  private String code;

  private StampModificationTypeCode(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

}
