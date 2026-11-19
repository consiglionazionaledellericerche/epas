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

  /**
   * Nessun riepilogo del contratto oltre il mese indicato.
   */
  private void assertNoRecapAfter(Person person, YearMonth lastMonth) {
    Contract contract = person.getContracts().get(0);
    contract.refresh();
    for (ContractMonthRecap cmr : contract.getContractMonthRecaps()) {
      assertFalse(new YearMonth(cmr.getYear(), cmr.getMonth()).isAfter(lastMonth));
    }
  }

  @Test
  public void futureMonthDoesNotRebuildContractRecaps() {
    LocalDate today = LocalDate.now();
    LocalDate begin = today.minusMonths(3).withDayOfMonth(1);
    Person person = h2Examples.normalEmployee(begin, Optional.absent());
    consistencyManager.updatePersonRecaps(person.id, begin);

    YearMonth firstMonth = new YearMonth(begin);
    YearMonth currentMonth = new YearMonth(today);
    assertTrue(recap(person, currentMonth).isPersistent());

    ContractMonthRecap first = recap(person, firstMonth);
    first.setRemainingMinutesCurrentYear(SENTINEL);
    first.save();

    consistencyManager.updatePersonRecaps(person.id, today.plusMonths(3));

    first = recap(person, firstMonth);
    first.refresh();
    assertEquals(Integer.valueOf(SENTINEL), first.getRemainingMinutesCurrentYear());
    assertNoRecapAfter(person, currentMonth);
  }

  @Test
  public void monthAfterContractEndDoesNotRebuildContractRecaps() {
    LocalDate today = LocalDate.now();
    LocalDate begin = today.minusMonths(6).withDayOfMonth(1);
    LocalDate end = today.minusMonths(3).dayOfMonth().withMaximumValue();
    Person person = h2Examples.normalEmployee(begin, Optional.of(end));
    consistencyManager.updatePersonRecaps(person.id, begin);

    YearMonth firstMonth = new YearMonth(begin);
    YearMonth endMonth = new YearMonth(end);
    assertTrue(recap(person, endMonth).isPersistent());

    ContractMonthRecap first = recap(person, firstMonth);
    first.setRemainingMinutesCurrentYear(SENTINEL);
    first.save();

    consistencyManager.updatePersonRecaps(person.id, end.plusMonths(1).withDayOfMonth(1));

    first = recap(person, firstMonth);
    first.refresh();
    assertEquals(Integer.valueOf(SENTINEL), first.getRemainingMinutesCurrentYear());
    assertNoRecapAfter(person, endMonth);
  }

  @Test
  public void pastMonthStillRebuildsFollowingRecaps() {
    LocalDate today = LocalDate.now();
    LocalDate begin = today.minusMonths(3).withDayOfMonth(1);
    Person person = h2Examples.normalEmployee(begin, Optional.absent());
    consistencyManager.updatePersonRecaps(person.id, begin);

    YearMonth currentMonth = new YearMonth(today);
    ContractMonthRecap current = recap(person, currentMonth);
    current.setRemainingMinutesCurrentYear(SENTINEL);
    current.save();

    consistencyManager.updatePersonRecaps(person.id, begin.plusMonths(1));

    current = recap(person, currentMonth);
    current.refresh();
    assertNotEquals(Integer.valueOf(SENTINEL), current.getRemainingMinutesCurrentYear());
  }

  /**
   * Se manca il riepilogo del mese precedente a quello richiesto vengono ricostruiti tutti i
   * riepiloghi del contratto, e il mese richiesto non deve poi essere ricalcolato senza
   * riepilogo precedente, perdendo i residui accumulati.
   */
  @Test
  public void missingPreviousRecapKeepsResiduals() {
    LocalDate today = LocalDate.now();
    LocalDate begin = today.minusMonths(3).withDayOfMonth(1);
    Person person = h2Examples.normalEmployee(begin, Optional.absent());
    // Inizializzazione con un residuo da portare avanti nei mesi successivi.
    Contract init = person.getContracts().get(0);
    LocalDate sourceDate = begin.dayOfMonth().withMaximumValue();
    init.setSourceDateResidual(sourceDate);
    init.setSourceDateMealTicket(sourceDate);
    init.setSourceRemainingMinutesCurrentYear(600);
    init.setSourceRemainingMinutesLastYear(0);
    init.setSourceRemainingMealTicket(0);
    init.save();
    consistencyManager.updatePersonRecaps(person.id, begin);

    YearMonth gapMonth = new YearMonth(begin).plusMonths(1);
    YearMonth requestedMonth = gapMonth.plusMonths(1);
    ContractMonthRecap expected = recap(person, requestedMonth);
    int expectedResidual =
        expected.getRemainingMinutesCurrentYear() + expected.getRemainingMinutesLastYear();

    ContractMonthRecap gap = recap(person, gapMonth);
    // Il test ha senso solo se il mese mancante porta avanti un residuo.
    assertNotEquals(0,
        gap.getRemainingMinutesCurrentYear() + gap.getRemainingMinutesLastYear());
    Contract contract = person.getContracts().get(0);
    contract.getContractMonthRecaps().remove(gap);
    gap.delete();

    consistencyManager.updatePersonRecaps(person.id, requestedMonth.toLocalDate(1));

    assertTrue(recap(person, gapMonth).isPersistent());
    ContractMonthRecap rebuilt = recap(person, requestedMonth);
    rebuilt.refresh();
    assertEquals(expectedResidual,
        rebuilt.getRemainingMinutesCurrentYear() + rebuilt.getRemainingMinutesLastYear());
  }
}
