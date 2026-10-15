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
import com.google.common.collect.ImmutableMap;
import common.injection.StaticInject;
import db.h2support.H2Examples;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.services.absences.AbsenceService;
import models.Person;
import models.PersonConfiguration;
import models.Role;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Test funzionali sulle pagine di visualizzazione (personShow) e modifica (personEdit e
 * personUpdate) del limite personale di giorni di lavoro agile.
 *
 * <p>Le richieste HTTP sono eseguite in un'altra transazione: i dati preparati dal test sono
 * resi visibili con il commit della transazione corrente.</p>
 */
@StaticInject
public class PersonConfigurationsTest extends FunctionalTest {

  private static final LocalDate BEGIN_2026 = new LocalDate(2026, 1, 1);
  private static final LocalDate END_JUNE_2026 = new LocalDate(2026, 6, 30);
  private static final LocalDate BEGIN_JULY_2026 = new LocalDate(2026, 7, 1);
  private static final LocalDate END_2026 = new LocalDate(2026, 12, 31);
  private static final LocalDate BEGIN_2027 = new LocalDate(2027, 1, 1);
  private static final String AGILE_WORK_LIMIT_LABEL =
      "Limite personale di giorni di lavoro agile al mese";

  @Inject
  private static H2Examples h2Examples;
  @Inject
  private static ConfigurationManager configurationManager;
  @Inject
  private static AbsenceService absenceService;

  /**
   * Il gestore dell'anagrafica della sede vede tutti i periodi del limite personale (passati,
   * attuale e futuri) con il link di modifica; il periodo valido oggi è evidenziato.
   */
  @Test
  public void personShowListsAllPeriodsForRegistryManager() {
    Person person = employeeWithThreePeriods();
    String username = userWithOfficeRole(person, Role.REGISTRY_MANAGER);
    commit();

    login(username);
    Response response = GET(personShowUrl(person));
    assertIsOk(response);
    String content = getContent(response);

    assertTrue(content.contains("01/01/2026"));
    assertTrue(content.contains("30/06/2026"));
    assertTrue(content.contains("01/07/2026"));
    assertTrue(content.contains("31/12/2026"));
    assertTrue(content.contains("01/01/2027"));
    assertTrue(content.contains("Indeterminato"));
    assertEquals(3, count(content, AGILE_WORK_LIMIT_LABEL));
    for (PersonConfiguration period : periods(person)) {
      assertTrue(content.contains(personEditUrl(period)));
    }
    // Solo il periodo valido oggi è evidenziato
    assertEquals(1, count(content, "<tr class=\"info\">"));
  }

  /**
   * L'amministratore tecnico della sede vede i periodi del limite personale ma non il link
   * di modifica.
   */
  @Test
  public void personShowWithoutEditLinkForTechnicalAdmin() {
    Person person = employeeWithThreePeriods();
    String username = userWithOfficeRole(person, Role.TECHNICAL_ADMIN);
    commit();

    login(username);
    Response response = GET(personShowUrl(person));
    assertIsOk(response);
    String content = getContent(response);

    assertEquals(3, count(content, AGILE_WORK_LIMIT_LABEL));
    for (PersonConfiguration period : periods(person)) {
      assertFalse(content.contains(personEditUrl(period)));
    }
  }

  /**
   * L'amministratore tecnico della sede non può aprire il form di modifica né salvare.
   */
  @Test
  public void personEditAndUpdateForbiddenForTechnicalAdmin() {
    Person person = employeeWithThreePeriods();
    String username = userWithOfficeRole(person, Role.TECHNICAL_ADMIN);
    PersonConfiguration current = periodBeginningAt(person, BEGIN_JULY_2026);
    commit();

    login(username);
    assertStatus(403, GET(personEditUrl(current)));
    assertStatus(403, POST(personUpdateUrl(), updateParams(current, "01/11/2026", "", "5",
        true)));

    assertEquals("8", reload(current).getFieldValue());
  }

  /**
   * Nel form di modifica è mostrato il periodo della riga selezionata. Per il periodo in
   * corso si propone come inizio il primo giorno del mese corrente, per un periodo futuro
   * si mantiene il suo inizio.
   */
  @Test
  public void personEditShowsSelectedPeriod() {
    Person person = employeeWithThreePeriods();
    String username = userWithOfficeRole(person, Role.REGISTRY_MANAGER);
    PersonConfiguration current = periodBeginningAt(person, BEGIN_JULY_2026);
    PersonConfiguration future = periodBeginningAt(person, BEGIN_2027);
    commit();

    login(username);
    Response response = GET(personEditUrl(current));
    assertIsOk(response);
    String content = getContent(response);
    assertTrue(content.contains("Periodo selezionato: dal <strong>01/07/2026</strong>"));
    assertTrue(content.contains("al <strong>31/12/2026</strong>"));
    assertTrue(content.contains(
        "value=\"" + LocalDate.now().dayOfMonth().withMinimumValue().toString("dd/MM/yyyy")
        + "\""));
    assertTrue(content.contains("name=\"configurationId\""));

    response = GET(personEditUrl(future));
    assertIsOk(response);
    content = getContent(response);
    assertTrue(content.contains("Periodo selezionato: dal <strong>01/01/2027</strong>"));
    assertTrue(content.contains("al <strong>indeterminato</strong>"));
    assertTrue(content.contains("value=\"01/01/2027\""));
  }

