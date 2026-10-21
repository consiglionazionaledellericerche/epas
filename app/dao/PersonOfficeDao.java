/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Person;
import models.PersonOffice;
import models.query.QPersonOffice;
import org.joda.time.LocalDate;

/**
 * Dao per l'accesso alle afferenze periodicizzate delle persone alle sedi.
 */
public class PersonOfficeDao extends DaoBase {

  @Inject
  PersonOfficeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * L'afferenza con l'id indicato.
   */
  public PersonOffice getById(Long id) {
    final QPersonOffice po = QPersonOffice.personOffice;
    return getQueryFactory().selectFrom(po).where(po.id.eq(id)).fetchOne();
  }

  /**
   * Le afferenze della persona ordinate per data di inizio.
   */
  public List<PersonOffice> byPerson(Person person) {
    final QPersonOffice po = QPersonOffice.personOffice;
    return getQueryFactory().selectFrom(po).where(po.person.eq(person))
        .orderBy(po.beginDate.asc()).fetch();
  }

  /**
   * Le afferenze che iniziano alla data indicata.
   */
  public List<PersonOffice> startingAt(LocalDate date) {
    final QPersonOffice po = QPersonOffice.personOffice;
    return getQueryFactory().selectFrom(po).where(po.beginDate.eq(date)).fetch();
  }
}
