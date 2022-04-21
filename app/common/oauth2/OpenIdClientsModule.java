package common.oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import common.injection.AutoRegister;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import it.cnr.iit.keycloak.api.RealmsAdminApi;
import it.cnr.iit.keycloak.api.UsersApi;
import it.cnr.iit.keycloak.invoker.ApiClient;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import play.Play;
import play.mvc.Router;

/**
 * Modulo OpenIdClient.
 *
 * @author Cristian
 *
 */
@Slf4j
@AutoRegister
public class OpenIdClientsModule extends AbstractModule {

  private static final String APP_PATH = "${applicationPath}";
  private static final long JWKS_CACHE_SIZE = 5;
  private static final long JWKS_CACHE_DURATION = 10; // minuti

  static final String KEYCLOAK_CLIENT_ID = "keycloak.client_id";
  private static final String KEYCLOAK_CLIENT_SECRET = "keycloak.client_secret";
  private static final String KEYCLOAK_CLIENT_CONFIG_URI = "keycloak.config.uri";
  private static final String KEYCLOAK_ADMIN_URI = "keycloak.admin.uri";
  static final String KEYCLOAK_REALM = "keycloak.realm";

  private static final String KEYCLOAK_JWT_FIELD = "keycloak.jwt.field";

  /**
   * Implementazione generica per un client openid basato sulle classi del play.
   *
   * @return Un wrapper della classe Oauth2 play
   */
  @Provides
  public OpenIdConnectClient openIdConnectClient(@Named(KEYCLOAK_CLIENT_ID) String clientId,
      @Named(KEYCLOAK_CLIENT_SECRET) String clientSecret,
      @Named(KEYCLOAK_CLIENT_CONFIG_URI) String configUrl,
      @Named(KEYCLOAK_JWT_FIELD) Optional<String> jwtField) {
    try {
      return new OpenIdConnectClient(configUrl,
          () -> Router.getFullUrl("Resecure.oauthCallback"),
          clientId, clientSecret, jwtField.orElse(null), JWKS_CACHE_SIZE, JWKS_CACHE_DURATION);
    } catch (IOException e) {
      log.error("Error retrieving configuration from keycloak {}",  e.getMessage());
      throw new RuntimeException(e);
    }
  }

  @Provides
  @Named(KEYCLOAK_ADMIN_URI)
  public String keycloakAdminUri() {
    return Play.configuration.getProperty(KEYCLOAK_ADMIN_URI);
  }

  @Provides
  @Named(KEYCLOAK_CLIENT_ID)
  public String keycloakClientId() {
    return Play.configuration.getProperty(KEYCLOAK_CLIENT_ID);
  }

  @Provides
  @Named(KEYCLOAK_CLIENT_SECRET)
  public String keycloakClientSecret() {
    return Play.configuration.getProperty(KEYCLOAK_CLIENT_SECRET);
  }

  @Provides
  @Named(KEYCLOAK_REALM)
  public String keycloakRealm() {
    return Play.configuration.getProperty(KEYCLOAK_REALM);
  }

  /**
   * Ritorna l'uri di configurazione del keycloack.
   *
   * @return l'uri di configurazione del keycloack
   */
  @Provides
  @Named(KEYCLOAK_CLIENT_CONFIG_URI)
  public String keycloakConfigUri() {
    return Play.configuration.getProperty(KEYCLOAK_CLIENT_CONFIG_URI)
        // da utilizzare per sostituire il percorso di configurazione
        .replace(APP_PATH, Play.applicationPath.getAbsolutePath());
  }

  @Provides
  @Named(KEYCLOAK_JWT_FIELD)
  public Optional<String> keycloakJwtField() {
    return Optional.ofNullable(Play.configuration.getProperty(KEYCLOAK_JWT_FIELD));
  }

