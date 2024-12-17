/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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
import java.util.Optional;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import manager.configurations.EpasParam;
import models.Person;
import models.PersonConfiguration;
import models.query.QPersonConfiguration;

public class ConfigurationDao extends DaoBase {

  @Inject
  ConfigurationDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }
  
  public List<PersonConfiguration> listByPerson(Person person) {
    final QPersonConfiguration personConfiguration = QPersonConfiguration.personConfiguration;
    return getQueryFactory().selectFrom(personConfiguration)
        .where(personConfiguration.person.eq(person)).fetch();
  }
  
  public Optional<PersonConfiguration> byPersonAndParam(Person person, EpasParam epasParam) {
    final QPersonConfiguration personConfiguration = QPersonConfiguration.personConfiguration;
    return Optional.ofNullable(getQueryFactory().selectFrom(personConfiguration)
        .where(personConfiguration.person.eq(person)
            .and(personConfiguration.epasParam.eq(epasParam))).fetchFirst());
  }
}
