package controllers;

import com.google.common.base.Optional;

import com.mysema.query.SearchResults;

import dao.NotificationDao;
import dao.NotificationDao.NotificationFilter;

import javax.inject.Inject;

import models.Notification;

import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

/**
 * Controller to manage notifications.
 *
 * @author marco
 * @author cristian
 */
@With(Resecure.class)
public class Notifications extends Controller {
  
  @Inject
  static NotificationDao notificationDao;
  @Inject
  static SecurityRules rules;
  
  /**
   * Applica i parametri per filtrare le notifiche.
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
    final SearchResults<Notification> notifications = notificationDao
        .listFor(Security.getUser().get(), Optional.fromNullable(message), 
            Optional.of(filter)).listResults();
    
    final SearchResults<Notification> unReadNotifications = notificationDao
        .listFor(Security.getUser().get(), Optional.fromNullable(message), 
            Optional.of(NotificationFilter.TO_READ)).listResults();
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

    notification.read = true;
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

    notification.read = true;
    notification.save();
    list(message, filter);
  }
  
  /**
   * Questa chiamata si assume che sia sempre POST+ajax.
   *
   */
  public static void readAll(String message, NotificationFilter filter) {
    
    rules.checkIfPermitted();

    for (Notification notification : notificationDao.listAllFor(Security.getUser().get(), 
        Optional.fromNullable(message), Optional.of(filter))) {
      if (notification.read) {
        continue;
      }
      notification.read = true;
      notification.save();
    }

    list(message, filter);
  }
}
