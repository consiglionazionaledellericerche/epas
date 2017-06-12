package dao;


import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQueryFactory;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.ShiftType;
import models.ShiftTypeMonth;
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

    return Optional
        .fromNullable(getQueryFactory().from(stm).where(stm.id.eq(id)).singleResult(stm));
  }

  public Optional<ShiftTypeMonth> byShiftTypeAndDate(ShiftType shiftType, LocalDate date) {
    final QShiftTypeMonth stm = QShiftTypeMonth.shiftTypeMonth;
    final YearMonth yearMonth = new YearMonth(date);

    return Optional.fromNullable(getQueryFactory().from(stm)
        .where(stm.shiftType.eq(shiftType).and(stm.yearMonth.eq(yearMonth))).singleResult(stm)
    );
  }
}
