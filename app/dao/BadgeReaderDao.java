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
import com.google.common.base.Splitter;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import helpers.jpa.ModelQuery;
import helpers.jpa.ModelQuery.SimpleResults;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.BadgeReader;
import models.BadgeSystem;
import models.Office;
import models.User;
import models.query.QBadgeReader;
import models.query.QBadgeSystem;
import models.query.QUser;

/**
 * Dao per l'accesso alle informazioni dei BadgeReader.
 *
 * @author Alessandro Martelli
 */
public class BadgeReaderDao extends DaoBase {

  public static final Splitter TOKEN_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();

  @Inject
  BadgeReaderDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Ritorna il lettore badge identificato dall'id passato.
   *
   * @return il badgereader associato al codice passato come parametro.
   */
  public BadgeReader byId(Long id) {

    final QBadgeReader badgeReader = QBadgeReader.badgeReader;
    return getQueryFactory().selectFrom(badgeReader).where(badgeReader.id.eq(id)).fetchOne();
  }

  /**
   * Ritorna il badgereader associato al codice passato.
   *
   * @return il badgereader associato al codice passato come parametro.
   */
  public BadgeReader byCode(String code) {

    final QBadgeReader badgeReader = QBadgeReader.badgeReader;
    return getQueryFactory().selectFrom(badgeReader).where(badgeReader.code.eq(code)).fetchOne();
  }

  /**
   * Il simple result dei badgeReaders.
   */
  public SimpleResults<BadgeReader> badgeReaders(Optional<String> name,
      Optional<BadgeSystem> badgeSystem) {

    final QBadgeReader badgeReader = QBadgeReader.badgeReader;
    final QBadgeSystem qBadgeSystem = QBadgeSystem.badgeSystem;

    JPQLQuery<?> query;

    final BooleanBuilder condition = new BooleanBuilder();

    if (badgeSystem.isPresent()) {
      query = getQueryFactory().select(badgeReader)
          .from(qBadgeSystem)
          .rightJoin(qBadgeSystem.badgeReaders, badgeReader);
      condition.and(qBadgeSystem.eq(badgeSystem.get()));
    } else {
      query = getQueryFactory().selectFrom(badgeReader);
    }

    if (name.isPresent()) {
      condition.and(matchBadgeReaderName(badgeReader, name.get()));
    }

    query.where(condition).distinct();

    return ModelQuery.wrap(query, badgeReader);

  }


  private BooleanBuilder matchBadgeReaderName(QBadgeReader badgeReader, String name) {
    final BooleanBuilder nameCondition = new BooleanBuilder();
    for (String token : TOKEN_SPLITTER.split(name)) {
      nameCondition.and(badgeReader.code.containsIgnoreCase(token));
    }
    return nameCondition.or(badgeReader.code.startsWithIgnoreCase(name));
  }

  /**
   * La lista degli account di tutti i badgeReader.
   */
  public List<User> usersBadgeReader() {

    final QUser user = QUser.user;
    final QBadgeReader badgeReader = QBadgeReader.badgeReader;

    return getQueryFactory().select(user).from(badgeReader).leftJoin(badgeReader.user)
        .where(badgeReader.user.isNotNull()).orderBy(badgeReader.code.asc()).fetch();
  }

  /**
   * Ritorna la lista di lettori badge associati alla sede.
   *
   * @return la lista di badgeReader di cui l'ufficio è proprietario.
   */
  public List<BadgeReader> getBadgeReaderByOffice(Office office) {
    final QBadgeReader badgeReader = QBadgeReader.badgeReader;
    return getQueryFactory().selectFrom(badgeReader)
        .where(badgeReader.user.owner.eq(office)).fetch();
  }

}
