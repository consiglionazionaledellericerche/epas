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

import com.google.common.net.UrlEscapers;
import dao.PersonDao;
import java.util.HashMap;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import play.Logger;
import play.Play;
import play.Play.Mode;
import play.cache.Cache;
import play.mvc.Router;

/**
 * Application specific implementation of the Security class, this one just logs when each hook is
 * called.
 *
 * @author Scott Phillips, http://www.scottphillips/
 */
@Slf4j
public class ShibbolethSecurity extends controllers.shib.Security {

  @Inject
  private static PersonDao personDao;

  /**
   * This method checks that a profile is allowed to view this page/method. This method is called
   * prior to the method's controller annotated with the check method.
   *
   * @return true if you are allowed to execute this controller method.
   */
  static boolean check(String profile) {
    log.debug("Security: Security.profile({})", profile);
    return isConnected();
  }

  /**
   * Indicate if a user is currently connected.
   *
   * @return true if the user is connected
   */
  static boolean isConnected() {
    log.debug("Security: Security.isConnected()");
    return session.contains("shibboleth");
  }

  /**
   * This method is called after a successful authentication. The user's attributes will already be
   * stored in the session object. Use this method if you require complex attribute strategies or
   * need to sync the data with an external data source.
   */
  static void onAuthenticated() {
    log.debug("Security: Security.onAuthenticated()");
    if (!Play.configuration.getProperty("shib.login", "false").equalsIgnoreCase("true")) {
      log.warn("Bloccato tentativo di autenticazione Shibboleth perché non attivo");
      session.clear();
      badRequest("Autenticazione Shibboleth non abilitata.");
    }

    String eppn = session.get("eppn");
    log.debug("Trasformazione dell'utente shibboleth in utente locale, email = {}", eppn);

    Person person = personDao.byEppn(eppn).orNull();

    if (person != null) {

      Cache.set(person.getUser().getUsername(), person, Security.CACHE_DURATION);
      Cache.set("personId", person.id, Security.CACHE_DURATION);

      session.put("username", person.getUser().getUsername());
      session.put("shibboleth", true);
      
      flash.success("Benvenuto " + person.getName() + ' ' + person.getSurname());
      log.info("Person {} successfully logged in", person.getUser().getUsername());
      log.trace("Permission list for {} {}: {}",
          person.getName(), person.getSurname(), person.getUser().getUsersRolesOffices());
    } else {
      flash.error(
          "Autenticazione shibboleth riuscita ma utente con eppn=%s non presente in ePAS", eppn);
      log.warn("Person with email {} successfully logged in Shibboleth but unknonw to ePAS", eppn);
    }

  }

  /**
   * This method is called before a user tries to sign off.
   */
  static void onDisconnect() {
    log.debug("Security: Security.onDisconnect()");
  }

  /**
   * This method is called after a successful sign off.
   */
  static void onDisconnected() {
    log.debug("Security: Security.onDisconnected()");
  }

  /**
   * This method is called if a check does not succeed. By default it shows the not allowed page
   * (the controller forbidden method).
   */
  static void onCheckFailed(String profile) {
    log.debug("Security: Security.onCheckFailed({})", profile);
    forbidden();
  }

  /**
   * This method is called when their is a failure to extract/map attributes, such as missing
   * required attributes.
   *
   * @param attributes Map of attributes found, may be null.
   */
  static void onAttributeFailure(HashMap<String, String> attributes) {
    Logger.debug("Security: Security.onAttributeFailure(" + attributes + ")");
    error("Authentication Failure");
  }

  /**
   * Check if Shibboleth is being Mocked for testing.
   *
   * @return Is Shibboleth being Mocked for testing?
   */
  private static boolean isMock() {
    return Play.mode == Mode.DEV && "mock".equalsIgnoreCase(Play.configuration.getProperty("shib"));
  }


  /**
   * Initiate a shibboleth login.
   */
  public static void login() throws Throwable {

    // Determine where the Shibboleth Login initiator is
    String shibLogin = Play.configuration.getProperty("shib.login.url", null);
    if (shibLogin == null) {
      shibLogin = request.getBase() + "/Shibboleth.sso/Login";
    }
    if (isMock()) {
      shibLogin = Router.reverse("shib.Shibboleth.authenticate").url;
    }

    // Append the target query string
    shibLogin += "?target="
        + Play.configuration.getProperty("application.baseUrl", request.getBase());
    shibLogin += Router.reverse("shib.Shibboleth.authenticate").url;

    // Since we are redirecting we can't actually set the flash, so we'll
    // embed it in the target url.
    if (flash.get("url") != null) {
      if (isMock()) {
        shibLogin += "&return=" + UrlEscapers.urlFormParameterEscaper().escape(flash.get("url"));
      } else {
        shibLogin += "?return=" + UrlEscapers.urlFormParameterEscaper().escape(flash.get("url"));
      }
    }
    log.debug("Shib: Redirecting to Shibboleth login initiator: {}", shibLogin);

    redirect(shibLogin);
  }
  
}
