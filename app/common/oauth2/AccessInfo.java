package common.oauth2;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * classe di informazioni sull'accesso.
 *
 * @author Cristian
 *
 */
@Data
public class AccessInfo {
  private final LocalDateTime when;
  private final String ip;
}
