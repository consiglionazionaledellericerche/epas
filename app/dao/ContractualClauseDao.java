package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import models.absences.query.QContractualClause;
import models.contractual.ContractualClause;
import org.joda.time.LocalDate;

/**
 * DAO per gli Istituti contrattuali.
 * @author cristian
 *
 */
public class ContractualClauseDao extends DaoBase {
 
  @Inject
  ContractualClauseDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Tutti gli istituti contrattuali.
   * @return la lista degli istituti contrattuali.
   */
  public List<ContractualClause> all(boolean includeInactive) {
    QContractualClause contractualClause = QContractualClause.contractualClause;
    BooleanBuilder condition = new BooleanBuilder();
    if (!includeInactive) {
      condition.and(contractualClause.beginDate.before(LocalDate.now()));
      condition.and(
          contractualClause.endDate.isNull().or(contractualClause.endDate.after(LocalDate.now())));
    }
    return getQueryFactory().from(contractualClause)
        .where(condition)
        .orderBy(contractualClause.name.asc()).list(contractualClause);
  }
}
