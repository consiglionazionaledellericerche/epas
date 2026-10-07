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
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import play.Play;
import play.mvc.Http;

/**
 * Determinazione dell'indirizzo IP del client da utilizzare per i controlli di sicurezza
 * (es. whitelist IP per la timbratura web).
 *
 * <p>Con XForwardedSupport attivo Play valorizza remoteAddress con l'intero contenuto
 * dell'header X-Forwarded-For, che può contenere una catena di indirizzi
 * ("client, proxy1, proxy2"). Gli indirizzi a sinistra sono forniti dal client e quindi
 * falsificabili: la catena viene scorsa da destra saltando i reverse proxy fidati
 * (quelli indicati in XForwardedSupport) e il primo indirizzo non fidato è quello del
 * client. Con XForwardedSupport=all non ci sono proxy fidati noti e si considera
 * l'ultimo indirizzo della catena.</p>
 */
public final class ClientAddress {

  public static final String X_FORWARDED_SUPPORT = "XForwardedSupport";

  private static final Splitter COMMA_SPLITTER =
      Splitter.on(',').trimResults().omitEmptyStrings();
  private static final Splitter PROXY_SPLITTER =
      Splitter.onPattern("[\\s,]+").omitEmptyStrings();

  private ClientAddress() {
  }

  /**
   * L'indirizzo IP attendibile del client a partire dal valore di remoteAddress.
   *
   * @param remoteAddress il remoteAddress della richiesta (eventualmente una catena XFF)
   * @param trustedProxies gli indirizzi dei reverse proxy fidati
   * @return il primo indirizzo non fidato partendo dalla fine della catena, absent se
   *     non presente.
   */
  public static Optional<String> of(String remoteAddress, Set<String> trustedProxies) {
    if (Strings.isNullOrEmpty(remoteAddress)) {
      return Optional.absent();
    }
    final List<String> chain = Lists.reverse(COMMA_SPLITTER.splitToList(remoteAddress));
    for (String address : chain) {
      if (!trustedProxies.contains(address)) {
        return Optional.of(address);
      }
    }
    // Tutta la catena è composta da proxy fidati: si usa il più vicino al client.
    return chain.isEmpty() ? Optional.absent() : Optional.of(chain.get(chain.size() - 1));
  }

  /**
   * L'indirizzo IP attendibile del client senza proxy fidati noti (ultimo della catena).
   */
  public static Optional<String> of(String remoteAddress) {
    return of(remoteAddress, ImmutableSet.of());
  }

  /**
   * I reverse proxy fidati configurati in XForwardedSupport (vuoto se non impostato o "all").
   */
  public static Set<String> trustedProxies() {
    final String value = Play.configuration.getProperty(X_FORWARDED_SUPPORT);
    if (Strings.isNullOrEmpty(value) || "all".equalsIgnoreCase(value.trim())) {
      return ImmutableSet.of();
    }
    return ImmutableSet.copyOf(PROXY_SPLITTER.split(value));
  }

  /**
   * L'indirizzo IP attendibile del client della richiesta HTTP corrente.
   *
   * @return l'indirizzo IP del client, absent se non c'è una richiesta corrente.
   */
  public static Optional<String> current() {
    final Http.Request request = Http.Request.current();
    return request == null ? Optional.absent() : of(request.remoteAddress, trustedProxies());
  }
}
