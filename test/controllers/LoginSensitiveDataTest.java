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
import db.h2support.H2Examples;
import java.util.UUID;
import javax.inject.Inject;
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

  @Inject
  private static H2Examples h2Examples;

  private String previousToken;

  @Before
  public void setUp() {
    previousToken = Play.configuration.getProperty(Metrics.METRICS_TOKEN);
    clearCookies();
  }

  @After
  public void restoreToken() {
    if (previousToken == null) {
      Play.configuration.remove(Metrics.METRICS_TOKEN);
    } else {
      Play.configuration.setProperty(Metrics.METRICS_TOKEN, previousToken);
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
  public void lostPasswordAnswersTheSameWayForKnownAndUnknownEmail() {
    Person person = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    person.setEmail("lost.password." + UUID.randomUUID() + "@example.com");
    person.save();
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();

    Response known = lostPassword(person.getEmail());
    clearCookies();
    Response unknown = lostPassword("sconosciuto." + UUID.randomUUID() + "@example.com");

    assertStatus(302, known);
    assertStatus(302, unknown);
    assertEquals(known.getHeader("Location"), unknown.getHeader("Location"));
    assertEquals(known.cookies.get("PLAY_FLASH").value, unknown.cookies.get("PLAY_FLASH").value);
  }

  @Test
  public void metricsAreForbiddenWithoutConfiguredToken() {
    Play.configuration.remove(Metrics.METRICS_TOKEN);
    assertStatus(403, GET(METRICS_URL));
    assertStatus(403, GET(metricsRequest(""), METRICS_URL));
  }

  @Test
  public void metricsRequireTheConfiguredToken() {
    Play.configuration.setProperty(Metrics.METRICS_TOKEN, TOKEN);
    assertStatus(403, GET(METRICS_URL));
    assertStatus(403, GET(metricsRequest("sbagliato"), METRICS_URL));
    assertIsOk(GET(metricsRequest(TOKEN), METRICS_URL));
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
    assertEquals("***", SensitiveParams.loggable("password", "segreta"));
    assertEquals("***", SensitiveParams.loggable("nuovaPassword", "segreta"));
    assertEquals("***", SensitiveParams.loggable("user.password", "segreta"));
    assertEquals("***", SensitiveParams.loggable("token", "segreto"));
    assertEquals("mario.rossi", SensitiveParams.loggable("username", "mario.rossi"));
  }

  private Response lostPassword(String email) {
    return POST("/lostpassword/sendtokenrecoverypassword", ImmutableMap.of("email", email));
  }

  private Request metricsRequest(String token) {
    Request request = newRequest();
    request.url = METRICS_URL;
    request.path = METRICS_URL;
    request.headers.put("authorization",
        new Http.Header("authorization", "Bearer " + token));
    return request;
  }
}
