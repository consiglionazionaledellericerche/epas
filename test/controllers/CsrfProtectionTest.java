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
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import models.Person;
import models.Role;
import models.UsersRolesOffices;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.db.jpa.JPA;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

/**
 * Test funzionali sulla verifica anti-CSRF (CsrfProtection).
 */
@StaticInject
public class CsrfProtectionTest extends FunctionalTest {

  private static final Pattern META_TOKEN =
      Pattern.compile("<meta name=\"csrf-token\" content=\"([^\"]+)\">");

  @Inject
  private static H2Examples h2Examples;

  private String previousMode;
  private Person employee;

  /**
   * Utente dipendente per le richieste autenticate e modalità enforce.
   */
  @Before
  public void setUp() {
    previousMode = Play.configuration.getProperty(CsrfProtection.CSRF_MODE);
    Play.configuration.setProperty(CsrfProtection.CSRF_MODE, "enforce");
    employee = h2Examples.activeNormalEmployee(new LocalDate(2026, 1, 1));
    Role role = Role.find("byName", Role.EMPLOYEE).first();
    if (role == null) {
      role = new Role();
      role.setName(Role.EMPLOYEE);
      role.save();
    }
    UsersRolesOffices uro = new UsersRolesOffices();
    uro.setUser(employee.getUser());
    uro.setOffice(employee.getOffice());
    uro.setRole(role);
    uro.save();
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
    clearCookies();
  }

  @After
  public void restoreMode() {
    if (previousMode == null) {
      Play.configuration.remove(CsrfProtection.CSRF_MODE);
    } else {
      Play.configuration.setProperty(CsrfProtection.CSRF_MODE, previousMode);
    }
  }

  @Test
  public void formPostWithoutTokenIsRejected() {
    login();
    assertStatus(403, POST(newRequest(), savePasswordUrl(), passwordParams(), noFiles()));
  }

  @Test
  public void formPostWithTokenIsAccepted() {
    login();
    Map<String, String> params = passwordParams();
    params.put("authenticityToken", token());
    assertNotForbidden(POST(newRequest(), savePasswordUrl(), params, noFiles()));
  }

  @Test
  public void ajaxPostWithHeaderTokenIsAccepted() {
    login();
    Request request = newRequest();
    header(request, "x-csrf-token", token());
    assertNotForbidden(POST(request, savePasswordUrl(), passwordParams(), noFiles()));
  }

  @Test
  public void reportModeOnlyLogs() {
    Play.configuration.setProperty(CsrfProtection.CSRF_MODE, "report");
    login();
    assertNotForbidden(POST(newRequest(), savePasswordUrl(), passwordParams(), noFiles()));
  }

  @Test
  public void offModeDoesNotCheck() {
    Play.configuration.setProperty(CsrfProtection.CSRF_MODE, "off");
    login();
    assertNotForbidden(POST(newRequest(), savePasswordUrl(), passwordParams(), noFiles()));
  }

  /**
   * I client REST (header Authorization) non sono verificati, anche se conservano il cookie
   * di sessione.
   */
  @Test
  public void restClientsAreNotChecked() {
    assertNotForbidden(POST(basicAuthRequest(), savePasswordUrl(), passwordParams(), noFiles()));
    // seconda richiesta con il cookie di sessione ricevuto dalla prima
    assertNotForbidden(POST(basicAuthRequest(), savePasswordUrl(), passwordParams(), noFiles()));
  }

  /**
   * Credenziali Basic inviate dal browser da una pagina di un altro sito: la verifica si
   * applica.
   */
  @Test
  public void basicAuthFromAnotherSiteIsChecked() {
    Request request = basicAuthRequest();
    header(request, "sec-fetch-site", "cross-site");
    header(request, "sec-fetch-mode", "navigate");
    assertStatus(403, POST(request, savePasswordUrl(), passwordParams(), noFiles()));
  }

  /**
   * Richieste da un altro sito che non sono navigazioni (es. un'immagine in una pagina
   * esterna) sono rifiutate anche in GET; le navigazioni (link) sono ammesse.
   */
  @Test
  public void crossSiteSubresourceRequestsAreRejected() {
    login();
    Request image = newRequest();
    header(image, "sec-fetch-site", "cross-site");
    header(image, "sec-fetch-mode", "no-cors");
    assertStatus(403, GET(image, "/"));

    Request link = newRequest();
    header(link, "sec-fetch-site", "cross-site");
    header(link, "sec-fetch-mode", "navigate");
    assertNotForbidden(GET(link, "/"));
  }

  /**
   * Anche il recupero della password richiede il token.
   */
  @Test
  public void lostPasswordRequiresToken() {
    Map<String, String> params = new HashMap<>();
    params.put("email", "nessuno@example.org");
    String url = Router.reverse("LostPassword.sendTokenRecoveryPassword").url;
    assertStatus(403, POST(newRequest(), url, params, noFiles()));

    String token = tokenFrom(getContent(GET(Router.reverse("LostPassword.lostPassword").url)));
    params.put("authenticityToken", token);
    assertNotForbidden(POST(newRequest(), url, params, noFiles()));
  }

  private void login() {
    POST("/login?username=" + employee.getUser().getUsername() + "&password="
        + H2Examples.DEFAULT_PASSWORD);
  }

  private String token() {
    return tokenFrom(getContent(GET(Router.reverse("Persons.changePassword").url)));
  }

  private String tokenFrom(String html) {
    Matcher matcher = META_TOKEN.matcher(html);
    assertTrue("meta csrf-token non trovato", matcher.find());
    return matcher.group(1);
  }

  private Request basicAuthRequest() {
    Request request = newRequest();
    request.user = employee.getUser().getUsername();
    request.password = H2Examples.DEFAULT_PASSWORD;
    header(request, "authorization", "Basic x");
    return request;
  }

  private void header(Request request, String name, String value) {
    request.headers.put(name, new Http.Header(name, value));
  }

  private String savePasswordUrl() {
    return Router.reverse("Persons.savePassword").url;
  }

  private Map<String, String> passwordParams() {
    Map<String, String> params = new HashMap<>();
    params.put("vecchiaPassword", "sbagliata");
    params.put("nuovaPassword", "nuovaPassword1");
    params.put("confermaPassword", "nuovaPassword1");
    return params;
  }

  private Map<String, File> noFiles() {
    return new HashMap<>();
  }

  private void assertNotForbidden(Response response) {
    assertFalse("risposta 403 inattesa", response.status == 403);
  }
}
