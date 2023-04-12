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
import models.Role;
import models.query.QRole;

/**
 * Dao per l'accesso alle informazioni dei Role.
 *
 * @author Dario Tagliaferri
 */
public class RoleDao extends DaoBase {

  @Inject
  RoleDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
    // TODO Auto-generated constructor stub
  }

  /**
   * Il ruolo identificato dall'id passato come parametro.
   *
   * @param id l'identificativo del ruolo
   * @return il ruolo identificato dall'id passato come parametro.
   */
  public Role getRoleById(Long id) {
    final QRole role = QRole.role;
    return getQueryFactory().selectFrom(role)
        .where(role.id.eq(id))
        .fetchOne();
  }

  /**
   * Il ruolo identificato dal nome passato come parametro.
   *
   * @name il nome del ruolo
   * @return il ruolo identificato dal nome passato come parametro.
   */
  public Role getRoleByName(String name) {
    final QRole role = QRole.role;
    return getQueryFactory().selectFrom(role)
        .where(role.name.eq(name))
        .fetchOne();
  }

  /**
   * La lista dei ruoli disponibili.
   *
   * @return Tutti i ruoli disponibili.
   */
  public List<Role> getAll() {
    final QRole role = QRole.role;
    return getQueryFactory().selectFrom(role).fetch();
  }

}