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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolver;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.security.Key;
import java.security.KeyPair;
import java.time.ZonedDateTime;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Verifica che vengano accettati solo i JWT firmati, con issuer, client e tipo attesi.
 */
public class JwtValidatorTest extends UnitTest {

  private static final String OIDC_ISSUER = "https://sso.example.org/auth/realms/TEST";
  private static final String LOCAL_ISSUER = "https://epas.example.org/";
  private static final String CLIENT_ID = "epas";
  private static final String EMAIL = "mario.rossi@example.org";

  private static final KeyPair IDP_KEYS = Keys.keyPairFor(SignatureAlgorithm.RS256);
  private static final KeyPair OTHER_KEYS = Keys.keyPairFor(SignatureAlgorithm.RS256);
  private static final SecretKey LOCAL_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512);

  private static final SigningKeyResolver RESOLVER = new SigningKeyResolverAdapter() {
    @Override
    public Key resolveSigningKey(@SuppressWarnings("rawtypes") JwsHeader header,
        Claims claims) {
      return IDP_KEYS.getPublic();
    }
  };

  /**
   * Un access token come quelli rilasciati da Keycloak, ancora da firmare.
   */
  private static JwtBuilder accessToken() {
    return Jwts.builder()
        .setIssuer(OIDC_ISSUER)
        .setAudience("account")
        .setSubject("96f1d4c0-2a98-47e6-84f8-ac95177743da")
        .claim("azp", CLIENT_ID)
        .claim("typ", "Bearer")
        .claim("email", EMAIL)
        .setExpiration(Date.from(ZonedDateTime.now().plusMinutes(10).toInstant()));
  }

  private static Claims validateOidc(String jwt) {
    return JwtValidator.validateOidc(jwt, RESOLVER, OIDC_ISSUER, CLIENT_ID);
  }

  @Test
  public void validAccessTokenIsAccepted() {
    String jwt = accessToken().signWith(IDP_KEYS.getPrivate()).compact();
    assertEquals(OIDC_ISSUER, JwtValidator.untrustedIssuer(jwt));
    assertEquals(EMAIL, validateOidc(jwt).get("email", String.class));
  }

  @Test(expected = UnsupportedJwtException.class)
  public void unsignedAccessTokenIsRejected() {
    // alg "none"
    validateOidc(accessToken().compact());
  }

  @Test(expected = SignatureException.class)
  public void accessTokenSignedWithAnotherKeyIsRejected() {
    validateOidc(accessToken().signWith(OTHER_KEYS.getPrivate()).compact());
  }

  @Test(expected = IncorrectClaimException.class)
  public void accessTokenOfAnotherIssuerIsRejected() {
    validateOidc(accessToken().setIssuer("https://sso.example.org/auth/realms/OTHER")
        .signWith(IDP_KEYS.getPrivate()).compact());
  }

  @Test(expected = IncorrectClaimException.class)
  public void accessTokenOfAnotherClientIsRejected() {
    validateOidc(accessToken().claim("azp", "altra-applicazione")
        .signWith(IDP_KEYS.getPrivate()).compact());
  }

  @Test(expected = MissingClaimException.class)
  public void accessTokenWithoutClientIsRejected() {
    validateOidc(accessToken().claim("azp", null)
        .signWith(IDP_KEYS.getPrivate()).compact());
  }

  @Test(expected = IncorrectClaimException.class)
  public void idTokenIsRejected() {
    validateOidc(accessToken().setAudience(CLIENT_ID).claim("typ", "ID")
        .signWith(IDP_KEYS.getPrivate()).compact());
  }

  @Test(expected = MalformedJwtException.class)
  public void tokenWithoutIssuerIsRejected() {
    JwtValidator.untrustedIssuer(accessToken().setIssuer(null)
        .signWith(IDP_KEYS.getPrivate()).compact());
  }

  @Test
  public void validLocalTokenIsAccepted() {
    String jwt = Jwts.builder().setSubject("mario.rossi").setIssuer(LOCAL_ISSUER)
        .signWith(LOCAL_KEY, SignatureAlgorithm.HS512).compact();
    assertEquals("mario.rossi",
        JwtValidator.validateLocal(jwt, LOCAL_KEY, LOCAL_ISSUER).getSubject());
  }

  @Test(expected = UnsupportedJwtException.class)
  public void unsignedLocalTokenIsRejected() {
    String jwt = Jwts.builder().setSubject("admin").setIssuer(LOCAL_ISSUER).compact();
    JwtValidator.validateLocal(jwt, LOCAL_KEY, LOCAL_ISSUER);
  }

  @Test(expected = IncorrectClaimException.class)
  public void localTokenOfAnotherIssuerIsRejected() {
    String jwt = Jwts.builder().setSubject("admin").setIssuer("https://altro.example.org/")
        .signWith(LOCAL_KEY, SignatureAlgorithm.HS512).compact();
    JwtValidator.validateLocal(jwt, LOCAL_KEY, LOCAL_ISSUER);
  }
}
