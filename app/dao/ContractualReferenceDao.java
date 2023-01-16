/*
 * Copyright (C) 2022  Consiglio Nazionale delle Ricerche
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
import models.contractual.ContractualReference;
import models.contractual.query.QContractualReference;
import org.joda.time.LocalDate;

/**
 * DAO per i riferimenti contrattuali.
 *
 * @author Cristian Lucchesi
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
   *     attivi alla data corrente.
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