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
import play.mvc.Util;
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
      return true;
    }

    // Oops
    log.info("Failed login for {}", username);
    return false;
  }

  /**
   * In questo metodo viene gestito sia il caso di connessione tramite interfaccia che tramite Basic
   * Auth. Non viene però salvata però da nessuna parte il tipo di autenticazione effettuata.
   *
   * Perciò è possibile utilizzare tramite autenticazione Basic sia i controller standard che quelli
   * con l'annotation '@BasicAuth'.
   *
   * Per cambiare questo comportamento bisognerebbe salvare quest'informazione
   * (anche in sessione volendo), e utilizzarla nel metodo Resecure.checkAccess.
   *
   * @return l'utente corrente, se presente, altrimenti "absent".
   */
  public static Optional<User> getUser() {
    if (session != null && isConnected()) {
      if (request.args.containsKey(CURRENT_USER)) {
        return Optional.of((User) request.args.get(CURRENT_USER));
      }
      Optional<User> user = Optional.fromNullable(userDao.byUsername(connected()));
      if (user.isPresent()){
        request.args.put(CURRENT_USER, user.get());
      }
      return user;
    }
    if (request.user != null && request.password != null && authenticate(request.user,
        request.password)) {
      session.put("username", request.user);
      return Optional.fromNullable(userDao.byUsername(connected()));
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
  @Util
  public static boolean checkForWebstamping() {

    final List<String> addresses = Lists.newArrayList(Splitter.on(",").trimResults()
        .split(Http.Request.current().remoteAddress));

    log.info("Remote addresses = {}", addresses);
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
