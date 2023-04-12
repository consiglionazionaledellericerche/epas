package common.oauth2;


import com.auth0.jwk.GuavaCachedJwkProvider;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolver;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import play.libs.OAuth2;
import play.libs.WS;
import play.mvc.results.Redirect;

/**
 * Classe openIdConnect.
 *
 * @author Cristian
 *
 */
@Slf4j
@Getter
public final class OpenIdConnectClient {

  private static final int STATE_LENGHT = 20;
  private static final String DEFAULT_FIELD = "email";
  private static final String STATE_PARAM = "state";
  private static final String SCOPE_PARAM = "scope";
  private static final String VERIFICATION_CODE_RESPONSE_TYPE_PARAM = "response_type";
  private static final String VERIFICATION_CODE_RESPONSE_TYPE = "code";
  private static final String TOKEN_GRANT_TYPE_PARAM = "grant_type";
  private static final String TOKEN_GRANT_TYPE = "authorization_code";
  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String LOGOUT_IDTOKEN_PARAM = "id_token_hint";
  private static final String REDIRECT_URI = "redirect_uri";

  @Getter
  private final ProviderConfig config;
  private final OAuth2 instance;
  private final Supplier<String> callBackUrl;
  private final String base64Auth;
  private final JwksResolver jwksResolver;
  // Il campo da considerare per il match dell'utente (default: email)
  private final String jwtField;

  /**
   * Costruttore.
   *
   * @param configUrl url di configurazione
   * @param callBackUrl url di callback
   * @param clientId id del client
   * @param secret il secret
   * @param jwtField campo da analizzare
   * @param jwksCacheSize dimensione della cache
   * @param jwksCacheDuration durata della cache 
   * @throws IOException eccezione IO
   */
  public OpenIdConnectClient(String configUrl, Supplier<String> callBackUrl, String clientId,
      String secret, String jwtField, long jwksCacheSize, long jwksCacheDuration) 
        throws IOException {

    this.callBackUrl = callBackUrl;
    this.jwtField = Objects.requireNonNullElse(jwtField, DEFAULT_FIELD);

    ObjectMapper mapper = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    config = mapper.readValue(new URL(configUrl), ProviderConfig.class);

    base64Auth = Base64.getEncoder()
        .encodeToString((clientId + ":" + secret).getBytes(StandardCharsets.UTF_8));
    instance = new OAuth2(config.getAuthorizationEndpoint(), config.getTokenEndpoint(),
        clientId, secret);
    JwkProvider provider = new GuavaCachedJwkProvider(new UrlJwkProvider(
        new URL(config.getJwksUri())), jwksCacheSize, jwksCacheDuration, TimeUnit.MINUTES);
    jwksResolver = new JwksResolver(provider);
    log.info("Correctly configured new identity provider {}", config.getIssuer());
  }

  /**
   * Recupera e verifica i codici, effettuando la redirect sul callback.
   *
   * @param saveState come salvare lo stato
   */
  public void retrieveVerificationCode(Consumer<String> saveState) {
    // Il Codice generato viene salvato in sessione per effettuare la validazione dopo la risposta
    String state = RandomStringUtils.randomAlphanumeric(STATE_LENGHT);
    saveState.accept(state);
    Map<String, String> params = new HashMap<>();
    params.put(VERIFICATION_CODE_RESPONSE_TYPE_PARAM, VERIFICATION_CODE_RESPONSE_TYPE);
    params.put(STATE_PARAM, state);
    params.put(SCOPE_PARAM, String.join(" ", config.getScopesSupported()));
    instance.retrieveVerificationCode(callBackUrl.get(), params);
  }

  /**
   * Ritorna la risposta OAuth2.
   *
   * @param accessCode il codice prelevato dai parametri della richiesta
   * @param state      lo stato prelevato dai parametri della richiesta
   * @param savedState lo stato prelevato dalla sessione
   * @return la risposta oauth2
   */
  public OAuth2.Response retrieveAccessToken(String accessCode, String state, String savedState) {

    if (state.equals(savedState)) {
      Map<String, Object> params = new HashMap<>();
      params.put(TOKEN_GRANT_TYPE_PARAM, TOKEN_GRANT_TYPE);
      params.put("redirect_uri", callBackUrl.get());
      params.put(VERIFICATION_CODE_RESPONSE_TYPE, accessCode);

      WS.WSRequest request = WS.url(instance.accessTokenURL)
          .setHeader("Accept", "application/json")
          .setHeader("Authorization", "Basic " + base64Auth)
          .params(params);

      WS.HttpResponse response = request.post();

      log.trace("token response: {}", response.getJson());
      if (validateAccessToken(response.getJson().getAsJsonObject())) {
        return new OAuth2.Response(response);
      }
    } else {
      log.error("state is different from saved state");
    }

    return null;
  }

