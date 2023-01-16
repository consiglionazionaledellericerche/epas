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

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Office;
import models.OrganizationShiftSlot;
import models.OrganizationShiftTimeTable;
import models.query.QOrganizationShiftSlot;
import models.query.QOrganizationShiftTimeTable;

/**
 * DAO per le OrganizationShiftTimeTable.
 */
public class OrganizationShiftTimeTableDao extends DaoBase {

  @Inject
  OrganizationShiftTimeTableDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory wrapperFactory) {
    super(queryFactory, emp);
  }

  /**
   * Metodo che ritorna la lista delle timetable definite per la sede passata come parametro.
   *
   * @param office la sede di cui si vogliono le timetable associate
   * @return la lista delle timetable definite per una sede.
   */
  public List<OrganizationShiftTimeTable> getAllFromOffice(Office office) {
    final QOrganizationShiftTimeTable timeTable = 
        QOrganizationShiftTimeTable.organizationShiftTimeTable;
    return getQueryFactory()
        .selectFrom(timeTable)
        .where(timeTable.office.eq(office)).fetch();
  }

  /**
   * Metodo che ritorna la timetable esterna associata all'id.
   *
   * @param id l'identificativo sul database della timetable esterna
   * @return l'optional contenente, se esiste, la timetable esterna associata all'identificativo 
   *     passato come parametro.
   */
  public Optional<OrganizationShiftTimeTable> getById(Long id) {
    final QOrganizationShiftTimeTable ostt = 
        QOrganizationShiftTimeTable.organizationShiftTimeTable;
    final OrganizationShiftTimeTable query = 
        getQueryFactory().selectFrom(ostt).where(ostt.id.eq(id)).fetchOne();
    
    return Optional.fromNullable(query);
  }

  /**
   * Metodo che ritorna la timetable esterna associata al nome se esiste.
   *
   * @param name la stringa contenente il nome da cercare
   * @return l'optional contenente, se esiste, la timetable esterna associata al nome
   *     passato come parametro.
   */
  public Optional<OrganizationShiftTimeTable> getByName(String name) {
    final QOrganizationShiftTimeTable ostt = 
        QOrganizationShiftTimeTable.organizationShiftTimeTable;
    final OrganizationShiftTimeTable query = 
        getQueryFactory().selectFrom(ostt).where(ostt.name.equalsIgnoreCase(name)).fetchOne();
    
    return Optional.fromNullable(query);
  }


  /*
   * ***********************************************************************************************
   * Parte relativa a query su OrganizationShiftSlot.         
   ***********************************************************************************************
   */

  /**
   * Preleva un OrganizationShiftSlot per nome e prefisso.
   */
  public Optional<OrganizationShiftSlot> getByNameAndPrefix(String str, String prefix) {
    final QOrganizationShiftSlot oss = QOrganizationShiftSlot.organizationShiftSlot;
    final OrganizationShiftSlot query = getQueryFactory()
        .selectFrom(oss).where(oss.name.contains(str)
            .and(oss.name.startsWith(prefix))).fetchFirst();
    return Optional.fromNullable(query);
  }
}
