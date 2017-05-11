package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import models.base.MutableModel;
import models.enumerate.NotificationSubject;

import play.data.validation.Required;

/**
 * Notification info and its database mapping.
 *
 * @author marco
 */
@Entity
@Table(name = "notifications")
public class Notification extends MutableModel {

  private static final long serialVersionUID = -7368104051600322496L;

  @NotNull
  @ManyToOne(optional = false)
  public User recipient;

  @Required
  @NotNull
  public String message;

  @Required
  @NotNull
  @Enumerated(EnumType.STRING)
  public NotificationSubject subject;

  // id dell'oggetto correlato indicato dal target.
  @Column(name = "subject_id")
  public Long subjectId;

  @NotNull
  public boolean read = false;

  @Transient
  public boolean isRedirect() {
    return subject.isRedirect() && subjectId != null && getUrl() != null;
  }

  @Transient
  public String getUrl() {
    return subject.toUrl(subjectId);
  }

  public interface NotificationBuilderTypeCreate {
    Notification create();
  }

  public interface NotificationBuilderType {

    NotificationBuilderTypeCreate subject(NotificationSubject type);

    NotificationBuilderTypeCreate subject(NotificationSubject type, Long id);

  }

  public interface NotificationBuilderMessage {
    NotificationBuilderType message(String text);
  }

  public interface NotificationBuilderOperator {
    NotificationBuilderMessage destination(User operator);
  }

  public static class NotificationBuilder implements
      NotificationBuilderTypeCreate, NotificationBuilderType,
      NotificationBuilderMessage, NotificationBuilderOperator {

    private User destination;
    private String message;
    private NotificationSubject subject;
    private Long subjectId;

    @Override
    public NotificationBuilderMessage destination(User operator) {
      destination = operator;
      return this;
    }

    @Override
    public NotificationBuilderType message(String text) {
      message = text;
      return this;
    }

    @Override
    public NotificationBuilderTypeCreate subject(NotificationSubject type) {
      subject = type;
      return this;
    }

    @Override
    public NotificationBuilderTypeCreate subject(final NotificationSubject type,
        final Long tid) {
      subject = type;
      subjectId = tid;
      return this;
    }

    @Override
    public Notification create() {
      final Notification notification = new Notification();
      notification.recipient = destination;
      notification.message = message;
      notification.subject = subject;
      notification.subjectId = subjectId;
      return notification.save();
    }

  }

  /**
   * @return a new notification, saved.
   */
  public static NotificationBuilderOperator builder() {
    return new NotificationBuilder();
  }
}
