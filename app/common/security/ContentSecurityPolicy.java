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

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.regex.Pattern;
import play.Play;

/**
 * Content Security Policy delle pagine di ePAS.
 *
 * <p>Il comportamento dipende dal parametro di configurazione csp.mode:
 * <ul>
 *   <li>off: l'header non è inviato;</li>
 *   <li>report (default): la policy è inviata con l'header
 *     Content-Security-Policy-Report-Only: il browser non blocca nulla e segnala le violazioni
 *     a /csp/report, che le scrive nel log;</li>
 *   <li>enforce: la policy è inviata con l'header Content-Security-Policy e il browser blocca
 *     ciò che la viola, oltre a segnalarlo.</li>
 * </ul>
 * La policy predefinita può essere sostituita con il parametro csp.policy.</p>
 *
 * <p>La policy predefinita elenca i CDN da cui le pagine caricano script, fogli di stile e
 * font. Non ammette gli script inline né gli attributi onclick/onload, che in ePAS esistono
 * ancora: in modalità report le segnalazioni indicano le pagine da correggere prima di poter
 * passare ad enforce.</p>
 */
public final class ContentSecurityPolicy {

  /**
   * Modalità di invio della policy.
   */
  public enum Mode {
    OFF(null),
    REPORT("Content-Security-Policy-Report-Only"),
    ENFORCE("Content-Security-Policy");

    /** Nome dell'header con cui la policy è inviata, null se non è inviata. */
    public final String header;

    Mode(String header) {
      this.header = header;
    }
  }

  public static final String CSP_MODE = "csp.mode";
  public static final String CSP_POLICY = "csp.policy";
  // usata se csp.mode non è impostato o non è valido
  public static final Mode DEFAULT_MODE = Mode.REPORT;
  public static final String REPORT_PATH = "/csp/report";

  static final String DEFAULT_POLICY = String.join("; ",
      "default-src 'self'",
      "script-src 'self' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net"
          + " https://cdn.datatables.net https://code.jquery.com"
          + " https://maxcdn.bootstrapcdn.com https://oss.maxcdn.com https://www.gstatic.com"
          // le segnalazioni riportano l'inizio dello script inline o dell'attributo onclick
          + " 'report-sample'",
      // gli attributi style e gli stili impostati dalle librerie javascript sono ammessi
      "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net"
          + " https://cdn.datatables.net https://maxcdn.bootstrapcdn.com"
          + " https://fonts.googleapis.com https://www.gstatic.com",
      // le immagini usate dai fogli di stile delle librerie caricate dai CDN
      "img-src 'self' data: blob: https://cdnjs.cloudflare.com https://cdn.datatables.net",
      "font-src 'self' data: https://maxcdn.bootstrapcdn.com https://fonts.gstatic.com"
          + " https://cdnjs.cloudflare.com",
      "connect-src 'self'",
      "object-src 'none'",
      "base-uri 'self'",
      "form-action 'self'",
      "frame-ancestors 'self'");

  private static final int MAX_FIELD_LENGTH = 200;
  // i browser inviano al massimo 40 caratteri dello script
  private static final int MAX_SAMPLE_LENGTH = 40;
  // segmenti del percorso che identificano un oggetto: numeri e uuid
  private static final Pattern ID_SEGMENT = Pattern.compile("(?<=/)(?:\\d+"
      + "|\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12})(?=/|$)");
  private static final Pattern DIGITS = Pattern.compile("\\d+");

  private ContentSecurityPolicy() {
  }

  /**
   * La modalità configurata con csp.mode.
   */
  public static Mode mode() {
    final String value = Play.configuration.getProperty(CSP_MODE, "");
    for (Mode mode : Mode.values()) {
      if (mode.name().equalsIgnoreCase(value.trim())) {
        return mode;
      }
    }
    return DEFAULT_MODE;
  }

  /**
   * Il valore dell'header: la policy configurata seguita dall'indirizzo a cui il browser
   * invia le segnalazioni.
   */
  public static String headerValue() {
    String policy = Strings.nullToEmpty(Play.configuration.getProperty(CSP_POLICY)).trim();
    if (policy.isEmpty()) {
      policy = DEFAULT_POLICY;
    }
    while (policy.endsWith(";")) {
      policy = policy.substring(0, policy.length() - 1).trim();
    }
    return policy + "; report-uri " + Play.ctxPath + REPORT_PATH;
  }

