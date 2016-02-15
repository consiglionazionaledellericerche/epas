package controllers;


import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.hash.Hashing;

import dao.UserDao;

import manager.ConfigurationManager;
import manager.OfficeManager;

import models.User;
import models.enumerate.Parameter;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.mvc.Http;
import play.utils.Java;

import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;


public class Security extends Secure.Security {

  public static final String CACHE_DURATION = "30mn";

  //FIXME residuo dei vecchi residui, rimuoverlo e sostituirlo nei metodi che lo utilizzano
  @Inject
  private static UserDao userDao;
  @Inject
  private static OfficeManager officeManager;

  /**
   * @return true se è autenticato, false altrimenti.
   */
  static boolean authenticate(String username, String password) {
    Logger.trace("Richiesta autenticazione di %s", username);

    User user = userDao.getUserByUsernameAndPassword(username, Optional
        .fromNullable(Hashing.md5().hashString(password, Charsets.UTF_8).toString()));

    if (user != null) {
      Cache.set(username, user, CACHE_DURATION);
      Cache.set("userId", user.id, CACHE_DURATION);

      Logger.info("user %s successfully logged in from ip %s", user.username,
          Http.Request.current().remoteAddress);

      return true;
    }

    // Oops
    Logger.info("Failed login for %s ", username);
    flash.put("username", username);
    flash.error("Login failed");
    return false;
  }

  private static Optional<User> getUser(String username) {

    if (username == null || username.isEmpty()) {
      Logger.trace("getUSer failed for username %s", username);
      return Optional.<User>absent();
    }
    Logger.trace("Richiesta getUser(), username=%s", username);

    //cache
    //User user = (User)Cache.get(username);
    //if(user!=null)
    //  return Optional.of(user);

    //db
    User user = userDao.getUserByUsernameAndPassword(username, Optional.<String>absent());

    Logger.trace("User.find('byUsername'), username=%s, e' %s", username, user);
    if (user == null) {
      Logger.info("Security.getUser(): USer con username = %s non trovata nel database", username);
      return Optional.<User>absent();
    }
    //Cache.set(username, user, CACHE_DURATION);
    return Optional.of(user);
  }

  /**
   * Preleva (opzionalmente) l'utente loggato.
   * @return l'utente correntemente loggato se presente
   */
  public static Optional<User> getUser() {
    return getUser(connected());
  }

  static String connected() {
    if (request == null) {
      return null;
    }
    if (request.user != null) {
      return request.user;
    } else {
      return Secure.Security.connected();
    }
  }

  static Object invoke(String m, Object... args) throws Throwable {

    try {
      return Java.invokeChildOrStatic(Security.class, m, args);
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    }
  }

  public static boolean checkForWebstamping() {
    if ("true".equals(Play.configuration.getProperty(Clocks.SKIP_IP_CHECK))) {
      return true;
    }
    String remoteAddress = Http.Request.current().remoteAddress;
    return !(Boolean)officeManager.getOfficesWithAllowedIp(remoteAddress).isEmpty();
  }
}
