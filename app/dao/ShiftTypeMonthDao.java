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
 * @author daniele
 * @since 10/06/17.
 */
public class ShiftTypeMonthDao extends DaoBase {

  @Inject
  ShiftTypeMonthDao(JPQLQueryFactory queryFactory,
      Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  public Optional<ShiftTypeMonth> byId(long id) {
    final QShiftTypeMonth stm = QShiftTypeMonth.shiftTypeMonth;

    return Optional.fromNullable(getQueryFactory().selectFrom(stm).where(stm.id.eq(id)).fetchOne());
  }

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

  public List<ShiftTypeMonth> byOfficeInMonth(Office office, YearMonth month) {
    final QShiftTypeMonth stm = QShiftTypeMonth.shiftTypeMonth;
    final QShiftCategories sc = QShiftCategories.shiftCategories;

    return getQueryFactory().selectFrom(stm)
        .leftJoin(stm.shiftType.shiftCategories, sc)
        .where(stm.yearMonth.eq(month).and(sc.office.eq(office))).distinct().fetch();

  }
}
