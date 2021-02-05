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

package manager.recaps.personstamping;

import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import javax.inject.Inject;
import manager.PersonDayManager;
import manager.PersonManager;
import models.Person;

/**
 * Factory per PersonStampingRecap.
 */
public class PersonStampingRecapFactory {

  private final PersonDayManager personDayManager;
  private final PersonDayDao personDayDao;
  private final PersonManager personManager;
  private final PersonStampingDayRecapFactory stampingDayRecapFactory;
  private final IWrapperFactory wrapperFactory;

  /**
   * Costruttore per l'injection.
   */
  @Inject
  PersonStampingRecapFactory(PersonDayManager personDayManager,
                             PersonDayDao personDayDao,
                             PersonManager personManager,
                             IWrapperFactory wrapperFactory,
                             PersonStampingDayRecapFactory stampingDayRecapFactory) {

    this.personDayManager = personDayManager;
    this.personDayDao = personDayDao;
    this.personManager = personManager;
    this.stampingDayRecapFactory = stampingDayRecapFactory;
    this.wrapperFactory = wrapperFactory;
  }

  /**
   * Costruisce il riepilogo mensile delle timbrature.
   */
  public PersonStampingRecap create(Person person, int year, int month, 
      boolean considerExitingNow) {

    return new PersonStampingRecap(personDayManager, personDayDao,
        personManager, stampingDayRecapFactory,
        wrapperFactory, year, month, person, considerExitingNow);
  }

}
