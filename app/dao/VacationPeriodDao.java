package dao;

import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.Contract;
import models.VacationPeriod;
import models.query.QVacationPeriod;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

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
    final JPQLQuery query = getQueryFactory().from(vacationPeriod)
            .where(vacationPeriod.contract.eq(contract));
    return query.orderBy(vacationPeriod.beginDate.asc()).list(vacationPeriod);
  }
}
