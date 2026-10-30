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

import com.google.common.base.Strings;
import common.metrics.MetricsProbe;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import play.Play;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

/**
 * Accesso alle metriche in stile Prometheus.
 *
 * @author Marco Andreini
 # @see https://github.com/besmartbeopen/play1-base
 */
@With(MetricsProbe.class)
public class Metrics extends Controller {

  static final String METRICS_TOKEN = "metrics.token";
  // Il nome dello schema non distingue maiuscole e minuscole (RFC 7235).
  private static final Pattern BEARER =
      Pattern.compile("\\s*bearer\\s+(\\S+)\\s*", Pattern.CASE_INSENSITIVE);

  @Inject
  static PrometheusMeterRegistry registry;

  /**
   * Metriche in formato Prometheus, accessibili solo con il token impostato in metrics.token.
   * Il controllo è qui e non in un @Before perché Resecure include questa classe con @With.
   */
  public static void prometheus() {
    if (!isTokenValid()) {
      forbidden();
    }
    renderText(registry.scrape());
  }

  private static boolean isTokenValid() {
    final String token = Play.configuration.getProperty(METRICS_TOKEN);
    final Http.Header authorization = request.headers.get("authorization");
    if (Strings.isNullOrEmpty(token) || authorization == null) {
      return false;
    }
    final Matcher matcher = BEARER.matcher(authorization.value());
    return matcher.matches() && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
        matcher.group(1).getBytes(StandardCharsets.UTF_8));
  }
}
