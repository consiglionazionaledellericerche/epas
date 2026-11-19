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

package manager;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import common.injection.StaticInject;
import dao.PersonDayDao;
import db.h2support.H2Examples;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.enumerate.MealTicketBehaviour;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Chiudendo un contratto i giorni successivi alla chiusura vanno azzerati, altrimenti
 * continuano a maturare residuo (#693).
 */
@StaticInject
public class ContractCloseResetPersonDaysTest extends UnitTest {

  private static final LocalDate BEGIN = new LocalDate(2026, 9, 1);
  private static final LocalDate CLOSE = new LocalDate(2026, 10, 10);
  private static final LocalDate AFTER_CLOSE_1 = new LocalDate(2026, 10, 12);
  private static final LocalDate AFTER_CLOSE_2 = new LocalDate(2026, 10, 13);

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static ContractManager contractManager;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static PersonDayDao personDayDao;

  private PersonDay dirtyPersonDay(Person person, LocalDate date, int progressive) {
    PersonDay pd = new PersonDay(person, date, 0, -432, progressive);
    pd.setTicketAvailable(MealTicketBehaviour.allowMealTicket);
    pd.save();
    return pd;
  }

  private void assertReset(Person person, LocalDate date) {
    PersonDay pd = personDayDao.getPersonDay(person, date).get();
    pd.refresh();
    assertEquals(Integer.valueOf(0), pd.getTimeAtWork());
    assertEquals(Integer.valueOf(0), pd.getDifference());
    assertEquals(Integer.valueOf(0), pd.getProgressive());
    assertFalse(pd.isTicketAvailable());
  }

  @Test
  public void closingContractResetsFollowingDays() {
    Person person = h2Examples.normalEmployee(BEGIN, Optional.absent());
    dirtyPersonDay(person, AFTER_CLOSE_1, -432);
    dirtyPersonDay(person, AFTER_CLOSE_2, -864);

    Contract contract = person.getContracts().get(0);
    contract.setEndContract(CLOSE);
    assertTrue(contractManager.properContractUpdate(contract, CLOSE, false));

    assertReset(person, AFTER_CLOSE_1);
    assertReset(person, AFTER_CLOSE_2);
  }

  @Test
  public void daysInsideContractAreNotReset() {
    Person person = h2Examples.normalEmployee(BEGIN, Optional.of(CLOSE));
    PersonDay inside = dirtyPersonDay(person, CLOSE.minusDays(3), -432);
    dirtyPersonDay(person, AFTER_CLOSE_1, -432);

    assertEquals(1, consistencyManager.resetPersonDaysOutOfContract(person, BEGIN));

    inside.refresh();
    assertEquals(Integer.valueOf(-432), inside.getDifference());
    assertReset(person, AFTER_CLOSE_1);
    // una seconda esecuzione non trova più nulla da azzerare
    assertEquals(0, consistencyManager.resetPersonDaysOutOfContract(person, BEGIN));
  }
}
