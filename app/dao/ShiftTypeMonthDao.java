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
import models.ShiftType;
import models.ShiftTypeMonth;
import models.query.QPersonShiftDay;
import models.query.QShiftCategories;
import models.query.QShiftTypeMonth;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

/**
 * Il dao sui riepiloghi di turno mensili.
 *
 * @author Daniele Murgia
 * @since 10/06/17
 */
public class ShiftTypeMonthDao extends DaoBase {

  @Inject
  ShiftTypeMonthDao(JPQLQueryFactory queryFactory,
      Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Il riepilogo di turno mensile con id passato come parametro.
   *
   * @param id l'id del riepilogo.
   * @return Il riepilogo di turno mensile con id passato come parametro.
   */
  public Optional<ShiftTypeMonth> byId(long id) {
    final QShiftTypeMonth stm = QShiftTypeMonth.shiftTypeMonth;

    return Optional.fromNullable(getQueryFactory().selectFrom(stm).where(stm.id.eq(id)).fetchOne());
  }

  /**
   * Il riepilogo di turno mensile relativo all'attività shiftType alla data date.
   *
   * @param shiftType l'attività di turno
   * @param date la data
   * @return il riepilogo di turno mensile relativo all'attività shiftType alla data date.
   */
  public Optional<ShiftTypeMonth> byShiftTypeAndDate(ShiftType shiftType, LocalDate date) {
    final QShiftTypeMonth stm = QShiftTypeMonth.shiftTypeMonth;
    final YearMonth yearMonth = new YearMonth(date);

    return Optional.fromNullable(getQueryFactory().selectFrom(stm)
        .where(stm.shiftType.eq(shiftType).and(stm.yearMonth.eq(yearMonth))).fetchOne());
  }

  /**
   * Questo metodo è utile in fase di assegnazione delle competenze in seguito all'approvazione del
   * responsabile di turno (bisogna ricalcolare tutte le competenze delle persone coinvolte).
   *
   * @param month mese richiesto
   * @param people lista delle persone coinvolte nel mese richiesto
   * @return La lista
   */
  public List<ShiftTypeMonth> approvedInMonthRelatedWith(YearMonth month, List<Person> people) {

    final QShiftTypeMonth stm = QShiftTypeMonth.shiftTypeMonth;
    final QPersonShiftDay psd = QPersonShiftDay.personShiftDay;

    final LocalDate monthBegin = month.toLocalDate(1);
    final LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();

    return getQueryFactory().select(stm).from(psd)
        .leftJoin(psd.shiftType.monthsStatus, stm)
        .where(psd.personShift.person.in(people)
            .and(psd.date.goe(monthBegin))
            .and(psd.date.loe(monthEnd))
            .and(stm.yearMonth.eq(month).and(stm.approved.isTrue()))).distinct().fetch();
  }

  /**
   * La lista dei riepiloghi mensili di turno della sede office nell'anno/mese.
   *
   * @param office la sede su cui cercare i riepiloghi
   * @param month l'anno/mese su cui cercare i riepiloghi
   * @return la lista dei riepiloghi mensili di turno della sede office nell'anno/mese.
   */
  public List<ShiftTypeMonth> byOfficeInMonth(Office office, YearMonth month) {
    final QShiftTypeMonth stm = QShiftTypeMonth.shiftTypeMonth;
    final QShiftCategories sc = QShiftCategories.shiftCategories;

    return getQueryFactory().selectFrom(stm)
        .leftJoin(stm.shiftType.shiftCategories, sc)
        .where(stm.yearMonth.eq(month).and(sc.office.eq(office))).distinct().fetch();

  }
}