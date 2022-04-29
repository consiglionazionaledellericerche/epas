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

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.filter.QFilters;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.query.QContract;
import models.query.QContractMonthRecap;
import models.query.QPerson;
import org.joda.time.YearMonth;

/**
 * DAO per i riepiloghi mensili.
 * <p>
 * - situazione residuale minuti anno passato e anno corrente - situazione residuale buoni pasto
 *
 * - situazione residuale ferie (solo per il mese di dicembre)
 * </p>
 *
 * @author Alessandro Martelli
 */
public class ContractMonthRecapDao extends DaoBase {

  @Inject
  ContractMonthRecapDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * I riepiloghi delle persone con un massimo di buoni pasto passato come parametro. TODO: il
   * filtro sugli office delle persone.
   */
  public List<ContractMonthRecap> getPersonMealticket(
      YearMonth yearMonth, Optional<Integer> max, Optional<String> name,
      Set<Office> offices) {

    final QContractMonthRecap recap = QContractMonthRecap.contractMonthRecap;
    final QContract contract = QContract.contract;
    final QPerson person = QPerson.person;

    final BooleanBuilder condition = new BooleanBuilder();
    if (max.isPresent()) {
      condition.and(recap.remainingMealTickets.loe(max.get()));
    }
    condition.and(new QFilters().filterNameFromPerson(person, name));

    return getQueryFactory().selectFrom(recap)
        .leftJoin(recap.contract, contract)
        .leftJoin(contract.person, person)
        .where(recap.year.eq(yearMonth.getYear())
            .and(recap.month.eq(yearMonth.getMonthOfYear())
                .and(person.office.in(offices))
                .and(condition))).orderBy(recap.contract.person.surname.asc())
        .fetch();
  }
  
  /**
   * Ritorna il riepilogo del contratto contract nell'anno/mese yearMonth.
   *
   * @param contract il contratto da riepilogare
   * @param yearMonth l'anno mese di riferimento
   * @return Il riepilogo del contratto nell'anno mese
   */
  public ContractMonthRecap getContractMonthRecap(Contract contract, YearMonth yearMonth) {
    final QContractMonthRecap recap = QContractMonthRecap.contractMonthRecap;
    return getQueryFactory().selectFrom(recap)
        .where(recap.contract.eq(contract).and(recap.year.eq(yearMonth.getYear())
            .and(recap.month.eq(yearMonth.getMonthOfYear())))).fetchFirst();
  }
}