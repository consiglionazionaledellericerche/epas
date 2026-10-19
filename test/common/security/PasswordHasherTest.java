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

package common.security;

import com.google.common.base.Strings;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Verifica dell'hash delle password con PBKDF2.
 */
public class PasswordHasherTest extends UnitTest {

  @Test
  public void hashMatchesOnlyTheSamePassword() {
    String hash = PasswordHasher.hash("unaPassword");
    assertTrue(PasswordHasher.matches("unaPassword", hash));
    assertFalse(PasswordHasher.matches("unaPasswordd", hash));
    assertFalse(PasswordHasher.matches("", hash));
    assertFalse(PasswordHasher.matches(null, hash));
  }

  @Test
  public void samePasswordHasDifferentSalts() {
    assertFalse(PasswordHasher.hash("unaPassword").equals(PasswordHasher.hash("unaPassword")));
  }

  @Test
  public void hashFormat() {
    String[] parts = PasswordHasher.hash("unaPassword").split("\\$");
    assertEquals(4, parts.length);
    assertEquals(PasswordHasher.ALGORITHM, parts[0]);
    assertEquals(String.valueOf(PasswordHasher.ITERATIONS), parts[1]);
  }

  @Test
  public void longPasswordsAreSupported() {
    String longPassword = Strings.repeat("àèìòù", 40);
    String hash = PasswordHasher.hash(longPassword);
    assertTrue(PasswordHasher.matches(longPassword, hash));
    assertFalse(PasswordHasher.matches(longPassword.substring(1), hash));
  }

  @Test
  public void invalidHashesNeverMatch() {
    assertFalse(PasswordHasher.matches("unaPassword", null));
    assertFalse(PasswordHasher.matches("unaPassword", ""));
    assertFalse(PasswordHasher.matches("unaPassword", "pbkdf2_sha256$x$y$z"));
    assertFalse(PasswordHasher.matches("unaPassword", "md5$1$abc$def"));
  }

  @Test
  public void hashesWithFewerIterationsNeedRehash() {
    assertFalse(PasswordHasher.needsRehash(PasswordHasher.hash("unaPassword")));
    assertTrue(PasswordHasher.needsRehash("pbkdf2_sha256$1000$AAAA$AAAA"));
    assertTrue(PasswordHasher.needsRehash("qualcosa"));
  }
}
