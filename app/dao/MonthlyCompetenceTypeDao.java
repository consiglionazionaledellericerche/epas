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
import dao.wrapper.IWrapperFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.MonthlyCompetenceType;
import models.query.QMonthlyCompetenceType;

/**
 * DAO per le MonthlyCompetenceType.
 */
public class MonthlyCompetenceTypeDao extends DaoBase {

  @Inject
  MonthlyCompetenceTypeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory wrapperFactory) {
    super(queryFactory, emp);
  }
  
  /**
   * Metodo che ritorna la lista di tutti i tipi di competenza mensile.
   *
   * @return la lista di tutti i tipi di competenza mensile.
   */
  public List<MonthlyCompetenceType> listTypes() {
    final QMonthlyCompetenceType monthlyType = QMonthlyCompetenceType.monthlyCompetenceType;
    return getQueryFactory().selectFrom(monthlyType).fetch();
  }
}