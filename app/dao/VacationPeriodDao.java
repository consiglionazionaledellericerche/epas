package dao;

import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Contract;
import models.VacationPeriod;
import models.query.QVacationPeriod;

/**
 * Dao per i periodi di ferie.
 *
 * @author dario
 */
public class VacationPeriodDao extends DaoBase {

  @Inject
  VacationPeriodDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
    // TODO Auto-generated constructor stub
  }

  /**
   * @return la lista dei vacationPeriod associati al contratto passato come parametro.
   */
  public List<VacationPeriod> getVacationPeriodByContract(Contract contract) {
    final QVacationPeriod vacationPeriod = QVacationPeriod.vacationPeriod;
    return getQueryFactory().selectFrom(vacationPeriod)
        .where(vacationPeriod.contract.eq(contract))
        .orderBy(vacationPeriod.beginDate.asc())
        .fetch();
  }
}
