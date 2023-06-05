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
import models.Institute;
import models.query.QInstitute;

/**
 * Dao per gli istituti.
 *
 * @author Cristian Lucchesi
 *
 */
public class InstituteDao extends DaoBase {

  @Inject
  InstituteDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Tutti gli istituti presenti.
   *
   * @return la lista di tutti gli uffici presenti sul database.
   */
  public List<Institute> getAllInstitutes() {

    final QInstitute institute = QInstitute.institute;

    return getQueryFactory().selectFrom(institute).orderBy(institute.name.asc()).fetch();
  }

}