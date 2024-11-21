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

import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import models.Person;
import models.PersonOvertime;
import models.query.QPersonOvertime;

/**
 * Dao per le attribuzioni di monte ore straordinari per le persone.
 *
 * @author Dario Tagliaferri
 * @author Cristian Lucchesi
 */

public class PersonOvertimeDao extends DaoBase {

  @Inject
  PersonOvertimeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);    
  }
  
  /**
   * La lista delle attribuzioni orarie per monte ore per dipendente in un anno.
   * @param person la persona per cui chiedere il monte ore
   * @param year l'anno di riferimento
   * @return la lista delle attribuzioni orarie per monte ore per dipendente in un anno.
   */
  public List<PersonOvertime> personListInYear(Person person, Integer year) {
    final QPersonOvertime personOvertime = QPersonOvertime.personOvertime;
    return getQueryFactory().selectFrom(personOvertime)
        .where(personOvertime.person.eq(person).and(personOvertime.year.eq(year))).fetch();
  }

}
