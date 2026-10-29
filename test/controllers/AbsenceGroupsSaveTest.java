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
import dao.absences.AbsenceComponentDao;
import db.h2support.H2Examples;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.services.absences.AbsenceService;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultGroup;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Test funzionali sui controlli dei permessi di AbsenceGroups.save: si possono inserire
 * assenze solo per persone su cui si hanno i diritti e solo con i gruppi proposti dalla
 * form di inserimento.
 */
@StaticInject
public class AbsenceGroupsSaveTest extends FunctionalTest {

  private static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static AbsenceService absenceService;
  @Inject
  private static AbsenceComponentDao absenceComponentDao;
  @Inject
  private static ConfigurationManager configurationManager;

  /**
   * Un dipendente abilitato al telelavoro non può inserire assenze per un'altra persona.
   */
  @Test
  public void employeeCannotSaveAbsenceForAnotherPerson() {
    absenceService.enumInitializator();
    Person attacker = teleworkEmployee();
    Person victim = h2Examples.activeNormalEmployee(BEGIN_2026);
    commit();

    login(attacker);
    assertStatus(403, POST(saveUrl(), saveParams(victim, DefaultGroup.TELELAVORO,
        "103", JustifiedTypeName.complete_day_and_add_overtime)));
    assertEquals(0, absences(victim));
  }

  /**
   * Un dipendente non può inserirsi codici di gruppi non proposti dalla form di inserimento
   * (es. ferie, che passano dal flusso di approvazione).
   */
  @Test
  public void employeeCannotSaveAbsenceOfNotPermittedGroup() {
    absenceService.enumInitializator();
    Person employee = teleworkEmployee();
    commit();

    login(employee);
    assertStatus(403, POST(saveUrl(), saveParams(employee, DefaultGroup.FERIE_CNR,
        "32", JustifiedTypeName.all_day)));
    assertEquals(0, absences(employee));
  }

  /**
   * Un amministratore del personale non può inserire assenze per persone di altre sedi.
   */
  @Test
  public void personnelAdminCannotSaveAbsenceInAnotherOffice() {
    absenceService.enumInitializator();
    Person otherOfficeAdmin = h2Examples.activeNormalEmployee(BEGIN_2026);
    String username = h2Examples
        .userWithOfficeRole(otherOfficeAdmin.getOffice(), Role.PERSONNEL_ADMIN).getUsername();
    Person victim = h2Examples.activeNormalEmployee(BEGIN_2026);
    commit();

    login(username);
    assertStatus(403, POST(saveUrl(), saveParams(victim,
        DefaultGroup.MISSIONE_GIORNALIERA, "92", JustifiedTypeName.complete_day_and_add_overtime)));
    assertEquals(0, absences(victim));
  }

  /**
   * Un dipendente abilitato al telelavoro può inserirsi il codice di telelavoro.
   */
  @Test
  public void employeeCanSaveTeleworkForHimself() {
    absenceService.enumInitializator();
    Person employee = teleworkEmployee();
    commit();

    login(employee);
    Response response = POST(saveUrl(), saveParams(employee, DefaultGroup.TELELAVORO,
        "103", JustifiedTypeName.complete_day_and_add_overtime));
    assertStatus(302, response);
    assertEquals(1, absences(employee));
  }

  private Person teleworkEmployee() {
    Person person = h2Examples.activeNormalEmployee(BEGIN_2026);
    configurationManager.updateBoolean(EpasParam.TELEWORK, person, true,
        Optional.of(BEGIN_2026), Optional.absent(), true);
    addRole(person, Role.EMPLOYEE);
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

  private Map<String, String> saveParams(Person person, DefaultGroup group, String code,
      JustifiedTypeName justifiedTypeName) {
    String date = lastWorkingDay().toString("dd/MM/yyyy");
    Map<String, String> params = new HashMap<>();
    params.put("personId", person.id.toString());
    params.put("from", date);
    params.put("to", date);
    params.put("groupAbsenceType.id",
        absenceComponentDao.groupAbsenceTypeByName(group.name()).get().id.toString());
    params.put("absenceType.id", absenceComponentDao.absenceTypeByCode(code).get().id.toString());
    params.put("justifiedType.id",
        absenceComponentDao.getOrBuildJustifiedType(justifiedTypeName).id.toString());
    return params;
  }

  private String saveUrl() {
    return Router.reverse("AbsenceGroups.save").url;
  }

  private LocalDate lastWorkingDay() {
    LocalDate date = LocalDate.now().minusDays(1);
    while (date.getDayOfWeek() > DateTimeConstants.FRIDAY) {
      date = date.minusDays(1);
    }
    return date;
  }

  private long absences(Person person) {
    JPA.em().clear();
    return Absence.count("personDay.person.id = ?1", person.id);
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  private void login(Person person) {
    login(person.getUser().getUsername());
  }

  private void login(String username) {
    POST("/login?username=" + username + "&password=" + H2Examples.DEFAULT_PASSWORD);
  }
}
