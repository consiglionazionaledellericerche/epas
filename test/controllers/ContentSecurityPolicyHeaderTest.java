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

import common.security.ContentSecurityPolicy;
import io.micrometer.core.instrument.Counter;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.db.jpa.JPA;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

/**
 * Verifica dell'header della Content Security Policy e dell'azione che riceve le
 * segnalazioni dei browser.
 */
public class ContentSecurityPolicyHeaderTest extends FunctionalTest {

  private static final String REPORT_ONLY = "Content-Security-Policy-Report-Only";
  private static final String ENFORCE = "Content-Security-Policy";

  private String savedMode;

  @Before
  public void saveMode() {
    clearCookies();
    savedMode = Play.configuration.getProperty(ContentSecurityPolicy.CSP_MODE);
  }

  /**
   * Ripristina la modalità configurata.
   */
  @After
  public void restoreMode() {
    if (savedMode == null) {
      Play.configuration.remove(ContentSecurityPolicy.CSP_MODE);
    } else {
      Play.configuration.setProperty(ContentSecurityPolicy.CSP_MODE, savedMode);
    }
  }

  @Test
  public void loginPageHasReportOnlyHeader() {
    Play.configuration.setProperty(ContentSecurityPolicy.CSP_MODE, "report");
    final Response response = GET("/login");
    assertIsOk(response);
    assertEquals(ContentSecurityPolicy.headerValue(), response.getHeader(REPORT_ONLY));
    assertNull(response.getHeader(ENFORCE));
  }

  @Test
  public void enforceUsesBlockingHeader() {
    Play.configuration.setProperty(ContentSecurityPolicy.CSP_MODE, "enforce");
    final Response response = GET("/login");
    assertEquals(ContentSecurityPolicy.headerValue(), response.getHeader(ENFORCE));
    assertNull(response.getHeader(REPORT_ONLY));
  }

  @Test
  public void offSendsNoHeader() {
    Play.configuration.setProperty(ContentSecurityPolicy.CSP_MODE, "off");
    final Response response = GET("/login");
    assertNull(response.getHeader(ENFORCE));
    assertNull(response.getHeader(REPORT_ONLY));
  }

  @Test
  public void reportIsAcceptedWithoutSession() {
    final Response response = POST("/csp/report", "application/csp-report",
        "{\"csp-report\":{\"document-uri\":\"http://localhost/login\","
            + "\"effective-directive\":\"script-src-elem\",\"blocked-uri\":\"inline\"}}");
    assertStatus(204, response);
  }

  @Test
  public void invalidOrHugeReportIsIgnored() {
    assertStatus(204, POST("/csp/report", "application/csp-report", "non è json"));
    final StringBuilder huge = new StringBuilder("{\"csp-report\":{\"blocked-uri\":\"");
    for (int i = 0; i < 100_000; i++) {
      huge.append('a');
    }
    huge.append("\"}}");
    assertStatus(204, POST("/csp/report", "application/csp-report", huge.toString()));
  }

  private static double reports(CspReports.Outcome outcome) {
    final Counter counter = CspReports.registry.find(CspReports.METRIC)
        .tag("outcome", outcome.tag).counter();
    return counter == null ? 0 : counter.count();
  }

  @Test
  public void reportOpensNoTransactionAndIsCounted() {
    final Statistics statistics = ((Session) JPA.em().getDelegate()).getSessionFactory()
        .getStatistics();
    final boolean enabled = statistics.isStatisticsEnabled();
    statistics.setStatisticsEnabled(true);
    try {
      final double invalid = reports(CspReports.Outcome.INVALID);
      final long transactions = statistics.getTransactionCount();
      assertStatus(204, POST("/csp/report", "application/csp-report", "non è json"));
      assertEquals(transactions, statistics.getTransactionCount());
      assertEquals(invalid + 1, reports(CspReports.Outcome.INVALID), 0);
      // controprova: una pagina dell'applicazione apre una transazione
      GET("/login");
      assertTrue(statistics.getTransactionCount() > transactions);
    } finally {
      statistics.setStatisticsEnabled(enabled);
    }
  }

  @Test
  public void sameViolationForAnotherPersonIsDuplicate() throws InterruptedException {
    final String page = "https://epas.example.org/test-" + System.nanoTime() + "/";
    // il limite di una riga al secondo accumula un permesso dopo un secondo di inattività
    Thread.sleep(1100);
    assertEquals(CspReports.Outcome.LOGGED, CspReports.handle(
        ContentSecurityPolicy.parseReport(violation(page + "12"))));
    assertEquals(CspReports.Outcome.DUPLICATE, CspReports.handle(
        ContentSecurityPolicy.parseReport(violation(page + "34"))));
  }

  private static String violation(String document) {
    return "{\"csp-report\":{\"document-uri\":\"" + document + "\","
        + "\"effective-directive\":\"script-src-elem\",\"blocked-uri\":\"inline\"}}";
  }
}
