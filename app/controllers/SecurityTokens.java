package controllers;

import com.google.common.base.Strings;
import common.oauth2.OpenIdConnectClient;
import controllers.Resecure.NoCheck;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.crypto.MacProvider;
import io.jsonwebtoken.security.SecurityException;
import java.security.Key;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import play.Play;
import play.cache.Cache;
import play.libs.OAuth2;
import play.mvc.*;
import play.mvc.Http.Header;
import play.mvc.Scope.Session;

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
  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String ID_TOKEN = "id_token";
  private static final String CACHE_ACCESS_TOKEN_POSTFIX = "__ACCESS_TOKEN";

  @Inject
  static OpenIdConnectClient openIdConnectClient;

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
    String username = Resecure.getCurrentUser().orElseThrow().username;
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
    //Attenzione dipende dalla Cache
    if (Cache.get(cacheAccessTokenKey()) != null) {
      log.debug("Prelevato token aouth dalla cache utilizzando la chiave {}",
          cacheAccessTokenKey());
      token = (String) Cache.get(cacheAccessTokenKey());
    } else if (authorization != null && authorization.value().startsWith(BEARER)) {
      log.debug("Prelevato token aouth dall'intestazione http");
      token = authorization.value().substring(BEARER.length());
    }
    if (token == null) {
      return java.util.Optional.empty();
    }
    try {
      val username = extractSubjectFromJwt(token);
      return java.util.Optional.ofNullable(username);
    } catch (ExpiredJwtException ex) {
      val refreshToken = Session.current().get(Resecure.REFRESH_TOKEN);
      val refreshed = openIdConnectClient.retrieveRefreshToken(refreshToken);
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
   * La chiave da utilizzare in sessione per l'access token dipende dalla
   * sessione dell'utente.
   */
  @Util
  public static String cacheAccessTokenKey() {
    return Session.current().getId() + CACHE_ACCESS_TOKEN_POSTFIX;
  }

  /**
   * Setta la sessione jwt.
   *
   * @param oauthResponse la risposta oauth
   */
  @Util
  public static void setJwtSession(OAuth2.Response oauthResponse) {
    //XXX l'accesso token viene impostato in Cache e non in sessione perché la sessione
    //viene inserita in un cookie che ha dimensione massima di 4096 caratteri e questa
    //dimensione non è sufficiente per contenere anche l'access token.
    Cache.safeAdd(cacheAccessTokenKey(), oauthResponse.accessToken, Scope.COOKIE_EXPIRE);
    log.debug("put Jwt in cache con chiave {}. lenght={}, value={}", 
        cacheAccessTokenKey(), oauthResponse.accessToken.length(), oauthResponse.accessToken);
    val body = oauthResponse.httpResponse.getJson().getAsJsonObject();
    String refreshToken = body.get(REFRESH_TOKEN).getAsString();
    String idToken = body.get(ID_TOKEN).getAsString();
    Session.current().put(Resecure.REFRESH_TOKEN, refreshToken);
    log.trace("put REFRESH_TOKEN in sessione. Length = {}, value = {}", 
        refreshToken.length(), refreshToken); 
    Session.current().put(Resecure.ID_TOKEN, idToken);
    log.trace("put ID_TOKEN in sessione. Length = {}, value = {}", idToken.length(), idToken);
  }

  @Util
  public static void clearJwtSession() {
    Session.current().remove(Resecure.REFRESH_TOKEN, Resecure.ID_TOKEN);
    Cache.delete(cacheAccessTokenKey());
  }

  @Util
  private static String extractSubjectFromJwt(String jwt) {
    // legge il jwt evitando la signature perché lo issuer non è noto a priori
    int i = jwt.lastIndexOf('.');
    String withoutSignature = jwt.substring(0, i + 1);
    @SuppressWarnings("rawtypes")
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
}
