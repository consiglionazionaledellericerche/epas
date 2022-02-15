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
import helpers.LogEnhancer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import models.User;
import play.Play;
import play.libs.OAuth2;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.Util;
import play.mvc.With;

/**
 * Contiene metodi per l'attivazione dei controlli sui permessi per le richieste ai controller.
 *
 * @author Marco Andreini
 */
@Slf4j
@With({RequestInit.class, LogEnhancer.class, Metrics.class})
public class Resecure extends Controller {

  private static final String REALM = "E-PAS";

  private static final String USERNAME = "username";
  private static final String URL = "url";

  //come le chiavi utilizzate in play
  private static final String KEY = "___";
  static final String ACCESS_TOKEN = KEY + "access_token";
  static final String REFRESH_TOKEN = KEY + "refresh_token";
  static final String ID_TOKEN = KEY + "id_token";
  static final String STATE = KEY + "oauth_state";

  @Inject
  static SecurityRules rules;
  @Inject
  static OpenIdConnectClient openIdConnectClient;
  @Inject
  static SecurityModule.SecurityLogin securityLogin;

  @Before(priority = 1, unless = {"login", "authenticate", "logout", "oauthLogin", "oauthLogout", "oauthCallback"})
  static void checkAccess() throws Throwable {
    if (getActionAnnotation(NoCheck.class) != null
        || getControllerInheritedAnnotation(NoCheck.class) != null) {
      return;
    }
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

  /**
   * This method returns the current connected username
   * @return
   */
  static String connected() {
    return session.get(USERNAME);
  }

  /**
   * Indicate if a user is currently connected
   * @return  true if the user is connected
   */
  static boolean isConnected() {
      return session.contains(USERNAME);
  }
  
  // ~~~ Utils
  @Util
  static void redirectToOriginalURL() {
    String url = flash.get(URL);
    if(url == null) {
      url = Play.ctxPath + "/";
    }
    redirect(url);
  }

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
  private static void setSessionUsernameIfAvailable() {
    try {
      // si verifica la presenza del jwt e si preleva il relativo username
      val jwtUsername = SecurityTokens.retrieveAndValidateJwtUsername();
      log.debug("JWT username = '{}'", jwtUsername.orElse(null));
      if (jwtUsername.isPresent() && (connected() == null)) {
        String username = jwtUsername.get(); 
        if (username.contains("@")) {
          username = username.substring(0, username.indexOf("@"));
        }
        // WARNING: lo username viene impostato solo se non c'è; questo per evitare di
        // sovrascrivere l'utente quando autenticato via JWT e in SUDO.
        session.put(USERNAME, username);
        log.info("Impostato nella sessione {} = {}", USERNAME, username);
      }
    } catch (SecurityTokens.InvalidUsername e) {
      // forza lo svuotamento per evitare accessi non graditi
      session.clear();
    }
  }

  @NoCheck
  public static void oauthCallback(String code, String state) {
    log.debug("Callback received code = {}, state = {}", code, state);
    OAuth2.Response oauthResponse = openIdConnectClient.retrieveAccessToken(code, state, session.get(STATE));
    if (oauthResponse == null) {
      error("Could not retrieve jwt");
    } else {
      log.debug("OAuth2 Response = {}", response);
      SecurityTokens.setJwtSession(oauthResponse);
      setSessionUsernameIfAvailable();
      redirectToOriginalURL();
    }
  }
  
  public static Optional<User> getCurrentUser() {
    return Security.getUser().toJavaUtil();
  }
}
