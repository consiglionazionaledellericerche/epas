package dao;


import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import helpers.jpa.PerseoModelQuery;
import helpers.jpa.PerseoModelQuery.PerseoSimpleResults;

import models.BadgeReader;
import models.BadgeSystem;
import models.Office;
import models.User;
import models.query.QBadgeReader;
import models.query.QBadgeSystem;
import models.query.QUser;

import java.util.List;

import javax.persistence.EntityManager;

/**
 * 
 * @author alessandro
 *
 */
public class BadgeReaderDao extends DaoBase {

  public static final Splitter TOKEN_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();

  @Inject
  BadgeReaderDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @return il badgereader associato al codice passato come parametro
   */
  public BadgeReader byId(Long id) {

    final QBadgeReader badgeReader = QBadgeReader.badgeReader;

    final JPQLQuery query = getQueryFactory().from(badgeReader).where(badgeReader.id.eq(id));
    return query.singleResult(badgeReader);
  }

  /**
   * @return il badgereader associato al codice passato come parametro
   */
  public BadgeReader byCode(String code) {

    final QBadgeReader badgeReader = QBadgeReader.badgeReader;

    final JPQLQuery query = getQueryFactory().from(badgeReader).where(badgeReader.code.eq(code));
    return query.singleResult(badgeReader);
  }

  /**
   * Il simple result dei badgeReaders-
   * 
   * @param name
   * @param badgeSystem
   * @return
   */
  public PerseoSimpleResults<BadgeReader> badgeReaders(Optional<String> name,
      Optional<BadgeSystem> badgeSystem) {

    final QBadgeReader badgeReader = QBadgeReader.badgeReader;
    final QBadgeSystem qBadgeSystem = QBadgeSystem.badgeSystem;

    JPQLQuery query = getQueryFactory()
        .from(badgeReader);
    
    final BooleanBuilder condition = new BooleanBuilder();
    
    if (badgeSystem.isPresent()) {
      query = getQueryFactory()
          .from(qBadgeSystem)
          .rightJoin(qBadgeSystem.badgeReaders, badgeReader);
      condition.and(qBadgeSystem.eq(badgeSystem.get()));
    }
   
    if (name.isPresent()) {
      condition.and(matchBadgeReaderName(badgeReader, name.get()));
    }
    
    query.where(condition).distinct();

    return PerseoModelQuery.wrap(query, badgeReader);

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

    return getQueryFactory().from(user).leftJoin(user.badgeReader)
        .where(user.badgeReader.isNotNull()).orderBy(user.badgeReader.code.asc()).list(user);
  }

  /**
   * @return la lista di badgeReader di cui l'ufficio è proprietario.
   */
  public List<BadgeReader> getBadgeReaderByOffice(Office office) {
    final QBadgeReader badgeReader = QBadgeReader.badgeReader;
    final JPQLQuery query = getQueryFactory().from(badgeReader).where(badgeReader.owner.eq(office));
    return query.list(badgeReader);
  }

}
