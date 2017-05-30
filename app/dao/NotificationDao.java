package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import com.mysema.query.BooleanBuilder;
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
  public ModelQuery.SimpleResults<Notification> listUnreadFor(User operator, 
      Optional<String> message) {
    final QNotification qn = QNotification.notification;
    final BooleanBuilder condition = new BooleanBuilder()
        .and(qn.recipient.eq(operator))
        .and(qn.read.isFalse());
    if (message.isPresent()) {
      condition.and(qn.message.toLowerCase().contains(message.get().toLowerCase()));
    }
    return ModelQuery.wrap(queryFactory.from(qn)
        .where(condition)
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
  public ModelQuery.SimpleResults<Notification> listFor(User operator, Optional<String> message, 
      boolean archived) {
    final QNotification qn = QNotification.notification;
    final BooleanBuilder condition = new BooleanBuilder()
        .and(qn.recipient.eq(operator))
        .and(qn.read.eq(archived));
    if (message.isPresent()) {
      condition.and(qn.message.toLowerCase().contains(message.get().toLowerCase()));
    }
    return ModelQuery.wrap(queryFactory.from(qn)
        .where(condition)
        .orderBy(qn.createdAt.desc()), qn);
  }
}
