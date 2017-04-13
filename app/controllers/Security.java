package controllers;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import dao.UserDao;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.OfficeManager;
import models.User;
import play.mvc.Http;
import play.utils.Java;

@Slf4j
public class Security extends Secure.Security {

  public static final String CACHE_DURATION = "30mn";
  public static final String CURRENT_USER = "current_user";

  //FIXME residuo dei vecchi residui, rimuoverlo e sostituirlo nei metodi che lo utilizzano
  @Inject
  private static UserDao userDao;
  @Inject
  private static OfficeManager officeManager;

  /**
   * @return true se è autenticato, false altrimenti.
   */
  static boolean authenticate(String username, String password) {
    log.debug("Richiesta autenticazione di {}", username);

    User user = userDao.getUserByUsernameAndPassword(username, Optional
        .fromNullable(Hashing.md5().hashString(password, Charsets.UTF_8).toString()));

    if (user != null) {
      log.info("user {} successfully logged in from ip {}", user.username,
          Http.Request.current().remoteAddress);
      request.args.put(CURRENT_USER, user);
      return true;
    }

    // Oops
    log.info("Failed login for {}", username);
    flash.put("username", username);
    flash.error("Login failed");
    return false;
  }

  /**
   * @return l'utente corrente, se presente, altrimenti "absent".
   */
  public static Optional<User> getUser() {
    if (request.args.containsKey(CURRENT_USER)) {
      return Optional.of((User) request.args.get(CURRENT_USER));
    }
    if (session != null && isConnected()) {
      final Optional<User> user = Optional.fromNullable(userDao.byUsername(connected()));
      if (user.isPresent()) {
        request.args.put(CURRENT_USER, user.get());
      }
      return user;
    }
    if (request.user != null && request.password != null && authenticate(request.user,
        request.password)) {
      return Optional.of((User) request.args.get(CURRENT_USER));
    }
    return Optional.absent();
  }

  static Object invoke(String method, Object... args) throws Throwable {
    try {
      return Java.invokeChildOrStatic(Security.class, method, args);
    } catch (InvocationTargetException ex) {
      throw ex.getTargetException();
    }
  }

  /**
   * @return Vero se c'è almeno un istituto abilitato dall'ip contenuto nella richiesta HTTP
   * ricevuta, false altrimenti.
   */
  public static boolean checkForWebstamping() {

    final List<String> addresses = Lists.newArrayList(Splitter.on(",").trimResults()
        .split(Http.Request.current().remoteAddress));

    return !officeManager.getOfficesWithAllowedIp(addresses).isEmpty();
  }

  /**
   * ridefinizione di logout per discriminare il messaggio a seconda della tipologia connessione.
   */
  public static void logout() {
    try {
      invoke("onDisconnect");
      session.clear();
      response.removeCookie("rememberme");
      invoke("onDisconnected");
      if (session.contains("shibboleth")) {
        flash.success("secure.logoutShibboleth");
      } else {
        flash.success("secure.logout");
      }
      Secure.login();
    } catch (Throwable ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    }
  }

}
