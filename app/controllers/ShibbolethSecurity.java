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

    String eppn = session.get("eppn");
    log.debug("Trasformazione dell'utente shibboleth in utente locale, email = {}", eppn);

    Person person = personDao.byEppn(eppn).orNull();

    if (person != null) {

      Cache.set(person.user.username, person, Security.CACHE_DURATION);
      Cache.set("personId", person.id, Security.CACHE_DURATION);

      session.put("username", person.user.username);
      session.put("shibboleth", true);
      
      flash.success("Benvenuto " + person.name + ' ' + person.surname);
      log.info("Person {} successfully logged in", person.user.username);
      log.trace("Permission list for {} {}: {}",
          person.name, person.surname, person.user.usersRolesOffices);
    } else {
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
