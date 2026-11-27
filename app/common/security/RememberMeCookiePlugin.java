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

import play.PlayPlugin;
import play.mvc.Http;
import play.mvc.Scope;

/**
 * Dà al cookie "rememberme" gli stessi attributi del cookie di sessione.
 *
 * <p>Il modulo secure di Play crea il cookie del "Remember me" con
 * response.setCookie(name, value, duration), che lo emette sempre senza HttpOnly e senza
 * Secure, qualunque sia la configurazione di application.session.httpOnly e
 * application.session.secure. Il cookie permette di rientrare senza password per
 * secure.rememberme.duration (30 giorni): senza HttpOnly è leggibile da JavaScript, senza
 * Secure viaggia anche in http. Con application.session.sameSite=None il browser lo rifiuta
 * del tutto, perché SameSite=None richiede Secure.</p>
 *
 * <p>Il plugin interviene alla fine di ogni azione, anche quando l'azione termina con un
 * redirect come Secure.authenticate, e prima che Play scriva gli header della risposta. Vale
 * anche per il cookie scaduto con cui il logout cancella il "Remember me".</p>
 */
public class RememberMeCookiePlugin extends PlayPlugin {

  static final String REMEMBER_ME = "rememberme";

  @Override
  public void onActionInvocationFinally() {
    final Http.Response response = Http.Response.current();
    if (response == null) {
      return;
    }
    harden(response.cookies.get(REMEMBER_ME), Scope.SESSION_HTTPONLY, Scope.COOKIE_SECURE);
  }

  /**
   * Imposta HttpOnly e Secure sul cookie come sono configurati per la sessione, senza
   * togliere attributi già presenti.
   */
  static void harden(Http.Cookie cookie, boolean httpOnly, boolean secure) {
    if (cookie == null) {
      return;
    }
    cookie.httpOnly = cookie.httpOnly || httpOnly;
    cookie.secure = cookie.secure || secure;
  }
}
