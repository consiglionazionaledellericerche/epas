/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

import it.cnr.iit.epas.AuthInfoBinder;
import lombok.extern.slf4j.Slf4j;
import models.exports.AuthInfo;
import play.data.binding.As;
import play.mvc.Controller;

/**
 * Autenticazione DIY via json.
 *
 * @author Cristian Lucchesi
 */
@Slf4j
public class SecureJson extends Controller {

  /**
   * Questo metodo deve essere chiamata passando i corretti header http Content-type:
   * application/json Accept: application/json.
   *
   * @param body json nella forma {"username": "cristian.lucchesi", "password": "lapassword"}
   *
   *             Restituisce {"login" : "ok"} se il logout è andato a buon fine, altrimenti ritorno
   *             un codice HTTP 401
   */
  public static void login(@As(binder = AuthInfoBinder.class) AuthInfo body) {
    log.trace("Chiamata SecureJson.login, authInfo=%s", body);

    if (body != null && Security.authenticate(body.getUsername(), body.getPassword())) {
      // Mark user as connected
      session.put("username", body.getUsername());
      renderJSON("{\"login\":\"ok\"}");
      log.debug("Login Json utente: %s completato con successo", body.getUsername());
    } else {
      unauthorized();
    }

  }

  /**
   * Questo metodo deve essere chiamata passando i corretti header http Content-type:
   * application/json Accept: application/json.
   * <p>
   * Restituisce {"logout" : "ok"} se il logout è andato a buon fine, altrimenti {"logout" : "ko"}
   * </p>
   */
  public static void logout() {

    if (session.contains("username")) {
      session.clear();
      renderJSON("{\"logout\":\"ok\"}");
      log.debug("Logout Json utente: %s completata con successo", session.contains("username"));
    } else {
      unauthorized();
    }
  }
}
