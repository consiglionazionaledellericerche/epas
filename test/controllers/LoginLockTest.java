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

import java.util.UUID;
import models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.cache.Cache;
import play.test.UnitTest;

/**
 * Verifica del blocco del login dopo troppi tentativi falliti.
 */
public class LoginLockTest extends UnitTest {

  private static final String PASSWORD = "unaPasswordDiTest";
  private static final int MAX_FAILURES = 10;

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
  }

  @After
  public void deleteUser() {
    Cache.safeDelete("loginFailures." + user.getUsername().toLowerCase());
    user.delete();
  }

  @Test
  public void loginIsLockedAfterTooManyFailures() {
    for (int i = 0; i < MAX_FAILURES; i++) {
      assertFalse(Security.authenticate(user.getUsername(), "sbagliata"));
    }
    assertTrue(Security.isLoginLocked(user.getUsername()));
    // anche con la password corretta il login resta bloccato
    assertFalse(Security.authenticate(user.getUsername(), PASSWORD));
    // il blocco non dipende dalle maiuscole dello username
    assertTrue(Security.isLoginLocked(user.getUsername().toUpperCase()));
  }

  @Test
  public void successfulLoginResetsFailures() {
    for (int i = 0; i < MAX_FAILURES - 1; i++) {
      assertFalse(Security.authenticate(user.getUsername(), "sbagliata"));
    }
    assertTrue(Security.authenticate(user.getUsername(), PASSWORD));
    assertFalse(Security.authenticate(user.getUsername(), "sbagliata"));
    assertFalse(Security.isLoginLocked(user.getUsername()));
    assertTrue(Security.authenticate(user.getUsername(), PASSWORD));
  }
}
