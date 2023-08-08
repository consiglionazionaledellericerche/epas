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
import controllers.Resecure.NoCheck;
import dao.JwtTokenDao;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import io.jsonwebtoken.security.SecurityException;
import java.security.Key;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.attestati.service.OauthToken;
import models.JwtToken;
import org.joda.time.LocalDateTime;
import lombok.val;
import play.Play;
import play.cache.Cache;
import play.libs.OAuth2;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Header;
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
      Object body = Jwts.parserBuilder().setSigningKey(key()).build().parse(token).getBody();
      String user = ((Claims) body).getSubject();
      renderText("success " + user);
    } catch (SecurityException e) {
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
    Header authorization = Http.Request.current.get().headers.get(AUTHORIZATION);
    String token = null;
    val idToken = getCurrentIdToken();
    val jwtToken = jwtTokenDao.byIdToken(idToken);
    if (jwtToken.isPresent()) {
      log.debug("Prelevato token oauth dal db utilizzando l'id token {}", idToken);
      token = jwtToken.get().getAccessToken();
    } else if (authorization != null && authorization.value().startsWith(BEARER)) {
      log.debug("Prelevato token oauth dall'intestazione http");
      token = authorization.value().substring(BEARER.length());
    }
    if (token == null) {
      return java.util.Optional.empty();
    }
    try {
      val username = extractSubjectFromJwt(token);
      return java.util.Optional.ofNullable(username);
    } catch (ExpiredJwtException ex) {
      val refreshed = openIdConnectClient.retrieveRefreshToken(getCurrentRefreshToken());
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
    OauthToken oauthToken = new Gson().fromJson(oauthResponse.httpResponse.getJson(), OauthToken.class);
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
  private static String getCurrentIdToken() {
    return Session.current().get(Resecure.ID_TOKEN);
  }
  
  @Util
  private static String getCurrentRefreshToken() {
    return Session.current().get(Resecure.REFRESH_TOKEN);
  }
  
  @Util
  public static Optional<String> getCurrentJwt() {
    val jwtToken = jwtTokenDao.byIdToken(getCurrentIdToken());
    //Se c'è un token
    if (!jwtToken.isEmpty()) {
      //ed il token è scaduto o scade a breve
      if (jwtToken.get().isExpiringSoon() 
            && getCurrentRefreshToken() != null) {
        val refreshed = openIdConnectClient.retrieveRefreshToken(getCurrentRefreshToken());
        if (refreshed != null) {
          jwtToken.get().setAccessToken(refreshed.accessToken);
          jwtTokenDao.save(jwtToken.get());
        }
      }
    } else if (getCurrentRefreshToken() != null && getCurrentIdToken() != null) {
      val refreshed = openIdConnectClient.retrieveRefreshToken(getCurrentRefreshToken());
      if (refreshed != null) {
        val newJwtToken = byRefreshTokenResponse(refreshed);
        return Optional.of(newJwtToken.getAccessToken());
      }
    }

    return jwtToken.isEmpty() ? Optional.absent() : Optional.of(jwtToken.get().getAccessToken());
  }

  @Util
  private static String extractSubjectFromJwt(String jwt) {
    // legge il jwt evitando la signature perché lo issuer non è noto a priori
    int i = jwt.lastIndexOf('.');
    String withoutSignature = jwt.substring(0, i + 1);
    val untrusted = Jwts.parserBuilder().build().parseClaimsJwt(withoutSignature);
    String issuer = untrusted.getBody().getIssuer();
    Object jwtBody;
    if (issuer.equals(Router.getBaseUrl())) {
      jwtBody = Jwts.parserBuilder().setSigningKey(key()).build().parse(jwt).getBody();
    } else {
      jwtBody = Jwts.parserBuilder()
          .setSigningKeyResolver(openIdConnectClient.getJwksResolver()).build()
          .parse(jwt).getBody();
    }
    return ((Claims) jwtBody).get(openIdConnectClient.getJwtField(), String.class);
  }
  
  private static JwtToken byRefreshTokenResponse(OAuth2.Response response) {
    val newJwtToken = new JwtToken();
    newJwtToken.setIdToken(getCurrentIdToken());
    newJwtToken.setRefreshToken(getCurrentRefreshToken());
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
