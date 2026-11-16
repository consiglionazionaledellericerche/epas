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

import org.junit.Test;
import play.Play;
import play.test.UnitTest;

/**
 * Verifica dei secret non utilizzabili in produzione.
 */
public class ApplicationSecretTest extends UnitTest {

  @Test
  public void missingSecretIsRejected() {
    assertTrue(ApplicationSecret.problem(null).isPresent());
    assertTrue(ApplicationSecret.problem("").isPresent());
    assertTrue(ApplicationSecret.problem("   ").isPresent());
  }

  @Test
  public void publishedSecretsAreRejected() {
    // il secret presente in application.conf fino alla 2.38.0
    assertTrue(ApplicationSecret.problem(
        "9BFoF22ydggvI8mVjbPteVzK6nHumoSksjCIBeTmYMM6H7kvK3oCLHaMUNpKartT").isPresent());
    // l'esempio del docker-compose.yml
    assertTrue(ApplicationSecret.problem(
        "y2RS8pYk7g8NFttsRbjmhpEUm00tAQILvOfI1LMQ2wIiVV20J2S0VfQOdTjys9yh").isPresent());
    // spazi iniziali e finali non bastano a renderli diversi
    assertTrue(ApplicationSecret.problem(
        " 9BFoF22ydggvI8mVjbPteVzK6nHumoSksjCIBeTmYMM6H7kvK3oCLHaMUNpKartT ").isPresent());
  }

  @Test
  public void developmentSecretIsRejected() {
    // il secret di sviluppo e test di application.conf non deve valere in produzione
    assertTrue(ApplicationSecret.problem(Play.configuration.getProperty("application.secret"))
        .isPresent());
  }

  @Test
  public void shortSecretIsRejected() {
    assertTrue(ApplicationSecret.problem("abcdefghijklmnopqrstuvwxyz01234").isPresent());
  }

  @Test
  public void randomSecretIsAccepted() {
    assertFalse(ApplicationSecret.problem(
        "Qm7xT2vLp9RsZk4Wn8YbHc3Jf6Gd1Ue5Ai0Ko2Xr7Vt9Sz4Lq6Pw8Nm3Bj5Hg1Fe").isPresent());
    assertFalse(ApplicationSecret.problem("abcdefghijklmnopqrstuvwxyz012345").isPresent());
  }
}
