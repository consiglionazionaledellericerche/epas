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
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import dao.wrapper.IWrapperFactory;
import models.GroupOvertime;
import models.flows.Group;
import models.query.QGroupOvertime;

/**
 * @author Cristian Lucchesi
 * @author Dario Tagliaferri
 *
 */
public class GroupOvertimeDao extends DaoBase {

  @Inject
  GroupOvertimeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory wrapperFactory) {
    super(queryFactory, emp);
  }
  
  /**
   * Ritorna la lista dei groupOvertime associati ad un gruppo per uno specifico anno.
   * 
   * @param year l'anno di riferimento
   * @param group il gruppo di cui prendere le variazioni dello straordinario
   * @return la lista dei groupOvertima associati ad un gruppo per uno specifico anno.
   */
  public List<GroupOvertime> getByYearAndGroup(Integer year, Group group) {
    final QGroupOvertime groupOvertime = QGroupOvertime.groupOvertime;
    return getQueryFactory().selectFrom(groupOvertime)
        .where(groupOvertime.year.eq(year).and(groupOvertime.group.eq(group))).fetch();
  }
}
