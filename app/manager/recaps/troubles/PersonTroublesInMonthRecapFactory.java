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

package manager.recaps.troubles;

import dao.PersonDayInTroubleDao;
import javax.inject.Inject;
import manager.PersonManager;
import models.Person;
import org.joda.time.LocalDate;

/**
 * Factory per PersonTroublesInMonthRecap.
 */
public class PersonTroublesInMonthRecapFactory {

  private final PersonDayInTroubleDao personDayInTroubleDao;
  private final PersonManager personManager;

  /**
   * Costruttore per l'injection.
   */
  @Inject
  PersonTroublesInMonthRecapFactory(PersonDayInTroubleDao personDayInTroubleDao,
                                    PersonManager personManager) {
    this.personDayInTroubleDao = personDayInTroubleDao;
    this.personManager = personManager;

  }

  /**
   * Metodo che crea il PersonTroublesInMonthRecap.
   *
   * @param person la persona
   * @param monthBegin l'inizio del mese
   * @param monthEnd la fine del mese
   * @return il PersonTroublesInMonthRecap del mese.
   */
  public PersonTroublesInMonthRecap create(Person person,
                                           LocalDate monthBegin, LocalDate monthEnd) {

    return new PersonTroublesInMonthRecap(personDayInTroubleDao,
            personManager, person, monthBegin, monthEnd);
  }

}
