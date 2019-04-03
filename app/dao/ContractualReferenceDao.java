package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import models.contractual.ContractualReference;
import models.contractual.query.QContractualReference;
import org.joda.time.LocalDate;

/**
 * DAO per i riferimenti contrattuali.
 *
 * @author cristian
 */
public class ContractualReferenceDao extends DaoBase {

  @Inject
  ContractualReferenceDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Lista degli istituti contrattuali.
   *
   * @param onlyEnabled se non presente o uguale a false mostra solo gli istituti contrattuali
   * attivi alla data corrente.
   * @return la lista degli istituti contrattuali.
   */
  public List<ContractualReference> all(Optional<Boolean> onlyEnabled) {
    QContractualReference contractualReference = QContractualReference.contractualReference;
    BooleanBuilder condition = new BooleanBuilder();
    if (onlyEnabled.or(true)) {
      condition.and(
          contractualReference.beginDate.loe(LocalDate.now()))
          .and(contractualReference.endDate.isNull()
              .or(contractualReference.beginDate.goe(LocalDate.now())));
    }
    return getQueryFactory().selectFrom(contractualReference)
        .where(condition)
        .orderBy(contractualReference.name.desc()).fetch();
  }

}
