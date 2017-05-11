package dao;

import com.google.inject.Inject;
import com.mysema.query.jpa.JPQLQueryFactory;

import helpers.jpa.ModelQuery;

import models.Notification;
import models.User;
import models.query.QNotification;

/**
 * Notification DAO.
 *
 * @author marco
 */
public class NotificationDao {

  private final JPQLQueryFactory queryFactory;

  @Inject
  NotificationDao(JPQLQueryFactory queryFactory) {
    this.queryFactory = queryFactory;
  }

  /**
   * Extract SimpleResults containing the Notifications which are not read, whose recipient is the
   * given operator and filtered by archived or not.
   *
   * @param operator operator which is the the notification recipient
   * @return SimpleResults object containing the Notification list
   */
  public ModelQuery.SimpleResults<Notification> listUnreadFor(User operator) {
    final QNotification qn = QNotification.notification;
    return ModelQuery.wrap(queryFactory.from(qn)
        .where(qn.recipient.eq(operator), qn.read.isFalse())
        .orderBy(qn.createdAt.desc()), qn);
  }

  /**
   * Extract SimpleResults containing the Notification list whose recipient is the given operator,
   * filtered by archived or not.
   *
   * @param operator operator which is the the notification recipient
   * @param archived specify if it returns only archived Notification or not
   * @return SimpleResults object containing the Notification list
   */
  public ModelQuery.SimpleResults<Notification> listFor(User operator, boolean archived) {
    final QNotification qn = QNotification.notification;
    return ModelQuery.wrap(queryFactory.from(qn)
        .where(qn.recipient.eq(operator), qn.read.eq(archived))
        .orderBy(qn.createdAt.desc()), qn);
  }
}
