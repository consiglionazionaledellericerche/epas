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
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.Person;
import models.PersonMonthRecap;
import models.Role;
import models.UsersRolesOffices;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Test funzionali sui controlli dei permessi delle ore di formazione e delle timbrature in
 * telelavoro: un dipendente opera solo sui propri dati, un amministratore solo sulle persone
 * della propria sede.
 */
@StaticInject
public class TrainingHoursAndTeleworkPermissionsTest extends FunctionalTest {

  private static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);
  private static final int YEAR = 2026;
  private static final int MONTH = 9;

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static ConfigurationManager configurationManager;

  /**
   * Un dipendente non può vedere, modificare o cancellare le ore di formazione di un collega.
   */
  @Test
  public void employeeCannotTouchTrainingHoursOfAColleague() {
    Person attacker = employee();
    Person victim = employee();
    PersonMonthRecap pm = trainingHours(victim, 5);
    commit();

    login(attacker);
    assertStatus(403, GET(url("PersonMonths.modifyTrainingHours",
        "personMonthSituationId", pm.id)));
    assertStatus(403, GET(url("PersonMonths.deleteTrainingHours", "personMonthRecapId", pm.id)));
    assertStatus(403, POST(url("PersonMonths.saveTrainingHours"), saveParams(pm, 2)));
    assertStatus(403, GET(url("PersonMonths.deleteTrainingHoursConfirmed",
        "personMonthRecapId", pm.id)));

    PersonMonthRecap reloaded = reload(pm);
    assertNotNull(reloaded);
    assertEquals(Integer.valueOf(5), reloaded.getTrainingHours());
  }

  /**
   * Un dipendente può cancellare le proprie ore di formazione.
   */
  @Test
  public void employeeCanDeleteOwnTrainingHours() {
    Person employee = employee();
    PersonMonthRecap pm = trainingHours(employee, 5);
    commit();

    login(employee);
    assertStatus(302, GET(url("PersonMonths.deleteTrainingHoursConfirmed",
        "personMonthRecapId", pm.id)));
    assertNull(reload(pm));
  }

  /**
   * Un amministratore del personale non può cancellare le ore di formazione di persone di
   * altre sedi indicando la propria sede.
   */
  @Test
  public void personnelAdminCannotDeleteTrainingHoursOfAnotherOffice() {
    Person colleague = h2Examples.activeNormalEmployee(BEGIN_2026);
    String admin = h2Examples.userWithOfficeRole(colleague.getOffice(), Role.PERSONNEL_ADMIN)
        .getUsername();
    Person victim = employee();
    PersonMonthRecap pm = trainingHours(victim, 5);
    commit();

    login(admin);
    Map<String, Object> args = new HashMap<>();
    args.put("personMonthRecapId", pm.id);
    args.put("officeId", colleague.getOffice().id);
    assertStatus(403, GET(Router.reverse("PersonMonths.deletePeopleTrainingHoursConfirmed",
        args).url));
    assertNotNull(reload(pm));
  }

  /**
   * Un dipendente abilitato alle timbrature in telelavoro non può inserirle per un'altra
   * persona.
   */
  @Test
  public void employeeCannotSaveTeleworkStampingForAnotherPerson() {
    Person attacker = employee();
    configurationManager.updateBoolean(EpasParam.TELEWORK_STAMPINGS, attacker, true,
        Optional.of(BEGIN_2026), Optional.absent(), true);
    Person victim = employee();
    commit();

    login(attacker);
    Map<String, String> params = new HashMap<>();
    params.put("personId", victim.id.toString());
    params.put("date", LocalDate.now().minusDays(1).toString("dd/MM/yyyy"));
    params.put("time", "09:00");
    assertStatus(403, POST(url("TeleworkStampings.save"), params));
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

  private PersonMonthRecap trainingHours(Person person, int value) {
    PersonMonthRecap pm = new PersonMonthRecap(person, YEAR, MONTH);
    pm.setHoursApproved(false);
    pm.setTrainingHours(value);
    pm.setFromDate(new LocalDate(YEAR, MONTH, 1));
    pm.setToDate(new LocalDate(YEAR, MONTH, 2));
    pm.save();
    return pm;
  }

  private Map<String, String> saveParams(PersonMonthRecap pm, int value) {
    Map<String, String> params = new HashMap<>();
    params.put("personMonthSituationId", pm.id.toString());
    params.put("begin", "1");
    params.put("end", "2");
    params.put("value", String.valueOf(value));
    params.put("month", String.valueOf(MONTH));
    params.put("year", String.valueOf(YEAR));
    return params;
  }

  private String url(String action) {
    return Router.reverse(action).url;
  }

  private String url(String action, String param, Object value) {
    Map<String, Object> args = new HashMap<>();
    args.put(param, value);
    return Router.reverse(action, args).url;
  }

  private PersonMonthRecap reload(PersonMonthRecap pm) {
    JPA.em().clear();
    return PersonMonthRecap.findById(pm.id);
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  private void login(Person person) {
    POST("/login?username=" + person.getUser().getUsername() + "&password="
        + H2Examples.DEFAULT_PASSWORD);
  }

  private void login(String username) {
    POST("/login?username=" + username + "&password=" + H2Examples.DEFAULT_PASSWORD);
  }
}
