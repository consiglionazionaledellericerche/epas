package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import models.contractual.ContractualClause;
import models.contractual.query.QContractualClause;
import org.joda.time.LocalDate;

/**
 * DAO per gli Istituti contrattuali.
 *
 * @author cristian
 */
public class ContractualClauseDao extends DaoBase {

  @Inject
  ContractualClauseDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Lista degli istituti contrattuali.
   *
   * @param onlyEnabled se non presente o uguale a false mostra solo gli istituti contrattuali
   *     attivi alla data corrente.
   * @return la lista degli istituti contrattuali.
   */
  public List<ContractualClause> all(Optional<Boolean> onlyEnabled) {
    QContractualClause contractualClause = QContractualClause.contractualClause;
    BooleanBuilder condition = new BooleanBuilder();
    if (onlyEnabled.or(true)) {
      condition.and(
          contractualClause.beginDate.loe(LocalDate.now()))
          .and(contractualClause.endDate.isNull()
              .or(contractualClause.beginDate.goe(LocalDate.now())));
    }
    return getQueryFactory().selectFrom(contractualClause).where(condition)
        .orderBy(contractualClause.name.desc()).fetch();
  }

}
