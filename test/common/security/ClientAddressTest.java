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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Verifica della determinazione dell'indirizzo IP attendibile del client.
 */
public class ClientAddressTest extends UnitTest {

  @Test
  public void singleAddress() {
    assertEquals(Optional.of("10.0.0.1"), ClientAddress.of("10.0.0.1"));
  }

  @Test
  public void onlyLastAddressOfForwardedChainIsTrusted() {
    // Il client invia "X-Forwarded-For: 192.168.1.10" e il proxy appende l'IP reale.
    assertEquals(Optional.of("203.0.113.5"),
        ClientAddress.of("192.168.1.10, 203.0.113.5"));
    assertEquals(Optional.of("203.0.113.5"),
        ClientAddress.of("1.1.1.1,192.168.1.10 ,  203.0.113.5"));
  }

  @Test
  public void missingAddress() {
    assertFalse(ClientAddress.of(null).isPresent());
    assertFalse(ClientAddress.of("").isPresent());
    assertFalse(ClientAddress.of(" , ").isPresent());
  }

  @Test
  public void trustedProxiesAreSkipped() {
    // Apache (10.0.0.2) -> traefik (172.17.0.1) -> Play: il client reale è 203.0.113.5.
    final ImmutableSet<String> proxies = ImmutableSet.of("10.0.0.2", "172.17.0.1");
    assertEquals(Optional.of("203.0.113.5"),
        ClientAddress.of("192.168.1.10, 203.0.113.5, 10.0.0.2", proxies));
    assertEquals(Optional.of("203.0.113.5"),
        ClientAddress.of("203.0.113.5, 10.0.0.2", proxies));
    // Un client che inserisce l'ip di un proxy fidato nell'header non ottiene nulla:
    // il proxy appende comunque il suo ip reale.
    assertEquals(Optional.of("203.0.113.5"),
        ClientAddress.of("10.0.0.2, 203.0.113.5", proxies));
  }

  @Test
  public void chainOfOnlyTrustedProxies() {
    assertEquals(Optional.of("10.0.0.2"),
        ClientAddress.of("10.0.0.2, 172.17.0.1", ImmutableSet.of("10.0.0.2", "172.17.0.1")));
  }
}
