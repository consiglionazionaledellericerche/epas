/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

package common.metrics;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import common.injection.StaticInject;
import controllers.Security;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.User;
import play.mvc.After;
import play.mvc.Before;
import play.mvc.Controller;

/**
 * Supporto alla sonda per le metriche sulle azioni dei controller.
 *
 * @author Marco Andreini
 # @see https://github.com/besmartbeopen/play1-base
 */
@StaticInject
@Slf4j
public class MetricsProbe extends Controller {

  /**
   * Nome della chiave nei request.args per i Sample
   */
  private static final String PROBE = "_probe";
  private static final long NANOS_TO_MS = 1_000_000;

  /**
   * Elementi da saltare nella serializzazione dei parametri delle azioni.
   */
  private static final Set<String> PARAMS_SKIP = ImmutableSet.of("controller", "action", "body");

  @Inject
  static MeterRegistry registry;
  @Inject
  static IMinDurationCheck minDurationCheck;

  @Before(priority = 0)
  static void starting() {
    request.args.put(PROBE, Timer.start(registry));
  }

  @After(priority = Integer.MAX_VALUE)
  static void ending() {
    val sample = (Timer.Sample) request.args.get(PROBE);
    if (sample == null) {
      log.debug("no timer-probe on {}", request.action);
    } else {
      val t = sample.stop(Timer.builder("http.server.requests")
          .description(null)
          .tag("action", request.action)
          //.publishPercentileHistogram(true)
          .publishPercentiles(0.5, 0.95)
          .register(registry));
      if (minDurationCheck.test(t)) {
        val params = request.params.all().entrySet().stream()
            .filter(i -> !PARAMS_SKIP.contains(i.getKey()))
            .map(e -> e.getKey() + "=" + Joiner.on(',').skipNulls().join((String[]) e.getValue()))
            .collect(Collectors.joining(","));
        log.info("duration: {} ms, request {}({}) as {}", t / NANOS_TO_MS, request.action,
            params, Security.getUser().transform(User::toString).or("-"));
      }
    }
  }

  /**
   * Tempo di esecuzione in nanosecondi.
   *
   * @return il tempo in nanosecondi dall'inizio dell'esecuzione dell'azione.
   */
  public static long elapsedNanosec() {
    val sample = (Timer.Sample) request.args.get(PROBE);
    if (sample == null) {
      return -1;
    }
    val registry = new SimpleMeterRegistry();
    try {
      return sample.stop(registry.timer(PROBE));
    } finally {
      registry.close();
    }
  }
}