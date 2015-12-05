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
import models.query.QBadgeReader;
import models.query.QBadgeSystem;

import java.util.List;

import javax.persistence.EntityManager;

/**
 * 
 * @author alessandro
 *
 */
public class BadgeSystemDao extends DaoBase {

  public static final Splitter TOKEN_SPLITTER = Splitter.on(' ')
          .trimResults().omitEmptyStrings();

  @Inject
  BadgeSystemDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @return il badge system associato al codice passato come parametro
   */
  public BadgeSystem byId(Long id) {

    final QBadgeSystem badgeSystem = QBadgeSystem.badgeSystem;

    final JPQLQuery query = getQueryFactory().from(badgeSystem)
            .where(badgeSystem.id.eq(id));
    return query.singleResult(badgeSystem);
  }

  /**
   * @return il badge system con quel nome
   */
  public BadgeSystem byName(String name) {

    final QBadgeSystem badgeSystem = QBadgeSystem.badgeSystem;

    final JPQLQuery query = getQueryFactory().from(badgeSystem)
            .where(badgeSystem.name.eq(name));
    return query.singleResult(badgeSystem);
  }

  /**
   * 
   * @param name
   * @return
   */
  public PerseoSimpleResults<BadgeSystem> badgeSystems(Optional<String> name, 
      Optional<BadgeReader> badgeReader) {

    final QBadgeSystem badgeSystem = QBadgeSystem.badgeSystem;
    final QBadgeReader qBadgeReader = QBadgeReader.badgeReader;

    final JPQLQuery query = getQueryFactory()
        .from(badgeSystem);

    
    final BooleanBuilder condition = new BooleanBuilder();
    if (name.isPresent()) {
      condition.and(matchBadgeSystemName(badgeSystem, name.get()));
    }
    
    if (badgeReader.isPresent()) {
      query.leftJoin(badgeSystem.badgeReaders, qBadgeReader);
      condition.and(badgeSystem.badgeReaders.contains(qBadgeReader));
    }

    query.where(condition).distinct();

    return PerseoModelQuery.wrap(query, badgeSystem);

  }


  private BooleanBuilder matchBadgeSystemName(QBadgeSystem badgeSystem, String name) {
    final BooleanBuilder nameCondition = new BooleanBuilder();
    for (String token : TOKEN_SPLITTER.split(name)) {
      nameCondition.and(badgeSystem.name.containsIgnoreCase(token));
    }
    return nameCondition.or(badgeSystem.name.startsWithIgnoreCase(name));
  }

  /**
   * @return la lista di badgeSystem di cui l'ufficio è proprietario.
   */
  public List<BadgeSystem> byOffice(Office office) {
	  
    final QBadgeSystem badgeSystem = QBadgeSystem.badgeSystem;

    return getQueryFactory()
    		.from(badgeSystem)
    		.where(badgeSystem.owner.eq(office)).list(badgeSystem);
  }

}
