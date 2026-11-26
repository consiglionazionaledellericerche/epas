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

import com.google.common.collect.ImmutableMap;
import common.injection.StaticInject;
import db.h2support.H2Examples;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.services.absences.AbsenceService;
import models.Configuration;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.enumerate.AccountRole;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Test funzionali sulla modifica dei parametri periodici della sede (#731): la validità
 * proposta parte dal mese corrente, per chi non è amministratore di sistema non può iniziare
 * prima del primo gennaio dell'anno precedente e la pagina di conferma mostra il costo del
 * ricalcolo.
 *
 * <p>Le richieste HTTP sono eseguite in un'altra transazione: i dati preparati dal test sono
 * resi visibili con il commit della transazione corrente.</p>
 */
@StaticInject
public class OfficeConfigurationsTest extends FunctionalTest {

  private static final LocalDate OFFICE_BEGIN = new LocalDate(2020, 1, 1);
  private static final String DEFAULT_LUNCH_INTERVAL = "12:00-15:00";
  private static final String NEW_LUNCH_INTERVAL = "12:30-15:00";

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static ConfigurationManager configurationManager;
  @Inject
  private static AbsenceService absenceService;

  /**
   * Per il periodo in corso, iniziato con la sede, si propone come inizio il primo giorno
   * del mese corrente e non l'inizio del periodo.
   */
  @Test
  public void editProposesCurrentMonthBegin() {
    Office office = officeWithEmployee();
    String username = personnelAdmin(office);
    Configuration lunchInterval = lunchInterval(office);
    commit();

    login(username);
    Response response = GET(editUrl(lunchInterval));
    assertIsOk(response);
    String content = getContent(response);
    assertTrue(content.contains("value=\"" + format(currentMonthBegin()) + "\""));
    assertFalse(content.contains("value=\"" + format(OFFICE_BEGIN) + "\""));
    assertTrue(content.matches("(?s).*non può essere precedente al\\s*<strong>"
        + format(validityBeginLimit()) + "</strong>.*"));
  }

  /**
   * L'amministratore del personale non può applicare il nuovo valore da prima del primo
   * gennaio dell'anno precedente: il form è ripresentato con l'errore. Da quella data in poi
   * la modifica arriva alla conferma.
   */
  @Test
  public void updateBeforeLimitRejectedForPersonnelAdmin() {
    Office office = officeWithEmployee();
    String username = personnelAdmin(office);
    Configuration lunchInterval = lunchInterval(office);
    commit();

    login(username);
    Response response = POST(updateUrl(), updateParams(lunchInterval, OFFICE_BEGIN, false));
    assertStatus(400, response);
    String content = getContent(response);
    assertTrue(content.contains(
        "non può essere precedente al " + format(validityBeginLimit()) + "."));
    assertFalse(content.contains("Persone coinvolte"));

    response = POST(updateUrl(), updateParams(lunchInterval, validityBeginLimit(), false));
    assertStatus(400, response);
    content = getContent(response);
    assertFalse(content.contains("non può essere precedente al"));
    assertTrue(content.contains("dal <strong>" + format(validityBeginLimit()) + "</strong>"));
    assertTrue(content.contains("Persone coinvolte: <strong>1</strong>"));

    JPA.em().clear();
    assertEquals(DEFAULT_LUNCH_INTERVAL,
        ((Configuration) Configuration.findById(lunchInterval.id)).getFieldValue());
  }

  /**
   * L'amministratore di sistema può applicare il nuovo valore dall'inizio del periodo: la
   * conferma mostra persone e giornate da ricalcolare, l'avviso per il ricalcolo di più di un
   * anno e l'alternativa di applicarlo dal mese corrente. Senza conferma non cambia nulla.
   */
  @Test
  public void updateFromPeriodBeginShowsCostAndSuggestion() {
    Office office = officeWithEmployee();
    String username = systemAdmin(office);
    Configuration lunchInterval = lunchInterval(office);
    commit();

    login(username);
    Response response = POST(updateUrl(), updateParams(lunchInterval, OFFICE_BEGIN, false));
    assertStatus(400, response);
    String content = getContent(response);
    assertTrue(content.contains("dal <strong>" + format(OFFICE_BEGIN) + "</strong>"));
    assertTrue(content.contains("Persone coinvolte: <strong>1</strong>"));
    assertTrue(content.contains("Attenzione:</strong> il ricalcolo riguarda più di un anno"));
    assertTrue(content.contains("Applica dal " + format(currentMonthBegin())));
    assertTrue(content.contains("name=\"configurationDto.validityBegin\" value=\""
        + format(currentMonthBegin()) + "\""));

    JPA.em().clear();
    assertEquals(DEFAULT_LUNCH_INTERVAL,
        ((Configuration) Configuration.findById(lunchInterval.id)).getFieldValue());
  }

  /**
   * Applicando il nuovo valore dal mese corrente non ci sono avvisi né alternative; alla
   * conferma il valore cambia solo dal mese corrente.
   */
  @Test
  public void updateFromCurrentMonthKeepsPastValue() {
    Office office = officeWithEmployee();
    String username = personnelAdmin(office);
    Configuration lunchInterval = lunchInterval(office);
    commit();

    login(username);
    Response response = POST(updateUrl(),
        updateParams(lunchInterval, currentMonthBegin(), false));
    assertStatus(400, response);
    String content = getContent(response);
    assertTrue(content.contains("Persone coinvolte: <strong>1</strong>"));
    assertFalse(content.contains("Attenzione:</strong>"));
    assertFalse(content.contains("Applica dal"));

    response = POST(updateUrl(), updateParams(lunchInterval, currentMonthBegin(), true));
    assertStatus(302, response);

    JPA.em().clear();
    Office reloaded = Office.findById(office.id);
    assertEquals(DEFAULT_LUNCH_INTERVAL, EpasParam.EpasParamValueType.formatValue(
        configurationManager.configValue(reloaded, EpasParam.LUNCH_INTERVAL,
            currentMonthBegin().minusDays(1))));
    assertEquals(NEW_LUNCH_INTERVAL, EpasParam.EpasParamValueType.formatValue(
        configurationManager.configValue(reloaded, EpasParam.LUNCH_INTERVAL,
            currentMonthBegin())));
  }

  /**
   * Inizio della validità proposto per periodi in corso, futuri e conclusi.
   */
  @Test
  public void proposedValidityBegin() {
    LocalDate today = new LocalDate(2026, 11, 26);
    LocalDate monthBegin = new LocalDate(2026, 11, 1);
    // Periodo in corso iniziato prima del mese corrente
    assertEquals(monthBegin, ConfigurationManager.proposedValidityBegin(
        OFFICE_BEGIN, null, today));
    assertEquals(monthBegin, ConfigurationManager.proposedValidityBegin(
        OFFICE_BEGIN, new LocalDate(2026, 12, 31), today));
    // Periodo iniziato nel mese corrente o futuro
    assertEquals(new LocalDate(2026, 11, 10), ConfigurationManager.proposedValidityBegin(
        new LocalDate(2026, 11, 10), null, today));
    assertEquals(new LocalDate(2027, 1, 1), ConfigurationManager.proposedValidityBegin(
        new LocalDate(2027, 1, 1), null, today));
    // Periodo concluso
    assertEquals(OFFICE_BEGIN, ConfigurationManager.proposedValidityBegin(
        OFFICE_BEGIN, new LocalDate(2025, 12, 31), today));
    // Limite all'inizio della validità: primo gennaio dell'anno precedente
    assertEquals(new LocalDate(2025, 1, 1), ConfigurationManager.minValidityBegin(today));
    assertEquals(new LocalDate(2025, 1, 1),
        ConfigurationManager.minValidityBegin(new LocalDate(2026, 1, 1)));
  }

  /**
   * Sede attiva dal 2020 con un dipendente.
   */
  private Office officeWithEmployee() {
    absenceService.enumInitializator();
    Person person = h2Examples.activeNormalEmployee(OFFICE_BEGIN);
    return person.getOffice();
  }

  /**
   * Amministratore del personale della sede che è anche amministratore di sistema.
   */
  private String systemAdmin(Office office) {
    User user = h2Examples.userWithOfficeRole(office, Role.PERSONNEL_ADMIN);
    user.getRoles().add(AccountRole.ADMIN);
    user.save();
    return user.getUsername();
  }

  private String personnelAdmin(Office office) {
    return h2Examples.userWithOfficeRole(office, Role.PERSONNEL_ADMIN).getUsername();
  }

  private Configuration lunchInterval(Office office) {
    return Configuration.find("office = ?1 and epasParam = ?2", office,
        EpasParam.LUNCH_INTERVAL).first();
  }

  private static LocalDate validityBeginLimit() {
    return new LocalDate(LocalDate.now().getYear() - 1, 1, 1);
  }

  private static LocalDate currentMonthBegin() {
    return LocalDate.now().dayOfMonth().withMinimumValue();
  }

  private static String format(LocalDate date) {
    return date.toString("dd/MM/yyyy");
  }

  /**
   * Rende visibili i dati preparati alle richieste HTTP, eseguite in un'altra transazione.
   */
  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  private void login(String username) {
    POST("/login?username=" + username + "&password=" + H2Examples.DEFAULT_PASSWORD);
  }

  private String editUrl(Configuration configuration) {
    Map<String, Object> args = new HashMap<>();
    args.put("configurationId", configuration.id);
    return Router.reverse("Configurations.edit", args).url;
  }

  private String updateUrl() {
    return Router.reverse("Configurations.update").url;
  }

  private Map<String, String> updateParams(Configuration configuration, LocalDate begin,
      boolean confirmed) {
    return ImmutableMap.<String, String>builder()
        .put("configuration.id", configuration.id.toString())
        .put("configurationDto.validityBegin", format(begin))
        .put("configurationDto.validityEnd", "")
        .put("configurationDto.stringNewValue", NEW_LUNCH_INTERVAL)
        .put("confirmed", Boolean.toString(confirmed))
        .build();
  }
}
