/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

package manager.attestati.service;

import lombok.Value;
import models.Office;
import models.Person;
import org.joda.time.YearMonth;

/**
 * Chiave della situazione attestati di una persona: nel mese di un trasferimento la persona ha
 * una situazione distinta per ciascuna sede.
 */
@Value
public class PersonCertDataKey {

  Person person;
  Office office;
  YearMonth yearMonth;

  /**
   * La chiave per la sede a cui la persona afferisce alla fine del mese.
   */
  public static PersonCertDataKey of(Person person, YearMonth yearMonth) {
    return new PersonCertDataKey(person,
        person.getOfficeAtOrCurrent(yearMonth.toLocalDate(1).dayOfMonth().withMaximumValue()),
        yearMonth);
  }
}
