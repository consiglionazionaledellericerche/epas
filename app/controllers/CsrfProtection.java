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

import controllers.Resecure.NoCheck;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import play.Play;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Util;

/**
 * Verifica del token anti-CSRF sulle richieste autenticate dalla sessione web.
 *
 * <p>Il comportamento dipende dal parametro di configurazione csrf.mode:
 * <ul>
 *   <li>off: nessuna verifica;</li>
 *   <li>report (default): le richieste non valide sono solo segnalate nel log;</li>
 *   <li>enforce: le richieste non valide sono rifiutate con 403.</li>
 * </ul>
 * Sono verificate:
 * <ul>
 *   <li>le richieste diverse da GET/HEAD/OPTIONS, che devono avere il token nell'header
 *     X-CSRF-Token (chiamate AJAX) o, per i form, nel parametro authenticityToken
 *     (inserito automaticamente dal tag #{form});</li>
 *   <li>tutte le richieste che il browser dichiara provenienti da un altro sito e che non
 *     sono navigazioni (header Fetch Metadata), per esempio un'immagine in una pagina
 *     esterna che punta ad un'azione di ePAS.</li>
 * </ul>
 * Sono escluse le azioni annotate con NoCheck e le richieste dei client REST, cioè quelle con
 * header Authorization che il browser non dichiara provenienti da un altro sito (header
 * Sec-Fetch-Site e Origin, che i client REST non inviano).</p>
 */
@Slf4j
public class CsrfProtection extends Controller {

  static final String CSRF_MODE = "csrf.mode";
  // usata se csrf.mode non è impostato o non è valido
  static final CsrfMode DEFAULT_MODE = CsrfMode.REPORT;
  private static final String CSRF_PARAM = "authenticityToken";
  // in play gli header della richiesta hanno il nome in minuscolo
  private static final String CSRF_HEADER = "x-csrf-token";
  private static final String AUTHORIZATION_HEADER = "authorization";
  /**
   * Header della risposta 403 della verifica, con il codice del motivo: il javascript lo usa
   * per riconoscere il rifiuto e spiegare all'utente cosa è successo.
   */
  static final String REJECTED_HEADER = "X-CSRF-Rejected";
  /** Argomento della richiesta che segnala alla pagina 403 il rifiuto della verifica. */
  static final String REJECTED_ARG = "csrfRejected";
  /** Chiave del token nella sessione di Play (Scope.Session.AT_KEY, non pubblica). */
  private static final String SESSION_TOKEN_KEY = "___AT";
  private static final String FETCH_SITE_HEADER = "sec-fetch-site";
  private static final String FETCH_MODE_HEADER = "sec-fetch-mode";
  private static final String ORIGIN_HEADER = "origin";
  private static final String REFERER_HEADER = "referer";
  private static final String USER_AGENT_HEADER = "user-agent";
  private static final String USERNAME = "username";
  private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
  private static final Set<String> FORM_CONTENT_TYPES =
      Set.of("application/x-www-form-urlencoded", "multipart/form-data");

  @Before(priority = 2, unless = {"login", "authenticate", "logout", "oauthLogin",
      "oauthLogout", "oauthCallback"})
  static void checkCsrf() {
    final CsrfMode mode = csrfMode();
    if (mode == CsrfMode.OFF) {
      return;
    }
    CsrfProblem problem;
    try {
      problem = csrfProblem();
    } catch (Exception ex) {
      log.error("Errore durante la verifica del token CSRF per {} {}",
          request.method, request.path, ex);
      // in enforce una verifica non completata non lascia passare la richiesta
      problem = mode == CsrfMode.ENFORCE ? CsrfProblem.CHECK_ERROR : null;
    }
    if (problem == null) {
      return;
    }
    log.warn("Richiesta CSRF non valida ({}): method={}, path={}, action={}, user={}, "
        + "referer={}, user-agent={}", problem.description, request.method, request.path,
        request.action, session.get(USERNAME), headerValue(REFERER_HEADER),
        headerValue(USER_AGENT_HEADER));
    if (mode == CsrfMode.ENFORCE) {
      response.setHeader(REJECTED_HEADER, problem.code);
      request.args.put(REJECTED_ARG, problem);
      forbidden("Richiesta non valida, ricaricare la pagina e riprovare.");
    }
  }

  /**
   * Il motivo per cui la richiesta non supera la verifica, null se la supera.
   */
  @Util
  static CsrfProblem csrfProblem() {
    if (getActionAnnotation(NoCheck.class) != null
        || getControllerInheritedAnnotation(NoCheck.class) != null
        || isRestClient()) {
      return null;
    }
    if ("cross-site".equals(headerValue(FETCH_SITE_HEADER))
        && !"navigate".equals(headerValue(FETCH_MODE_HEADER))) {
      return CsrfProblem.CROSS_SITE;
    }
    if (SAFE_METHODS.contains(request.method)) {
      return null;
    }
    return tokenProblem();
  }

