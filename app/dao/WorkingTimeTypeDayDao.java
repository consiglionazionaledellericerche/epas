package dao;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.Person;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.query.QWorkingTimeTypeDay;

/**
 * @author dario
 */
public class WorkingTimeTypeDayDao extends DaoBase {

  private final WorkingTimeTypeDao workingTimeTypeDao;

  @Inject
  WorkingTimeTypeDayDao(JPQLQueryFactory queryFactory,
                        Provider<EntityManager> emp,
                        WorkingTimeTypeDao workingTimeTypeDao) {
    super(queryFactory, emp);
    this.workingTimeTypeDao = workingTimeTypeDao;
  }

  /**
   * @return il workingTimeTypeDay relativo al workingTimeType e al giorno passati come parametro
   */
  private WorkingTimeTypeDay getWorkingTimeTypeDayByDayOfWeek(WorkingTimeType wtt, Integer dayOfWeek) {

    final QWorkingTimeTypeDay wttd = QWorkingTimeTypeDay.workingTimeTypeDay;

    final JPQLQuery query = getQueryFactory().from(wttd)
            .where(wttd.workingTimeType.eq(wtt).and(wttd.dayOfWeek.eq(dayOfWeek)));

    return query.singleResult(wttd);
  }

  public WorkingTimeTypeDay getWorkingTimeTypeDay(Person person, LocalDate date) {

    //Prendo il WorkingTimeType
    Optional<WorkingTimeType> wtt = workingTimeTypeDao.getWorkingTimeType(date, person);

    if (!wtt.isPresent())
      return null;

    return getWorkingTimeTypeDayByDayOfWeek(wtt.get(), date.getDayOfWeek());
  }
}
