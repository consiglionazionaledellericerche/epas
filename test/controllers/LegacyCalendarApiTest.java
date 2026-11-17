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

package controllers;

import common.injection.StaticInject;
import db.h2support.H2Examples;
import java.util.UUID;
import javax.inject.Inject;
import models.CompetenceCode;
import models.MonthlyCompetenceType;
import models.Person;
import models.PersonReperibilityType;
import models.Role;
import models.ShiftCategories;
import models.ShiftType;
import models.enumerate.LimitType;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

/**
 * Le azioni del vecchio calendario di reperibilità e turni (portale sistorg) sono permesse
 * solo sulle reperibilità e sui turni delle sedi su cui si ha il ruolo.
 */
@StaticInject
public class LegacyCalendarApiTest extends FunctionalTest {

  private static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);
  private static final String PERIOD = "/2026/09/01/2026/09/30";
  private static final String JSON = "application/json";

  @Inject
  private static H2Examples h2Examples;

  @Test
  public void reperibilityActionsAreScopedToTheOffice() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    PersonReperibilityType own = reperibilityType(colleague);
    PersonReperibilityType otherType = reperibilityType(other);
    String manager = h2Examples
        .userWithOfficeRole(colleague.getOffice(), Role.REPERIBILITY_MANAGER).getUsername();
    commit();

    login(manager);
    String base = "/reperibility/" + otherType.id;
    assertStatus(403, getJson(base + "/personList"));
    assertStatus(403, getJson(base + "/find" + PERIOD));
    assertStatus(403, getJson(base + "/who" + PERIOD));
    assertStatus(403, getJson(base + "/absence" + PERIOD));
    assertStatus(403, getJson(base + "/whoIsAbsent" + PERIOD));
    assertStatus(403, PUT(base + "/update/2026/9", JSON, "[]"));
    assertStatus(403, PUT(base + "/changePeriods", JSON, "[]"));

    assertIsOk(getJson("/reperibility/" + own.id + "/personList"));
    assertIsOk(getJson("/reperibility/" + own.id + "/find" + PERIOD));
    assertStatus(404, getJson("/reperibility/999999999/personList"));
  }

  @Test
  public void shiftActionsAreScopedToTheOffice() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    ShiftType own = shiftType(colleague);
    ShiftType otherType = shiftType(other);
    String manager = h2Examples
        .userWithOfficeRole(colleague.getOffice(), Role.SHIFT_MANAGER).getUsername();
    commit();

    login(manager);
    String base = "/shift/" + otherType.getType();
    assertStatus(403, getJson(base + "/personList"));
    assertStatus(403, getJson(base + "/timeTable"));
    assertStatus(403, getJson(base + "/find" + PERIOD));
    assertStatus(403, getJson(base + "/absence" + PERIOD));
    assertStatus(403, PUT(base + "/update/2026/9", JSON, "[]"));
    assertStatus(403, GET("/shift/exportMonthCalAsPDF?year=2026&month=9&type="
        + otherType.getShiftCategories().id));

    assertIsOk(getJson("/shift/" + own.getType() + "/personList"));
    assertStatus(404, getJson("/shift/inesistente-" + UUID.randomUUID() + "/personList"));
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

  private ShiftType shiftType(Person person) {
    ShiftCategories category = new ShiftCategories();
    category.setDescription("servizio-" + UUID.randomUUID());
    category.setOffice(person.getOffice());
    category.setSupervisor(person);
    category.save();
    ShiftType shiftType = new ShiftType();
    shiftType.setType("turno-" + UUID.randomUUID());
    shiftType.setDescription("Turno");
    shiftType.setShiftCategories(category);
    shiftType.save();
    return shiftType;
  }

  private Response getJson(String url) {
    Request request = newRequest();
    request.format = "json";
    return GET(request, url);
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  private void login(String username) {
    clearCookies();
    POST("/login?username=" + username + "&password=" + H2Examples.DEFAULT_PASSWORD);
  }
}