  /**
   * Costruisce l'oggetto TokenData.
   *
   * @author Cristian
   *
   */
  @Data
  public static class TokenData {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("expires_in")
    private Integer expiresIn;
    @JsonProperty("not-before-policy")
    private int notBeforePolicy = 0;
    @JsonProperty("refresh_expires_in")
    private Integer refreshExpiresIn;
    @JsonProperty("refresh_token")
    private String refreshToken;
    private String scope;
    @JsonProperty("session_state")
    private String sessionState;
    // must be bearer
    @JsonProperty("token_type")
    private String tokenType;
  }

  /**
   * Api auth.
   *
   * @author Cristian
   *
   */
  public interface AuthApi {

    @RequestLine("POST /protocol/openid-connect/token")
    @Headers({"Accept: application/json", "Content-Type: application/x-www-form-urlencoded"})
    TokenData generateToken(@Param("realm") String realm,
                            @Param("grant_type") String grantType,
                            @Param("client_id") String clientId,
                            @Param("client_secret") String clientSecret);
  }

  /**
   * Costruisce il configData.
   *
   * @author Cristian
   *
   */
  @Data
  public static class ConfigData {
    private final OkHttpClient client;
    private final Slf4jLogger logger;
  }

  /**
   * ClientFactory.
   *
   * @author Cristian
   *
   */
  @FunctionalInterface
  public interface ClientFactory {
    ApiClient builder(Function<Exception, ?> fallback);
  }

  @Singleton
  @Provides
  public ConfigData config() {
    return new ConfigData(new OkHttpClient(/* httpClient */),
        new Slf4jLogger("keycloak"));
  }

  /**
   * Ritorna l'api di autenticazione.
   *
   * @param openIdConnectClient il client openidconnect
   * @param config la configurazione
   * @param encoder l'encoder
   * @param decoder il decoder
   * @return l'api auth.
   */
  @Singleton
  @Provides
  public AuthApi authApi(OpenIdConnectClient openIdConnectClient,
                         ConfigData config,
                         JacksonEncoder encoder, JacksonDecoder decoder) {
    return Feign.builder().logger(config.getLogger()).client(config.getClient())
        .encoder(new FormEncoder(encoder))
        .decoder(decoder)
        .target(AuthApi.class, openIdConnectClient.getConfig().getIssuer());
  }

  /**
   * Il clientFactory.
   *
   * @param adminUri l'uri admin del keycloack
   * @param realm il realm keycloack
   * @param clientId l'id client del keycloack
   * @param clientSecret il secret del keycloack
   * @param config la configurazione
   * @param authApi l'api di autenticazione
   * @return il clientFactory.
   */
  @Singleton
  @Provides
  public ClientFactory clientFactory(@Named(KEYCLOAK_ADMIN_URI) String adminUri,
                                     @Named(KEYCLOAK_REALM) String realm,
                                     @Named(KEYCLOAK_CLIENT_ID) String clientId,
                                     @Named(KEYCLOAK_CLIENT_SECRET) String clientSecret,
                                     ConfigData config,
                                     AuthApi authApi) {

    // Attenzione: occorre un feignbuilder separato per evitare interazioni con il 
    //request-interceptor
    return fallback -> {
      val api = new ApiClient().setBasePath(adminUri);
      val decorator = FeignDecorators.builder()
          .withFallbackFactory(fallback)
          .build();
      val feignBuilder = Resilience4jFeign.builder(decorator)
          .encoder(new FormEncoder(new JacksonEncoder(api.getObjectMapper())))
          .decoder(new JacksonDecoder(api.getObjectMapper()))
          .client(config.getClient()).logger(config.getLogger())
          .requestInterceptor(new Oauth2Authorization(authApi, realm, clientId, clientSecret));
      return api.setFeignBuilder(feignBuilder);
    };
  }

  @Singleton
  @Provides
  public UsersApi keycloakUserApi(ClientFactory factory) {
    return factory.builder(UsersApiFallback::new).buildClient(UsersApi.class);
  }

  @Singleton
  @Provides
  public RealmsAdminApi keycloakAdminApi(ClientFactory factory) {
    return factory.builder(RealmsAdminApiFallback::new).buildClient(RealmsAdminApi.class);
  }

  //  @Override
  //  public void configure() {
  //    bind(UserManagerEvents.class).asEagerSingleton();
  //  }
}
