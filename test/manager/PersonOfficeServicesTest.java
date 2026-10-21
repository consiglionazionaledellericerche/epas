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
import java.util.List;
import models.CompetenceCode;
import models.MonthlyCompetenceType;
import models.Office;
import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonShift;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ShiftCategories;
import models.ShiftType;
import models.enumerate.LimitType;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * Verifica che il trasferimento di sede termini la presenza della persona nei servizi di
 * reperibilità e turno della sede lasciata, rimuovendo i giorni di calendario successivi.
 */
@StaticInject
public class PersonOfficeServicesTest extends UnitTest {

  private static final LocalDate TRANSFER = new LocalDate(2026, 3, 15);

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static PersonOfficeManager personOfficeManager;

  @Test
  public void transferEndsReperibilityAndShiftsInSourceOffice() {
    final Person person = h2Examples.normalEmployee(new LocalDate(2026, 1, 1), Optional.absent());
    final Office source = person.getOffice();
    final Office destination = office(person);

    // reperibilità nella sede di provenienza, con giorni prima e dopo il trasferimento
    final PersonReperibilityType reperibilityType = reperibilityType(person, source);
    final PersonReperibility reperibility = new PersonReperibility();
    reperibility.setPerson(person);
    reperibility.setPersonReperibilityType(reperibilityType);
    reperibility.setStartDate(new LocalDate(2026, 1, 1));
    reperibility.save();
    person.getReperibility().add(reperibility);
    final PersonReperibilityDay before = reperibilityDay(reperibility, new LocalDate(2026, 3, 10));
    final PersonReperibilityDay after = reperibilityDay(reperibility, new LocalDate(2026, 3, 20));

    // turno nella sede di provenienza
    final ShiftType shiftType = shiftType(person, source);
    final PersonShift personShift = new PersonShift();
    personShift.setPerson(person);
    personShift.setBeginDate(new LocalDate(2026, 1, 1));
    personShift.save();
    person.getPersonShifts().add(personShift);
    final PersonShiftShiftType psst = new PersonShiftShiftType();
    psst.setPersonShift(personShift);
    psst.setShiftType(shiftType);
    psst.setBeginDate(new LocalDate(2026, 1, 1));
    psst.save();
    if (personShift.getPersonShiftShiftTypes() == null) {
      personShift.setPersonShiftShiftTypes(new java.util.ArrayList<>());
    }
    personShift.getPersonShiftShiftTypes().add(psst);
    final PersonShiftDay shiftBefore = shiftDay(personShift, shiftType, new LocalDate(2026, 3, 12));
    final PersonShiftDay shiftAfter = shiftDay(personShift, shiftType, new LocalDate(2026, 3, 16));
    JPA.em().flush();

    final List<String> messages =
        personOfficeManager.changeOffice(person, destination, TRANSFER).getMessages();
    JPA.em().flush();

    assertEquals(new LocalDate(2026, 3, 14), reperibility.getEndDate());
    assertTrue(reperibility.getPersonReperibilityDays().contains(before));
    assertFalse(reperibility.getPersonReperibilityDays().contains(after));
    assertNull(PersonReperibilityDay.findById(after.id));

    assertEquals(new LocalDate(2026, 3, 14), psst.getEndDate());
    assertTrue(personShift.getPersonShiftDays().contains(shiftBefore));
    assertNull(PersonShiftDay.findById(shiftAfter.id));

    assertFalse(messages.isEmpty());
  }

  private Office office(Person person) {
    final Office office = new Office();
    office.setName("Sede destinazione servizi " + person.id);
    office.setInstitute(person.getOffice().getInstitute());
    office.setBeginDate(new LocalDate(2020, 1, 1));
    office.setCodeId("SRV-" + person.id);
    office.setCode("SRV-" + person.id);
    office.save();
    return office;
  }

  private PersonReperibilityType reperibilityType(Person person, Office office) {
    final CompetenceCode code = new CompetenceCode();
    code.setCode("R" + person.id);
    code.setDescription("Reperibilità di test");
    code.setLimitType(LimitType.monthly);
    code.save();
    final MonthlyCompetenceType monthly = new MonthlyCompetenceType();
    monthly.setName("Reperibilità di test " + person.id);
    monthly.setWorkdaysCode(code);
    monthly.setHolidaysCode(code);
    monthly.save();
    final PersonReperibilityType type = new PersonReperibilityType();
    type.setDescription("Reperibilità di test " + person.id);
    type.setSupervisor(person);
    type.setOffice(office);
    type.setMonthlyCompetenceType(monthly);
    type.save();
    return type;
  }

  private PersonReperibilityDay reperibilityDay(PersonReperibility reperibility, LocalDate date) {
    final PersonReperibilityDay day = new PersonReperibilityDay();
    day.setPersonReperibility(reperibility);
    day.setReperibilityType(reperibility.getPersonReperibilityType());
    day.setDate(date);
    day.setHolidayDay(false);
    day.save();
    if (reperibility.getPersonReperibilityDays() == null) {
      reperibility.setPersonReperibilityDays(new java.util.ArrayList<>());
    }
    reperibility.getPersonReperibilityDays().add(day);
    return day;
  }

  private ShiftType shiftType(Person person, Office office) {
    final ShiftCategories category = new ShiftCategories();
    category.setDescription("Servizio turni di test " + person.id);
    category.setSupervisor(person);
    category.setOffice(office);
    category.save();
    final ShiftType shiftType = new ShiftType();
    shiftType.setType("Turno di test " + person.id);
    shiftType.setDescription("Turno di test");
    shiftType.setShiftCategories(category);
    shiftType.save();
    return shiftType;
  }

  private PersonShiftDay shiftDay(PersonShift personShift, ShiftType shiftType, LocalDate date) {
    final PersonShiftDay day = new PersonShiftDay();
    day.setPersonShift(personShift);
    day.setShiftType(shiftType);
    day.setDate(date);
    day.save();
    personShift.getPersonShiftDays().add(day);
    return day;
  }
}
