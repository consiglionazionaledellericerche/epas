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

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import dao.UserDao;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.OfficeManager;
import models.User;
import play.mvc.Http;
import play.mvc.Util;
import play.utils.Java;

/**
 * Classe di gestione della security.
 *
 * @author dario
 *
 */
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
   * Verifica se i dati sono corretti per l'autenticazione.
   *
   * @param username l'username che intende autenticarsi
   * @param password la password che intende autenticarsi
   * @return true se è autenticato, false altrimenti.
   */
  static boolean authenticate(String username, String password) {
    log.debug("Richiesta autenticazione di {}", username);

    @SuppressWarnings("deprecation")
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
   * <p>Perciò è possibile utilizzare tramite autenticazione Basic sia i controller standard che 
   * quelli con l'annotation '@BasicAuth'.</p>
   *
   * <p>Per cambiare questo comportamento bisognerebbe salvare quest'informazione
   * (anche in sessione volendo), e utilizzarla nel metodo Resecure.checkAccess.</p>
   *
   * @return l'utente corrente, se presente, altrimenti "absent".
   */
  public static Optional<User> getUser() {
    if (session != null && isConnected()) {
      if (request.args.containsKey(CURRENT_USER)) {
        return Optional.of((User) request.args.get(CURRENT_USER));
      }
      Optional<User> user = Optional.fromNullable(userDao.byUsername(connected()));
      if (user.isPresent()) {
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
   * Verifica se abilitare o meno la trimbratura web.
   *
   * @return Vero se c'è almeno un istituto abilitato dall'ip contenuto nella richiesta HTTP
   *        ricevuta, false altrimenti.
   */
  @Util
  public static boolean checkForWebstamping() {

    if (Http.Request.current() == null || Http.Request.current().remoteAddress == null) {
      log.debug("Remote addresses not present, web stamping not permitted");
      return false;
    }
    final List<String> addresses = Lists.newArrayList(Splitter.on(",").trimResults()
        .split(Http.Request.current().remoteAddress));

    log.debug("Remote addresses = {}", addresses);
    return !officeManager.getOfficesWithAllowedIp(addresses).isEmpty();
  }

  /**
   * ridefinizione di logout per discriminare il messaggio a seconda della tipologia connessione.
   */
  public static void logout() {
    //Verifico se siamo nel caso di autenticazione OAuth
    if (session.contains(Resecure.OAUTH)) {
      log.info("Logout sessione oauth per {}", connected());
      // copia della sessione perché nel logout viene svuotata
      val sessionCopy = new HashMap<>(session.all());
      flash.success("secure.logout");
      Resecure.oauthLogout(sessionCopy);
    } else {
      //Caso autenticazione locale o shibboleth
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
      } catch (Throwable ex) {
        log.error("Eccezione durante il logout", ex);
      }
    }

    try {
      Secure.login();
    } catch (Throwable e) {
      log.error("Eccezione durante il redirect alla login", e);
    }
  }

}
