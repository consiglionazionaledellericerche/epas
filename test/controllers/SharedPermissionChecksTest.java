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
 * Gli esiti dei controlli senza target sono condivisi tra le richieste dello stesso utente, ma un
 * ruolo assegnato all'utente vale dalla richiesta successiva.
 */
@StaticInject
public class SharedPermissionChecksTest extends FunctionalTest {

  @Inject
  private static H2Examples h2Examples;

  @Test
  public void newOfficeRoleIsVisibleOnNextPage() {
    Person employee = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    addRole(employee, Role.EMPLOYEE);
    commit();

    POST("/login?username=" + employee.getUser().getUsername() + "&password="
        + H2Examples.DEFAULT_PASSWORD);
    String stampings = Router.reverse("Stampings.stampings").url + "?year="
        + LocalDate.now().getYear() + "&month=" + LocalDate.now().getMonthOfYear();
    // voce del menu di amministrazione visibile all'amministratore del personale
    String dailyPresence = "href=\"/stampings/dailypresence";

    Response before = GET(stampings);
    assertIsOk(before);
    assertFalse("senza ruolo di amministrazione niente presenza giornaliera",
        getContent(before).contains(dailyPresence));

    addRole(employee, Role.PERSONNEL_ADMIN);
    commit();

    Response after = GET(stampings);
    assertIsOk(after);
    assertTrue("il nuovo ruolo vale subito",
        getContent(after).contains(dailyPresence));
  }

  private void addRole(Person person, String roleName) {
    Role role = Role.find("byName", roleName).first();
    if (role == null) {
      role = new Role();
      role.setName(roleName);
      role.save();
    }
    UsersRolesOffices uro = new UsersRolesOffices();
    uro.setUser(person.getUser());
    uro.setOffice(person.getOffice());
    uro.setRole(role);
    uro.save();
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }
}
