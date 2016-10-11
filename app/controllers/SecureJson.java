
package controllers;

import it.cnr.iit.epas.AuthInfoBinder;

import models.exports.AuthInfo;

import play.Logger;
import play.data.binding.As;
import play.mvc.Controller;

/**
 * Autenticazione DIY via json.
 * 
 * @author cristian
 */
public class SecureJson extends Controller {

  /**
   * Questo metodo deve essere chiamata passando i corretti header http Content-type:
   * application/json Accept: application/json
   *
   * @param body json nella forma {"username": "cristian.lucchesi", "password": "lapassword"}
   *
   *             Restituisce {"login" : "ok"} se il logout è andato a buon fine, altrimenti ritorno
   *             un codice HTTP 401
   */
  public static void login(@As(binder = AuthInfoBinder.class) AuthInfo body) {
    Logger.trace("Chiamata SecureJson.login, authInfo=%s", body);

    if (body != null && Security.authenticate(body.getUsername(), body.getPassword())) {
      // Mark user as connected
      session.put("username", body.getUsername());
      renderJSON("{\"login\":\"ok\"}");
      Logger.debug("Login Json utente: %s completato con successo", body.getUsername());
    } else {
      unauthorized();
    }

  }

  /**
   * Questo metodo deve essere chiamata passando i corretti header http Content-type:
   * application/json Accept: application/json.
   *
   * <p>
   * Restituisce {"logout" : "ok"} se il logout è andato a buon fine, altrimenti {"logout" : "ko"}
   * </p>
   */
  public static void logout() {

    if (session.contains("username")) {
      session.clear();
      renderJSON("{\"logout\":\"ok\"}");
      Logger.debug("Logout Json utente: %s completata con successo", session.contains("username"));
    } else {
      unauthorized();
    }
  }
}
