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

package controllers;

import common.oauth2.OpenIdConnectClient;
import common.security.SecurityModule;
import common.security.SecurityRules;
import dao.PersonDao;
import helpers.LogEnhancer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.User;
import play.Play;
import play.libs.OAuth2;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.Util;
import play.mvc.With;

/**
 * Contiene metodi per l'attivazione dei controlli sui permessi per le richieste
 * ai controller.
 *
 * @author Marco Andreini
 * @author Cristian Lucchesi
 */
@Slf4j
@With({RequestInit.class, LogEnhancer.class, Metrics.class})
public class Resecure extends Controller {

  private static final String REALM = "E-PAS";

  private static final String USERNAME = "username";
  public static final String OAUTH = "oauth";
  private static final String URL = "url";

  private static final String OAUTH_LOGIN = "oauth.login";
  
  //come le chiavi utilizzate in play
  private static final String KEY = "___";
  static final String REFRESH_TOKEN = KEY + "refresh_token";
  static final String ID_TOKEN = KEY + "id_token";
  static final String STATE = KEY + "oauth_state";

  private static final String CSRF_MODE = "csrf.mode";
  private static final String CSRF_PARAM = "authenticityToken";
  // in play gli header della richiesta hanno il nome in minuscolo
  private static final String CSRF_HEADER = "x-csrf-token";
  private static final String AUTHORIZATION_HEADER = "authorization";
  private static final String REFERER_HEADER = "referer";
  private static final String USER_AGENT_HEADER = "user-agent";
  private static final Set<String> CSRF_SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

  @Inject
  static SecurityRules rules;
  @Inject
  static OpenIdConnectClient openIdConnectClient;
  @Inject
  static SecurityModule.SecurityLogin securityLogin;
  @Inject
  static PersonDao personDao;

  @Before(priority = 1, unless = {"login", "authenticate", "logout", "oauthLogin", 
      "oauthLogout", "oauthCallback"})
  static void checkAccess() throws Throwable {
    //Se è annotato con NoCheck non si fanno controlli
    if (getActionAnnotation(NoCheck.class) != null
        || getControllerInheritedAnnotation(NoCheck.class) != null) {
      return;
    } else {
      //Cominciano i controlli

      if (oauthLoginEnabled()) {
        //Si verifica se nel token jwt ci sono informazioni per impostare
        //l'utente corrente in session
        setSessionUsernameIfAvailable();
      }

      //Se l'autenticazione è dichiarata di tipo BasicAuth allora se non c'è 
      //l'utente si ritorna un 404 con il REALM
      if (getActionAnnotation(BasicAuth.class) != null
          || getControllerInheritedAnnotation(BasicAuth.class) != null) {
        if (!Secure.Security.isConnected()) {
          unauthorized(REALM);
        }
      } else {
        Secure.checkAccess();
      }
      rules.checkIfPermitted();
    }
  }

  /**
   * Verifica del token anti-CSRF sulle richieste che possono modificare dati.
   *
   * <p>Il comportamento dipende dal parametro di configurazione csrf.mode:
   * <ul>
   *   <li>off (default): nessuna verifica;</li>
   *   <li>report: le richieste senza token valido sono solo segnalate nel log;</li>
   *   <li>enforce: le richieste senza token valido sono rifiutate.</li>
   * </ul>
   * Il token è letto dal parametro authenticityToken (inserito automaticamente dal
   * tag #{form}) oppure dall'header X-CSRF-Token (inviato dalle chiamate AJAX).
   * Sono escluse le richieste con header Authorization (client REST autenticati
   * ad ogni richiesta e non tramite il cookie di sessione) e le azioni annotate
   * con NoCheck.</p>
   */
  @Before(priority = 2, unless = {"login", "authenticate", "logout", "oauthLogin",
      "oauthLogout", "oauthCallback"})
  static void checkCsrf() {
    final CsrfMode mode = csrfMode();
    if (mode == CsrfMode.OFF) {
      return;
    }
    boolean valid;
    try {
      valid = !isCsrfCheckRequired() || isCsrfTokenValid();
    } catch (Exception ex) {
      // la verifica non deve mai bloccare le richieste per un errore inatteso
      log.error("Errore durante la verifica del token CSRF per {} {}",
          request.method, request.path, ex);
      return;
    }
    if (valid) {
      return;
    }
    log.warn("Token CSRF assente o non valido: method={}, path={}, action={}, user={}, "
        + "referer={}, user-agent={}", request.method, request.path, request.action,
        session.get(USERNAME), headerValue(REFERER_HEADER), headerValue(USER_AGENT_HEADER));
    if (mode == CsrfMode.ENFORCE) {
      forbidden("Richiesta non valida, ricaricare la pagina e riprovare.");
    }
  }

  @Util
  private static boolean isCsrfCheckRequired() {
    return !CSRF_SAFE_METHODS.contains(request.method)
        && getActionAnnotation(NoCheck.class) == null
        && getControllerInheritedAnnotation(NoCheck.class) == null
        && !request.headers.containsKey(AUTHORIZATION_HEADER);
  }

  @Util
  private static boolean isCsrfTokenValid() {
    String token = params.get(CSRF_PARAM);
    if (token == null) {
      token = headerValue(CSRF_HEADER);
    }
    return token != null && MessageDigest.isEqual(
        token.getBytes(StandardCharsets.UTF_8),
        session.getAuthenticityToken().getBytes(StandardCharsets.UTF_8));
  }