  /**
   * Il periodo deve coprire mesi interi e il limite non può essere negativo: il form è
   * ripresentato con gli errori e la configurazione non cambia.
   */
  @Test
  public void personUpdateValidatesPeriodAndValue() {
    Person person = employeeWithThreePeriods();
    String username = userWithOfficeRole(person, Role.REGISTRY_MANAGER);
    PersonConfiguration current = periodBeginningAt(person, BEGIN_JULY_2026);
    commit();

    login(username);
    Response response = POST(personUpdateUrl(),
        updateParams(current, "15/11/2026", "29/11/2026", "5", true));
    assertStatus(400, response);
    String content = getContent(response);
    assertTrue(content.contains("deve essere il primo giorno del mese."));
    assertTrue(content.contains("deve essere l&#039;ultimo giorno del mese.")
        || content.contains("deve essere l'ultimo giorno del mese."));

    response = POST(personUpdateUrl(),
        updateParams(current, "01/11/2026", "30/11/2026", "-1", true));
    assertStatus(400, response);
    assertTrue(getContent(response).contains("valore non valido."));

    assertEquals("8", reload(current).getFieldValue());
  }

  /**
   * Il gestore dell'anagrafica salva un nuovo limite per un mese: alla prima richiesta è
   * mostrato il riepilogo da confermare, alla conferma il periodo viene diviso.
   */
  @Test
  public void personUpdateSavesLimitAfterConfirmation() {
    Person person = employeeWithThreePeriods();
    String username = userWithOfficeRole(person, Role.REGISTRY_MANAGER);
    PersonConfiguration current = periodBeginningAt(person, BEGIN_JULY_2026);
    commit();

    login(username);
    Response response = POST(personUpdateUrl(),
        updateParams(current, "01/11/2026", "30/11/2026", "5", false));
    assertStatus(400, response);
    assertTrue(getContent(response).contains("Conferma"));
    // Senza conferma non cambia nulla
    assertEquals("8", reload(current).getFieldValue());

    response = POST(personUpdateUrl(),
        updateParams(current, "01/11/2026", "30/11/2026", "5", true));
    assertStatus(302, response);

    JPA.em().clear();
    Person reloaded = Person.findById(person.id);
    assertEquals(8, configurationManager.configValue(reloaded,
        EpasParam.AGILE_WORK_MONTHLY_LIMIT, new LocalDate(2026, 10, 1)));
    assertEquals(5, configurationManager.configValue(reloaded,
        EpasParam.AGILE_WORK_MONTHLY_LIMIT, new LocalDate(2026, 11, 1)));
    assertEquals(8, configurationManager.configValue(reloaded,
        EpasParam.AGILE_WORK_MONTHLY_LIMIT, new LocalDate(2026, 12, 1)));
    assertEquals(4, configurationManager.configValue(reloaded,
        EpasParam.AGILE_WORK_MONTHLY_LIMIT, BEGIN_2027));
  }

  /**
   * Dipendente attivo dal primo gennaio 2026 con tre periodi del limite personale:
   * 6 giorni nel primo semestre 2026, 8 nel secondo, 4 dal 2027.
   */
  private Person employeeWithThreePeriods() {
    absenceService.enumInitializator();
    Person person = h2Examples.activeNormalEmployee(BEGIN_2026);
    setLimit(person, 6, BEGIN_2026, Optional.of(END_JUNE_2026));
    setLimit(person, 8, BEGIN_JULY_2026, Optional.of(END_2026));
    setLimit(person, 4, BEGIN_2027, Optional.absent());
    return person;
  }

  private void setLimit(Person person, Integer limit, LocalDate begin, Optional<LocalDate> end) {
    configurationManager.updateInteger(EpasParam.AGILE_WORK_MONTHLY_LIMIT, person, limit,
        Optional.of(begin), end, true);
  }

  private String userWithOfficeRole(Person person, String roleName) {
    return h2Examples.userWithOfficeRole(person.getOffice(), roleName).getUsername();
  }

  private Iterable<PersonConfiguration> periods(Person person) {
    return PersonConfiguration.find("person = ?1 and epasParam = ?2 order by beginDate",
        person, EpasParam.AGILE_WORK_MONTHLY_LIMIT).fetch();
  }

  private PersonConfiguration periodBeginningAt(Person person, LocalDate begin) {
    return PersonConfiguration.find("person = ?1 and epasParam = ?2 and beginDate = ?3",
        person, EpasParam.AGILE_WORK_MONTHLY_LIMIT, begin).first();
  }

  private PersonConfiguration reload(PersonConfiguration configuration) {
    JPA.em().clear();
    return PersonConfiguration.findById(configuration.id);
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

  private String personShowUrl(Person person) {
    Map<String, Object> args = new HashMap<>();
    args.put("personId", person.id);
    return Router.reverse("Configurations.personShow", args).url;
  }

  private String personEditUrl(PersonConfiguration configuration) {
    Map<String, Object> args = new HashMap<>();
    args.put("configurationId", configuration.id);
    return Router.reverse("Configurations.personEdit", args).url;
  }

  private String personUpdateUrl() {
    return Router.reverse("Configurations.personUpdate").url;
  }

  private Map<String, String> updateParams(PersonConfiguration configuration,
      String begin, String end, String value, boolean confirmed) {
    return ImmutableMap.<String, String>builder()
        .put("configurationId", configuration.id.toString())
        .put("configurationDto.validityBegin", begin)
        .put("configurationDto.validityEnd", end)
        .put("configurationDto.integerNewValue", value)
        .put("confirmed", Boolean.toString(confirmed))
        .build();
  }

  private static int count(String content, String text) {
    Matcher matcher = Pattern.compile(Pattern.quote(text)).matcher(content);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }
}
