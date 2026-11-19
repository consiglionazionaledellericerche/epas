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
import dao.wrapper.IWrapperFactory;
import db.h2support.H2Examples;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Un ricalcolo a partire da un mese successivo all'ultimo da riepilogare (per esempio per
 * ferie inserite a tre mesi da oggi) non deve ricostruire tutti i riepiloghi del contratto
 * (#710).
 */
@StaticInject
public class FutureMonthRecapTest extends UnitTest {

  private static final int SENTINEL = 123456;

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static IWrapperFactory wrapperFactory;

  private ContractMonthRecap recap(Person person, YearMonth yearMonth) {
    Contract contract = person.getContracts().get(0);
    contract.refresh();
    return wrapperFactory.create(contract).getContractMonthRecap(yearMonth).get();
  }

  @Test
  public void futureMonthDoesNotRebuildContractRecaps() {
    LocalDate begin = LocalDate.now().minusMonths(3).withDayOfMonth(1);
    Person person = h2Examples.normalEmployee(begin, Optional.absent());
    consistencyManager.updatePersonRecaps(person.id, begin);

    YearMonth firstMonth = new YearMonth(begin);
    YearMonth currentMonth = new YearMonth(LocalDate.now());
    assertTrue(recap(person, currentMonth).isPersistent());

    ContractMonthRecap first = recap(person, firstMonth);
    first.setRemainingMinutesCurrentYear(SENTINEL);
    first.save();

    consistencyManager.updatePersonRecaps(person.id, LocalDate.now().plusMonths(3));

    first = recap(person, firstMonth);
    first.refresh();
    assertEquals(Integer.valueOf(SENTINEL), first.getRemainingMinutesCurrentYear());
    // nessun riepilogo oltre il mese corrente
    Contract contract = person.getContracts().get(0);
    contract.refresh();
    for (ContractMonthRecap cmr : contract.getContractMonthRecaps()) {
      assertFalse(new YearMonth(cmr.getYear(), cmr.getMonth()).isAfter(currentMonth));
    }
  }

  @Test
  public void pastMonthStillRebuildsFollowingRecaps() {
    LocalDate begin = LocalDate.now().minusMonths(3).withDayOfMonth(1);
    Person person = h2Examples.normalEmployee(begin, Optional.absent());
    consistencyManager.updatePersonRecaps(person.id, begin);

    YearMonth currentMonth = new YearMonth(LocalDate.now());
    ContractMonthRecap current = recap(person, currentMonth);
    current.setRemainingMinutesCurrentYear(SENTINEL);
    current.save();

    consistencyManager.updatePersonRecaps(person.id, begin.plusMonths(1));

    current = recap(person, currentMonth);
    current.refresh();
    assertFalse(SENTINEL == current.getRemainingMinutesCurrentYear());
  }
}
