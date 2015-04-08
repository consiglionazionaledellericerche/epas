package security;

import com.google.common.base.MoreObjects;

/**
 * Seam like check.
 *
 * @author marco
 *
 */
public class PermissionCheck {

    private final Object target;
    private final String action;
    private boolean granted;

    public PermissionCheck(Object target, String action) {
    	this.target = target;
    	this.action = action;
    	granted = false;
    }

    public Object getTarget() {
    	return target;
    }

    public String getAction() {
    	return action;
    }

    public void grant() {
    	this.granted = true;
    }

    public void revoke() {
    	this.granted = false;
    }

    public boolean isGranted() {
    	return granted;
    }

    @Override
    public String toString() {
    	return MoreObjects.toStringHelper(this).omitNullValues()
    			.add("action", action)
    			.add("target", target)
    			.addValue(granted ? "GRANTED" : "DENIED").toString();
    }
}
