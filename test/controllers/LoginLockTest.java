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

package controllers;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.db.jpa.JPA;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

/**
 * Verifica del limite ai tentativi di login.
 */
public class LoginLockTest extends FunctionalTest {

  private static final String PASSWORD = "unaPasswordDiTest";

  private User user;

  /**
   * Crea un utente di test con uno username diverso per ogni test.
   */
  @Before
  public void createUser() {
    user = new User();
    user.setUsername("login.lock." + UUID.randomUUID());
    user.updatePassword(PASSWORD);
    user.save();
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }

  @After
  public void clearAttempts() {
    Security.clearLoginAttempts(user.getUsername());
  }

  @Test
  public void loginIsLockedAfterTooManyFailures() {
    for (int i = 0; i < Security.loginMaxFailures(); i++) {
      assertFalse(Security.authenticate(user.getUsername(), "sbagliata"));
    }
    // anche con la password corretta il login resta bloccato
    assertFalse(Security.authenticate(user.getUsername(), PASSWORD));
    // il blocco non dipende dalle maiuscole dello username
    assertFalse(Security.authenticate(user.getUsername().toUpperCase(), PASSWORD));
  }

  @Test
  public void successfulLoginResetsAttempts() {
    for (int i = 0; i < Security.loginMaxFailures() - 1; i++) {
      assertFalse(Security.authenticate(user.getUsername(), "sbagliata"));
    }
    assertTrue(Security.authenticate(user.getUsername(), PASSWORD));
    for (int i = 0; i < Security.loginMaxFailures() - 1; i++) {
      assertFalse(Security.authenticate(user.getUsername(), "sbagliata"));
    }
    assertTrue(Security.authenticate(user.getUsername(), PASSWORD));
  }

  /**
   * Tentativi in parallelo consumano ciascuno un tentativo: non si può aggirare il limite
   * inviando molte richieste contemporaneamente.
   */
  @Test
  public void parallelAttemptsCannotBypassTheLimit() throws Exception {
    final String username = user.getUsername();
    final int attempts = Security.loginMaxFailures() * 3;
    ExecutorService executor = Executors.newFixedThreadPool(attempts);
    try {
      List<Callable<Boolean>> logins = Lists.newArrayList();
      for (int i = 0; i < attempts; i++) {
        logins.add(() -> {
          try {
            return JPA.withTransaction("default", true,
                () -> Security.authenticate(username, "sbagliata"));
          } catch (Throwable e) {
            throw new Exception(e);
          }
        });
      }
      executor.invokeAll(logins);
    } finally {
      executor.shutdown();
    }
    assertFalse(Security.authenticate(username, PASSWORD));
  }

  /**
   * Un valore non valido nella configurazione non impedisce i login: si usa il default.
   */
  @Test
  public void invalidConfigurationFallsBackToDefault() {
    String previous = Play.configuration.getProperty(Security.LOGIN_MAX_FAILURES);
    Play.configuration.setProperty(Security.LOGIN_MAX_FAILURES, "10 # commento");
    try {
      assertEquals(10, Security.loginMaxFailures());
      assertTrue(Security.authenticate(user.getUsername(), PASSWORD));
    } finally {
      if (previous == null) {
        Play.configuration.remove(Security.LOGIN_MAX_FAILURES);
      } else {
        Play.configuration.setProperty(Security.LOGIN_MAX_FAILURES, previous);
      }
    }
  }

  /**
   * Username con spazi o molto lunghi non causano errori.
   */
  @Test
  public void unusualUsernamesDoNotFail() {
    assertFalse(Security.authenticate("a b\nc", "sbagliata"));
    assertFalse(Security.authenticate(Strings.repeat("x", 500), "sbagliata"));
    Security.clearLoginAttempts("a b\nc");
    Security.clearLoginAttempts(Strings.repeat("x", 500));
  }

  /**
   * I tentativi da un indirizzo non bloccano il login dello stesso utente da un altro
   * indirizzo; dall'indirizzo bloccato la pagina di login mostra il motivo.
   */
  @Test
  public void lockIsPerClientAddress() {
    final String attacker = "203.0.113.10";
    for (int i = 0; i < Security.loginMaxFailures(); i++) {
      login(attacker, "sbagliata");
    }
    Response locked = login(attacker, PASSWORD);
    assertTrue(locked.getHeader("Location").contains("login"));
    assertTrue(getContent(GET(newRequest(attacker), "/login"))
        .contains("Troppi tentativi di accesso falliti"));

    clearCookies();
    Response ok = login("198.51.100.20", PASSWORD);
    assertFalse(ok.getHeader("Location").contains("login"));

    clearCookies();
    login(attacker, PASSWORD);
  }

  private Response login(String address, String password) {
    return POST(newRequest(address), "/login?username=" + user.getUsername()
        + "&password=" + password, "application/x-www-form-urlencoded", "");
  }

  private Request newRequest(String address) {
    Request request = newRequest();
    request.remoteAddress = address;
    return request;
  }
}
