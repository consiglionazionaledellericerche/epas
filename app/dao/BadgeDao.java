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
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Badge;
import models.BadgeReader;
import models.BadgeSystem;
import models.Office;
import models.Person;
import models.query.QBadge;

/**
 * DAO per i badge.
 */
public class BadgeDao extends DaoBase {

  @Inject
  BadgeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Ritorna il badge identificato dal codice e dal badgereader.
   *
   * @param code il codice del badge.
   * @param badgeReader opzionale
   * @return l'oggetto badge identificato dal codice code passato come parametro.
   */
  public Optional<Badge> byCode(String code, BadgeReader badgeReader) {
    final QBadge badge = QBadge.badge;

    final JPQLQuery<Badge> query = getQueryFactory()
        .selectFrom(badge)
        .where(badge.code.eq(code)
            .and(badge.badgeReader.eq(badgeReader)));

    return Optional.fromNullable(query.fetchOne());
  }

  /**
   * Ritorna il badge identificato dall'id passato come parametro.
   *
   * @param id identificativo del badge richiesto
   * @return il badge con identificativo passato come parametro.
   */
  public Badge byId(Long id) {
    final QBadge badge = QBadge.badge;
    return getQueryFactory().selectFrom(badge).where(badge.id.eq(id)).fetchOne();
  }

  /**
   * RItorna la lista di badge per codice e persona.
   *
   * @param code il numero badge.
   * @param person la persona proprietaria dei badge
   * @return la lista di tutti i record di badge con lo stesso code per la persona specificata
   */
  public List<Badge> byCodeAndPerson(String code, Person person) {
    final QBadge badge = QBadge.badge;

    return queryFactory.selectFrom(badge).where(badge.code.eq(code).and(badge.person.eq(person)))
        .fetch();
  }

  /**
   * La lista dei badge per ufficio.
   *
   * @param office la sede per cui ricercare i badge
   * @return la lista dei badge appartenenti alla sede.
   */
  public List<Badge> byOffice(Office office) {
    final QBadge badge = QBadge.badge;
    
    return queryFactory.selectFrom(badge)
        .where(badge.badgeSystem.office.eq(office))
        .fetch();
  }

  /**
   * La lista dei badge per gruppo badge.
   *
   * @param badgeSystem il gruppo badge di cui ritornare i badge associati
   * @return la lista di badge associata al gruppo badge.
   */
  public List<Badge> byBadgeSystem(BadgeSystem badgeSystem) {
    final QBadge badge = QBadge.badge;
    
    return queryFactory.selectFrom(badge)
        .where(badge.badgeSystem.eq(badgeSystem))
        .fetch();
  }
}