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
import play.libs.OAuth2;
import play.mvc.*;
import play.mvc.Http.Header;
import play.mvc.Scope.Session;

/**
 * Integrazione essenziale con JWT per la generazione di token e la
 * successiva rilettura/verifica.
 *
 * @author Marco Andreini
 */
@With(Resecure.class)
@Slf4j
public class SecurityTokens extends Controller {

  public static final String BEARER = "Bearer ";
  public static final String AUTHORIZATION = "authorization";
  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String ID_TOKEN = "id_token";
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
      return new SecretKeySpec(decodedKey, 0, decodedKey.length, SignatureAlgorithm.HS512.getJcaName());
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

  public static class InvalidUsername extends Exception {
    InvalidUsername(Exception e) {
      super(e);
    }
  }

  @Util
  public static java.util.Optional<String> retrieveAndValidateJwtUsername() throws InvalidUsername {
    Header authorization = Http.Request.current.get().headers.get(AUTHORIZATION);
    String token = null;
    if (Session.current().contains(Resecure.ACCESS_TOKEN)) {
      token = Session.current().get(Resecure.ACCESS_TOKEN);
    } else if (authorization != null && authorization.value().startsWith(BEARER)) {
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

  @Util
  public static void setJwtSession(OAuth2.Response oauthResponse) {
    Session.current().put(Resecure.ACCESS_TOKEN, oauthResponse.accessToken);
    log.trace("put Jwt in session {}: {}", Session.current().getId(), oauthResponse.accessToken);
    val body = oauthResponse.httpResponse.getJson().getAsJsonObject();
    String refreshToken = body.get(REFRESH_TOKEN).getAsString();
    String idToken = body.get(ID_TOKEN).getAsString();
    Session.current().put(Resecure.REFRESH_TOKEN, refreshToken);
    Session.current().put(Resecure.ID_TOKEN, idToken);
  }

  @Util
  public static void clearJwtSession() {
    Session.current().remove(Resecure.ACCESS_TOKEN, Resecure.REFRESH_TOKEN, Resecure.ID_TOKEN);
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
      jwtBody = Jwts.parserBuilder().setSigningKeyResolver(openIdConnectClient.getJwksResolver()).build()
          .parse(jwt).getBody();
    }
    return ((Claims) jwtBody).get(openIdConnectClient.getJwtField(), String.class);
  }
}
