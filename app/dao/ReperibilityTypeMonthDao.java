/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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
import models.PersonReperibilityType;
import models.ReperibilityTypeMonth;
import models.query.QPersonReperibilityDay;
import models.query.QPersonReperibilityType;
import models.query.QReperibilityTypeMonth;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;


/**
 * DAO per ReperibilityTypeMonth.
 */
public class ReperibilityTypeMonthDao extends DaoBase {

  @Inject
  ReperibilityTypeMonthDao(JPQLQueryFactory queryFactory,
      Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Il riepilogo mensile della reperibilità, se esiste, che contiene le info.
   *
   * @param reperibilityType il tipo di attività di reperibilità
   * @param date la data da ricercare
   * @return il reperibilityTypeMonth che contiene le informazioni richieste.
   */
  public Optional<ReperibilityTypeMonth> byReperibilityTypeAndDate(
      PersonReperibilityType reperibilityType, LocalDate date) {
    final QReperibilityTypeMonth rtm = QReperibilityTypeMonth.reperibilityTypeMonth;
    final YearMonth yearMonth = new YearMonth(date);

    return Optional.fromNullable(getQueryFactory()
        .selectFrom(rtm).where(rtm.personReperibilityType.eq(reperibilityType)
            .and(rtm.yearMonth.eq(yearMonth))).fetchOne());
  }

  /**
   * Il riepilogo mensile della reperibilità, se esiste, relativo all'id passato.
   *
   * @param id l'identificativo del reperibilityTypeMonth
   * @return il reperibilityTypeMonth con id passato come parametro.
   */
  public Optional<ReperibilityTypeMonth> byId(long id) {
    final QReperibilityTypeMonth rtm = QReperibilityTypeMonth.reperibilityTypeMonth;

    return Optional.fromNullable(getQueryFactory().selectFrom(rtm).where(rtm.id.eq(id)).fetchOne());
  }

  /**
   * La lista dei riepiloghi di reperibilità mensile relativi alla sede nell'anno/mese.
   *
   * @param office la sede di riferimento
   * @param month l'anno/mese da controllare
   * @return la lista dei reperibilityTypeMonth appartenenti alla sede e all'anno/mese passati come
   *     parametro.
   */
  public List<ReperibilityTypeMonth> byOfficeInMonth(Office office, YearMonth month) {
    final QReperibilityTypeMonth rtm = QReperibilityTypeMonth.reperibilityTypeMonth;
    final QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;

    return getQueryFactory().selectFrom(rtm)
        .leftJoin(rtm.personReperibilityType, prt)
        .where(rtm.yearMonth.eq(month).and(prt.office.eq(office))).distinct().fetch();

  }

  /**
   * Questo metodo è utile in fase di assegnazione delle competenze in seguito all'approvazione del
   * responsabile di turno (bisogna ricalcolare tutte le competenze delle persone coinvolte).
   *
   * @param month mese richiesto
   * @param people lista delle persone coinvolte nel mese richiesto
   * @return La lista
   */
  public List<ReperibilityTypeMonth> approvedInMonthRelatedWith(YearMonth month,
      List<Person> people) {

    final QReperibilityTypeMonth rtm = QReperibilityTypeMonth.reperibilityTypeMonth;
    final QPersonReperibilityDay prd = QPersonReperibilityDay.personReperibilityDay;

    final LocalDate monthBegin = month.toLocalDate(1);
    final LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();

    return getQueryFactory().select(rtm).from(prd)
        .leftJoin(prd.reperibilityType.monthsStatus, rtm)
        .where(prd.personReperibility.person.in(people)
            .and(prd.date.goe(monthBegin))
            .and(prd.date.loe(monthEnd))
            .and(rtm.yearMonth.eq(month).and(rtm.approved.isTrue()))).distinct().fetch();
  }
}