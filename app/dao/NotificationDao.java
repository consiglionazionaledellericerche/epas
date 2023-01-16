/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

package dao;

import com.google.common.base.Optional;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import helpers.jpa.ModelQuery;
import java.util.List;
import javax.inject.Inject;
import models.Notification;
import models.User;
import models.enumerate.NotificationSubject;
import models.query.QNotification;

/**
 * Notification DAO.
 *
 * @author Marco Andreini
 */
public class NotificationDao {

  /**
   * Possibili fitri sulle Notifiche.
   */
  public enum NotificationFilter {
    ALL, TO_READ, ARCHIVED
  }

  private final JPQLQueryFactory queryFactory;

  @Inject
  NotificationDao(JPQLQueryFactory queryFactory) {
    this.queryFactory = queryFactory;
  }

  private JPQLQuery<Notification> notifications(User operator, Optional<String> message,
      Optional<NotificationFilter> filter, Optional<NotificationSubject> subject) {

    final QNotification qn = QNotification.notification;
    final BooleanBuilder condition = new BooleanBuilder().and(qn.recipient.eq(operator));
    if (filter.isPresent()) {
      if (filter.get() == NotificationFilter.ARCHIVED) {
        condition.and(qn.read.eq(true));
      } 
      if (filter.get() == NotificationFilter.TO_READ) {
        condition.and(qn.read.eq(false));
      } 
    }
    if (message.isPresent()) {
      condition.and(qn.message.toLowerCase().contains(message.get().toLowerCase()));
    }
    if (subject.isPresent()) {
      if (subject.get() == NotificationSubject.ABSENCE_REQUEST) {
        condition.and(qn.subject.eq(NotificationSubject.ABSENCE_REQUEST));
      }
    }
    
    return queryFactory.selectFrom(qn)
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
      Optional<NotificationFilter> filter, Optional<NotificationSubject> subject) {
    final QNotification qn = QNotification.notification;
    return ModelQuery.wrap(notifications(operator, message, filter, subject), qn);
  }

  /**
   * Tutte le notifiche.
   *
   * @param operator operator which is the the notification recipient
   * @param message text filter
   * @param filter specify if it returns ALL, TO_READ, ARCHIVED Notification
   */
  public List<Notification> listAllFor(User operator, Optional<String> message, 
      Optional<NotificationFilter> filter, Optional<NotificationSubject> subject) {
    return notifications(operator, message, filter, subject).fetch();
  } 

}