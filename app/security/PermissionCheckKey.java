package security;

import lombok.Data;

/**
 * Created by daniele on 10/12/15.
 */
@Data
public class PermissionCheckKey {
  private final Object target;
  private final String action;
}
