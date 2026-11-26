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

import common.security.ContentSecurityPolicy.Mode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.test.UnitTest;

/**
 * Verifica della configurazione della Content Security Policy e della lettura delle
 * segnalazioni dei browser.
 */
public class ContentSecurityPolicyTest extends UnitTest {

  private String savedMode;
  private String savedPolicy;

  /**
   * Salva la configurazione corrente e parte da quella predefinita.
   */
  @Before
  public void saveConfiguration() {
    savedMode = Play.configuration.getProperty(ContentSecurityPolicy.CSP_MODE);
    savedPolicy = Play.configuration.getProperty(ContentSecurityPolicy.CSP_POLICY);
    Play.configuration.remove(ContentSecurityPolicy.CSP_MODE);
    Play.configuration.remove(ContentSecurityPolicy.CSP_POLICY);
  }

  /**
   * Ripristina la configurazione.
   */
  @After
  public void restoreConfiguration() {
    restore(ContentSecurityPolicy.CSP_MODE, savedMode);
    restore(ContentSecurityPolicy.CSP_POLICY, savedPolicy);
  }

  private static void restore(String key, String value) {
    if (value == null) {
      Play.configuration.remove(key);
    } else {
      Play.configuration.setProperty(key, value);
    }
  }

  @Test
  public void modeDefaultsToReport() {
    assertEquals(Mode.REPORT, ContentSecurityPolicy.mode());
    Play.configuration.setProperty(ContentSecurityPolicy.CSP_MODE, "non-valido");
    assertEquals(Mode.REPORT, ContentSecurityPolicy.mode());
  }

  @Test
  public void modeIsReadFromConfiguration() {
    Play.configuration.setProperty(ContentSecurityPolicy.CSP_MODE, " Enforce ");
    assertEquals(Mode.ENFORCE, ContentSecurityPolicy.mode());
    Play.configuration.setProperty(ContentSecurityPolicy.CSP_MODE, "off");
    assertEquals(Mode.OFF, ContentSecurityPolicy.mode());
  }

  @Test
  public void defaultPolicyForbidsInlineScripts() {
    final String header = ContentSecurityPolicy.headerValue();
    assertTrue(header.startsWith("default-src 'self'; script-src 'self' https://"));
    final String scriptSrc = header.substring(header.indexOf("script-src"),
        header.indexOf(";", header.indexOf("script-src")));
    assertFalse(scriptSrc.contains("'unsafe-inline'"));
    assertFalse(scriptSrc.contains("'unsafe-eval'"));
    assertTrue(header.contains("object-src 'none'"));
    assertTrue(header.endsWith("; report-uri " + Play.ctxPath + "/csp/report"));
  }

  @Test
  public void policyCanBeReplaced() {
    Play.configuration.setProperty(ContentSecurityPolicy.CSP_POLICY, "default-src 'none' ; ");
    assertEquals("default-src 'none'; report-uri " + Play.ctxPath + "/csp/report",
        ContentSecurityPolicy.headerValue());
  }

  @Test
  public void reportIsSummarizedWithoutQueryString() {
    final String body = "{\"csp-report\":{\"document-uri\":\"https://epas.example.org/"
        + "stampings/personStamping?personId=42&year=2026\",\"violated-directive\":"
        + "\"script-src-elem\",\"effective-directive\":\"script-src-elem\",\"blocked-uri\":"
        + "\"inline\",\"line-number\":12,\"source-file\":\"https://epas.example.org/x?y=1#z\","
        + "\"original-policy\":\"default-src 'self'\"}}";
    assertEquals("directive=script-src-elem, blocked=inline, "
        + "document=https://epas.example.org/stampings/personStamping, "
        + "source=https://epas.example.org/x:12", ContentSecurityPolicy.describeReport(body));
  }

  @Test
  public void reportFieldsAreCleanedAndTruncated() {
    final StringBuilder longUri = new StringBuilder("https://evil.example.org/");
    for (int i = 0; i < 500; i++) {
      longUri.append('a');
    }
    final String description = ContentSecurityPolicy.describeReport(
        "{\"csp-report\":{\"violated-directive\":\"img-src\\r\\nRiga falsa\","
            + "\"blocked-uri\":\"" + longUri + "\"}}");
    assertNotNull(description);
    assertFalse(description.contains("\n"));
    assertFalse(description.contains("\r"));
    assertTrue(description.startsWith("directive=img-srcRiga falsa, blocked=https://evil"));
    assertTrue(description.length() < 300);
  }

  @Test
  public void invalidReportsAreIgnored() {
    assertNull(ContentSecurityPolicy.describeReport(null));
    assertNull(ContentSecurityPolicy.describeReport(""));
    assertNull(ContentSecurityPolicy.describeReport("non è json {"));
    assertNull(ContentSecurityPolicy.describeReport("[1,2]"));
    assertNull(ContentSecurityPolicy.describeReport("{\"csp-report\":\"testo\"}"));
    assertNull(ContentSecurityPolicy.describeReport("{\"csp-report\":{\"referrer\":\"x\"}}"));
    assertNull(ContentSecurityPolicy.describeReport("{\"csp-report\":{\"blocked-uri\":{}}}"));
  }
}