  /**
   * Ritorna la risposta OAuth2 generata dal token.
   *
   * @param refreshToken il token aggiornato
   * @return la risposta OAuth2
   */
  public OAuth2.Response retrieveRefreshToken(String refreshToken) {

    Map<String, Object> params = new HashMap<>();
    params.put(TOKEN_GRANT_TYPE_PARAM, REFRESH_TOKEN);
    params.put(REFRESH_TOKEN, refreshToken);

    WS.WSRequest request = WS.url(instance.accessTokenURL)
        .setHeader("Accept", "application/json")
        .setHeader("Authorization", "Basic " + base64Auth)
        .params(params);

    WS.HttpResponse response = request.post();

    log.trace("token response: {}", response.getJson());

    if (validateAccessToken(response.getJson().getAsJsonObject())) {
      return new OAuth2.Response(response);
    } else {
      return null;
    }
  }

  /**
   * Funzione di logout.
   *
   * @param idToken identificativo del token
   * @param redirectUri uri di reindirizzamento
   */
  public void logout(String idToken, String redirectUri) {
    // TODO: 06/05/20 c'è da ripulire la sessione, ma forse conviene farlo prima di chiamare
    // questo metodo
    throw new Redirect(config.getEndSessionEndpoint(), Map.of(LOGOUT_IDTOKEN_PARAM, idToken,
        REDIRECT_URI, redirectUri));
  }

  /**
   * Verifica la validità dell'accessToken.
   *
   * @param response l'oggetto json di risposta
   * @return se è valido l'access token
   */
  public boolean validateAccessToken(JsonObject response) {
    //    https://openid.net/specs/openid-connect-core-1_0.html#ImplicitTokenValidation
    //    3.2.2.9.  Access Token Validation
    //    To validate an Access Token issued from the Authorization Endpoint with an ID Token, 
    //    the Client SHOULD do the following:
    //    Hash the octets of the ASCII representation of the access_token with the hash algorithm 
    //    specified in JWA [JWA] for the alg Header Parameter of the ID Token's JOSE Header. 
    //    For instance, if the alg is RS256, the hash algorithm used is SHA-256.
    //    Take the left-most half of the hash and base64url encode it.
    //    The value of at_hash in the ID Token MUST match the value produced in the previous step.
    //TODO: 27/04/20 prevedere altri tipi di codifica oltre lo SHA-256
    String accessToken;
    try {
      accessToken = response.get("access_token").getAsString();
    } catch (NullPointerException e) {
      return false;
    }
    byte[] atBytes = Hashing.sha256().hashString(accessToken, StandardCharsets.UTF_8).asBytes();
    atBytes = Arrays.copyOfRange(atBytes, 0, atBytes.length / 2);
    String forgedAtHash = Base64.getUrlEncoder().withoutPadding().encodeToString(atBytes);

    String rawIdToken = response.get("id_token").getAsString();
    String jsonIdToken = new String(Base64.getDecoder().decode(rawIdToken.split("\\.")[1]),
        StandardCharsets.UTF_8);
    JsonObject idToken = new JsonParser().parse(jsonIdToken).getAsJsonObject();
    JsonElement hash = idToken.get("at_hash");
    // FIXME: 22/05/20 il keycloack non restituisce questo campo, a quanto pare è opzionale
    if (null != hash) {
      String atHash = idToken.get("at_hash").getAsString();

      log.debug("ID TOKEN at_hash: {}; Calculated at_hash: {}", atHash, forgedAtHash);
      boolean isValid = atHash.equals(forgedAtHash);
      if (!isValid) {
        log.error("Token Validation failed: {}", accessToken);
      }
      return isValid;
    }
    return true;
  }

  /**
   * Classe Resolver.
   *
   * @author Cristian
   *
   */
  public static class JwksResolver implements SigningKeyResolver {

    private final JwkProvider keyStore;

    public JwksResolver(JwkProvider keyStore) {
      this.keyStore = keyStore;
    }

    @SneakyThrows
    @Override
    public Key resolveSigningKey(@SuppressWarnings("rawtypes") JwsHeader jwsHeader, Claims claims) {
      return keyStore.get(jwsHeader.getKeyId()).getPublicKey();
    }

    @SneakyThrows
    @Override
    public Key resolveSigningKey(@SuppressWarnings("rawtypes") JwsHeader jwsHeader, String s) {
      return keyStore.get(jwsHeader.getKeyId()).getPublicKey();
    }
  }

}
