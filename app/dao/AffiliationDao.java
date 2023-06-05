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

import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.Optional;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.flows.Affiliation;
import models.flows.query.QAffiliation;

/**
 * DAO per le affiliazioni.
 */
public class AffiliationDao extends DaoBase {

  @Inject
  AffiliationDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Affiliazione prelevata per id.
   */
  public Optional<Affiliation> byId(Long id) {
    QAffiliation affilation = QAffiliation.affiliation;
    return Optional.ofNullable((Affiliation) getQueryFactory().from(affilation)
        .where(affilation.id.eq(id)).fetchOne());
  }
}