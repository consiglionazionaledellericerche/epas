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
import db.h2support.H2Examples;
import db.h2support.base.H2AbsenceSupport;
import java.util.UUID;
import manager.services.absences.AbsenceService;
import models.CompetenceCode;
import models.MonthlyCompetenceType;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.absences.Absence;
import models.absences.definitions.DefaultAbsenceType;
import models.enumerate.LimitType;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.i18n.Messages;
import play.test.UnitTest;

/**
 * La reperibilità è impedita solo dalle assenze giornaliere con codice non compatibile con la
 * reperibilità, non da quelle orarie (#702).
 */
@StaticInject
public class ReperibilityAbsenceCompatibilityTest extends UnitTest {

  private static final LocalDate BEGIN = new LocalDate(2026, 1, 1);
  private static final LocalDate HOURLY_ABSENCE_DAY = new LocalDate(2026, 3, 10);
  private static final LocalDate DAILY_ABSENCE_DAY = new LocalDate(2026, 3, 11);

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static H2AbsenceSupport h2AbsenceSupport;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static ReperibilityManager2 reperibilityManager2;

  @Test
  public void hourlyAbsenceIsCompatible() {
    absenceService.enumInitializator();
    Absence hourly = h2AbsenceSupport.absenceInstance(DefaultAbsenceType.A_18M,
        HOURLY_ABSENCE_DAY, Optional.absent(), 60);
    assertFalse(hourly.getAbsenceType().isReperibilityCompatible());
    assertFalse(PersonDayManager.isAbsenceIncompatibleWithReperibility(hourly));
  }

  @Test
  public void dailyAbsenceIsIncompatible() {
    absenceService.enumInitializator();
    Absence daily = h2AbsenceSupport.absenceInstance(DefaultAbsenceType.A_32,
        DAILY_ABSENCE_DAY, Optional.absent(), null);
    assertFalse(daily.getAbsenceType().isReperibilityCompatible());
    assertTrue(PersonDayManager.isAbsenceIncompatibleWithReperibility(daily));
  }

  @Test
  public void reperibilityPermittedWithHourlyAbsenceOnly() {
    absenceService.enumInitializator();
    Person person = h2Examples.normalEmployee(BEGIN, Optional.absent());
    PersonReperibilityType type = reperibilityType(person);
    PersonReperibility personReperibility = new PersonReperibility();
    personReperibility.setPerson(person);
    personReperibility.setPersonReperibilityType(type);
    personReperibility.setStartDate(BEGIN);
    personReperibility.save();

    h2AbsenceSupport.absence(DefaultAbsenceType.A_18M, HOURLY_ABSENCE_DAY,
        Optional.absent(), 60, person);
    h2AbsenceSupport.absence(DefaultAbsenceType.A_32, DAILY_ABSENCE_DAY,
        Optional.absent(), null, person);

    assertFalse(reperibilityManager2
        .reperibilityPermitted(reperibilityDay(personReperibility, HOURLY_ABSENCE_DAY))
        .isPresent());

    Optional<String> error = reperibilityManager2
        .reperibilityPermitted(reperibilityDay(personReperibility, DAILY_ABSENCE_DAY));
    assertTrue(error.isPresent());
    assertEquals(Messages.get("reperibility.absenceInDay"), error.get());
  }

  private PersonReperibilityDay reperibilityDay(PersonReperibility personReperibility,
      LocalDate date) {
    PersonReperibilityDay day = new PersonReperibilityDay();
    day.setPersonReperibility(personReperibility);
    day.setReperibilityType(personReperibility.getPersonReperibilityType());
    day.setDate(date);
    return day;
  }

  private PersonReperibilityType reperibilityType(Person person) {
    MonthlyCompetenceType monthlyType = new MonthlyCompetenceType();
    monthlyType.setName("tipo-" + UUID.randomUUID());
    monthlyType.setWorkdaysCode(competenceCode());
    monthlyType.setHolidaysCode(competenceCode());
    monthlyType.save();
    PersonReperibilityType type = new PersonReperibilityType();
    type.setDescription("reperibilita-" + UUID.randomUUID());
    type.setOffice(person.getOffice());
    type.setSupervisor(person);
    type.setMonthlyCompetenceType(monthlyType);
    type.save();
    return type;
  }

  private CompetenceCode competenceCode() {
    CompetenceCode code = new CompetenceCode();
    code.setCode("c-" + UUID.randomUUID());
    code.setCodeToPresence(code.getCode());
    code.setDescription("codice " + code.getCode());
    code.setLimitType(LimitType.noLimit);
    code.save();
    return code;
  }
}
