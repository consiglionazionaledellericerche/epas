/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import common.security.ClientAddress;
import dao.UserDao;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.OfficeManager;
import models.User;
import play.Play;
import play.cache.Cache;
import play.mvc.Http;
import play.mvc.Scope;
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

  static final String LOGIN_MAX_FAILURES = "security.login.maxFailures";
  static final String LOGIN_LOCK_MINUTES = "security.login.lockMinutes";
  static final String LOGIN_LOCKED_FLASH = "loginLocked";
  private static final int DEFAULT_LOGIN_MAX_FAILURES = 10;
  private static final int DEFAULT_LOGIN_LOCK_MINUTES = 15;
  private static final String LOGIN_ATTEMPTS_KEY = "loginAttempts.";
  private static final Object LOGIN_ATTEMPTS_LOCK = new Object();

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
    if (Strings.isNullOrEmpty(password)) {
      return false;
    }
    if (Strings.isNullOrEmpty(username)) {
      return false;
    }

    // Il tentativo viene contato prima della verifica della password, così anche le richieste
    // in parallelo consumano ciascuna un tentativo.
    final String attemptsKey = loginAttemptsKey(username);
    if (registerLoginAttempt(attemptsKey) > loginMaxFailures()) {
      log.warn("Login bloccato per {}: troppi tentativi falliti, ip {}", username,
          ClientAddress.current().orNull());
      if (Scope.Flash.current() != null) {
        Scope.Flash.current().put(LOGIN_LOCKED_FLASH, true);
      }
      return false;
    }

    User user = 
        userDao.getUserByUsernameAndPassword(username, password);

    if (user != null) {
      log.info("user {} successfully logged in from ip {}", user.getUsername(),
          Http.Request.current() != null ? Http.Request.current().remoteAddress : null);
      if (user.passwordNeedsRehash()) {
        userDao.rehashPassword(user, password);
        log.info("Aggiornato l'hash della password per {}", user.getUsername());
      }
      Cache.safeDelete(attemptsKey);
      return true;
    }

    // Oops
    log.info("Failed login for {}", username);
    return false;
  }

  /**
   * Chiave della cache con i tentativi di login per lo username dall'indirizzo del client
   * corrente: chi prova password da un altro indirizzo non blocca il login dell'utente (es. dei
   * lettori badge). L'hash rende la chiave valida anche per memcached, qualunque sia lo
   * username ricevuto.
   */
  private static String loginAttemptsKey(String username) {
    return LOGIN_ATTEMPTS_KEY + Hashing.sha256().hashString(
        username.toLowerCase(Locale.ROOT) + "|" + ClientAddress.current().or("-"),
        StandardCharsets.UTF_8);
  }

  /**
   * Registra un tentativo di login e restituisce il numero di tentativi nella finestra
   * corrente, compreso questo. La finestra dura {@link #LOGIN_LOCK_MINUTES} minuti dal primo
   * tentativo; un login riuscito la azzera.
   */
  private static int registerLoginAttempt(String key) {
    final long windowMillis = loginLockMinutes() * 60_000L;
    synchronized (LOGIN_ATTEMPTS_LOCK) {
      final long now = System.currentTimeMillis();
      long start = now;
      int count = 1;
      final String value = Cache.get(key, String.class);
      if (value != null) {
        final List<String> parts = Splitter.on('|').splitToList(value);
        final long previousStart = Long.parseLong(parts.get(1));
        if (now - previousStart < windowMillis) {
          start = previousStart;
          count = Integer.parseInt(parts.get(0)) + 1;
        }
      }
      final long remainingSeconds = Math.max(1, (start + windowMillis - now) / 1000);
      Cache.safeSet(key, count + "|" + start, remainingSeconds + "s");
      if (count == loginMaxFailures() + 1) {
        log.warn("Raggiunto il numero massimo di tentativi di login per {}", key);
      }
      return count;
    }
  }

  /**
   * Azzera i tentativi di login dello username dall'indirizzo del client corrente (es. dopo
   * il recupero della password).
   */
  static void clearLoginAttempts(String username) {
    Cache.safeDelete(loginAttemptsKey(username));
  }

  static int loginMaxFailures() {
    return intProperty(LOGIN_MAX_FAILURES, DEFAULT_LOGIN_MAX_FAILURES);
  }

  private static int loginLockMinutes() {
    return intProperty(LOGIN_LOCK_MINUTES, DEFAULT_LOGIN_LOCK_MINUTES);
  }

  /**
   * Un parametro intero positivo della configurazione, con il valore di default se assente o
   * non valido (un valore errato non deve impedire i login).
   */
  private static int intProperty(String name, int defaultValue) {
    final String value = Play.configuration.getProperty(name);
    if (Strings.isNullOrEmpty(value)) {
      return defaultValue;
    }
    try {
      final int parsed = Integer.parseInt(value.trim());
      return parsed > 0 ? parsed : defaultValue;
    } catch (NumberFormatException e) {
      log.warn("Valore non valido per {}: '{}', uso {}", name, value, defaultValue);
      return defaultValue;
    }
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
    final Optional<String> address = ClientAddress.current();
    log.debug("Remote address = {}", address);
    return !officeManager.getOfficesWithAllowedIp(address,
        ClientAddress.compatibilityAddresses()).isEmpty();
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
