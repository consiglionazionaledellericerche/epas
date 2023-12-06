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
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import java.time.LocalDate;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Office;
import models.Person;
import models.flows.Group;
import models.flows.query.QAffiliation;
import models.flows.query.QGroup;

/**
 * DAO per i gruppi.
 */
public class GroupDao extends DaoBase {

  @Inject
  GroupDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory wrapperFactory) {
    super(queryFactory, emp);
  }

  /**
   * Restisce il gruppo prelevare per id.
   */
  public Optional<Group> byId(Long id) {
    final QGroup group = QGroup.group;
    return Optional.fromNullable(
        getQueryFactory().selectFrom(group).where(group.id.eq(id)).fetchFirst());
  }

  /**
   * Restisce il gruppo da prelevare per externalId e office.
   */
  public Optional<Group> byOfficeAndExternalId(Office office, String externalId) {
    final QGroup group = QGroup.group;
    return Optional.fromNullable(
        getQueryFactory().selectFrom(group)
          .where(group.office.eq(office), group.externalId.eq(externalId))
          .fetchFirst());
  }

  /**
   * Metodo che ritorna la lista dei gruppi attivi che appartengono alla sede passata 
   * come parametro.
   *
   * @param office la sede di cui si richiedono i gruppi
   * @return la lista dei gruppi i cui responsabili appartengono alla sede passata come parametro.
   */
  public List<Group> groupsByOffice(Office office, Optional<Person> manager,
      Optional<Boolean> includeDisabled) {
    final QGroup group = QGroup.group;
    BooleanBuilder condition = new BooleanBuilder();
    if (manager.isPresent()) {
      condition.and(group.manager.eq(manager.get()));
    }
    if (!includeDisabled.isPresent()) {
      condition.and(group.endDate.isNull().or(group.endDate.after(LocalDate.now())));  
    }
    
    return getQueryFactory().selectFrom(group).where(group.office.eq(office), condition)
        .orderBy(group.name.asc()).fetch();
  }

  /**
   * Metodo che ritorna la lista dei gruppi di cui Person è responsabile.
   *
   * @param person la persona di cui cerco i gruppi in cui è responsabile
   * @return la lista dei gruppi di cui Person è responsabile.
   */
  public List<Group> groupsByManager(Optional<Person> person) {
    final QGroup group = QGroup.group;
    BooleanBuilder condition = new BooleanBuilder();
    if (person.isPresent()) {
      condition.and(group.manager.eq(person.get()));
    }
    condition.and(group.endDate.isNull().or(group.endDate.after(LocalDate.now())));
    return getQueryFactory().selectFrom(group).where(condition).fetch();
  }

  private Predicate personAffiliationCondition(
      QAffiliation affiliation, Person person, LocalDate atDate) {
    BooleanBuilder endDateCondition = new BooleanBuilder(affiliation.endDate.isNull());
    endDateCondition = endDateCondition.or(affiliation.endDate.after(atDate));
    return affiliation.person.eq(person)
        .and(affiliation.beginDate.before(atDate)
            .and(endDateCondition));
  } 
  
  /**
   * Metodo che ritorna la lista dei gruppi di cui fa parte la person passata come parametro
   * ed alla data indicata.
   *
   * @param person la persona di cui si cercano i gruppi di cui fa parte
   * @param atDate la data in cui cercare l'appartenenza ai gruppi.
   * @return la lista dei gruppi di cui fa parte la persona passata come parametro.
   */
  public List<Group> myGroups(Person person, LocalDate atDate) {
    final QGroup group = QGroup.group;
    final QAffiliation affiliation = QAffiliation.affiliation;
    BooleanBuilder endDateCondition = new BooleanBuilder(affiliation.endDate.isNull());
    endDateCondition = endDateCondition.or(affiliation.endDate.after(atDate));
    return getQueryFactory().selectFrom(group)
        .join(group.affiliations, affiliation)
        .where(personAffiliationCondition(affiliation, person, atDate)).fetch();
  }
  
  public List<Group> myGroups(Person person) {
    return myGroups(person, LocalDate.now());
  }
  
  
  /**
   * Metodo che ritorna il gruppo, se esiste, con manager manager e come appartenente person.
   *
   * @param manager il manager del gruppo
   * @param person la persona appartenente al gruppo
   * @return il gruppo, se esiste, che ha come manager manager e come appartenente person.
   */
  public Optional<Group> checkManagerPerson(Person manager, Person person) {
    final QGroup group = QGroup.group;
    final QAffiliation affiliation = QAffiliation.affiliation;
    final Group result = getQueryFactory().selectFrom(group)
        .join(group.affiliations, affiliation)
        .where(
            personAffiliationCondition(
                affiliation, person, LocalDate.now()), group.manager.eq(manager))
        .fetchFirst();
    return Optional.fromNullable(result);
  }
}