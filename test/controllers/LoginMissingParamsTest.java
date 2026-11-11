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
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.CookieDataCodec;
import play.mvc.Http.Cookie;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

/**
 * Un POST /login senza username o senza password deve comportarsi come un login fallito
 * (redirect alla pagina di login) e non rispondere 500.
 */
public class LoginMissingParamsTest extends FunctionalTest {

  private static final String PASSWORD = "unaPasswordDiTest";

  private User user;

  /**
   * Crea un utente di test esistente, per verificare che senza password non si entri.
   */
  @Before
  public void createUser() {
    clearCookies();
    user = new User();
    user.setUsername("login.missing." + UUID.randomUUID());
    user.updatePassword(PASSWORD);
    user.save();
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  /**
   * Azzera i tentativi di login dell'utente e lo cancella.
   *
   * <p>La chiave dei tentativi dipende dall'indirizzo del client della richiesta corrente: per
   * azzerare quelli registrati dalle richieste del test si usa una richiesta costruita come
   * quelle di FunctionalTest.</p>
   */
  @After
  public void cleanUp() {
    final Request previous = Request.current();
    Request.current.set(newRequest());
    try {
      Security.clearLoginAttempts(user.getUsername());
    } finally {
      Request.current.set(previous);
    }
    if (!JPA.em().getTransaction().isActive()) {
      JPA.em().getTransaction().begin();
    }
    final User saved = User.findById(user.getId());
    if (saved != null) {
      saved.delete();
    }
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  @Test
  public void loginWithoutPasswordIsAFailedLogin() {
    assertFailedLogin(POST("/login", ImmutableMap.of("username", user.getUsername())));
  }

  @Test
  public void loginWithoutUsernameIsAFailedLogin() {
    assertFailedLogin(POST("/login", ImmutableMap.of("password", PASSWORD)));
  }

  @Test
  public void loginWithoutCredentialsIsAFailedLogin() {
    assertFailedLogin(POST("/login", ImmutableMap.<String, String>of()));
  }

  @Test
  public void loginWithCredentialsStillWorks() {
    Response response = POST("/login",
        ImmutableMap.of("username", user.getUsername(), "password", PASSWORD));
    assertStatus(302, response);
    assertEquals(user.getUsername(), session(response).get("username"));
  }

  private void assertFailedLogin(Response response) {
    assertStatus(302, response);
    assertTrue(response.getHeader("Location").contains("/login"));
    assertFalse(session(response).containsKey("username"));
  }

  /**
   * I dati della sessione impostati dalla risposta, vuoti se la risposta non imposta il
   * cookie di sessione. Il valore del cookie è "firma-dati", con i dati codificati da
   * CookieDataCodec.
   */
  private static Map<String, String> session(Response response) {
    final Map<String, String> data = new HashMap<>();
    final Cookie cookie = response.cookies.get("PLAY_SESSION");
    if (cookie == null || cookie.value == null || cookie.value.isEmpty()) {
      return data;
    }
    final int dash = cookie.value.indexOf('-');
    assertTrue("cookie di sessione non firmato: " + cookie.value, dash > 0);
    try {
      CookieDataCodec.decode(data, cookie.value.substring(dash + 1));
    } catch (UnsupportedEncodingException ex) {
      throw new IllegalStateException(ex);
    }
    return data;
  }
}
