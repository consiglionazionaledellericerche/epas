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

package models.exports;

import lombok.AllArgsConstructor;
import models.Person;
import models.PersonReperibilityType;
import org.joda.time.LocalDate;

/**
 * Classe di supporto per l'esportazione delle informazioni relative alla reperibilità delle
 * persone.
 *
 * @author Cristian Lucchesi
 */
@AllArgsConstructor
public class ReperibilityPeriod {

  public final Person person;
  public final LocalDate start;
  public LocalDate end;
  public PersonReperibilityType reperibilityType;

  /**
   * Costruttore.
   *
   * @param person la persona
   * @param start la data di inizio della reperibilità
   * @param end la data di fine della reperibilità
   */
  public ReperibilityPeriod(Person person, LocalDate start, LocalDate end) {
    this.person = person;
    this.start = start;
    this.end = end;
  }

}
