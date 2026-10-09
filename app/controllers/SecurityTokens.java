/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import common.oauth2.OpenIdConnectClient;
import common.security.JwtValidator;
import controllers.Resecure.NoCheck;
import dao.JwtTokenDao;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import java.security.Key;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.attestati.service.OauthToken;
import models.JwtToken;
import org.joda.time.LocalDateTime;
import play.Play;
import play.cache.Cache;
import play.libs.OAuth2;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.Scope;
import play.mvc.Scope.Session;
import play.mvc.Util;
import play.mvc.With;

/**
 * Integrazione essenziale con JWT per la generazione di token e la
 * successiva rilettura/verifica.
 *
 * @author Marco Andreini
 * @author Cristian Lucchesi
 */
@With(Resecure.class)
@Slf4j
public class SecurityTokens extends Controller {

  public static final String BEARER = "Bearer ";
  public static final String AUTHORIZATION = "authorization";
  private static final int DEFAULT_REFRESHED_TOKEN_EXPIRES_IN_SECONDS = 300;

  @Inject
  static OpenIdConnectClient openIdConnectClient;
  @Inject
  static JwtTokenDao jwtTokenDao;
  
  private static Key key() {
    String encodedKey = Play.configuration.getProperty("jwt.key");
    if (Strings.isNullOrEmpty(encodedKey)) {
      val key = MacProvider.generateKey();
      encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
      log.warn("the new jwt.key = \"{}\" must be saved into application.conf", encodedKey);
      Play.configuration.setProperty("jwt.key", encodedKey);
      return key;
    } else {
      val decodedKey = Base64.getDecoder().decode(encodedKey);
      return new SecretKeySpec(decodedKey, 0, decodedKey.length, 
          SignatureAlgorithm.HS512.getJcaName());
    }
  }

  /**
   * Risponde con un nuovo token attivo per 1 ora.
   */
  public static void token() {
    String username = Resecure.getCurrentUser().orElseThrow().getUsername();
    String token = Jwts.builder().setSubject(username)
        .setIssuer(Router.getBaseUrl())
        .setExpiration(Date.from(ZonedDateTime.now().plusHours(1).toInstant()))
        .signWith(key(), SignatureAlgorithm.HS512).compact();
    renderText(token);
  }

  /**
   * Check del token.
   *
   * @param token il token da verificare
   */
  @NoCheck
  public static void check(String token) {
    try {
      String user = JwtValidator.validateLocal(token, key(), Router.getBaseUrl()).getSubject();
      renderText("success " + user);
    } catch (JwtException | IllegalArgumentException e) {
      renderText("fail");
    }
  }

  /**
   * Classe di controllo della validità dell'username.
   *
   * @author dario
   *
   */
  public static class InvalidUsername extends Exception {
    private static final long serialVersionUID = 681032973379857729L;

    InvalidUsername(Exception e) {
      super(e);
    }
  }

  /**
   * Ritorna e valida l'username.
   *
   * @return l'username se valido.
   * @throws InvalidUsername eccezione di username non valido
   */
  @Util
  public static java.util.Optional<String> retrieveAndValidateJwtUsername() throws InvalidUsername {
    val idToken = getCurrentIdToken();
    if (!idToken.isPresent()) {
      return java.util.Optional.empty();
    }
    val jwtToken = jwtTokenDao.byIdToken(idToken.get());
    if (!jwtToken.isPresent()) {
      // Il token non è più presente (es. rimosso perché scaduto): non si utilizza
      // l'eventuale token inviato dal client nell'intestazione http.
      log.debug("Token oauth non presente nel db per l'id token in sessione, "
          + "rimosso il jwt dalla sessione");
      clearJwtSession();
      return java.util.Optional.empty();
    }
    log.debug("Prelevato token oauth dal db utilizzando l'id token");
    String token = jwtToken.get().getAccessToken();
    if (token == null) {
      return java.util.Optional.empty();
    }
    try {
      val username = extractSubjectFromJwt(token);
      return java.util.Optional.ofNullable(username);
    } catch (ExpiredJwtException ex) {
      val refreshed = openIdConnectClient.retrieveRefreshToken(getCurrentRefreshToken().get());
      if (refreshed != null) {
        setJwtSession(refreshed);
        val username = extractSubjectFromJwt(refreshed.accessToken);
        return java.util.Optional.ofNullable(username);
      } else {
        clearJwtSession();
        throw new InvalidUsername(ex);
      }
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("Error validating JWT: {}", e.getMessage());
      throw new InvalidUsername(e);
    }
  }

