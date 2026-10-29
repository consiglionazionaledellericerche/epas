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
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Test funzionali sui permessi di Charts.exportPersonalTimesheetSituation: un dipendente può
 * esportare solo la propria situazione.
 */
@StaticInject
public class ChartsExportPermissionsTest extends FunctionalTest {

  private static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);

  @Inject
  private static H2Examples h2Examples;

  /**
   * Un dipendente non può esportare la situazione di un collega.
   */
  @Test
  public void employeeCannotExportTimesheetOfAColleague() {
    Person attacker = employee();
    Person victim = employee();
    commit();

    login(attacker);
    assertStatus(403, POST(exportUrl(), exportParams(victim)));
  }

  /**
   * Un dipendente può esportare la propria situazione.
   */
  @Test
  public void employeeCanExportOwnTimesheet() {
    Person employee = employee();
    commit();

    login(employee);
    Response response = POST(exportUrl(), exportParams(employee));
    assertIsOk(response);
    assertContentType("application/zip", response);
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

  private Map<String, String> exportParams(Person person) {
    Map<String, String> params = new HashMap<>();
    params.put("exportFile", "CSV");
    params.put("onlyMission", "false");
    params.put("beginDate", "01/09/2026");
    params.put("endDate", "30/09/2026");
    params.put("personId", person.id.toString());
    return params;
  }

  private String exportUrl() {
    return Router.reverse("Charts.exportPersonalTimesheetSituation").url;
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
