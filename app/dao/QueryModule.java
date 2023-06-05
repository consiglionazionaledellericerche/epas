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

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import common.injection.AutoRegister;
import javax.persistence.EntityManager;
import play.db.jpa.JPA;

/**
 * Modulo per l'injection del EntityManager e del JPQLQueryFactory.
 */
@AutoRegister
public class QueryModule extends AbstractModule {

  /**
   * Fornisce un EntityManager per l'injection.
   */
  @Provides
  public EntityManager getEntityManager() {
    return JPA.em();
  }

  /**
   * Fornisce un JPQLQueryFactory per l'injection.
   */
  @Provides
  public JPQLQueryFactory createJpqlQueryFactory(Provider<EntityManager> emp) {
    return new JPAQueryFactory(emp);
  }

  @Override
  protected void configure() {

  }

}