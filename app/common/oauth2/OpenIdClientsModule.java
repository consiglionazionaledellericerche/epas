package common.oauth2;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import common.injection.AutoRegister;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;
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
  @Named(KEYCLOAK_CLIENT_ID)
  public String keycloakClientId() {
    return Play.configuration.getProperty(KEYCLOAK_CLIENT_ID);
  }

  @Provides
  @Named(KEYCLOAK_CLIENT_SECRET)
  public String keycloakClientSecret() {
    return Play.configuration.getProperty(KEYCLOAK_CLIENT_SECRET);
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
}