  /**
   * Client REST: header Authorization in una richiesta che non arriva da un altro sito. Un
   * browser può inviare automaticamente le credenziali Basic in cache anche da una pagina
   * esterna, ma in quel caso dichiara la provenienza con Sec-Fetch-Site o Origin; i client
   * REST non inviano questi header, anche quando conservano il cookie di sessione.
   */
  @Util
  private static boolean isRestClient() {
    return request.headers.containsKey(AUTHORIZATION_HEADER) && !isFromAnotherSite();
  }

  @Util
  private static boolean isFromAnotherSite() {
    final String site = headerValue(FETCH_SITE_HEADER);
    if (site != null) {
      return !"same-origin".equals(site) && !"none".equals(site);
    }
    final String origin = headerValue(ORIGIN_HEADER);
    return origin != null && !origin.equals(request.getBase());
  }

  /**
   * Il problema del token inviato, null se è quello della sessione.
   *
   * <p>Il token è letto dall'header X-CSRF-Token e, solo per i form, dal parametro
   * authenticityToken: leggere i parametri di una richiesta json consumerebbe il corpo che
   * l'azione deve ancora leggere.</p>
   *
   * <p>Un token diverso da quello della sessione è di solito quello di una pagina aperta in
   * una sessione precedente: con il login locale la pagina di login crea il token prima
   * dell'autenticazione, quindi dopo un logout e un nuovo login la sessione ha già un token
   * diverso (INVALID_TOKEN). Solo una sessione appena creata nella stessa richiesta, per
   * esempio dal login automatico OAuth, non ha ancora un token (NEW_SESSION). Una sessione
   * locale scaduta non arriva qui: Resecure.checkAccess rimanda prima al login.</p>
   */
  @Util
  private static CsrfProblem tokenProblem() {
    String token = headerValue(CSRF_HEADER);
    if (token == null && isFormRequest()) {
      token = params.get(CSRF_PARAM);
    }
    if (token == null || token.isEmpty()) {
      return CsrfProblem.MISSING_TOKEN;
    }
    // da controllare prima di getAuthenticityToken(), che crea il token se manca
    final boolean newSession = !session.contains(SESSION_TOKEN_KEY);
    if (MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
        session.getAuthenticityToken().getBytes(StandardCharsets.UTF_8))) {
      return null;
    }
    return newSession ? CsrfProblem.NEW_SESSION : CsrfProblem.INVALID_TOKEN;
  }

  @Util
  private static boolean isFormRequest() {
    final String contentType = request.contentType;
    return contentType != null && FORM_CONTENT_TYPES.stream().anyMatch(contentType::startsWith);
  }

  @Util
  private static String headerValue(String name) {
    val header = request.headers.get(name);
    return header != null ? header.value() : null;
  }

  @Util
  static CsrfMode csrfMode() {
    val value = Play.configuration.getProperty(CSRF_MODE, DEFAULT_MODE.name()).trim();
    for (CsrfMode mode : CsrfMode.values()) {
      if (mode.name().equalsIgnoreCase(value)) {
        return mode;
      }
    }
    log.warn("Valore '{}' non valido per {}, uso {}", value, CSRF_MODE,
        DEFAULT_MODE.name().toLowerCase());
    return DEFAULT_MODE;
  }

  /**
   * Motivo per cui una richiesta non supera la verifica: la descrizione è per il log, il
   * codice per l'header X-CSRF-Rejected della risposta. Pubblico perché la pagina 403 legge
   * isStalePage().
   */
  public enum CsrfProblem {
    CROSS_SITE("cross-site", "richiesta da un altro sito", false),
    MISSING_TOKEN("missing-token", "token assente", false),
    NEW_SESSION("new-session",
        "token di una sessione precedente, la sessione corrente è appena stata creata", true),
    INVALID_TOKEN("invalid-token", "token diverso da quello della sessione, di solito di una "
        + "pagina aperta prima di un nuovo login", true),
    CHECK_ERROR("check-error", "errore nella verifica", false);

    final String code;
    final String description;
    private final boolean stalePage;

    CsrfProblem(String code, String description, boolean stalePage) {
      this.code = code;
      this.description = description;
      this.stalePage = stalePage;
    }

    /**
     * Il token è quello di una pagina aperta in un'altra sessione: ricaricare la pagina
     * risolve. Per gli altri motivi (token assente, richiesta da un altro sito, errore)
     * ricaricare non basta.
     */
    public boolean isStalePage() {
      return stalePage;
    }
  }

  /**
   * Modalità di verifica del token anti-CSRF.
   */
  enum CsrfMode {
    OFF, REPORT, ENFORCE
  }
}
