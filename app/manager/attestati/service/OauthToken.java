package manager.attestati.service;

import org.joda.time.LocalDateTime;

import java.io.Serializable;

/**
 * @author dario.
 */
public class OauthToken implements Serializable {
  private static final long serialVersionUID = -1971342980047760996L;
  public String access_token;
  public String token_type;
  public String refresh_token;
  public int expires_in;
  public String scope;
  public LocalDateTime took_at = LocalDateTime.now();
}
