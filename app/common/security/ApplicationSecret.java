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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

/**
 * Verifica dell'application.secret usato da Play per firmare il cookie di sessione.
 *
 * <p>Chi conosce il secret può firmare un cookie di sessione con qualunque username e
 * autenticarsi come quell'utente, quindi in produzione il secret deve essere specifico
 * dell'installazione e non uno di quelli pubblicati nel repository.</p>
 */
public final class ApplicationSecret {

  public static final int MIN_LENGTH = 32;

  /**
   * SHA-256 dei secret pubblicati nel repository e quindi non utilizzabili: il secret
   * presente in application.conf fino alla versione 2.38.0, l'esempio del docker-compose.yml
   * e il valore attuale di sviluppo e test di application.conf.
   */
  private static final ImmutableSet<String> PUBLISHED_SECRETS_SHA256 = ImmutableSet.of(
      "8722ff673451a3faa941a4429ff1be9258255debe913368f60dc5a0381571d35",
      "f6466f3fc83ac768311a6b7701e9b7e5af02c8ba0851490aabc2a3f627921098",
      "5aa4a55823bf3342ef1f724b736b51efe81f8d97efea7cca06760e41bc33f209");

  private ApplicationSecret() {
  }

  /**
   * Il motivo per cui il secret non è utilizzabile in produzione.
   *
   * @param secret il valore di application.secret
   * @return il motivo, oppure absent se il secret è utilizzabile
   */
  public static Optional<String> problem(String secret) {
    final String value = Strings.nullToEmpty(secret).trim();
    if (value.isEmpty()) {
      return Optional.of("application.secret non impostato");
    }
    if (PUBLISHED_SECRETS_SHA256.contains(
        Hashing.sha256().hashString(value, StandardCharsets.UTF_8).toString())) {
      return Optional.of("application.secret è uno dei valori pubblicati nel repository");
    }
    if (value.length() < MIN_LENGTH) {
      return Optional.of(String.format(
          "application.secret è troppo corto (%d caratteri, minimo %d)",
          value.length(), MIN_LENGTH));
    }
    return Optional.absent();
  }
}
