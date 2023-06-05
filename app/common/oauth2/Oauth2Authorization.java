package common.oauth2;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * classe di autorizzazione oauth2.
 *
 * @author cristian
 *
 */
@Slf4j
@RequiredArgsConstructor
public class Oauth2Authorization implements RequestInterceptor {

  private final OpenIdClientsModule.AuthApi authClient;
  private final String realm;
  private final String clientId;
  private final String clientSecret;
  private long expiringMs = 0;
  private String token;

  @Override
  public void apply(RequestTemplate requestTemplate) {
    val current = System.currentTimeMillis();
    if (current >= expiringMs) {
      log.debug("retrieve new oauth2 token \"{}\" for client \"{}\"", realm, clientId);
      val tokenData = authClient.generateToken(realm, "client_credentials", clientId, clientSecret);
      expiringMs = current + tokenData.getExpiresIn() * 1_000; // * msec
      token = tokenData.getAccessToken();
    }
    requestTemplate.header("Authorization", "Bearer " + token);
  }
}