  /**
   * Riassume in una riga la segnalazione di violazione inviata dal browser.
   *
   * @param body il corpo della richiesta (JSON con l'oggetto csp-report)
   * @return la descrizione della violazione, null se il corpo non è una segnalazione
   * @see #parseReport(String)
   */
  public static String describeReport(String body) {
    final Report report = parseReport(body);
    return report == null ? null : report.description();
  }

  /**
   * Legge la segnalazione di violazione inviata dal browser.
   *
   * <p>La segnalazione arriva da una richiesta non autenticata: sono letti solo i campi
   * che servono a correggere la pagina, senza query string (può contenere dati personali),
   * senza caratteri di controllo e con una lunghezza massima. Nell'estratto dello script le
   * cifre sono sostituite con #, perché possono essere id di persone.</p>
   *
   * @param body il corpo della richiesta (JSON con l'oggetto csp-report)
   * @return la segnalazione, null se il corpo non è una segnalazione
   */
  public static Report parseReport(String body) {
    final JsonObject report;
    try {
      final JsonElement root = new JsonParser().parse(Strings.nullToEmpty(body));
      if (!root.isJsonObject() || !root.getAsJsonObject().has("csp-report")
          || !root.getAsJsonObject().get("csp-report").isJsonObject()) {
        return null;
      }
      report = root.getAsJsonObject().getAsJsonObject("csp-report");
    } catch (RuntimeException ex) {
      return null;
    }
    String directive = field(report, "effective-directive");
    if (directive.isEmpty()) {
      directive = field(report, "violated-directive");
    }
    final String blocked = withoutQuery(field(report, "blocked-uri"));
    if (directive.isEmpty() && blocked.isEmpty()) {
      return null;
    }
    String sample = field(report, "script-sample");
    if (sample.length() > MAX_SAMPLE_LENGTH) {
      sample = sample.substring(0, MAX_SAMPLE_LENGTH);
    }
    return new Report(directive, blocked, withoutQuery(field(report, "document-uri")),
        withoutQuery(field(report, "source-file")), field(report, "line-number"),
        DIGITS.matcher(sample).replaceAll("#"));
  }

  /**
   * Una segnalazione di violazione, con i campi già ripuliti.
   */
  public static final class Report {

    private final String directive;
    private final String blocked;
    private final String document;
    private final String source;
    private final String line;
    private final String sample;

    Report(String directive, String blocked, String document, String source, String line,
        String sample) {
      this.directive = directive;
      this.blocked = blocked;
      this.document = document;
      this.source = source;
      this.line = line;
      this.sample = sample;
    }

    /**
     * La descrizione da scrivere nel log.
     */
    public String description() {
      final StringBuilder description = new StringBuilder(String.format(
          "directive=%s, blocked=%s, document=%s", directive, blocked, document));
      if (!source.isEmpty()) {
        description.append(", source=").append(source).append(':').append(line);
      }
      if (!sample.isEmpty()) {
        description.append(", sample=").append(sample);
      }
      return description.toString();
    }

    /**
     * La chiave con cui la violazione è scritta nel log una sola volta.
     *
     * <p>La stessa violazione arriva da pagine che differiscono solo per gli id nel
     * percorso (es. /stampings/personStamping/12345), che sono sostituiti con {id}. Per gli
     * script inline e gli attributi onclick il sorgente è la pagina stessa e il numero di
     * riga cambia con i dati mostrati: la chiave usa allora l'estratto dello script invece
     * della riga. Se il browser non invia l'estratto (la policy configurata non contiene
     * 'report-sample') resta la riga, altrimenti script diversi della stessa pagina
     * avrebbero la stessa chiave.</p>
     */
    public String dedupKey() {
      final boolean inline = source.isEmpty() || source.equals(document);
      final String position = inline && !sample.isEmpty() ? "" : withoutIds(source) + ":" + line;
      return String.join("|", directive, withoutIds(blocked), withoutIds(document), position,
          sample);
    }
  }

  static String withoutIds(String uri) {
    return ID_SEGMENT.matcher(uri).replaceAll("{id}");
  }

  private static String field(JsonObject report, String name) {
    final JsonElement element = report.get(name);
    if (element == null || !element.isJsonPrimitive()) {
      return "";
    }
    final String value = CharMatcher.javaIsoControl().removeFrom(element.getAsString()).trim();
    return value.length() > MAX_FIELD_LENGTH ? value.substring(0, MAX_FIELD_LENGTH) : value;
  }

  private static String withoutQuery(String uri) {
    for (int i = 0; i < uri.length(); i++) {
      if (uri.charAt(i) == '?' || uri.charAt(i) == '#') {
        return uri.substring(0, i);
      }
    }
    return uri;
  }
}
