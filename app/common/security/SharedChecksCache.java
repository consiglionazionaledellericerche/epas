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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import models.User;
import models.UsersRolesOffices;
import play.mvc.Http;

/**
 * Esiti dei controlli dei permessi senza target (es. le voci dei menu), condivisi tra le
 * richieste dello stesso utente per un tempo limitato.
 *
 * <p>Ogni controllo crea una sessione Drools e prende il lock della rulebase, condiviso da tutte
 * le richieste: una pagina con il layout principale ne esegue decine, quasi tutti senza target e
 * con lo stesso esito per lo stesso utente. La chiave contiene l'utente, i suoi ruoli di sistema
 * e i suoi ruoli per sede, quindi un cambio di ruoli vale subito.</p>
 *
 * <p>La chiave contiene anche una generazione, che {@link #invalidateAll()} incrementa: gli esiti
 * calcolati prima diventano irraggiungibili. La invalidano il salvataggio dei dati letti dalle
 * regole (persona, qualifica, sede, configurazioni, turni e turnisti, reperibilità, impostazioni
 * generali, vedi {@link PermissionsDataListener}) e il ricaricamento delle regole.</p>
 *
 * <p>Ogni richiesta usa la generazione letta al suo inizio ({@link #beginInvocation()}), prima
 * che apra la transazione e carichi i dati: una richiesta che ha già letto i dati prima di una
 * modifica può calcolare esiti superati, che finiscono sotto la generazione vecchia e non vengono
 * riusati. Leggerla al momento del controllo non basta: gli oggetti già caricati dalla richiesta
 * restano quelli precedenti al commit della modifica.</p>
 *
 * <p>Per le richieste HTTP la generazione è conservata anche negli argomenti della richiesta:
 * una richiesta sospesa con {@code await} riprende con una nuova invocazione, ma con gli stessi
 * oggetti già caricati (es. l'utente corrente in {@code request.args}), quindi deve continuare a
 * usare la generazione letta al suo inizio.</p>
 *
 * <p>Una richiesta che modifica dati letti dalle regole non usa né condivide esiti fino alla
 * fine dell'invocazione ({@link #hasPendingChanges()}): prima del commit i suoi esiti dipendono
 * da dati che le altre richieste non vedono ancora. Allo stesso modo non condivide esiti
 * finché ha dati delle regole modificati in memoria e non salvati
 * ({@link PermissionsDataListener#hasUnsavedChanges()}).</p>
 *
 * <p>L'invalidazione vale per la sola JVM: con più istanze dell'applicazione le altre vedono le
 * modifiche entro la durata della cache. I controlli con un target non passano da qui.</p>
 */
@Slf4j
public class SharedChecksCache {

  static final String TTL_SECONDS = "security.checks.cacheSeconds";
  static final int DEFAULT_TTL_SECONDS = 60;
  private static final int MAX_SIZE = 50_000;

  /**
   * Generazione corrente degli esiti, condivisa da tutte le istanze della cache.
   */
  private static final AtomicLong GENERATION = new AtomicLong();

  /**
   * Generazione letta all'inizio dell'invocazione (richiesta o job) in corso sul thread.
   */
  private static final ThreadLocal<Long> INVOCATION_GENERATION = new ThreadLocal<>();

  /**
   * Vero se l'invocazione in corso ha modificato, senza ancora committarli, dati letti dalle
   * regole.
   */
  private static final ThreadLocal<Boolean> PENDING_CHANGES = new ThreadLocal<>();

  /**
   * Chiave di {@code request.args} con la generazione letta all'inizio della richiesta HTTP.
   */
  static final String REQUEST_GENERATION = "sharedChecksGeneration";

  private final long ttlSeconds;
  private final Cache<String, Boolean> results;
  /**
   * Ultima generazione vista da questa istanza: quando cambia gli esiti vecchi vengono rimossi.
   */
  private volatile long seenGeneration = GENERATION.get();

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

  /**
   * La durata in secondi letta dal valore della configurazione: se manca o non è un numero
   * intero si usa {@link #DEFAULT_TTL_SECONDS}, così un valore errato non impedisce i controlli
   * dei permessi (e quindi tutte le richieste).
   */
  public static long ttlSeconds(String value) {
    if (value == null || value.trim().isEmpty()) {
      return DEFAULT_TTL_SECONDS;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException ex) {
      log.warn("Valore non valido per {}: \"{}\" (serve un numero di secondi), uso {}",
          TTL_SECONDS, value, DEFAULT_TTL_SECONDS);
      return DEFAULT_TTL_SECONDS;
    }
  }

