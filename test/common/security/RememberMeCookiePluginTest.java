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

package common.security;

import org.junit.After;
import org.junit.Test;
import play.Play;
import play.mvc.Http;
import play.test.UnitTest;

/**
 * Verifica degli attributi del cookie "rememberme" impostati da RememberMeCookiePlugin.
 */
public class RememberMeCookiePluginTest extends UnitTest {

  @After
  public void clearResponse() {
    Http.Response.current.remove();
  }

  @Test
  public void isRegistered() {
    assertNotNull("RememberMeCookiePlugin non registrato in conf/play.plugins",
        Play.pluginCollection.getPluginInstance(RememberMeCookiePlugin.class));
  }

  /**
   * Il cookie creato dal modulo secure con setCookie(name, value, duration) non ha né
   * HttpOnly né Secure: il plugin li imposta come per il cookie di sessione.
   */
  @Test
  public void setsHttpOnlyAndSecureLikeTheSession() {
    final Http.Cookie cookie = rememberMe();
    assertFalse(cookie.httpOnly);
    assertFalse(cookie.secure);

    RememberMeCookiePlugin.harden(cookie, true, true);

    assertTrue(cookie.httpOnly);
    assertTrue(cookie.secure);
  }

  /**
   * Con application.session.secure=false (ePAS raggiunto in http) il cookie resta senza
   * Secure, altrimenti il browser non lo invierebbe.
   */
  @Test
  public void keepsSecureOffWhenTheSessionIsNotSecure() {
    final Http.Cookie cookie = rememberMe();

    RememberMeCookiePlugin.harden(cookie, true, false);

    assertTrue(cookie.httpOnly);
    assertFalse(cookie.secure);
  }

  @Test
  public void ignoresTheMissingCookie() {
    RememberMeCookiePlugin.harden(null, true, true);
  }

  /**
   * Alla fine dell'azione il plugin corregge il cookie presente nella risposta corrente e
   * lascia stare gli altri.
   */
  @Test
  public void hardensTheCookieOfTheCurrentResponse() {
    final Http.Response response = new Http.Response();
    Http.Response.current.set(response);
    response.setCookie(RememberMeCookiePlugin.REMEMBER_ME, "firma-utente-123", "30d");
    response.setCookie("altro", "valore", "30d");

    new RememberMeCookiePlugin().onActionInvocationFinally();

    final Http.Cookie rememberMe = response.cookies.get(RememberMeCookiePlugin.REMEMBER_ME);
    assertEquals(play.mvc.Scope.SESSION_HTTPONLY, rememberMe.httpOnly);
    assertEquals(play.mvc.Scope.COOKIE_SECURE, rememberMe.secure);
    assertFalse(response.cookies.get("altro").httpOnly);
  }

  private static Http.Cookie rememberMe() {
    final Http.Response response = new Http.Response();
    response.setCookie(RememberMeCookiePlugin.REMEMBER_ME, "firma-utente-123", "30d");
    return response.cookies.get(RememberMeCookiePlugin.REMEMBER_ME);
  }
}
