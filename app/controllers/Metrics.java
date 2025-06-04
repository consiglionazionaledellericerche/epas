/*
 * Copyright (C) 2025  Consiglio Nazionale delle Ricerche
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

import common.metrics.MetricsProbe;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Accesso alle metriche in stile Prometheus.
 *
 * @author Marco Andreini
 # @see https://github.com/besmartbeopen/play1-base
 */
@With(MetricsProbe.class)
public class Metrics extends Controller {

  @Inject
  static PrometheusMeterRegistry registry;

  public static void prometheus() {
    renderText(registry.scrape());
  }
}
