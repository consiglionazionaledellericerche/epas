/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.contractual.ContractualClause;
import models.contractual.query.QContractualClause;
import org.joda.time.LocalDate;

/**
 * DAO per gli Istituti contrattuali.
 *
 * @author Cristian Lucchesi
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