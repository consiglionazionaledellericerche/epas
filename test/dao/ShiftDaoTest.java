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

package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import common.injection.StaticInject;
import db.h2support.H2Examples;
import java.util.List;
import manager.ShiftManager2;
import models.Person;
import models.PersonShift;
import models.PersonShiftShiftType;
import models.ShiftCategories;
import models.ShiftType;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Test sui periodi di associazione tra persone e attività di turno.
 * Le date di inizio e di fine sono comprese nel periodo.
 */
@StaticInject
public class ShiftDaoTest extends UnitTest {

  public static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static ShiftDao shiftDao;
  @Inject
  private static ShiftManager2 shiftManager2;

  /**
   * Un periodo chiuso si sovrappone ai periodi che ne contengono almeno un giorno, compresi
   * il primo e l'ultimo.
   */
  @Test
  public void overlappingClosedPeriod() {
    final PersonShiftShiftType psst = period(new LocalDate(2026, 3, 1), new LocalDate(2026, 3, 31));

    assertTrue(overlapping(psst, new LocalDate(2026, 2, 1), new LocalDate(2026, 2, 28)).isEmpty());
    assertTrue(overlapping(psst, new LocalDate(2026, 4, 1), null).isEmpty());
    assertEquals(1, overlapping(psst, new LocalDate(2026, 2, 1), new LocalDate(2026, 3, 1)).size());
    assertEquals(1, overlapping(psst, new LocalDate(2026, 3, 31), null).size());
    assertEquals(1, overlapping(psst, new LocalDate(2026, 3, 10), new LocalDate(2026, 3, 12))
        .size());
  }

  /**
   * Un periodo aperto si sovrappone a tutti i periodi che terminano dopo il suo inizio.
   */
  @Test
  public void overlappingOpenPeriod() {
    final PersonShiftShiftType psst = period(new LocalDate(2026, 3, 1), null);

    assertTrue(overlapping(psst, new LocalDate(2026, 1, 1), new LocalDate(2026, 2, 28)).isEmpty());
    assertEquals(1, overlapping(psst, new LocalDate(2026, 1, 1), new LocalDate(2026, 3, 1)).size());
    assertEquals(1, overlapping(psst, new LocalDate(2027, 1, 1), null).size());
  }

  /**
   * L'associazione in modifica non è confrontata con se stessa.
   */
  @Test
  public void excludedPeriodIsIgnored() {
    final PersonShiftShiftType psst = period(new LocalDate(2026, 3, 1), null);

    assertTrue(shiftDao.getOverlappingPeriods(psst.getPersonShift(), psst.getShiftType(),
        new LocalDate(2026, 2, 1), Optional.absent(), Optional.of(psst)).isEmpty());
  }

  /**
   * Con due periodi consecutivi nell'intervallo la persona è considerata una sola volta,
   * col periodo più recente.
   */
  @Test
  public void consecutivePeriodsAreNotDuplicated() {
    final PersonShiftShiftType first =
        period(new LocalDate(2026, 3, 1), new LocalDate(2026, 3, 15));
    final PersonShiftShiftType second = newPeriod(first.getPersonShift(), first.getShiftType(),
        new LocalDate(2026, 3, 16), null);
    first.getShiftType().refresh();

    List<PersonShiftShiftType> workers = shiftManager2.shiftWorkers(first.getShiftType(),
        new LocalDate(2026, 3, 1), new LocalDate(2026, 3, 31));
    assertEquals(1, workers.size());
    assertEquals(second, workers.get(0));
  }

  private List<PersonShiftShiftType> overlapping(PersonShiftShiftType psst, LocalDate begin,
      LocalDate end) {
    return shiftDao.getOverlappingPeriods(psst.getPersonShift(), psst.getShiftType(), begin,
        Optional.fromNullable(end), Optional.absent());
  }

  /**
   * Associa una nuova persona ad una nuova attività di turno nel periodo indicato.
   */
  private PersonShiftShiftType period(LocalDate begin, LocalDate end) {
    final Person person = h2Examples.normalEmployee(BEGIN_2026, Optional.absent());

    final ShiftCategories category = new ShiftCategories();
    category.setDescription("Servizio di test");
    category.setSupervisor(person);
    category.setOffice(person.getOffice());
    category.save();

    final ShiftType shiftType = new ShiftType();
    shiftType.setType("Attività di test");
    shiftType.setDescription("Attività di test");
    shiftType.setShiftCategories(category);
    shiftType.save();

    final PersonShift personShift = new PersonShift();
    personShift.setPerson(person);
    personShift.setBeginDate(BEGIN_2026);
    personShift.save();

    return newPeriod(personShift, shiftType, begin, end);
  }

  private PersonShiftShiftType newPeriod(PersonShift personShift, ShiftType shiftType,
      LocalDate begin, LocalDate end) {
    final PersonShiftShiftType psst = new PersonShiftShiftType();
    psst.setPersonShift(personShift);
    psst.setShiftType(shiftType);
    psst.setBeginDate(begin);
    psst.setEndDate(end);
    psst.save();
    return psst;
  }
}
