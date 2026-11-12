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
import java.util.UUID;
import models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.db.jpa.JPA;
import play.mvc.Http.Cookie;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope;
import play.test.FunctionalTest;

/**
 * I cookie di Play (sessione e flash) hanno l'attributo HttpOnly, quindi non sono leggibili da
 * JavaScript. L'attributo Secure dipende dalla configurazione (nel Docker da PROTOCOL) e nei
 * test resta disattivato.
 */
public class SessionCookieTest extends FunctionalTest {

  private static final String PASSWORD = "unaPasswordDiTest";

  private User user;

  /**
   * Crea un utente di test.
   */
  @Before
  public void createUser() {
    clearCookies();
    user = new User();
    user.setUsername("session.cookie." + UUID.randomUUID());
    user.updatePassword(PASSWORD);
    user.save();
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  /**
   * Azzera i tentativi di login dell'utente e lo cancella.
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
  public void sessionCookieIsHttpOnly() {
    Response response = POST("/login",
        ImmutableMap.of("username", user.getUsername(), "password", PASSWORD));
    assertStatus(302, response);
    Cookie session = response.cookies.get(Scope.COOKIE_PREFIX + "_SESSION");
    assertNotNull("il login deve impostare il cookie di sessione", session);
    assertTrue("il cookie di sessione deve avere HttpOnly", session.httpOnly);
    assertFalse("nei test il cookie di sessione non ha Secure", session.secure);
  }

  @Test
  public void flashCookieIsHttpOnly() {
    Response response = POST("/login",
        ImmutableMap.of("username", user.getUsername(), "password", "passwordSbagliata"));
    assertStatus(302, response);
    Cookie flash = response.cookies.get(Scope.COOKIE_PREFIX + "_FLASH");
    assertNotNull("il login fallito deve impostare il messaggio flash", flash);
    assertTrue("il cookie flash deve avere HttpOnly", flash.httpOnly);
  }
}
