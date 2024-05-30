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
import models.Office;
import models.Person;
import models.PersonShift;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ShiftCancelled;
import models.ShiftCategories;
import models.ShiftTimeTable;
import models.ShiftType;
import models.query.QPerson;
import models.query.QPersonShift;
import models.query.QPersonShiftDay;
import models.query.QPersonShiftShiftType;
import models.query.QShiftCancelled;
import models.query.QShiftCategories;
import models.query.QShiftTimeTable;
import models.query.QShiftType;
import org.joda.time.LocalDate;

/**
 * Dao per i turni.
 *
 * @author Dario Tagliaferri
 */
public class ShiftDao extends DaoBase {

  @Inject
  ShiftDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);

  }

  /**
   * L'attività di turno col nome passato come parametro.
   *
   * @param type il nome dell'attività di turno
   * @return lo shiftType corrispondente al tipo type passato come parametro.
   */
  public ShiftType getShiftTypeByType(String type) {
    final QShiftType shiftType = QShiftType.shiftType;
    return getQueryFactory().selectFrom(shiftType).where(shiftType.type.eq(type)).fetchOne();
  }

  /**
   * Il servizio di turno, se esiste, relativo all'id passato.
   *
   * @param id l'identificativo numerico dell'attività sul turno
   * @return l'attività del servizio.
   */
  public Optional<ShiftType> getShiftTypeById(Long id) {
    final QShiftType shiftType = QShiftType.shiftType;
    final ShiftType result = getQueryFactory().selectFrom(shiftType)
        .where(shiftType.id.eq(id)).fetchOne();
    return Optional.fromNullable(result);
  }

  /**
   * Il giorno di turno relativo all'id passato.
   *
   * @param id l'id della giorno di turno che si intende ritornare
   * @return il giorno di turno.
   */
  public PersonShiftDay getPersonShiftDayById(Long id) {
    final QPersonShiftDay psd = QPersonShiftDay.personShiftDay;
    return getQueryFactory().selectFrom(psd).where(psd.id.eq(id)).fetchOne();
  }

  /**
   * La lista dei giorni di turno tra begin e to dell'attività type.
   *
   * @param begin la data da cui cercare
   * @param to la data fino a cui cercare
   * @param type l'attività su cui cercare
   * @return la lista dei personShiftDay con ShiftType 'type' presenti nel periodo tra 'begin' e
   *     'to'.
   */
  public List<PersonShiftDay> getShiftDaysByPeriodAndType(
      LocalDate begin, LocalDate to, ShiftType type) {
    final QPersonShiftDay psd = QPersonShiftDay.personShiftDay;
    return getQueryFactory().selectFrom(psd)
        .where(psd.date.between(begin, to)
            .and(psd.shiftType.eq(type))).orderBy(psd.organizationShiftSlot.name.asc(), 
                psd.date.asc())
        .fetch();
  }

  /**
   * La lista dei giorni di turno tra begin e end del tipo type per la persona person.
   *
   * @param begin la data da cui cercare
   * @param to la data fino a cui cercare
   * @param type l'attività su cui cercare
   * @param person la persona di cui cercare i giorni
   * @return la lista dei 'personShiftDay' della persona 'person' di tipo 'type' presenti nel
   *     periodo tra 'begin' e 'to'.
   */
  public List<PersonShiftDay> getPersonShiftDaysByPeriodAndType(
      LocalDate begin, LocalDate to, Optional<ShiftType> type, Person person) {
    final QPersonShiftDay psd = QPersonShiftDay.personShiftDay;
    BooleanBuilder conditions = new BooleanBuilder(psd.date.between(begin, to));
    conditions.and(psd.personShift.person.eq(person));
    if (type.isPresent()) {
      conditions.and(psd.shiftType.eq(type.get()));
    }
    return getQueryFactory().selectFrom(psd)
        .where(conditions)
        .orderBy(psd.organizationShiftSlot.name.asc(), psd.date.asc())
        .fetch();
  }

  /**
   * La lista dei turni cancellati tra from e to dell'attività type.
   *
   * @param from la data da cui cercare
   * @param to la data fino a cui cercare
   * @param type l'attività su cui cercare
   * @return la lista dei turni cancellati relativi al tipo 'type' nel periodo compreso tra 'from' e
   *     'to'.
   */
  public List<ShiftCancelled> getShiftCancelledByPeriodAndType(
      LocalDate from, LocalDate to, Optional<ShiftType> type) {
    final QShiftCancelled sc = QShiftCancelled.shiftCancelled;
    BooleanBuilder conditions = new BooleanBuilder(sc.date.between(from, to));
    if (type.isPresent()) {
      conditions.and(sc.type.eq(type.get()));
    }
    return getQueryFactory().selectFrom(sc).where(conditions).orderBy(sc.date.asc()).fetch();
  }

  /**
   * Il turno cancellato sul giorno day dell'attività type.
   *
   * @param day il giorno
   * @param type l'attività di turno
   * @return il turno cancellato relativo al giorno 'day' e al tipo 'type' passati come parametro.
   */
  public ShiftCancelled getShiftCancelled(LocalDate day, ShiftType type) {
    final QShiftCancelled sc = QShiftCancelled.shiftCancelled;
    return getQueryFactory().selectFrom(sc).where(sc.date.eq(day).and(sc.type.eq(type))).fetchOne();
  }

  /**
   * La quantità di shiftCancelled nel giorno day del tipo type.
   *
   * @param type l'attività di turno
   * @day il giorno
   * @return il quantitativo di shiftCancelled effettivamente cancellati.
   */
  public Long deleteShiftCancelled(ShiftType type, LocalDate day) {
    final QShiftCancelled sc = QShiftCancelled.shiftCancelled;
    return getQueryFactory().delete(sc).where(sc.date.eq(day).and(sc.type.eq(type))).execute();
  }

  /**
   * IL quantitativo di turni eliminati relativi al personShiftDay.
   *
   * @param personShiftDay il giorno di turno
   * @return il quantitativo di turni effettivamente cancellati.
   */
  public Long deletePersonShiftDay(PersonShiftDay personShiftDay) {

    final QPersonShiftDay sc = QPersonShiftDay.personShiftDay;
    return getQueryFactory().delete(sc)
        .where(sc.date.eq(personShiftDay.getDate())
            .and(sc.shiftType.eq(personShiftDay.getShiftType())
                .and(sc.personShift.person.eq(personShiftDay.getPersonShift().getPerson()))))
        .execute();
  }


  /**
   * L'associazione persona/attività se esiste sul db.
   *
   * @param personId l'id della persona
   * @param type il nome dell'attività
   * @return il PersonShift relativo alla persona person e al tipo type passati come parametro.
   */
  public List<PersonShift> getPersonShiftByPerson(Long personId) {
    final QPersonShiftShiftType psst = QPersonShiftShiftType.personShiftShiftType;

    BooleanBuilder conditions = 
        new BooleanBuilder(psst.personShift.person.id.eq(personId));
    conditions.and(psst.beginDate.isNull().or(psst.beginDate.loe(LocalDate.now())));
    conditions.and(psst.endDate.isNull().or(psst.endDate.goe(LocalDate.now())));
    return getQueryFactory().select(psst.personShift).from(psst)
        .where(conditions).fetch();
  }
  
  /**
   * L'associazione persona/attività se esiste sul db.
   *
   * @param personId l'id della persona
   * @param type il nome dell'attività
   * @return il PersonShift relativo alla persona person e al tipo type passati come parametro.
   */
  public PersonShift getPersonShiftByPersonAndType(Long personId, String type) {
    final QPersonShiftShiftType psst = QPersonShiftShiftType.personShiftShiftType;

    BooleanBuilder conditions = 
        new BooleanBuilder(psst.personShift.person.id.eq(personId));
    conditions.and(psst.beginDate.isNull().or(psst.beginDate.loe(LocalDate.now())));
    conditions.and(psst.endDate.isNull().or(psst.endDate.goe(LocalDate.now())));
    conditions.and(psst.shiftType.type.eq(type));
    return getQueryFactory().select(psst.personShift).from(psst)
        .where(conditions).fetchOne();
  }

  /**
   * La lista delle persone abilitate al turno nella sede office alla data date.
   *
   * @param office la sede di cui si vogliono le persone che stanno in turno
   * @param date la data
   * @return la lista dei personShift con persone che appartengono all'ufficio passato come
   *     parametro.
   */
  public List<PersonShift> getPeopleForShift(Office office, LocalDate date) {
    final QPersonShift ps = QPersonShift.personShift;
    final QPerson person = QPerson.person;
    return getQueryFactory().select(ps).from(person)
        .leftJoin(person.personShifts, ps).fetchAll()
        .where(person.office.eq(office)
            .and(ps.beginDate.loe(date).andAnyOf(ps.endDate.isNull(), ps.endDate.goe(date)))
            .and(person.eq(ps.person).and(ps.disabled.eq(false)))).fetch();
  }

  /**
   * L'associazione persona/turno con id passato come parametro.
   *
   * @param id l'id del personShift
   * @return il personShift associato all'id passato come parametro.
   */
  public PersonShift getPersonShiftById(Long id) {
    final QPersonShift ps = QPersonShift.personShift;
    return getQueryFactory().selectFrom(ps).where(ps.id.eq(id)).fetchOne();
  }

  /**
   * Il servizio di turno con nome type.
   *
   * @param type il nome del servizio di turno
   * @return la categoria associata al tipo di turno.
   */
  public ShiftCategories getShiftCategoryByType(String type) {
    final QShiftType st = QShiftType.shiftType;

    return getQueryFactory().select(st.shiftCategories).from(st)
        .where(st.type.eq(type)).fetchOne();
  }

  /**
   * La lista dei servizi per cui è stato attivato il turno.
   *
   * @param office l'ufficio per cui si chiede la lista dei servizi
   * @param isActive se passato, controlla solo i servizi attivi
   * @return la lista dei servizi per cui è stato attivato il turno.
   */
  public List<ShiftCategories> getAllCategoriesByOffice(Office office,
      Optional<Boolean> isActive) {
    final QShiftCategories sc = QShiftCategories.shiftCategories;
    BooleanBuilder condition = new BooleanBuilder();
    if (isActive.isPresent()) {
      condition.and(sc.disabled.eq(isActive.get()));
    }
    return getQueryFactory().selectFrom(sc).where(sc.office.eq(office).and(condition)).fetch();
  }

  /**
   * Il servizio di turno con id passato come parametro.
   *
   * @param id l'identificativo del turno
   * @return la categoria di turno corrispondente all'id passato come parametro.
   */
  public ShiftCategories getShiftCategoryById(Long id) {
    final QShiftCategories sc = QShiftCategories.shiftCategories;

    return getQueryFactory().selectFrom(sc)
        .where(sc.id.eq(id)).fetchOne();
  }

  /**
   * La lista dei servizi di turno di cui person è responsabile.
   *
   * @param person il responsabile di cui si vuol sapere i turni
   * @return la lista dei turni in cui person è responsabile.
   */
  public List<ShiftCategories> getCategoriesBySupervisor(Person person) {
    final QShiftCategories sc = QShiftCategories.shiftCategories;
    return getQueryFactory().selectFrom(sc).where(sc.supervisor.eq(person)).fetch();
  }


  /**
   * La lista delle attività di turno associate al servizio di turno passato come parametro.
   *
   * @param sc la categoria di turno
   * @return la lista dei tipi turno associati alla categoria passata come parametro.
   */
  public List<ShiftType> getTypesByCategory(ShiftCategories sc) {
    final QShiftType shiftType = QShiftType.shiftType;
    return getQueryFactory().selectFrom(shiftType).where(shiftType.shiftCategories.eq(sc))
        .fetch();
  }

  /**
   * La lista delle timetable associate alla sede.
   *
   * @param office la sede di cui si cercano le timetable
   * @return la lista di tutti i tipi di turno disponibili in anagrafica.
   */
  public List<ShiftTimeTable> getAllShifts(Office office) {
    final QShiftTimeTable stt = QShiftTimeTable.shiftTimeTable;
    return getQueryFactory().selectFrom(stt).where(stt.office.isNull().or(stt.office.eq(office)))
        .fetch();
  }


  /**
   * La timetable con id passato come parametro.
   *
   * @param id l'id della timeTable che si intende ritornare
   * @return la timeTable per i turni da associare al servizio.
   */
  public ShiftTimeTable getShiftTimeTableById(Long id) {
    final QShiftTimeTable stt = QShiftTimeTable.shiftTimeTable;
    return getQueryFactory().selectFrom(stt).where(stt.id.eq(id)).fetchOne();
  }

  /**
   * La lista delle persone associate all'attività type alla data date (opzionale).
   *
   * @param shiftType l'attività per cui si vogliono le persone associate
   * @param date se presente, la data in cui si richiede la situazione dei dipendenti associati al
   *     turno
   * @return la lista di persone associate all'attività passata come parametro.
   */
  public List<PersonShiftShiftType> getAssociatedPeopleToShift(ShiftType shiftType,
      Optional<LocalDate> date) {
    final QPersonShiftShiftType psst = QPersonShiftShiftType.personShiftShiftType;
    BooleanBuilder condition = new BooleanBuilder();
    if (date.isPresent()) {
      condition.and(psst.beginDate.loe(date.get())
          .andAnyOf(psst.endDate.isNull(),
              psst.endDate.gt(date.get())));

    }
    return getQueryFactory().selectFrom(psst).where(psst.shiftType.eq(shiftType)
        .and(condition)).fetch();
  }

  /**
   * L'associazione tra persona e attività di turno, se esiste.
   *
   * @param personShift la persona associata al turno
   * @param shiftType l'attività di un servizio di turno
   * @return l'eventuale associazione tra persona e attività di turno se presente.
   */
  public Optional<PersonShiftShiftType> getByPersonShiftAndShiftType(PersonShift personShift,
      ShiftType shiftType) {
    final QPersonShiftShiftType psst = QPersonShiftShiftType.personShiftShiftType;
    final PersonShiftShiftType result = getQueryFactory().selectFrom(psst)
        .where(psst.personShift.eq(personShift)
            .and(psst.shiftType.eq(shiftType))).fetchOne();
    return Optional.fromNullable(result);
  }

  /**
   * L'associazione tra persona e attività di turno.
   *
   * @param id l'id dell'associazione tra persona e attività di turno
   * @return l'associazione tra persona e attività di turno identificata dall'id passato.
   */
  public PersonShiftShiftType getById(Long id) {
    final QPersonShiftShiftType psst = QPersonShiftShiftType.personShiftShiftType;
    return getQueryFactory().selectFrom(psst)
        .where(psst.id.eq(id)).fetchOne();
  }

  /**
   * La lista delle associazioni tra persona e attività di turno ad una certa data.
   *
   * @param personShift la persona abilitata al turno
   * @param date la data a cui si cercano le associazioni
   * @return la lista delle associazioni persona/attività relative ai parametri passati.
   */
  public List<PersonShiftShiftType> getByPersonShiftAndDate(
      PersonShift personShift, LocalDate date) {
    final QPersonShiftShiftType psst = QPersonShiftShiftType.personShiftShiftType;
    return getQueryFactory().selectFrom(psst)
        .where(psst.personShift.eq(personShift)
            .and(psst.beginDate.loe(date).andAnyOf(psst.endDate.isNull(), psst.endDate.goe(date))))
        .fetch();
  }

  public List<ShiftType> getShiftTypesForPerson(Person person) {
    final QPersonShiftShiftType psst = QPersonShiftShiftType.personShiftShiftType;
    return getQueryFactory().selectFrom(psst).select(psst.shiftType)
        .where(psst.personShift.person.eq(person)).fetch();
  }

}