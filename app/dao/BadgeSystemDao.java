package dao;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import helpers.jpa.ModelQuery;
import helpers.jpa.ModelQuery.SimpleResults;
import java.util.List;
import javax.persistence.EntityManager;
import models.Badge;
import models.BadgeReader;
import models.BadgeSystem;
import models.query.QBadge;
import models.query.QBadgeReader;
import models.query.QBadgeSystem;

/**
 * Dao per l'accesso alle informazioni dei BadgeSystem.
 *
 * @author alessandro
 */
public class BadgeSystemDao extends DaoBase {

  public static final Splitter TOKEN_SPLITTER = Splitter.on(' ')
      .trimResults().omitEmptyStrings();

  @Inject
  BadgeSystemDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Ritorna il gruppo badge relativo all'id passato.
   * @return il badge system associato al codice passato come parametro.
   */
  public BadgeSystem byId(Long id) {

    final QBadgeSystem badgeSystem = QBadgeSystem.badgeSystem;

    return getQueryFactory().selectFrom(badgeSystem)
        .where(badgeSystem.id.eq(id)).fetchOne();
  }

  /**
   * Ritorna il gruppo badge associato al nome passato.
   * @return il badge system con quel nome.
   */
  public BadgeSystem byName(String name) {

    final QBadgeSystem badgeSystem = QBadgeSystem.badgeSystem;

    return getQueryFactory().selectFrom(badgeSystem)
        .where(badgeSystem.name.eq(name)).fetchOne();
  }

  /**
   * Ritorna il gruppo badge con nome (opzionale) e appartenente al lettore
   * badge passati.
   * @param name il nome del gruppo badge
   * @param badgeReader il lettore badge
   * @return il gruppo badge relativo a nome e lettore badge passati.
   */
  public SimpleResults<BadgeSystem> badgeSystems(Optional<String> name,
      Optional<BadgeReader> badgeReader) {

    final QBadgeSystem badgeSystem = QBadgeSystem.badgeSystem;
    final QBadgeReader qBadgeReader = QBadgeReader.badgeReader;

    JPQLQuery<?> query;

    final BooleanBuilder condition = new BooleanBuilder();

    if (badgeReader.isPresent()) {
      query = getQueryFactory().select(badgeSystem)
          .from(qBadgeReader)
          .rightJoin(qBadgeReader.badgeSystems, badgeSystem);
      condition.and(qBadgeReader.eq(badgeReader.get()));
    } else {
      query = getQueryFactory().from(badgeSystem);
    }
    if (name.isPresent()) {
      condition.and(matchBadgeSystemName(badgeSystem, name.get()));
    }

    query.where(condition).distinct();

    return ModelQuery.wrap(query, badgeSystem);

  }


  private BooleanBuilder matchBadgeSystemName(QBadgeSystem badgeSystem, String name) {
    final BooleanBuilder nameCondition = new BooleanBuilder();
    for (String token : TOKEN_SPLITTER.split(name)) {
      nameCondition.and(badgeSystem.name.containsIgnoreCase(token));
    }
    return nameCondition.or(badgeSystem.name.startsWithIgnoreCase(name));
  }

  /**
   * Tutti i badge di badgeSystem ordinati per codice badge (e persona).
   */
  public List<Badge> badges(BadgeSystem badgeSystem) {

    final QBadge badge = QBadge.badge;

    return getQueryFactory()
        .selectFrom(badge)
        .where(badge.badgeSystem.eq(badgeSystem))
        .orderBy(badge.code.asc()).orderBy(badge.person.id.asc()).fetch();

  }

}
