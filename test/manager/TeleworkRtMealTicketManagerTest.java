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
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import common.injection.StaticInject;
import dao.absences.AbsenceComponentDao;
import db.h2support.H2Examples;
import java.util.List;
import manager.TeleworkRtMealTicketManager.AnalyzedDay;
import manager.TeleworkRtMealTicketManager.Outcome;
import manager.TeleworkRtMealTicketManager.Report;
import manager.TeleworkRtMealTicketManager.Row;
import manager.services.absences.AbsenceService;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * Simulazione del ricalcolo del buono pasto nei giorni con il 103RT.
 */
@StaticInject
public class TeleworkRtMealTicketManagerTest extends UnitTest {

  // Mercoledì
  private static final LocalDate DAY = new LocalDate(2026, 10, 14);

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  @Inject
  private static PersonDayManager personDayManager;
  @Inject
  private static TeleworkRtMealTicketManager manager;

  @Test
  public void normalEmployeeWithEnoughStampingsGainsMealTicket() {
    absenceService.enumInitializator();
    Person person = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    dayWith103rt(person, 9, 16);

    AnalyzedDay day = onlyDay(analyze(person));
    assertEquals(Outcome.GAINS_TICKET, day.getOutcome());
    assertEquals("Normale", day.getPartTime());
    assertEquals(420, day.getStampingMinutes());
    assertFalse(day.isWithoutRight());
  }

  @Test
  public void normalEmployeeWithFewStampingsDoesNotGainMealTicket() {
    absenceService.enumInitializator();
    Person person = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    dayWith103rt(person, 9, 13);

    Report report = analyze(person);
    assertTrue(report.getGaining().isEmpty());
    assertEquals(Outcome.NOT_ENOUGH_TIME, onlyDay(report).getOutcome());
  }

  @Test
  public void partTimeUnderSixHoursDoesNotGainMealTicket() {
    absenceService.enumInitializator();
    Person person = h2Examples.partTime50Employee(new LocalDate(2026, 1, 1));
    dayWith103rt(person, 9, 16);

    Report report = analyze(person);
    assertTrue(report.getGaining().isEmpty());
    assertEquals("Part time orizzontale", onlyDay(report).getPartTime());
  }

  private static Report analyze(Person person) {
    JPA.em().flush();
    Office office = person.getOffice();
    return manager.analyze(DAY, DAY, Optional.of(office));
  }

  private static AnalyzedDay onlyDay(Report report) {
    List<Row> rows = Lists.newArrayList(report.getGaining());
    rows.addAll(report.getUnchanged());
    assertEquals(1, rows.size());
    assertEquals(1, rows.get(0).getDays());
    return rows.get(0).getAnalyzedDays().get(0);
  }

  private static void dayWith103rt(Person person, int inHour, int outHour) {
    PersonDay pd = personDayManager.getOrCreateAndPersistPersonDay(person, DAY);
    Stamping in = new Stamping(pd, new LocalDateTime(2026, 10, 14, inHour, 0));
    in.setWay(WayType.in);
    in.setMarkedByTelework(true);
    in.save();
    Stamping out = new Stamping(pd, new LocalDateTime(2026, 10, 14, outHour, 0));
    out.setWay(WayType.out);
    out.setMarkedByTelework(true);
    out.save();

    Absence absence = new Absence();
    absence.setPersonDay(pd);
    absence.setAbsenceType(absenceComponentDao.absenceTypeByCode("103RT").get());
    absence.setJustifiedType(
        absenceComponentDao.getOrBuildJustifiedType(JustifiedTypeName.nothing));
    absence.save();
    pd.getAbsences().add(absence);
    pd.save();
  }
}
