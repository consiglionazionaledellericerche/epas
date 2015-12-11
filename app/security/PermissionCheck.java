package security;

import com.google.common.base.MoreObjects;

import lombok.Getter;

/**
 * Seam like check.
 *
 * @author marco
 */
@Getter
public class PermissionCheck {

  private final PermissionCheckKey key;
  private boolean granted = false;

  public PermissionCheck(Object target, String action) {
    key = new PermissionCheckKey(target, action);
  }

  public String getAction() {
    return key.getAction();
  }

  public Object getTarget() {
    return key.getTarget();
  }

  public void grant() {
    this.granted = true;
  }

  public void revoke() {
    this.granted = false;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).omitNullValues()
        .add("action", getAction())
        .add("target", getTarget())
            .addValue(granted ? "GRANTED" : "DENIED").toString();
  }
}
