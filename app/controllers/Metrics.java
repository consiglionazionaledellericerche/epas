package controllers;

import common.metrics.MetricsProbe;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Accesso alle metriche in stile Prometheus.
 *
 * @author marco
 *
 */
@With(MetricsProbe.class)
public class Metrics extends Controller {

  @Inject
  static PrometheusMeterRegistry registry;

  public static void prometheus() {
    renderText(registry.scrape());
  }
}
