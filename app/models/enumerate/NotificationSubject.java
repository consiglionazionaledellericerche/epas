package models.enumerate;

import com.google.common.collect.Maps;

import play.mvc.Router;

import java.util.Map;

/**
 * Notification subject types.
 *
 * @author marco
 */
public enum NotificationSubject {
  /**
   * Notifiche di sistema.
   */
  SYSTEM,
  /**
   * Commento.
   */
  COMMENT,
  /**
   * Messaggio.
   */
  MESSAGE;

  private String toUrl(String action, Long id) {
    if (id == null) {
      return Router.reverse(action).url;
    } else {
      final Map<String, Object> params = Maps.newHashMap();
      params.put("id", id);
      return Router.reverse(action, params).url;
    }
  }

  public String toUrl(Long referenceId) {
    switch (this) {
      case COMMENT:
        return toUrl("Comments.show", referenceId);
      case MESSAGE:
        return toUrl("Messages.show", referenceId);
      // case SYSTEM:
      default:
        throw new IllegalStateException("unknown target: " + this.name());
    }
  }


  public boolean isRedirect() {
    return this != SYSTEM;
  }
}
