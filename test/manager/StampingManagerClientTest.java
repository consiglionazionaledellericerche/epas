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

package manager;

import com.google.common.base.Strings;
import common.injection.StaticInject;
import javax.inject.Inject;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Verifiche sui dati delle timbrature inviate dai client (lettori badge).
 */
@StaticInject
public class StampingManagerClientTest extends UnitTest {

  @Inject
  private static StampingManager stampingManager;

  @Test
  public void stampingsInTheFutureBeyondToleranceAreRejected() {
    assertFalse(stampingManager.isInTheFuture(LocalDateTime.now()));
    assertFalse(stampingManager.isInTheFuture(LocalDateTime.now().plusMinutes(
        StampingManager.MAX_MINUTES_IN_FUTURE_FOR_CLIENTS - 1)));
    assertTrue(stampingManager.isInTheFuture(LocalDateTime.now().plusMinutes(
        StampingManager.MAX_MINUTES_IN_FUTURE_FOR_CLIENTS + 1)));
    assertTrue(stampingManager.isInTheFuture(LocalDateTime.now().plusDays(1)));
  }

  @Test
  public void clientTextIsNormalized() {
    assertNull(StampingManager.clientText(null));
    assertNull(StampingManager.clientText("  \t "));
    assertEquals("ingresso principale", StampingManager.clientText(" ingresso principale\r\n"));
    assertEquals("riga1riga2", StampingManager.clientText("riga1\nriga2\u0000"));
    String tooLong = Strings.repeat("a", StampingManager.MAX_CLIENT_TEXT_LENGTH + 10);
    assertEquals(StampingManager.MAX_CLIENT_TEXT_LENGTH,
        StampingManager.clientText(tooLong).length());
  }
}