  /**
   * Setta la sessione jwt.
   *
   * @param oauthResponse la risposta oauth
   */
  @Util
  public static void setJwtSession(OAuth2.Response oauthResponse) {
    //XXX l'accesso token viene salvato sul db non in sessione perché la sessione
    //viene inserita in un cookie che ha dimensione massima di 4096 caratteri e questa
    //dimensione non è sufficiente per contenere anche l'access token.
    OauthToken oauthToken = 
        new Gson().fromJson(oauthResponse.httpResponse.getJson(), OauthToken.class);
    log.trace("oauthToken = {}", oauthToken);
    val jwtToken = jwtTokenDao.persist(byOauthToken(oauthToken));
    log.debug("Effettuato salvataggio sul db del jwt token {}", jwtToken);

    Session.current().put(Resecure.REFRESH_TOKEN, jwtToken.getRefreshToken());
    log.trace("put REFRESH_TOKEN in sessione. Length = {}, value = {}", 
        jwtToken.getRefreshToken().length(), jwtToken.getRefreshToken()); 
    Session.current().put(Resecure.ID_TOKEN, jwtToken.getIdToken());
    log.debug("put ID_TOKEN in sessione. Length = {}, value = {}", 
        jwtToken.getIdToken().length(), jwtToken.getIdToken());
  }

  @Util
  public static void clearJwtSession() {
    jwtTokenDao.deleteByIdToken(Session.current().get(Resecure.ID_TOKEN));
    Session.current().remove(Resecure.REFRESH_TOKEN, Resecure.ID_TOKEN);
  }

  @Util
  private static Optional<String> getCurrentIdToken() {
    return Optional.fromNullable(Session.current().get(Resecure.ID_TOKEN));
  }

  @Util
  private static Optional<String> getCurrentRefreshToken() {
    return Optional.fromNullable(Session.current().get(Resecure.REFRESH_TOKEN));
  }

  /**
   * Preleva il jwt dell'utente corrente se presente, altrimenti Optional.absent().
   */
  @Util
  public static Optional<String> getCurrentJwt() {
    if (!getCurrentIdToken().isPresent()) {
      return Optional.absent();
    }
    val jwtToken = jwtTokenDao.byIdToken(getCurrentIdToken().get());
    //Se c'è un token
    if (jwtToken.isPresent()) {
      //ed il token è scaduto o scade a breve
      if (jwtToken.get().isExpiringSoon() 
            && getCurrentRefreshToken().isPresent()) {
        log.debug("current jwt token {} is expiring or expired, retriving a new one by "
            + "refresh token", jwtToken.get());
        val refreshed = openIdConnectClient.retrieveRefreshToken(getCurrentRefreshToken().get());
        if (refreshed != null) {
          jwtToken.get().setAccessToken(refreshed.accessToken);
          jwtTokenDao.save(jwtToken.get());
        }
      }
    } else if (getCurrentRefreshToken().isPresent() && getCurrentIdToken().isPresent()) {
      val refreshed = openIdConnectClient.retrieveRefreshToken(getCurrentRefreshToken().get());
      if (refreshed != null) {
        val newJwtToken = byRefreshTokenResponse(refreshed);
        return Optional.of(newJwtToken.getAccessToken());
      }
    }

    return jwtToken.isEmpty() ? Optional.absent() : Optional.of(jwtToken.get().getAccessToken());
  }

  @Util
  private static String extractSubjectFromJwt(String jwt) {
    // legge l'issuer senza verificare la firma solo per scegliere la chiave con cui
    // validare il token, l'issuer viene poi verificato insieme alla firma
    String issuer = JwtValidator.untrustedIssuer(jwt);
    Claims claims;
    if (issuer.equals(Router.getBaseUrl())) {
      claims = JwtValidator.validateLocal(jwt, key(), Router.getBaseUrl());
    } else {
      claims = JwtValidator.validateOidc(jwt, openIdConnectClient.getJwksResolver(),
          openIdConnectClient.getConfig().getIssuer(), openIdConnectClient.getClientId());
    }
    return claims.get(openIdConnectClient.getJwtField(), String.class);
  }
  
  private static JwtToken byRefreshTokenResponse(OAuth2.Response response) {
    val newJwtToken = new JwtToken();
    newJwtToken.setIdToken(getCurrentIdToken().orNull());
    newJwtToken.setRefreshToken(getCurrentRefreshToken().orNull());
    newJwtToken.setAccessToken(response.accessToken);
    newJwtToken.setTakenAt(LocalDateTime.now());
    newJwtToken.setExpiresIn(DEFAULT_REFRESHED_TOKEN_EXPIRES_IN_SECONDS);
    return jwtTokenDao.save(newJwtToken);
  }

  @Util
  private static JwtToken byOauthToken(OauthToken oauthToken) {
    val jwtToken = new JwtToken();
    jwtToken.setIdToken(oauthToken.getId_token());
    jwtToken.setAccessToken(oauthToken.getAccess_token());
    jwtToken.setRefreshToken(oauthToken.getRefresh_token());
    jwtToken.setScope(oauthToken.getScope());
    jwtToken.setTakenAt(oauthToken.getTaken_at());
    jwtToken.setExpiresIn(oauthToken.getExpires_in());
    jwtToken.setTokenType(oauthToken.getToken_type());
    return jwtToken;
  }
}
