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

import models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Verifica che l'autenticazione richieda sempre una password valida.
 */
public class AuthenticationTest extends UnitTest {

  private static final String USERNAME = "auth.test.user";
  private static final String PASSWORD = "unaPasswordDiTest";

  private User user;

  /**
   * Crea un utente di test con password MD5 e SHA-512 valorizzate.
   */
  @Before
  public void createUser() {
    user = new User();
    user.setUsername(USERNAME);
    user.setPassword(User.cryptPasswordMd5(PASSWORD));
    user.setPasswordSha512(User.cryptPasswordSha512(PASSWORD));
    user.save();
  }

  @After
  public void deleteUser() {
    if (user != null) {
      user.delete();
    }
  }

  @Test
  public void validPasswordIsAccepted() {
    assertTrue(Security.authenticate(USERNAME, PASSWORD));
  }

  @Test
  public void wrongPasswordIsRejected() {
    assertFalse(Security.authenticate(USERNAME, "sbagliata"));
  }

  @Test
  public void missingPasswordIsRejected() {
    assertFalse(Security.authenticate(USERNAME, null));
    assertFalse(Security.authenticate(USERNAME, ""));
  }

  @Test
  public void missingUsernameIsRejected() {
    assertFalse(Security.authenticate(null, PASSWORD));
    assertFalse(Security.authenticate("", PASSWORD));
  }

  @Test
  public void disabledUserIsRejected() {
    user.setDisabled(true);
    user.save();
    assertFalse(Security.authenticate(USERNAME, PASSWORD));
  }
}
