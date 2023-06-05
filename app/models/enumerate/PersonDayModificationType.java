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

package models.enumerate;

/**
 * Enumerato relativo alle possibili modifiche del tempo di lavoro del personDay.
 *
 * @author Dario Tagliaferri
 */
public enum PersonDayModificationType {

  p("Tempo calcolato togliendo dal tempo di lavoro la durata dell'intervallo pranzo"),
  d("Considerato presente se non ci sono codici di assenza (orario di lavoro autodichiarato)"),
  x("Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte");

  private String description;

  PersonDayModificationType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
