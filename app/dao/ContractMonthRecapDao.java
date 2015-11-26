package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import dao.filter.QFilters;

import models.ContractMonthRecap;
import models.Office;
import models.query.QContract;
import models.query.QContractMonthRecap;
import models.query.QPerson;

import org.joda.time.YearMonth;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

/**
 * DAO per i riepiloghi mensili.
 *
 * - situazione residuale minuti anno passato e anno corrente - situazione residuale buoni pasto
 *
 * - situazione residuale ferie (solo per il mese di dicembre)
 *
 * @author alessandro
 */
public class ContractMonthRecapDao extends DaoBase {

  @Inject
  ContractMonthRecapDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * I riepiloghi delle persone con un massimo di buoni pasto passato come parametro.
   *
   * TODO: il filtro sugli office delle persone.
   */
  public List<ContractMonthRecap> getPersonMealticket(YearMonth yearMonth,
                                                      Optional<Integer> max, Optional<String> name, Set<Office> offices) {

    final QContractMonthRecap recap = QContractMonthRecap.contractMonthRecap;
    final QContract contract = QContract.contract;
    final QPerson person = QPerson.person;

    final BooleanBuilder condition = new BooleanBuilder();
    if (max.isPresent()) {
      condition.and(recap.remainingMealTickets.loe(max.get()));
    }
    condition.and(new QFilters().filterNameFromPerson(person, name));

    final JPQLQuery query = getQueryFactory().from(recap)
            .leftJoin(recap.contract, contract)
            .leftJoin(contract.person, person)
            .where(recap.year.eq(yearMonth.getYear())
                    .and(recap.month.eq(yearMonth.getMonthOfYear())
                            .and(person.office.in(offices))
                            .and(condition))).orderBy(recap.contract.person.surname.asc());

    return query.list(recap);
  }
}
