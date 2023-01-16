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
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Office;
import models.Person;
import models.StampModificationType;
import models.Stamping;
import models.Stamping.WayType;
import models.query.QPerson;
import models.query.QStampModificationType;
import models.query.QStamping;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;

/**
 * DAO per l'accesso alle informazioni delle timbrature.
 *
 * @author Dario Tagliaferri
 */
public class StampingDao extends DaoBase {

  @Inject
  StampingDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Ritorna la prima (eventuale) timbratura che corrisponde ai dati passati.
   *
   * @param dateTime data della timbratura
   * @param person persona a cui si riferisce
   * @param way verso.
   * @return la prima timbratura (ordinando per id decrescente) trovata, oppure Optional::absent
   */
  public Optional<Stamping> getStamping(LocalDateTime dateTime, Person person, WayType way) {
    final QStamping stamping = QStamping.stamping;
    final Stamping result = getQueryFactory().selectFrom(stamping)
        .where(stamping.date.eq(dateTime).and(stamping.personDay.person.eq(person))
            .and(stamping.way.eq(way)))
        .orderBy(stamping.id.desc())
        .limit(1)
        .fetchOne();
    return Optional.fromNullable(result);
  }

  /**
   * Preleva una timbratura tramite il suo id.
   *
   * @param id l'id associato alla Timbratura sul db.
   * @return la timbratura corrispondente all'id passato come parametro.
   */
  public Stamping getStampingById(Long id) {
    final QStamping stamping = QStamping.stamping;
    return getQueryFactory().selectFrom(stamping)
        .where(stamping.id.eq(id)).fetchOne();
  }

  /**
   * lo stampModificationType relativo all'id passato come parametro.
   */
  @Deprecated
  public StampModificationType getStampModificationTypeById(Long id) {
    final QStampModificationType smt = QStampModificationType.stampModificationType;

    return getQueryFactory().selectFrom(smt)
        .where(smt.id.eq(id)).fetchOne();
  }


  /**
   * Lista delle timbrature inserire dall'amministratore in un determinato mese.
   *
   * @param yearMonth mese di riferimento
   * @param office ufficio
   * @return lista delle timbrature inserite dell'amministratore
   */
  public List<Stamping> adminStamping(YearMonth yearMonth, Office office) {
    final QStamping stamping = QStamping.stamping;
    final QPerson person = QPerson.person;
    return getQueryFactory().selectFrom(stamping)
        .join(stamping.personDay.person, person)
        .where(stamping.markedByAdmin.eq(true)
            .and(stamping.personDay.date.goe(yearMonth.toLocalDate(1)))
            .and(stamping.personDay.date
                .loe(yearMonth.toLocalDate(1).dayOfMonth()
                    .withMaximumValue()))
            .and(person.office.isNotNull())
            .and(person.office.eq(office)))
        .orderBy(person.surname.asc(), stamping.personDay.date.asc())
        .fetch();
  }
}