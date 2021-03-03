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
import com.querydsl.jpa.impl.JPAQueryFactory;
import javax.persistence.EntityManager;

/**
 * Base dao which provides the JPQLQueryFactory and the EntityManager.
 *
 * @author Marco Andreini
 */
public abstract class DaoBase {

  protected final JPQLQueryFactory queryFactory;
  protected final Provider<EntityManager> emp;

  protected DaoBase(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    this.emp = emp;
    this.queryFactory = new JPAQueryFactory(this.emp);
  }

  protected JPQLQueryFactory getQueryFactory() {
    return queryFactory;
  }

  protected EntityManager getEntityManager() {
    return emp.get();
  }
}