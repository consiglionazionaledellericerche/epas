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
    Response response = GET("/rest/v4/me");
    assertJsonError(401, response);
    assertNotNull(response.getHeader("WWW-Authenticate"));
    assertJsonError(401, GET(REQUESTS));
  }

  @Test
  public void userWithoutEmployeeRoleIsForbidden() {
    Person person = h2Examples.activeNormalEmployee(BEGIN_2026);
    commit();

    login(person);
    // negato dalle regole di sicurezza prima di arrivare al controller
    assertJsonError(403, GET("/rest/v4/me"));
    assertJsonError(403, GET(REQUESTS));
  }

  @Test
  public void employeeReadsOwnMonthAndMealTickets() {
    Person employee = employee();
    commit();

    login(employee);
    assertIsOk(GET("/rest/v4/me/month?year=2026&month=9"));
    assertIsOk(GET("/rest/v4/me/mealTickets"));
    assertJsonError(400, GET("/rest/v4/me/month?year=2026&month=13"));
    assertJsonError(404, GET("/rest/v4/me/day?date=2026-12-24"));
  }

  @Test
  public void invalidNewRequestsAreRejected() {
    Person employee = employee();
    commit();

    login(employee);
    // startAt obbligatorio
    assertJsonError(400, POST(REQUESTS, JSON, "{\"type\":\"VACATION_REQUEST\"}"));
    // tipo non gestito dalle API
    assertJsonError(400, POST(REQUESTS, JSON,
        "{\"type\":\"SHORT_TERM_PERMIT\",\"startAt\":\"2026-11-02\"}"));
    // JSON non valido
    assertJsonError(400, POST(REQUESTS, JSON, "{"));
    // fine precedente all'inizio
    Response response = POST(REQUESTS, JSON,
        "{\"type\":\"VACATION_REQUEST\",\"startAt\":\"2026-11-03\",\"endTo\":\"2026-11-02\"}");
    assertStatus(400, response);
    assertContentType(JSON, response);
    assertContentMatch("\"errors\":\\[", response);
    assertEquals(0, AbsenceRequest.count("person = ?1", employee));
  }

  @Test
  public void newRequestIsCreatedOnlyByPost() {
    Person employee = employee();
    commit();

    login(employee);
    Response response = GET("/rest/v4/me/createAbsenceRequest");
    assertJsonError(405, response);
    assertEquals("POST", response.getHeader("Allow"));
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
    assertJsonError(404, GET(REQUESTS + "/" + other.id));
    assertJsonError(404, DELETE(REQUESTS + "/" + other.id));
    // la cancellazione è ammessa solo con DELETE
    assertJsonError(405, GET("/rest/v4/me/deleteAbsenceRequest/" + own.id));

    Response list = GET(REQUESTS + "?from=2026-01-01");
    assertIsOk(list);
    assertContentMatch("\"id\":" + own.id + ",", list);
    assertFalse(getContent(list).contains("\"id\":" + other.id + ","));

    Response deleted = DELETE(REQUESTS + "/" + own.id);
    assertIsOk(deleted);
    assertContentMatch("\"status\":\"DELETED\"", deleted);
    JPA.em().clear();
    assertTrue(AbsenceRequest.<AbsenceRequest>findById(own.id).isFlowEnded());
    assertFalse(AbsenceRequest.<AbsenceRequest>findById(other.id).isFlowEnded());
    // una richiesta ritirata non si può ritirare di nuovo
    assertJsonError(409, DELETE(REQUESTS + "/" + own.id));
  }

  @Test
  public void approvedRequestCannotBeDeleted() {
    Person employee = employee();
    AbsenceRequest approved = request(employee);
    approved.setOfficeHeadApproved(LocalDateTime.now());
    approved.save();
    commit();

    login(employee);
    assertJsonError(409, DELETE(REQUESTS + "/" + approved.id));
  }

  /**
   * Tutti gli errori delle API sono in JSON nella forma {"error": "..."}.
   */
  private void assertJsonError(int status, Response response) {
    assertStatus(status, response);
    assertContentType(JSON, response);
    assertContentMatch("^\\{\"error\":\".+\"\\}$", response);
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
