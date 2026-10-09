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

import com.google.common.base.Strings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SigningKeyResolver;
import java.security.Key;

/**
 * Validazione dei JWT utilizzati per l'autenticazione.
 *
 * <p>I token vengono sempre letti con parseClaimsJws, che ne verifica la firma.</p>
 */
public final class JwtValidator {

  /**
   * Il claim con il client OAuth a cui è stato rilasciato l'access token. Keycloak imposta
   * l'audience degli access token ad "account", per cui il client va verificato con azp.
   */
  public static final String AUTHORIZED_PARTY = "azp";
  public static final String TOKEN_TYPE = "typ";
  /**
   * Il tipo degli access token Keycloak (gli id token hanno tipo "ID").
   */
  public static final String ACCESS_TOKEN_TYPE = "Bearer";
  public static final long ALLOWED_CLOCK_SKEW_SECONDS = 30;

  private JwtValidator() {
  }

  /**
   * L'issuer del token letto senza verificarne la firma, da utilizzare solo per scegliere
   * con quale chiave validare il token.
   *
   * @param jwt il token
   * @return l'issuer non verificato del token
   * @throws MalformedJwtException se il token non contiene l'issuer
   */
  public static String untrustedIssuer(String jwt) {
    int i = jwt.lastIndexOf('.');
    String withoutSignature = jwt.substring(0, i + 1);
    String issuer = Jwts.parserBuilder().build()
        .parseClaimsJwt(withoutSignature).getBody().getIssuer();
    if (Strings.isNullOrEmpty(issuer)) {
      throw new MalformedJwtException("JWT privo di issuer");
    }
    return issuer;
  }

  /**
   * Valida un token firmato da ePAS con la chiave simmetrica locale.
   *
   * @param jwt il token
   * @param key la chiave di firma di ePAS
   * @param issuer l'issuer atteso (l'url di ePAS)
   * @return i claims del token validato
   */
  public static Claims validateLocal(String jwt, Key key, String issuer) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .requireIssuer(issuer)
        .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
        .build()
        .parseClaimsJws(jwt)
        .getBody();
  }

  /**
   * Valida un access token rilasciato dall'identity provider OpenID Connect a ePAS.
   *
   * @param jwt il token
   * @param resolver il resolver delle chiavi pubbliche dell'identity provider (JWKS)
   * @param issuer l'issuer atteso (quello dell'identity provider configurato)
   * @param clientId il client id di ePAS, che deve corrispondere al claim azp
   * @return i claims del token validato
   */
  public static Claims validateOidc(
      String jwt, SigningKeyResolver resolver, String issuer, String clientId) {
    return Jwts.parserBuilder()
        .setSigningKeyResolver(resolver)
        .requireIssuer(issuer)
        .require(AUTHORIZED_PARTY, clientId)
        .require(TOKEN_TYPE, ACCESS_TOKEN_TYPE)
        .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
        .build()
        .parseClaimsJws(jwt)
        .getBody();
  }
}
