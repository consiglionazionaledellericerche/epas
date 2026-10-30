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
import common.security.LocalUrls;
import common.security.SensitiveParams;
import controllers.shib.MockShibboleth;
import db.h2support.H2Examples;
import java.util.UUID;
import javax.inject.Inject;
import jobs.PasswordRecoveryJob;
import models.Person;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.db.jpa.JPA;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

/**
 * Verifica che credenziali e informazioni sugli utenti non escano da login, recupero password,
 * log e metriche.
 */
@StaticInject
public class LoginSensitiveDataTest extends FunctionalTest {

  private static final String METRICS_URL = "/metrics/prometheus";
  private static final String TOKEN = "un-token-di-test";
  private static final String SHIB_EMAIL = "cristian.lucchesi@iit.cnr.it";

  @Inject
  private static H2Examples h2Examples;

  private String previousToken;
  private String previousShibLogin;

  /**
   * Salva la configurazione modificata dai test.
   */
  @Before
  public void setUp() {
    previousToken = Play.configuration.getProperty(Metrics.METRICS_TOKEN);
    previousShibLogin = Play.configuration.getProperty("shib.login");
    clearCookies();
  }

  /**
   * Ripristina la configurazione e annulla i dati creati, per non influenzare gli altri test.
   */
  @After
  public void tearDown() {
    restore(Metrics.METRICS_TOKEN, previousToken);
    restore("shib.login", previousShibLogin);
    MockShibboleth.reload();
    if (JPA.em().getTransaction().isActive()) {
      JPA.em().getTransaction().setRollbackOnly();
    }
  }

  @Test
  public void failedLoginDoesNotPutThePasswordInTheFlash() {
    final String password = "passwordInChiaro" + UUID.randomUUID();
    Response response = POST("/login",
        ImmutableMap.of("username", "utente.inesistente", "password", password));
    assertStatus(302, response);
    String flash = response.cookies.get("PLAY_FLASH").value;
    assertTrue(flash.contains("utente.inesistente"));
    // il messaggio di errore può contenere la parola password, il parametro no
    assertFalse(flash.contains("password="));
    assertFalse(flash.contains(password));
  }

  @Test
  public void lostPasswordAnswersWithAGenericMessage() {
    Response response = POST("/lostpassword/sendtokenrecoverypassword",
        ImmutableMap.of("email", "sconosciuto." + UUID.randomUUID() + "@example.com"));
    assertStatus(302, response);
    assertTrue(response.cookies.get("PLAY_FLASH").value.contains("success"));
  }

  @Test
  public void recoveryJobGeneratesTheTokenOnlyForKnownEmail() {
    Person person = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    person.setEmail("lost.password." + UUID.randomUUID() + "@example.com");
    person.save();

    new PasswordRecoveryJob("sconosciuto." + UUID.randomUUID() + "@example.com").doJob();
    assertNull(person.getUser().getRecoveryToken());

    new PasswordRecoveryJob(person.getEmail()).doJob();
    assertNotNull(person.getUser().getRecoveryToken());
  }

  @Test
  public void shibbolethLoginIgnoresExternalReturn() {
    assertFalse(shibbolethLogin("https://evil.example/").contains("evil.example"));
    assertTrue(shibbolethLogin("/stampings/stampings").endsWith("/stampings/stampings"));
  }

  @Test
  public void metricsAreForbiddenWithoutConfiguredToken() {
    Play.configuration.remove(Metrics.METRICS_TOKEN);
    assertStatus(403, GET(METRICS_URL));
    assertStatus(403, GET(metricsRequest("Bearer "), METRICS_URL));
  }

  @Test
  public void metricsRequireTheConfiguredToken() {
    Play.configuration.setProperty(Metrics.METRICS_TOKEN, TOKEN);
    assertStatus(403, GET(METRICS_URL));
    assertStatus(403, GET(metricsRequest("Bearer sbagliato"), METRICS_URL));
    assertStatus(403, GET(metricsRequest("Basic " + TOKEN), METRICS_URL));
    assertIsOk(GET(metricsRequest("Bearer " + TOKEN), METRICS_URL));
    assertIsOk(GET(metricsRequest("bearer   " + TOKEN), METRICS_URL));
  }

  @Test
  public void onlyLocalPathsAreAcceptedForRedirect() {
    assertTrue(LocalUrls.isLocal("/"));
    assertTrue(LocalUrls.isLocal("/stampings/stampings?year=2026"));
    assertFalse(LocalUrls.isLocal(null));
    assertFalse(LocalUrls.isLocal(""));
    assertFalse(LocalUrls.isLocal("https://example.com/"));
    assertFalse(LocalUrls.isLocal("//example.com/"));
    assertFalse(LocalUrls.isLocal("/\\example.com/"));
    assertFalse(LocalUrls.isLocal("/a\r\nLocation: https://example.com/"));
  }

  @Test
  public void credentialsAreMaskedInLogs() {
    for (String name : new String[] {"password", "nuovaPassword", "user.password", "passwd", "pwd",
        "token", "apiKey", "api_key", "credentials", "code", "state", "pin"}) {
      assertEquals(name, "***", SensitiveParams.loggable(name, "segreto"));
    }
    for (String name : new String[] {"username", "stampingId", "mapping", "absenceCode"}) {
      assertEquals(name, "valore", SensitiveParams.loggable(name, "valore"));
    }
  }

  private String shibbolethLogin(String returnUrl) {
    clearCookies();
    MockShibboleth.removeAll();
    MockShibboleth.set("eppn", SHIB_EMAIL);
    Play.configuration.setProperty("shib.login", "true");
    Response response = GET("/shibboleth/authenticate?return=" + returnUrl);
    assertStatus(302, response);
    return response.getHeader("Location");
  }

  private Request metricsRequest(String authorization) {
    Request request = newRequest();
    request.headers.put("authorization", new Http.Header("authorization", authorization));
    return request;
  }

  private void restore(String key, String value) {
    if (value == null) {
      Play.configuration.remove(key);
    } else {
      Play.configuration.setProperty(key, value);
    }
  }
}
