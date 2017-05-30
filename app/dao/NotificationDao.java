package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import helpers.jpa.ModelQuery;

import java.util.List;

import models.Notification;
import models.User;
import models.query.QNotification;

/**
 * Notification DAO.
 *
 * @author marco
 */
public class NotificationDao {

  public static enum NotificationFilter {
    ALL, TO_READ, ARCHIVED;
  }
  
  private final JPQLQueryFactory queryFactory;

  @Inject
  NotificationDao(JPQLQueryFactory queryFactory) {
    this.queryFactory = queryFactory;
  }
  
  private JPQLQuery notifications(User operator, Optional<String> message, 
      Optional<NotificationFilter> filter) {
    
    final QNotification qn = QNotification.notification;
    final BooleanBuilder condition = new BooleanBuilder().and(qn.recipient.eq(operator));
    if (filter.isPresent()) {
      if (filter.get().equals(NotificationFilter.ARCHIVED)) {
        condition.and(qn.read.eq(true));
      } 
      if (filter.get().equals(NotificationFilter.TO_READ)) {
        condition.and(qn.read.eq(false));
      } 
    }
    if (message.isPresent()) {
      condition.and(qn.message.toLowerCase().contains(message.get().toLowerCase()));
    }
    
    return queryFactory.from(qn)
        .where(condition)
        .orderBy(qn.createdAt.desc()); 
  }
  
  /**
   * Extract SimpleResults containing the Notification list whose recipient is the given operator,
   * filtered by archived or not.
   *
   * @param operator operator which is the the notification recipient
   * @param message text filter
   * @param filter specify if it returns ALL, TO_READ, ARCHIVED Notification
   * @return SimpleResults object containing the Notification list
   */
  public ModelQuery.SimpleResults<Notification> listFor(User operator, Optional<String> message, 
      Optional<NotificationFilter> filter) {
    final QNotification qn = QNotification.notification;
    return ModelQuery.wrap(notifications(operator, message, filter), qn);
  }
  
  /**
   * Tutte le notifiche.
   * @param operator operator which is the the notification recipient
   * @param message text filter
   * @param filter specify if it returns ALL, TO_READ, ARCHIVED Notification
   */
  public List<Notification> listAllFor(User operator, Optional<String> message, 
      Optional<NotificationFilter> filter) {
    final QNotification qn = QNotification.notification;
    return notifications(operator, message, filter).list(qn);
  }
}