  /**
   * La generazione corrente.
   */
  public static long generation() {
    return GENERATION.get();
  }

  /**
   * Annota la generazione corrente all'inizio di un'invocazione, prima che apra la transazione
   * e carichi dati. Se l'invocazione riprende una richiesta HTTP sospesa con {@code await},
   * riusa la generazione letta all'inizio della richiesta.
   */
  public static void beginInvocation() {
    PENDING_CHANGES.remove();
    final Http.Request request = Http.Request.current();
    if (request == null) {
      INVOCATION_GENERATION.set(GENERATION.get());
      return;
    }
    final Object started = request.args.get(REQUEST_GENERATION);
    if (started instanceof Long) {
      INVOCATION_GENERATION.set((Long) started);
    } else {
      final long generation = GENERATION.get();
      request.args.put(REQUEST_GENERATION, generation);
      INVOCATION_GENERATION.set(generation);
    }
  }

  /**
   * Dimentica la generazione dell'invocazione conclusa.
   */
  public static void endInvocation() {
    INVOCATION_GENERATION.remove();
    PENDING_CHANGES.remove();
  }

  /**
   * Annota che l'invocazione in corso ha modificato dati letti dalle regole, non ancora
   * committati.
   */
  static void markPendingChanges() {
    PENDING_CHANGES.set(Boolean.TRUE);
  }

  /**
   * Vero se l'invocazione in corso ha modificato dati letti dalle regole: da quel momento i suoi
   * controlli non usano gli esiti condivisi, calcolati con i dati precedenti, e non condividono
   * i propri, calcolati con dati che le altre richieste non vedono ancora (e che un rollback
   * può annullare).
   */
  public static boolean hasPendingChanges() {
    return Boolean.TRUE.equals(PENDING_CHANGES.get());
  }

  /**
   * La generazione da passare a {@link #get} e {@link #put}: quella letta all'inizio
   * dell'invocazione in corso, o la corrente fuori da un'invocazione.
   */
  public static long invocationGeneration() {
    final Long generation = INVOCATION_GENERATION.get();
    return generation != null ? generation : GENERATION.get();
  }

  /**
   * Rende irraggiungibili tutti gli esiti già calcolati, in tutte le istanze.
   */
  public static void invalidateAll() {
    log.trace("Invalidati gli esiti condivisi dei controlli dei permessi");
    GENERATION.incrementAndGet();
  }

  public boolean isEnabled() {
    return ttlSeconds > 0;
  }

  /**
   * L'esito già calcolato per l'utente e l'azione nella generazione indicata, null se assente o
   * se la cache è disattivata.
   */
  public Boolean get(long generation, User user, String action) {
    if (!isEnabled() || user == null) {
      return null;
    }
    dropStaleResults();
    return results.getIfPresent(key(generation, user, action));
  }

  /**
   * Conserva l'esito per l'utente e l'azione, calcolato nella generazione indicata.
   */
  public void put(long generation, User user, String action, boolean granted) {
    if (isEnabled() && user != null && generation == GENERATION.get()) {
      results.put(key(generation, user, action), granted);
    }
  }

  /**
   * Libera la memoria degli esiti delle generazioni precedenti, già irraggiungibili.
   */
  private void dropStaleResults() {
    final long current = GENERATION.get();
    if (current != seenGeneration) {
      seenGeneration = current;
      results.invalidateAll();
    }
  }

  /**
   * Chiave dell'esito: generazione, utente, ruoli di sistema, ruoli per sede e azione.
   */
  @VisibleForTesting
  static String key(long generation, User user, String action) {
    final String accountRoles = user.getRoles().stream()
        .map(Enum::name).sorted().collect(Collectors.joining(","));
    final String officeRoles = user.getUsersRolesOffices().stream()
        .map(SharedChecksCache::officeRole).sorted().collect(Collectors.joining(","));
    return generation + "|" + user.getId() + "|" + accountRoles + "|" + officeRoles + "|"
        + action;
  }

  private static String officeRole(UsersRolesOffices uro) {
    return Objects.toString(uro.getRole() == null ? null : uro.getRole().getId()) + "@"
        + Objects.toString(uro.getOffice() == null ? null : uro.getOffice().getId());
  }
}
