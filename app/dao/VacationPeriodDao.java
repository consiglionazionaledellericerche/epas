/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Contract;
import models.VacationPeriod;
import models.query.QVacationPeriod;

/**
 * Dao per i periodi di ferie.
 *
 * @author Dario Tagliaferri
 */
public class VacationPeriodDao extends DaoBase {

  @Inject
  VacationPeriodDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * La lista dei vacationPeriod associati al contratto passato come parametro.
   *
   * @param contract il contratto su cui cercare
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
