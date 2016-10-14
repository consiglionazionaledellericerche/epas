package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.Badge;
import models.BadgeReader;
import models.Person;
import models.query.QBadge;

import java.util.List;

import javax.persistence.EntityManager;

public class BadgeDao extends DaoBase {

  @Inject
  BadgeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @param code        il codice del badge.
   * @param badgeReader opzionale
   * @return l'oggetto badge identificato dal codice code passato come parametro.
   */
  public Optional<Badge> byCode(String code, BadgeReader badgeReader) {
    final QBadge badge = QBadge.badge;

    final JPQLQuery query = getQueryFactory()
        .from(badge)
        .where(badge.code.eq(code)
            .and(badge.badgeReader.eq(badgeReader)));

    return Optional.fromNullable(query.singleResult(badge));
  }

  /**
   * @param id identificativo del badge richiesto
   * @return il badge con identificativo passato come parametro.
   */
  public Badge byId(Long id) {
    final QBadge badge = QBadge.badge;
    final JPQLQuery query = getQueryFactory().from(badge).where(badge.id.eq(id));
    return query.singleResult(badge);
  }

  /**
   * @param code   il numero badge.
   * @param person la persona proprietaria dei badge
   * @return la lista di tutti i record di badge con lo stesso code per la persona specificata
   */
  public List<Badge> byCodeAndPerson(String code, Person person) {
    final QBadge badge = QBadge.badge;

    return queryFactory.from(badge).where(badge.code.eq(code).and(badge.person.eq(person)))
        .list(badge);
  }

}