  @Util
  private static String headerValue(String name) {
    val header = request.headers.get(name);
    return header != null ? header.value() : null;
  }

  @Util
  private static CsrfMode csrfMode() {
    val value = Play.configuration.getProperty(CSRF_MODE, "off").trim();
    for (CsrfMode mode : CsrfMode.values()) {
      if (mode.name().equalsIgnoreCase(value)) {
        return mode;
      }
    }
    log.warn("Valore '{}' non valido per {}, verifica CSRF disattivata", value, CSRF_MODE);
    return CsrfMode.OFF;
  }

  /**
   * Modalità di verifica del token anti-CSRF.
   */
  enum CsrfMode {
    OFF, REPORT, ENFORCE
  }


  /**
   * True se si può eseguire l'azione sull'istanza, false altrimenti.
   *
   * @param action l'azione da eseguire
   * @param instance l'oggetto su cui eseguirla
   * @return se è possibile eseguire l'azione action sull'istanza instance.
   */
  public static boolean check(String action, Object instance) {
    if (instance != null) {
      return session.contains("username") && rules.check(action, instance);
    } else {
      return session.contains("username") && rules.checkAction(action);
    }
  }

  /**
   * Con questo si evitano i controlli.
   *
   * @author Marco Andreini
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface NoCheck {
    //Empty
  }

  /**
   * Con questo si adotta soltanto la basicauth.
   *
   * @author Marco Andreini
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface BasicAuth {
    //Empty
  }


  static String connected() {
    return session.get(USERNAME);
  }

  /**
   * Indicate if a user is currently connected.
   *
   * @return  true if the user is connected
   */
  static boolean isConnected() {
    return session.contains(USERNAME);
  }

  // ~~~ Utils
  @Util
  static void redirectToOriginalURL() {
    String url = flash.get(URL);
    if (url == null) {
      url = Play.ctxPath + "/";
    }
    redirect(url);
  }

  /**
   * Utilizzata come callback dell'eventuale autenticazione OAuth.
   *
   * @throws Throwable eccezione.
   */
  public static void logout() throws Throwable {
    session.clear();
    flash.success("secure.logout");
    Secure.login();
  }


  
  /**
   * Logout oauth.
   *
   * @param data map with logout info 
   */
  @Util
  public static void oauthLogout(Map<String, String> data) {
    val idToken = data.get(ID_TOKEN);
    if (idToken == null) {
      error("no id-token available");
    }

    openIdConnectClient.logout(idToken, Router.getFullUrl("Resecure.logout"));
  }

  @NoCheck
  public static void oauthLogin() {
    flash.keep(URL);
    openIdConnectClient.retrieveVerificationCode(state -> session.put(STATE, state));
  }

  @Util
  private static Optional<String> setSessionUsernameIfAvailable() {
    //Il JWT viene controllato solo se non c'è già una sessione attiva;
    if (connected() != null) {
      return Optional.of(connected());
    }
    try {
      // si verifica la presenza del jwt e si preleva il relativo username
      val jwtUsername = SecurityTokens.retrieveAndValidateJwtUsername();
      log.debug("JWT username = '{}'", jwtUsername.orElse(null));

      if (jwtUsername.isPresent()) {
        String username = jwtUsername.get();
        val person = personDao.byEppn(username);
        if (person.isPresent()) {
          username = person.get().getUser().getUsername();
          session.put(USERNAME, username);
          session.put(OAUTH, true);
          log.info("Login OAuth per l'utente {}", username);
        } else {
          log.warn("Ricevuto dal JWT {} contenente un valore che non corrisponde "
              + "a nessun eppn presente in ePAS.", username);
        }
      }
      return jwtUsername;
    } catch (SecurityTokens.InvalidUsername e) {
      // forza lo svuotamento per evitare accessi non graditi
      log.warn("OAuth Login invalid username", e);
      session.clear();
      return Optional.empty();
    }
  }

  /**
   * Callback function on oauth connection.
   *
   * @param code the code 
   * @param state the state
   * @throws Throwable exception
   */
  @NoCheck
  public static void oauthCallback(String code, String state) throws Throwable {
    log.debug("Callback received code = {}, state = {}", code, state);
    OAuth2.Response oauthResponse = openIdConnectClient
        .retrieveAccessToken(code, state, session.get(STATE));
    if (oauthResponse == null) {
      error("Could not retrieve jwt");
    } else {
      log.debug("Ricevuta OAuth2 Response, status = {}", response.status);
      SecurityTokens.setJwtSession(oauthResponse);
      val jwtUsername = setSessionUsernameIfAvailable();
      if (!isConnected()) {
        flash.error("Ricevuto dall'authentication server lo username '%s' non associato "
            + "a nessun utente presente in ePAS",
            jwtUsername.orElse(""));
        Secure.login();
      }
      log.trace("oauthCallback riuscita, faccio il redirect all'url originale");
      redirectToOriginalURL();
    }
  }

  public static Optional<User> getCurrentUser() {
    return Security.getUser().toJavaUtil();
  }
  
  /**
   * Verifica se l'autenticazione oauth è abilitata.
   *
   * @return true se l'autenticazione oauth è abilitata in configurazione,
   *     false altrimenti.
   */
  public static boolean oauthLoginEnabled() {
    return "true".equalsIgnoreCase(
        Play.configuration.getProperty(OAUTH_LOGIN, "false"));
  }
}
