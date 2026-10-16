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
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Test funzionali sulle azioni che operano su persone o dati di una sede: chi ha un ruolo su
 * una sede non può usarle sulle persone di altre sedi, mentre continua a poterle usare sulle
 * persone della propria sede.
 */
@StaticInject
public class OfficeScopedActionsTest extends FunctionalTest {

  private static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);

  @Inject
  private static H2Examples h2Examples;

  /**
   * Appartenenze ai gruppi: solo per le persone della propria sede.
   */
  @Test
  public void personnelAdminSeesAffiliationsOnlyInOwnOffice() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    String admin = roleOn(colleague, Role.PERSONNEL_ADMIN);
    commit();

    login(admin);
    assertIsOk(GET(url("Affiliations.personAffiliations", "personId", colleague.id)));
    assertStatus(403, GET(url("Affiliations.personAffiliations", "personId", other.id)));
    assertStatus(403, GET(url("Affiliations.blankByPerson", "personId", other.id)));
  }

  /**
   * Storico dei contratti: solo per le persone della propria sede.
   */
  @Test
  public void personnelAdminSeesContractHistoryOnlyInOwnOffice() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    String admin = roleOn(colleague, Role.PERSONNEL_ADMIN_MINI);
    commit();

    login(admin);
    assertIsOk(GET(contractHistoryUrl(colleague)));
    assertStatus(403, GET(contractHistoryUrl(other)));
  }

  /**
   * Elenco delle persone per la stampa dei cartellini: solo della propria sede.
   */
  @Test
  public void personnelAdminListsPeopleForPrintTagsOnlyInOwnOffice() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    String admin = roleOn(colleague, Role.PERSONNEL_ADMIN_MINI);
    commit();

    login(admin);
    assertIsOk(GET(printTagsUrl(colleague)));
    assertStatus(403, GET(printTagsUrl(other)));
  }

  /**
   * Figli dei dipendenti: solo per le persone della propria sede.
   */
  @Test
  public void registryManagerSeesChildrenOnlyInOwnOffice() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    String manager = roleOn(colleague, Role.REGISTRY_MANAGER);
    commit();

    login(manager);
    assertIsOk(GET(url("Persons.children", "personId", colleague.id)));
    assertStatus(403, GET(url("Persons.children", "personId", other.id)));
  }

  /**
   * Il dettaglio del tempo disponibile per straordinari è visibile solo per sé stessi.
   */
  @Test
  public void employeeSeesOnlyOwnResidualForOvertime() {
    Person employee = h2Examples.activeNormalEmployee(BEGIN_2026);
    addRole(employee, Role.EMPLOYEE);
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    commit();

    login(employee.getUser().getUsername());
    assertIsOk(GET(residualUrl(employee)));
    assertStatus(403, GET(residualUrl(colleague)));
  }

  /**
   * Il client REST di una sede legge i giorni solo delle persone della propria sede.
   */
  @Test
  public void restClientReadsDaysOnlyInOwnOffice() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    colleague.setEmail("colleague" + colleague.id + "@example.org");
    colleague.save();
    Person other = h2Examples.activeNormalEmployee(BEGIN_2026);
    other.setEmail("other" + other.id + "@example.org");
    other.save();
    String client = roleOn(colleague, Role.REST_CLIENT);
    commit();

    login(client);
    assertIsOk(GET(restDaysUrl(colleague)));
    assertStatus(403, GET(restDaysUrl(other)));
  }

  private String roleOn(Person person, String roleName) {
    return h2Examples.userWithOfficeRole(person.getOffice(), roleName).getUsername();
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

  private String contractHistoryUrl(Person person) {
    Map<String, Object> args = new HashMap<>();
    args.put("contractId", person.getContracts().get(0).id);
    args.put("type", "CONTRACT");
    return Router.reverse("Historicals.contractHistory", args).url;
  }

  private String printTagsUrl(Person person) {
    Map<String, Object> args = new HashMap<>();
    args.put("year", 2026);
    args.put("month", 9);
    args.put("officeId", person.getOffice().id);
    return Router.reverse("PrintTags.listPersonForPrintTags", args).url;
  }

  private String residualUrl(Person person) {
    Map<String, Object> args = new HashMap<>();
    args.put("year", 2026);
    args.put("month", 9);
    args.put("personId", person.id);
    return Router.reverse("PersonDays.checkPositiveResidualForOvertime", args).url;
  }

  private String restDaysUrl(Person person) {
    return "/rest/persons/" + person.getEmail() + "/days?start=2026-09-01&end=2026-09-30";
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
