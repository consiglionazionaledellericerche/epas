package common.metrics;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import controllers.Security;
import injection.StaticInject;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import models.User;
import play.mvc.After;
import play.mvc.Before;
import play.mvc.Controller;

/**
 * Supporto alla sonda per le metriche sulle azioni dei controller.
 *
 * @author marco
 *
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

  @Before(priority=0)
  static void starting() {
    request.args.put(PROBE, Timer.start(registry));
  }

  @After(priority=Integer.MAX_VALUE)
  static void ending() {
    val sample = (Timer.Sample) request.args.get(PROBE);
    if (sample == null) {
      log.debug("no timer-probe on {}", request.action);
    } else {
      val t = sample.stop(Timer.builder("http.requests")
          .description(null)
          .tag("action", request.action)
          //.publishPercentileHistogram(true)
          .publishPercentiles(0.5, 0.95)
          .register(registry));
      if (minDurationCheck.test(t)) {
        val params = request.params.all().entrySet().stream()
            .filter(i -> !PARAMS_SKIP.contains(i.getKey()))
            .map(e -> e.getKey() + "=" + Joiner.on(',').join((String[]) e.getValue()))
            .collect(Collectors.joining(","));
        log.info("duration: {} ms, request {}({}) as {}", t / NANOS_TO_MS, request.action,
            params, Security.getUser().transform(User::toString).or("-"));
      }
    }
  }

  /**
   * @return il tempo in nanosecondi dall'inizio dell'esecuzione dell'azione.
   */
  public static long elapsedNanosec() {
    val sample = (Timer.Sample) request.args.get(PROBE);
    if (sample == null) {
      return -1;
    }
    try (val registry = new SimpleMeterRegistry()) {
      return sample.stop(registry.timer(PROBE));
    }
  }
}
