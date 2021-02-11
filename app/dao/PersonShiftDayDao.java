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

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Person;
import models.PersonShift;
import models.PersonShiftDay;
import models.ShiftCategories;
import models.ShiftType;
import models.enumerate.ShiftSlot;
import models.query.QPersonShift;
import models.query.QPersonShiftDay;
import models.query.QShiftCategories;
import org.joda.time.LocalDate;

/**
 * Dao per i PersonShift.
 *
 * @author Dario Tagliaferri
 */
public class PersonShiftDayDao extends DaoBase {


  @Inject
  PersonShiftDayDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }


  /**
   * Il giorno di turno della persona person nella data date se esiste.
   *
   * @param person la persona per cui cercare il turno
   * @param date la data in cui cercare il turno
   * @return il personShiftDay relativo alla persona person nel caso in cui in data date fosse in
   *     turno Null altrimenti.
   */
  public Optional<PersonShiftDay> getPersonShiftDay(Person person, LocalDate date) {
    final QPersonShiftDay personShiftDay = QPersonShiftDay.personShiftDay;
    final QPersonShift personShift = QPersonShift.personShift;

    final PersonShiftDay result = getQueryFactory().selectFrom(personShiftDay)
        .join(personShiftDay.personShift, personShift)
        .where(personShift.person.eq(person).and(personShiftDay.date.eq(date))).fetchOne();

    return Optional.fromNullable(result);
  }


  /**
   * La lista dei giorni di turno nel periodo compreso tra from e to per l'attività type per la 
   * persona person (opzionale).
   *
   * @param from la data da cui cercare i giorni di turno
   * @param to la data fino a cui cercare i giorni di tunro
   * @param type l'attività su cui cercare i turni
   * @param person la persona per cui cercare i turni
   * @return la lista dei personShiftDay presenti nel periodo compreso tra 'from' e 'to' aventi lo
   *     shiftType 'type'. Se specificato filtra sulla persona richiesta.
   */
  public List<PersonShiftDay> byTypeInPeriod(
      LocalDate from, LocalDate to, ShiftType type, Optional<Person> person) {
    final QPersonShiftDay personShiftDay = QPersonShiftDay.personShiftDay;

    final BooleanBuilder condition = new BooleanBuilder()
        .and(personShiftDay.date.goe(from))
        .and(personShiftDay.date.loe(to))
        .and(personShiftDay.shiftType.eq(type));

    if (person.isPresent()) {
      condition.and(personShiftDay.personShift.person.eq(person.get()));
    }

    return getQueryFactory().selectFrom(personShiftDay)
        .where(condition).orderBy(personShiftDay.date.asc()).fetch();
  }


  /**
   * Cerca il PersonShiftDay per ShiftType, data, ShiftSlot.
   *
   * @param shiftType l'attività su cui cercare il giorno di turno
   * @param date la data su cui cercare il giorno di turno
   * @param shiftSlot lo slot di turno
   * @return il personShiftDay relativo al tipo 'shiftType' nel giorno 'date' con lo slot
   *     'shiftSlot'.
   */
  public PersonShiftDay getPersonShiftDayByTypeDateAndSlot(
      ShiftType shiftType, LocalDate date, ShiftSlot shiftSlot) {
    final QPersonShiftDay personShiftDay = QPersonShiftDay.personShiftDay;

    return getQueryFactory().selectFrom(personShiftDay).where(personShiftDay.date.eq(date)
        .and(personShiftDay.shiftType.eq(shiftType)
            .and(personShiftDay.shiftSlot.eq(shiftSlot))))
        .fetchFirst();
  }


  /**
   * PersonShift associato alla persona passata.
   *
   * @return il personShift associato alla persona passata come parametro.
   */
  public PersonShift getPersonShiftByPerson(Person person, LocalDate date) {
    final QPersonShift personShift = QPersonShift.personShift;
    return getQueryFactory().selectFrom(personShift).where(personShift.person.eq(person)
        .and(personShift.beginDate.loe(date)
            .andAnyOf(personShift.endDate.goe(date), personShift.endDate.isNull())))
        .fetchOne();
  }

  /**
   * Metodo che ritorna la lista dei personShift disabilitati.
   *
   * @return la lista dei personShift disabilitati.
   */
  public List<PersonShift> getDisabled() {
    final QPersonShift personShift = QPersonShift.personShift;
    return getQueryFactory().selectFrom(personShift).where(personShift.disabled.eq(true)).fetch();
  }
  
  /**
   * Metodo di utilità che ritorna i casi di turnisti erroneamente disabilitati nonostante le date
   * di inizio e fine attività di turnista contengano la data odierna.
   *
   * @return la lista delle persone erroneamente disabilitate.
   */
  public List<PersonShift> getWrongDisabled() {
    final QPersonShift personShift = QPersonShift.personShift;
    return getQueryFactory().selectFrom(personShift).where(personShift.disabled.eq(true)
        .and(personShift.beginDate.loe(LocalDate.now())
            .andAnyOf(personShift.endDate.isNull(), 
                personShift.endDate.goe(LocalDate.now())))).fetch();
  }

  /**
   * Tutti gli ShiftCategories.
   *
   * @return la lista di tutti i PersonReperibilityType presenti sul db.
   */
  public List<ShiftCategories> getAllShiftType() {
    final QShiftCategories shift = QShiftCategories.shiftCategories;
    return getQueryFactory().selectFrom(shift).orderBy(shift.description.asc()).fetch();
  }

  /**
   * Cerca un PersonShiftDay per persona e data.
   *
   * @param person Person da cercare.
   * @param date data del personShiftDay.
   */
  public Optional<PersonShiftDay> byPersonAndDate(Person person, LocalDate date) {
    final QPersonShiftDay shiftDay = QPersonShiftDay.personShiftDay;

    return Optional.fromNullable(getQueryFactory().selectFrom(shiftDay)
        .where(shiftDay.personShift.person.eq(person).and(shiftDay.date.eq(date)))
        .fetchFirst());
  }

  /**
   * Cerca un PersonShiftDay per typo e data.
   *
   * @param shiftType tipo del PersonShiftDay
   * @param date data del PersonShiftDay da cercare
   */
  public Optional<PersonShiftDay> byTypeAndDate(ShiftType shiftType, LocalDate date) {
    final QPersonShiftDay shiftDay = QPersonShiftDay.personShiftDay;

    return Optional.fromNullable(getQueryFactory().selectFrom(shiftDay)
        .where(shiftDay.shiftType.eq(shiftType).and(shiftDay.date.eq(date)))
        .fetchFirst());
  }

  /**
   * Il conteggio dei personShiftDay nel giorno date per la persona person.
   *
   * @param person la persona per cui cercare i turni
   * @param date la data in cui cercare i turni
   * @return il numero di personShiftDay.
   */
  public long countByPersonAndDate(Person person, LocalDate date) {

    final QPersonShiftDay shiftDay = QPersonShiftDay.personShiftDay;
    return getQueryFactory().from(shiftDay)
        .where(shiftDay.personShift.person.eq(person).and(shiftDay.date.eq(date))).fetchCount();
  }

  /**
   * La lista dei personShiftDay nel giorno date per l'attività activity.
   *
   * @param date il giorno in cui cercare
   * @param activity l'attività di turno su cui cercare
   * @return la lista dei personShiftDay.
   */
  public List<PersonShiftDay> listByDateAndActivity(LocalDate date, ShiftType activity) {
    final QPersonShiftDay shiftDay = QPersonShiftDay.personShiftDay;
    return getQueryFactory().selectFrom(shiftDay)
        .where(shiftDay.date.eq(date).and(shiftDay.shiftType.eq(activity))).fetch();
  }
  
  /**
   * La lista dei giorni di turno per una persona in un periodo.
   *
   * @param from la data da cui cercare i giorni di turno
   * @param to la data fino a cui cercare i giorni di turno
   * @return La lista dei giorni di turno per una persona in un periodo.
   */
  public List<PersonShiftDay> listByPeriod(Person person, LocalDate from, LocalDate to) {
    final QPersonShiftDay shiftDay = QPersonShiftDay.personShiftDay;
    return getQueryFactory().selectFrom(shiftDay)
        .where(shiftDay.personShift.person.eq(person)
            .and(shiftDay.date.goe(from).and(shiftDay.date.loe(to))))
        .orderBy(shiftDay.date.asc()).fetch();
  }

}