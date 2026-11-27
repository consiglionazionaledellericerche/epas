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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import common.security.ContentSecurityPolicy;
import common.security.ContentSecurityPolicy.Report;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import play.db.jpa.NoTransaction;
import play.mvc.Controller;
import play.mvc.Util;

/**
 * Riceve le segnalazioni di violazione della Content Security Policy inviate dai browser.
 *
 * <p>Il browser le invia da solo, anche dalla pagina di login e senza il token anti-CSRF:
 * l'azione non richiede quindi l'autenticazione. Per questo il corpo letto è limitato, ogni
 * violazione è scritta nel log una sola volta all'ora e il numero di righe al secondo è
 * limitato, così l'indirizzo non può essere usato per riempire il log.</p>
 *
 * <p>Ogni pagina con script inline genera più segnalazioni: l'azione non usa il database e
 * non apre una transazione, per non prendere una connessione dal pool. Non passa nemmeno da
 * MetricsProbe, che per le richieste lente legge l'utente dal database: il numero di
 * segnalazioni ricevute è nella metrica csp.reports, distinta per esito.</p>
 *
 * @see ContentSecurityPolicy
 */
@Slf4j
@NoTransaction
public class CspReports extends Controller {

  static final int MAX_BODY_BYTES = 16 * 1024;
  static final String METRIC = "csp.reports";

  /**
   * Esito di una segnalazione, usato come tag della metrica.
   */
  enum Outcome {
    // scritta nel log
    LOGGED,
    // già scritta nel log nell'ultima ora
    DUPLICATE,
    // non scritta per il limite di righe al secondo
    RATE_LIMITED,
    // il corpo non è una segnalazione
    INVALID;

    final String tag = name().toLowerCase();
  }

  @Inject
  static MeterRegistry registry;

  // violazioni già scritte nel log
  private static final Cache<String, Boolean> LOGGED = CacheBuilder.newBuilder()
      .maximumSize(2000).expireAfterWrite(1, TimeUnit.HOURS).build();
  // al massimo una riga di log al secondo, in media
  private static final RateLimiter LOG_RATE = RateLimiter.create(1);

  /**
   * Scrive nel log la violazione segnalata dal browser. Risponde sempre 204.
   */
  public static void report() {
    final Outcome outcome = handle(ContentSecurityPolicy.parseReport(readBody()));
    registry.counter(METRIC, "outcome", outcome.tag).increment();
    response.status = 204;
    renderText("");
  }

  @Util
  static Outcome handle(Report report) {
    if (report == null) {
      return Outcome.INVALID;
    }
    final String key = report.dedupKey();
    if (LOGGED.getIfPresent(key) != null) {
      return Outcome.DUPLICATE;
    }
    if (!LOG_RATE.tryAcquire()) {
      return Outcome.RATE_LIMITED;
    }
    LOGGED.put(key, Boolean.TRUE);
    log.warn("Violazione della Content Security Policy ({}): {}",
        ContentSecurityPolicy.mode().name().toLowerCase(), report.description());
    return Outcome.LOGGED;
  }

  /**
   * I primi MAX_BODY_BYTES del corpo della richiesta: una segnalazione più lunga risulta
   * troncata, quindi non valida, ed è ignorata.
   */
  @Util
  static String readBody() {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final byte[] buffer = new byte[4096];
    try {
      int read;
      while (out.size() < MAX_BODY_BYTES && (read = request.body.read(buffer, 0,
          Math.min(buffer.length, MAX_BODY_BYTES - out.size()))) != -1) {
        out.write(buffer, 0, read);
      }
    } catch (IOException ex) {
      return "";
    }
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }
}
