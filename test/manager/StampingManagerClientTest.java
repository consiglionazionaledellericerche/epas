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
import play.Play;
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
    int hours = StampingManager.DEFAULT_MAX_HOURS_IN_FUTURE_FOR_CLIENTS;
    assertFalse(stampingManager.isInTheFuture(LocalDateTime.now()));
    assertFalse(stampingManager.isInTheFuture(LocalDateTime.now().plusHours(2)));
    assertFalse(stampingManager.isInTheFuture(LocalDateTime.now().plusHours(hours).minusMinutes(1)));
    assertTrue(stampingManager.isInTheFuture(LocalDateTime.now().plusHours(hours).plusMinutes(1)));
    assertTrue(stampingManager.isInTheFuture(new LocalDateTime(4019, 5, 3, 14, 43)));
  }

  @Test
  public void toleranceIsConfigurable() {
    String previous = Play.configuration.getProperty(
        StampingManager.MAX_HOURS_IN_FUTURE_FOR_CLIENTS);
    try {
      Play.configuration.setProperty(StampingManager.MAX_HOURS_IN_FUTURE_FOR_CLIENTS, "2");
      assertTrue(stampingManager.isInTheFuture(LocalDateTime.now().plusHours(3)));
      Play.configuration.setProperty(StampingManager.MAX_HOURS_IN_FUTURE_FOR_CLIENTS, "non valido");
      assertEquals(StampingManager.DEFAULT_MAX_HOURS_IN_FUTURE_FOR_CLIENTS,
          StampingManager.maxHoursInFutureForClients());
    } finally {
      if (previous == null) {
        Play.configuration.remove(StampingManager.MAX_HOURS_IN_FUTURE_FOR_CLIENTS);
      } else {
        Play.configuration.setProperty(StampingManager.MAX_HOURS_IN_FUTURE_FOR_CLIENTS, previous);
      }
    }
  }

  @Test
  public void clientTextIsNormalized() {
    assertNull(StampingManager.clientText(null));
    assertNull(StampingManager.clientText("  \t "));
    assertEquals("ingresso principale", StampingManager.clientText(" ingresso principale\r\n"));
    assertEquals("riga1 riga2", StampingManager.clientText("riga1\nriga2\u0000"));
    assertEquals("a b", StampingManager.clientText("a \t\n  b"));
    String tooLong = Strings.repeat("a", StampingManager.MAX_CLIENT_TEXT_LENGTH + 10);
    assertEquals(StampingManager.MAX_CLIENT_TEXT_LENGTH,
        StampingManager.clientText(tooLong).length());
  }

  @Test
  public void truncationDoesNotSplitSurrogatePairs() {
    String emoji = "\uD83D\uDE00";
    String text = Strings.repeat("a", StampingManager.MAX_CLIENT_TEXT_LENGTH - 1) + emoji + "bbb";
    String cleaned = StampingManager.clientText(text);
    assertEquals(StampingManager.MAX_CLIENT_TEXT_LENGTH - 1, cleaned.length());
    assertFalse(Character.isHighSurrogate(cleaned.charAt(cleaned.length() - 1)));
  }
}
