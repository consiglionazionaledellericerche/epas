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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import models.User;
import models.UsersRolesOffices;

/**
 * Esiti dei controlli dei permessi senza target (es. le voci dei menu), condivisi tra le
 * richieste dello stesso utente per un tempo limitato.
 *
 * <p>Ogni controllo crea una sessione Drools e prende il lock della rulebase, condiviso da tutte
 * le richieste: una pagina con il layout principale ne esegue decine, quasi tutti senza target e
 * con lo stesso esito per lo stesso utente. La chiave contiene l'utente, i suoi ruoli di sistema
 * e i suoi ruoli per sede, quindi un cambio di ruoli vale subito. Le altre modifiche che le regole
 * considerano (configurazioni della sede o della persona, impostazioni generali) valgono entro la
 * durata della cache. I controlli con un target non passano da qui.</p>
 */
public class SharedChecksCache {

  static final String TTL_SECONDS = "security.checks.cacheSeconds";
  static final int DEFAULT_TTL_SECONDS = 60;
  private static final int MAX_SIZE = 50_000;

  private final long ttlSeconds;
  private final Cache<String, Boolean> results;

  /**
   * Cache con la durata indicata in secondi; con 0 o meno è disattivata.
   */
  public SharedChecksCache(long ttlSeconds) {
    this.ttlSeconds = ttlSeconds;
    this.results = CacheBuilder.newBuilder()
        .maximumSize(MAX_SIZE)
        .expireAfterWrite(Math.max(ttlSeconds, 1), TimeUnit.SECONDS)
        .build();
  }

  public boolean isEnabled() {
    return ttlSeconds > 0;
  }

  /**
   * L'esito già calcolato per l'utente e l'azione, null se assente o se la cache è disattivata.
   */
  public Boolean get(Object rules, User user, String action) {
    if (!isEnabled() || user == null) {
      return null;
    }
    return results.getIfPresent(key(rules, user, action));
  }

  /**
   * Conserva l'esito per l'utente e l'azione.
   */
  public void put(Object rules, User user, String action, boolean granted) {
    if (isEnabled() && user != null) {
      results.put(key(rules, user, action), granted);
    }
  }

  public void invalidateAll() {
    results.invalidateAll();
  }

  /**
   * Chiave dell'esito: regole caricate, utente, ruoli di sistema, ruoli per sede e azione.
   */
  @VisibleForTesting
  static String key(Object rules, User user, String action) {
    final String accountRoles = user.getRoles().stream()
        .map(Enum::name).sorted().collect(Collectors.joining(","));
    final String officeRoles = user.getUsersRolesOffices().stream()
        .map(SharedChecksCache::officeRole).sorted().collect(Collectors.joining(","));
    return System.identityHashCode(rules) + "|" + user.getId() + "|" + accountRoles + "|"
        + officeRoles + "|" + action;
  }

  private static String officeRole(UsersRolesOffices uro) {
    return Objects.toString(uro.getRole() == null ? null : uro.getRole().getId()) + "@"
        + Objects.toString(uro.getOffice() == null ? null : uro.getOffice().getId());
  }
}
