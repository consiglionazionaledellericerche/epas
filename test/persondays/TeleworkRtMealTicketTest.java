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

package persondays;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import common.injection.StaticInject;
import manager.PersonDayManager;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultAbsenceType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Il 103RT non giustifica tempo e consente il buono pasto: matura solo se le timbrature in
 * telelavoro raggiungono la soglia prevista dall'orario.
 */
@StaticInject
public class TeleworkRtMealTicketTest extends UnitTest {

  private static final LocalDate DAY = new LocalDate(2026, 10, 14);

  @Inject
  private static PersonDayManager personDayManager;

  private final PersonDaysTest support = new PersonDaysTest();

  @Test
  public void enoughStampingsGiveMealTicket() {
    PersonDay personDay = dayWith103rt(9, 0, 16, 0);
    compute(personDay);

    assertEquals(420, personDay.getStampingsTime().intValue());
    assertTrue(personDay.isTicketAvailable());
  }

  @Test
  public void notEnoughStampingsGiveNoMealTicket() {
    PersonDay personDay = dayWith103rt(9, 0, 13, 0);
    compute(personDay);

    assertEquals(240, personDay.getStampingsTime().intValue());
    assertFalse(personDay.isTicketAvailable());
  }

  @Test
  public void code103rtDoesNotJustifyTime() {
    PersonDay personDay = dayWith103rt(9, 0, 13, 0);
    compute(personDay);

    assertEquals(240, personDay.getTimeAtWork().intValue());
  }

  private static void compute(PersonDay personDay) {
    personDayManager.updateTimeAtWork(personDay, new PersonDaysTest().normalDay(), false,
        PersonDaysTest.startLunch, PersonDaysTest.endLunch, PersonDaysTest.startWork,
        PersonDaysTest.endWork, Optional.<Stamping>absent());
  }

  private PersonDay dayWith103rt(int inHour, int inMinute, int outHour, int outMinute) {
    PersonDay personDay = new PersonDay(new Person(), DAY);
    support.stampings(personDay, inHour, inMinute, WayType.in, null, null);
    support.stampings(personDay, outHour, outMinute, WayType.out, null, null);

    DefaultAbsenceType rt = DefaultAbsenceType.A_103RT;
    AbsenceType absenceType = new AbsenceType();
    absenceType.setCode("103RT");
    absenceType.setMealTicketBehaviour(rt.mealTicketBehaviour);
    JustifiedType nothing = new JustifiedType();
    nothing.setName(JustifiedTypeName.nothing);

    Absence absence = new Absence();
    absence.setPersonDay(personDay);
    absence.setAbsenceType(absenceType);
    absence.setJustifiedType(nothing);
    personDay.getAbsences().add(absence);
    return personDay;
  }
}
