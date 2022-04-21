package common.oauth2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Classe di configurazione del provider.
 *
 * @author Cristian
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ProviderConfig {

  private String issuer;
  private String authorizationEndpoint;
  private String tokenEndpoint;
  private String jwksUri;
  private List<String> responseTypesSupported;
  private List<String> claimsSupported;
  private List<String> grantTypesSupported;
  private List<String> responseModesSupported;
  private String userinfoEndpoint;
  private List<String> scopesSupported;
  private List<String> tokenEndpointAuthMethodsSupported;
  private List<String> userinfoSigningAlgValuesSupported;
  private List<String> idTokenSigningAlgValuesSupported;
  private String revocationEndpoint;
  private String endSessionEndpoint;
}
