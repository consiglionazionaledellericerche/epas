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

package controllers.rest.v4;

import common.injection.StaticInject;
import db.h2support.H2Examples;
import javax.inject.Inject;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import models.flows.AbsenceRequest;
import models.flows.enumerate.AbsenceRequestType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

/**
 * Test funzionali delle API REST self-service: il dipendente vede e modifica solo i propri
 * dati.
 */
@StaticInject
public class MeTest extends FunctionalTest {

  private static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);
  private static final String JSON = "application/json";
  private static final String REQUESTS = "/rest/v4/me/absenceRequests";

  @Inject
  private static H2Examples h2Examples;
  @Test
  public void employeeReadsOwnProfile() {
    Person employee = employee();
    commit();

    login(employee);
    Response response = GET("/rest/v4/me");
    assertIsOk(response);
    assertContentType(JSON, response);
    assertContentMatch("\"id\":" + employee.id + ",", response);
    assertContentMatch("VACATION_REQUEST", response);
  }

  @Test
  public void anonymousRequestIsUnauthorized() {
    assertStatus(401, GET("/rest/v4/me"));
    assertStatus(401, GET(REQUESTS));
  }

  @Test
  public void userWithoutEmployeeRoleIsForbidden() {
    Person person = h2Examples.activeNormalEmployee(BEGIN_2026);
    commit();

    login(person);
    assertStatus(403, GET("/rest/v4/me"));
    assertStatus(403, GET(REQUESTS));
  }

  @Test
  public void employeeReadsOwnMonthAndMealTickets() {
    Person employee = employee();
    commit();

    login(employee);
    assertIsOk(GET("/rest/v4/me/month?year=2026&month=9"));
    assertIsOk(GET("/rest/v4/me/mealTickets"));
    assertStatus(400, GET("/rest/v4/me/month?year=2026&month=13"));
  }

  @Test
  public void invalidNewRequestsAreRejected() {
    Person employee = employee();
    commit();

    login(employee);
    // startAt obbligatorio
    assertStatus(400, POST(REQUESTS, JSON, "{\"type\":\"VACATION_REQUEST\"}"));
    // tipo non gestito dalle API
    assertStatus(400, POST(REQUESTS, JSON,
        "{\"type\":\"SHORT_TERM_PERMIT\",\"startAt\":\"2026-11-02\"}"));
    // JSON non valido
    assertStatus(400, POST(REQUESTS, JSON, "{"));
    // fine precedente all'inizio
    Response response = POST(REQUESTS, JSON,
        "{\"type\":\"VACATION_REQUEST\",\"startAt\":\"2026-11-03\",\"endTo\":\"2026-11-02\"}");
    assertStatus(400, response);
    assertContentMatch("errors", response);
    assertEquals(0, AbsenceRequest.count("person = ?1", employee));
  }

  @Test
  public void newRequestIsCreatedOnlyByPost() {
    Person employee = employee();
    commit();

    login(employee);
    assertStatus(400, GET("/rest/v4/me/createAbsenceRequest"));
  }

  @Test
  public void employeeSeesAndDeletesOnlyOwnRequests() {
    Person employee = employee();
    Person colleague = employee();
    AbsenceRequest own = request(employee);
    AbsenceRequest other = request(colleague);
    commit();

    login(employee);
    assertIsOk(GET(REQUESTS + "/" + own.id));
    assertStatus(404, GET(REQUESTS + "/" + other.id));
    assertStatus(404, DELETE(REQUESTS + "/" + other.id));
    // la cancellazione è ammessa solo con DELETE
    assertStatus(400, GET("/rest/v4/me/deleteAbsenceRequest/" + own.id));

    Response list = GET(REQUESTS + "?from=2026-01-01");
    assertIsOk(list);
    assertContentMatch("\"id\":" + own.id + ",", list);
    assertFalse(getContent(list).contains("\"id\":" + other.id + ","));

    assertIsOk(DELETE(REQUESTS + "/" + own.id));
    JPA.em().clear();
    assertTrue(AbsenceRequest.<AbsenceRequest>findById(own.id).isFlowEnded());
    assertFalse(AbsenceRequest.<AbsenceRequest>findById(other.id).isFlowEnded());
    // una richiesta ritirata non si può ritirare di nuovo
    assertStatus(409, DELETE(REQUESTS + "/" + own.id));
  }

  @Test
  public void approvedRequestCannotBeDeleted() {
    Person employee = employee();
    AbsenceRequest approved = request(employee);
    approved.setOfficeHeadApproved(LocalDateTime.now());
    approved.save();
    commit();

    login(employee);
    assertStatus(409, DELETE(REQUESTS + "/" + approved.id));
  }

  private Person employee() {
    Person person = h2Examples.activeNormalEmployee(BEGIN_2026);
    Role role = Role.find("byName", Role.EMPLOYEE).first();
    if (role == null) {
      role = new Role();
      role.setName(Role.EMPLOYEE);
      role.save();
    }
    UsersRolesOffices uro = new UsersRolesOffices();
    uro.setUser(person.getUser());
    uro.setOffice(person.getOffice());
    uro.setRole(role);
    uro.save();
    return person;
  }

  private AbsenceRequest request(Person person) {
    AbsenceRequest request = new AbsenceRequest();
    request.setType(AbsenceRequestType.VACATION_REQUEST);
    request.setPerson(person);
    request.setStartAt(new LocalDateTime(2026, 11, 2, 0, 0));
    request.setEndTo(new LocalDateTime(2026, 11, 3, 0, 0));
    request.setFlowStarted(true);
    return request.save();
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  private void login(Person person) {
    POST("/login?username=" + person.getUser().getUsername() + "&password="
        + H2Examples.DEFAULT_PASSWORD);
  }
}
