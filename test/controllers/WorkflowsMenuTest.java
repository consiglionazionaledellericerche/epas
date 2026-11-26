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

import com.google.common.base.Optional;
import common.injection.StaticInject;
import db.h2support.H2Examples;
import java.util.regex.Pattern;
import javax.inject.Inject;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.flows.AbsenceRequest;
import models.flows.AbsenceRequestEvent;
import models.flows.enumerate.AbsenceRequestEventType;
import models.flows.enumerate.AbsenceRequestType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Badge del menu dei flussi di lavoro: i conteggi delle richieste da approvare, calcolati una
 * volta per richiesta HTTP, compaiono sia sul menu sia sulla voce corrispondente.
 */
@StaticInject
public class WorkflowsMenuTest extends FunctionalTest {

  private static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static ConfigurationManager configurationManager;

  /**
   * Il responsabile di sede vede il numero di richieste di ferie da approvare sul menu e sulla
   * voce delle ferie.
   */
  @Test
  public void seatSupervisorSeesPendingVacationRequests() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    Office office = colleague.getOffice();
    enableFlows(office);
    User supervisor = h2Examples.userWithOfficeRole(office, Role.SEAT_SUPERVISOR);
    pendingVacationRequest(colleague, supervisor);
    commit();

    login(supervisor.getUsername());
    String vacationsToApprove = Router.reverse("AbsenceRequests.vacationsToApprove").url;
    // vacationsToApprove richiama listToApprove, che Play trasforma in un redirect
    Response response = GET("/absencerequests/listtoapprove?type=VACATION_REQUEST");
    assertIsOk(response);
    String body = getContent(response);

    String badge = "<span class=\"label label-danger label-badge\">1</span>";
    assertTrue("badge totale sul menu dei flussi",
        Pattern.compile("fa-recycle fa-fw\"></i>\\s*" + Pattern.quote(badge))
            .matcher(body).find());
    assertTrue("badge sulla voce delle ferie da approvare",
        Pattern.compile(Pattern.quote(vacationsToApprove) + "\"[^>]*>\\s*"
            + "<i class=\"fa fa-plane\" aria-hidden=\"true\"></i>\\s*" + Pattern.quote(badge))
            .matcher(body).find());
  }

  /**
   * Un dipendente senza ruoli di approvazione vede le pagine con il menu dei flussi senza badge.
   */
  @Test
  public void employeeWithoutApprovalRolesHasNoBadge() {
    Person employee = h2Examples.activeNormalEmployee(BEGIN_2026);
    enableFlows(employee.getOffice());
    Role role = Role.find("byName", Role.EMPLOYEE).first();
    if (role == null) {
      role = new Role();
      role.setName(Role.EMPLOYEE);
      role.save();
    }
    UsersRolesOffices uro = new UsersRolesOffices();
    uro.setUser(employee.getUser());
    uro.setOffice(employee.getOffice());
    uro.setRole(role);
    uro.save();
    commit();

    login(employee.getUser().getUsername());
    Response response = GET(Router.reverse("Stampings.stampings").url
        + "?year=" + LocalDate.now().getYear() + "&month=" + LocalDate.now().getMonthOfYear());
    assertIsOk(response);
    assertFalse(Pattern.compile("fa-recycle fa-fw\"></i>\\s*<span class=\"label")
        .matcher(getContent(response)).find());
  }

  private void enableFlows(Office office) {
    configurationManager.updateBoolean(EpasParam.ENABLE_FLOWS, office, true,
        Optional.of(BEGIN_2026), Optional.absent(), true);
    office.refresh();
  }

  private void pendingVacationRequest(Person person, User owner) {
    AbsenceRequest request = new AbsenceRequest();
    request.setType(AbsenceRequestType.VACATION_REQUEST);
    request.setPerson(person);
    request.setStartAt(LocalDateTime.now().plusDays(7));
    request.setEndTo(LocalDateTime.now().plusDays(8));
    request.setManagerApprovalRequired(false);
    request.setAdministrativeApprovalRequired(false);
    request.setOfficeHeadApprovalRequired(true);
    request.setOfficeHeadApprovalForManagerRequired(false);
    request.setFlowStarted(true);
    request.save();

    AbsenceRequestEvent event = AbsenceRequestEvent.builder()
        .absenceRequest(request).owner(owner)
        .eventType(AbsenceRequestEventType.STARTING_APPROVAL_FLOW).build();
    event.save();
    request.getEvents().add(event);
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  private void login(String username) {
    POST("/login?username=" + username + "&password=" + H2Examples.DEFAULT_PASSWORD);
  }
}
