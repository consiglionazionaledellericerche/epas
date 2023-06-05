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

package controllers;

import com.google.common.base.Optional;
import com.querydsl.core.QueryResults;
import common.security.SecurityRules;
import dao.NotificationDao;
import dao.NotificationDao.NotificationFilter;
import java.util.List;
import javax.inject.Inject;
import models.Notification;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller to manage notifications.
 *
 * @author Marco Andreini
 * @author Cristian Lucchesi
 */
@With(Resecure.class)
public class Notifications extends Controller {

  @Inject
  static NotificationDao notificationDao;
  @Inject
  static SecurityRules rules;

  /**
   * Applica i parametri per filtrare le notifiche.
   *
   * @param message se definito filtra sul testo del messaggio
   * @param filter filtra per notifiche da leggere o archiviate
   */
  public static void filter(String message, NotificationFilter filter) {
    list(message, filter);
  }

  /**
   * Visualizza le notifiche dell'operatore corrente.
   */
  public static void list(String message, NotificationFilter filter) {
    if (filter == null) {
      filter = NotificationFilter.ALL;
    }
    final QueryResults<Notification> notifications = notificationDao
        .listFor(Security.getUser().get(), Optional.fromNullable(message),
            Optional.of(filter), Optional.absent()).listResults();

    final QueryResults<Notification> unReadNotifications = notificationDao
        .listFor(Security.getUser().get(), Optional.fromNullable(message),
            Optional.of(NotificationFilter.TO_READ), Optional.absent()).listResults();
    render(notifications, unReadNotifications, message, filter);
  }

  /**
   * Mark a notification as read and redirect the user to the url referred by the notification.
   *
   * @param id id of the Notification to read
   */
  public static void readAndRedirect(Long id) {
    final Notification notification = Notification.findById(id);
    notFoundIfNull(notification);
    rules.checkIfPermitted(notification);

    notification.setRead(true);
    notification.save();
    redirect(notification.getUrl());
  }

  /**
   * Questa chiamata si assume che sia sempre POST+ajax.
   *
   * @param id id of the Notification to read
   */
  public static void read(Long id, String message, NotificationFilter filter) {
    final Notification notification = Notification.findById(id);
    notFoundIfNull(notification);
    rules.checkIfPermitted(notification);

    notification.setRead(true);
    notification.save();
    list(message, filter);
  }

  /**
   * Questa chiamata si assume che sia sempre POST+ajax.
   */
  public static void readAll(String message, NotificationFilter filter) {

    rules.checkIfPermitted();
    List<Notification> list = notificationDao.listAllFor(Security.getUser().get(),
        Optional.fromNullable(message), Optional.fromNullable(filter), Optional.absent());
    for (Notification notification : list) {
      if (notification.isRead()) {
        continue;
      }
      notification.setRead(true);
      notification.save();
    }

    list(message, filter);
  }
}
