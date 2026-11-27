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
import java.util.regex.Pattern;
import javax.inject.Inject;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.services.absences.AbsenceService;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import models.absences.GroupAbsenceType;
import models.absences.definitions.DefaultGroup;
import models.absences.definitions.DefaultTab;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

/**
 * Test funzionali sulla form di inserimento assenze per i dipendenti abilitati a inserirsi
 * da soli i congedi parentali: la form propone solo i gruppi che possono inserire e tutti
 * quelli proposti si possono selezionare.
 */
@StaticInject
public class AbsenceGroupsParentalLeaveTest extends FunctionalTest {

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
   * Un dipendente abilitato ai congedi parentali può selezionare i gruppi dei codici
   * provvisori all'80% (24PROV, 242PROV...), del quinto figlio e del genitore unico, che
   * prima gli erano proposti ma ne causavano l'errore.
   */
  @Test
  public void parentalLeaveEmployeeCanSelectAllProposedChildGroups() {
    absenceService.enumInitializator();
    Person employee = employeeWith(EpasParam.PARENTAL_LEAVE_AND_CHILD_ILLNESS);
    commit();

    login(employee);
    for (DefaultGroup group : new DefaultGroup[] {DefaultGroup.G_24PROV,
        DefaultGroup.G_242PROV, DefaultGroup.G_243PROV, DefaultGroup.G_244PROV,
        DefaultGroup.G_245PROV, DefaultGroup.G_235, DefaultGroup.G_25S,
        DefaultGroup.G_252S, DefaultGroup.MALATTIA_FIGLIO_5}) {
      assertStatus(200, GET(insertUrl(employee) + "&groupAbsenceType.id=" + group(group).id));
    }
  }

  /**
   * La form non propone a un dipendente abilitato ai congedi parentali i gruppi che non può
   * inserire, come i congedi prenatali, il congedo matrimoniale e la paternità.
   */
  @Test
  public void parentalLeaveEmployeeDoesNotSeeNotPermittedGroups() {
    absenceService.enumInitializator();
    Person employee = employeeWith(EpasParam.PARENTAL_LEAVE_AND_CHILD_ILLNESS);
    commit();

    login(employee);
    Response response = GET(insertUrl(employee) + "&groupAbsenceType.id="
        + group(DefaultGroup.G_23).id);
    assertStatus(200, response);
    String content = groupSelect(getContent(response));
    assertTrue(content.contains(option(DefaultGroup.G_24PROV)));
    assertTrue(content.contains(option(DefaultGroup.G_242PROV)));
    assertFalse(content.contains(option(DefaultGroup.G_CONGEDI_PRENATALI)));
    assertFalse(content.contains(option(DefaultGroup.G_CONGEDO_MATRIMONIO)));
    assertFalse(content.contains(option(DefaultGroup.G_21P)));
  }

  /**
   * Un dipendente abilitato solo al congedo di paternità apre la tab dei congedi parentali
   * sul primo gruppo che può inserire e può selezionare la paternità del secondo e terzo
   * figlio.
   */
  @Test
  public void fatherCanOpenParentalLeaveTabAndSelectPaternityGroups() {
    absenceService.enumInitializator();
    Person employee = employeeWith(EpasParam.PARENTAL_LEAVE_FOR_FATHERS);
    commit();

    login(employee);
    GroupAbsenceType paternity = group(DefaultGroup.G_21P);
    assertEquals(DefaultTab.CONGEDI_PARENTALI.name(), paternity.getCategory().getTab().getName());
    Response response = GET(insertUrl(employee) + "&categoryTab.id="
        + paternity.getCategory().getTab().id);
    assertStatus(200, response);
    // la tab si apre sulla paternità, non sul primo gruppo della tab (23)
    String groupSelect = groupSelect(getContent(response));
    assertTrue(groupSelect, isSelected(groupSelect, DefaultGroup.G_21P));
    assertFalse(groupSelect.contains(option(DefaultGroup.G_23)));
    assertStatus(200, GET(insertUrl(employee) + "&groupAbsenceType.id="
        + group(DefaultGroup.G_212P).id));
    assertStatus(200, GET(insertUrl(employee) + "&groupAbsenceType.id="
        + group(DefaultGroup.G_213P).id));
  }

  private Person employeeWith(EpasParam param) {
    Person person = h2Examples.activeNormalEmployee(BEGIN_2026);
    configurationManager.updateBoolean(param, person, true,
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

  private GroupAbsenceType group(DefaultGroup group) {
    return absenceComponentDao.groupAbsenceTypeByName(group.name()).get();
  }

  /**
   * Solo le opzioni della select dei gruppi: gli id dei tipi assenza possono coincidere.
   */
  private String groupSelect(String content) {
    content = content.substring(content.indexOf("name=\"groupAbsenceType.id\""));
    return content.substring(0, content.indexOf("</select>"));
  }

  private boolean isSelected(String groupSelect, DefaultGroup group) {
    return Pattern.compile(Pattern.quote(option(group)) + "\\s*selected")
        .matcher(groupSelect).find();
  }

  private String option(DefaultGroup group) {
    return "<option value=\"" + group(group).id + "\"";
  }

  private String insertUrl(Person person) {
    return "/absencegroups/insert?personId=" + person.id + "&from="
        + lastWorkingDay().toString("dd/MM/yyyy");
  }

  private LocalDate lastWorkingDay() {
    LocalDate date = LocalDate.now().minusDays(1);
    while (date.getDayOfWeek() > DateTimeConstants.FRIDAY) {
      date = date.minusDays(1);
    }
    return date;
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
