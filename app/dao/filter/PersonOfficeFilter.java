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

package dao.filter;

import com.google.common.collect.ImmutableSet;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import java.util.Collection;
import models.Office;
import models.query.QPerson;
import models.query.QPersonOffice;
import org.joda.time.LocalDate;

/**
 * Filtri QueryDSL sull'afferenza periodicizzata delle persone alle sedi (person_offices).
 */
public final class PersonOfficeFilter {

  private PersonOfficeFilter() {
  }

  /**
   * Sottoquery con gli id delle persone che afferiscono oggi alla sede.
   */
  public static JPQLQuery<Long> personIdsInOffices(Office office) {
    return personIdsInOffices(ImmutableSet.of(office));
  }

  /**
   * Sottoquery con gli id delle persone che afferiscono oggi ad una delle sedi.
   */
  public static JPQLQuery<Long> personIdsInOffices(Collection<Office> offices) {
    final LocalDate today = LocalDate.now();
    return personIdsInOffices(offices, today, today);
  }

  /**
   * Sottoquery con gli id delle persone che hanno afferito alla sede in almeno un giorno
   * dell'intervallo [begin, end].
   */
  public static JPQLQuery<Long> personIdsInOffices(Office office, LocalDate begin,
      LocalDate end) {
    return personIdsInOffices(ImmutableSet.of(office), begin, end);
  }

  /**
   * Sottoquery con gli id delle persone che hanno afferito ad almeno una delle sedi in almeno un
   * giorno dell'intervallo [begin, end]. Se end è null l'intervallo è aperto.
   */
  public static JPQLQuery<Long> personIdsInOffices(Collection<Office> offices, LocalDate begin,
      LocalDate end) {
    final QPersonOffice po = new QPersonOffice("personOfficeInPeriod");
    return JPAExpressions.selectFrom(po)
        .where(po.office.in(offices)
            .and(end == null ? null : po.beginDate.loe(end))
            .and(po.endDate.isNull().or(po.endDate.goe(begin))))
        .select(po.person.id);
  }

  /**
   * Condizione vera se la persona afferiva ad una delle sedi alla data indicata, tipicamente la
   * data del record su cui si sta filtrando (giorno di lavoro, timbratura, ecc.).
   */
  public static BooleanExpression inOfficesAt(QPerson person, Collection<Office> offices,
      DateExpression<LocalDate> date) {
    final QPersonOffice po = new QPersonOffice("personOfficeAtDate");
    return JPAExpressions.selectOne().from(po)
        .where(po.person.eq(person),
            po.office.in(offices),
            po.beginDate.loe(date),
            po.endDate.isNull().or(po.endDate.goe(date)))
        .exists();
  }

  /**
   * Condizione vera se la persona afferiva alla sede alla data indicata.
   */
  public static BooleanExpression inOfficeAt(QPerson person, Office office,
      DateExpression<LocalDate> date) {
    return inOfficesAt(person, ImmutableSet.of(office), date);
  }
}
