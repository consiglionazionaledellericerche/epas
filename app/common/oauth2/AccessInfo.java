package common.oauth2;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AccessInfo {
  private final LocalDateTime when;
  private final String ip;
}
