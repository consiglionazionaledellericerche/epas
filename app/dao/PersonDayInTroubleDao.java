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
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Office;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.enumerate.Troubles;
import models.query.QPersonDay;
import models.query.QPersonDayInTrouble;
import models.query.QPersonsOffices;
import org.joda.time.LocalDate;

/**
 * DAO per i PersonDayInTrouble.
 */
public class PersonDayInTroubleDao extends DaoBase {

  @Inject
  PersonDayInTroubleDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * La lista dei trouble relativi alla persona nel periodo (opzionale) tra begin e end
   * appartenenti alla lista di tipi troubles.
   *
   * @param person la persona di cui si vogliono i trouble
   * @param begin (opzionale) da quando si cerca
   * @param end (opzionale) fino a quando si cerca
   * @param troubles la lista dei tipi di trouble da cercare
   * @return la lista dei personDayInTrouble relativi alla persona person nel periodo begin-end. 
   *     E possibile specificare se si vuole ottenere quelli fixati (fixed = true) o no 
   *     (fixed = false).
   */
  public List<PersonDayInTrouble> getPersonDayInTroubleInPeriod(
      Person person, Optional<LocalDate> begin, Optional<LocalDate> end,
      Optional<List<Troubles>> troubles) {

    QPersonDayInTrouble pdit = QPersonDayInTrouble.personDayInTrouble;


    BooleanBuilder conditions = new BooleanBuilder(pdit.personDay.person.eq(person));
    if (begin.isPresent()) {
      conditions.and(pdit.personDay.date.goe(begin.get()));
    }
    if (end.isPresent()) {
      conditions.and(pdit.personDay.date.loe(end.get()));
    }
    if (troubles.isPresent()) {
      conditions.and(pdit.cause.in(troubles.get()));
    }

    return getQueryFactory().selectFrom(pdit).where(conditions).fetch();
  }

  /**
   * Il persondayintrouble, se esiste, relativo ai parametri passati.
   *
   * @param pd il personDay per cui si ricerca il trouble
   * @param trouble la causa per cui si ricerca il trouble
   * @return il personDayInTrouble, se esiste, relativo ai parametri passati al metodo.
   */
  public Optional<PersonDayInTrouble> getPersonDayInTroubleByType(PersonDay pd, Troubles trouble) {
    QPersonDayInTrouble pdit = QPersonDayInTrouble.personDayInTrouble;
    final PersonDayInTrouble result = getQueryFactory()
        .selectFrom(pdit)
        .where(pdit.personDay.eq(pd).and(pdit.cause.eq(trouble))).fetchOne();
    return Optional.fromNullable(result);
  }
  
  /**
   * La lista dei trouble per sede.
   * @param office la sede di cui si vogliono i trouble
   * @param begin la data di inizio da quando si ricerca
   * @param end la data di fine in cui si ricerca
   * @return la lista dei trouble per sede.
   */
  public List<PersonDayInTrouble> getPersonDayInTroubleByOfficeInPeriod(Office office, 
      LocalDate begin, LocalDate end, Troubles trouble) {
    QPersonDayInTrouble pdit = QPersonDayInTrouble.personDayInTrouble;
    QPersonDay pd = QPersonDay.personDay;
    QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    BooleanBuilder conditions = new BooleanBuilder();
    conditions.and(pdit.personDay.date.goe(begin));
    conditions.and(pdit.personDay.date.loe(end));
    conditions.and(pdit.cause.eq(trouble));
    return getQueryFactory().selectFrom(pdit).leftJoin(pdit.personDay, pd).leftJoin(pdit.personDay.person.personsOffices, personsOffices)
        .where(conditions.and(personsOffices.office.eq(office))).fetch();
  }
}