package models.enumerate;

import com.google.common.collect.Maps;

import models.Absence;
import models.Stamping;

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
  MESSAGE,
  /**
   * Notifiche relative a timbrature inserite o modificate
   */
  STAMPING,
  /**
   * Notifiche relative alle assenze inserite o modificate
   */
  ABSENCE;

  private String toUrl(String action, Map<String, Object> params) {
    if (params == null) {
      return Router.reverse(action).url;
    } else {
      return Router.reverse(action, params).url;
    }
  }

  public String toUrl(Long referenceId) {
    final Map<String, Object> params = Maps.newHashMap();
    switch (this) {
      case COMMENT:
        params.put("id", referenceId);
        return toUrl("Comments.show", params);
      case MESSAGE:
        params.put("id", referenceId);
        return toUrl("Messages.show", params);
      case STAMPING:
        final Stamping stamping = Stamping.findById(referenceId);
        if (stamping == null) {
          return null;
        }
        params.put("month", stamping.date.getMonthOfYear());
        params.put("year", stamping.date.getYear());
        params.put("personId", stamping.personDay.person.id);
        return toUrl("Stampings.personStamping", params);
      case ABSENCE:
        final Absence absence = Absence.findById(referenceId);
        if (absence == null) {
          return null;
        }
        params.put("month", absence.personDay.date.getMonthOfYear());
        params.put("year", absence.personDay.date.getYear());
        params.put("personId", absence.personDay.person.id);
        return toUrl("Stampings.personStamping", params);
      // case SYSTEM:
      default:
        throw new IllegalStateException("unknown target: " + this.name());
    }
  }


  public boolean isRedirect() {
    return this != SYSTEM;
  }
}
