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
import java.util.regex.Pattern;
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
 * Verifica sulle pagine reali che il nome di una persona contenente HTML sia mostrato come
 * testo nei titoli delle finestre modali (tag modalAsync), che lo ricevono dalle viste.
 */
@StaticInject
public class TemplateEscapeTest extends FunctionalTest {

  private static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);
  private static final String PAYLOAD = "<img src=x onerror=alert(document.domain)>";
  private static final String ESCAPED_PAYLOAD =
      "&lt;img src=x onerror=alert(document.domain)&gt;";

  @Inject
  private static H2Examples h2Examples;

  @Test
  public void personNameIsEscapedInAffiliationModalTitle() {
    Person person = personWithHtmlSurname();
    String admin = h2Examples.userWithOfficeRole(person.getOffice(), Role.PERSONNEL_ADMIN)
        .getUsername();
    commit();

    login(admin);
    assertModalTitleIsEscaped(GET(url("Affiliations.blankByPerson", "personId", person.id)));
  }

  @Test
  public void personNameIsEscapedInResidualForOvertimeModalTitle() {
    Person person = personWithHtmlSurname();
    addRole(person, Role.EMPLOYEE);
    commit();

    login(person.getUser().getUsername());
    Map<String, Object> args = new HashMap<>();
    args.put("year", 2026);
    args.put("month", 9);
    args.put("personId", person.id);
    assertModalTitleIsEscaped(
        GET(Router.reverse("PersonDays.checkPositiveResidualForOvertime", args).url));
  }

  private void assertModalTitleIsEscaped(Response response) {
    assertIsOk(response);
    String html = getContent(response);
    assertTrue("Il titolo della modale non contiene il nome codificato",
        html.matches("(?s).*<h3>[^<]*" + Pattern.quote(ESCAPED_PAYLOAD)
            + "[^<]*</h3>.*"));
    assertFalse("Il nome della persona è inserito come HTML", html.contains(PAYLOAD));
  }

  private Person personWithHtmlSurname() {
    Person person = h2Examples.activeNormalEmployee(BEGIN_2026);
    person.setSurname(PAYLOAD);
    person.save();
    return person;
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

  private String url(String action, String param, Object value) {
    Map<String, Object> args = new HashMap<>();
    args.put(param, value);
    return Router.reverse(action, args).url;
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  private void login(String username) {
    POST("/login?username=" + username + "&password=" + H2Examples.DEFAULT_PASSWORD);
  }
}
