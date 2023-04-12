/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.base.MutableModel;
import models.enumerate.NotificationSubject;
import play.data.validation.Required;

/**
 * Notification info and its database mapping.
 *
 * @author Marco Andreini
 */
@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification extends MutableModel {

  private static final long serialVersionUID = -7368104051600322496L;

  @NotNull
  @ManyToOne(optional = false)
  private User recipient;

  @Required
  @NotNull
  private String message;

  @Required
  @NotNull
  @Enumerated(EnumType.STRING)
  private NotificationSubject subject;

  // id dell'oggetto correlato indicato dal target.
  @Column(name = "subject_id")
  private Long subjectId;

  @NotNull
  private boolean read = false;

  @Transient
  public boolean isRedirect() {
    return subject.isRedirect() && subjectId != null && getUrl() != null;
  }

  @Transient
  public String getUrl() {
    return subject.toUrl(subjectId);
  }

  /**
   * Interfaccia per la creazione del tipo di notifica.
   */
  public interface NotificationBuilderTypeCreate {
    Notification create();
  }

  /**
   * Interfaccia per la creazione della notifica.
   */
  public interface NotificationBuilderType {

    NotificationBuilderTypeCreate subject(NotificationSubject type);

    NotificationBuilderTypeCreate subject(NotificationSubject type, Long id);

  }

  /**
   * Interfaccia del builder dei messaggi all'interno delle notifice. 
   */
  public interface NotificationBuilderMessage {
    NotificationBuilderType message(String text);
  }

  /**
   * Interfaccia del builder del destinatario nei messaggi di notifica. 
   */
  public interface NotificationBuilderOperator {
    NotificationBuilderMessage destination(User operator);
  }

  /**
   * Builder per costruire i messaggi di Notifica.
   *
   * @author Marco Andreini
   */
  static class NotificationBuilder implements
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
   * Crea un builder per costruire le notifiche.
   *
   * @return a new notification, saved.
   */
  public static NotificationBuilderOperator builder() {
    return new NotificationBuilder();
  }
}
