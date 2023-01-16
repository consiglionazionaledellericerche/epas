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
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Office;
import models.Person;
import models.PersonDay;
import models.absences.Absence;
import models.absences.query.QAbsence;
import models.absences.query.QAbsenceType;
import models.enumerate.StampTypes;
import models.query.QPerson;
import models.query.QPersonDay;
import models.query.QPersonDayInTrouble;
import models.query.QStamping;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;


/**
 * Il dao dei personDay.
 *
 * @author Dario Tagliaferri
 */
public class PersonDayDao extends DaoBase {

  @Inject
  PersonDayDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Il personday relativo all'id passato come parametro.
   *
   * @param personDayId l'id del personday
   * @return il personday relativo all'id passato come parametro.
   */
  public PersonDay getPersonDayById(Long personDayId) {

    final QPersonDay personDay = QPersonDay.personDay;

    return getQueryFactory()
        .selectFrom(personDay)
        .where(personDay.id.eq(personDayId))
        .fetchOne();
  }

  /**
   * Il personday, se esiste, per una persona a una certa data.
   *
   * @param person la persona
   * @param date la data
   * @return un personday se esiste per quella persona in quella data.
   */
  public Optional<PersonDay> getPersonDay(Person person, LocalDate date) {

    final QPersonDay personDay = QPersonDay.personDay;

    final PersonDay result = getQueryFactory()
        .selectFrom(personDay)
        .where(personDay.person.eq(person).and(personDay.date.eq(date))).fetchOne();

    return Optional.fromNullable(result);
  }


  /**
   * Il primo personDay esistente precedente a date per person.
   *
   * @param person la persona da cercare
   * @param date la data da cui cercare indietro
   * @return Il primo personDay esistente precedente a date per person.
   */
  public PersonDay getPreviousPersonDay(Person person, LocalDate date) {

    final QPersonDay personDay = QPersonDay.personDay;

    return getQueryFactory()
        .selectFrom(personDay)
        .where(personDay.person.eq(person).and(personDay.date.lt(date)))
        .orderBy(personDay.date.desc())
        .fetchFirst();
  }

  /**
   * La lista di tutti i personday di una persona.
   *
   * @param person la persona di cui cercare i personday
   * @return tutti i personDay relativi alla persona person passata come parametro.
   */
  public List<PersonDay> getAllPersonDay(Person person) {

    final QPersonDay personDay = QPersonDay.personDay;
    return getQueryFactory().selectFrom(personDay)
        .where(personDay.person.eq(person))
        .fetch();
  }

  /**
   * Supporto alla ricerca dei personday. Default: fetch delle timbrature e ordinamento crescente
   * per data
   *
   * @param fetchAbsences true se fetch di absences anzichè stampings
   * @param orderedDesc true se si vuole ordinamento decrescente
   */
  private List<PersonDay> getPersonDaysFetched(Person person,
      LocalDate begin, Optional<LocalDate> end, boolean fetchAbsences,
      boolean orderedDesc, boolean onlyIsTicketAvailable) {

    final QPersonDay personDay = QPersonDay.personDay;
    final QStamping stamping = QStamping.stamping;

    JPQLQuery<PersonDay> query = build(person, begin, end, orderedDesc, onlyIsTicketAvailable);
    query = query.leftJoin(personDay.stampings, stamping).fetchJoin();
    query.fetch();

    final QPersonDayInTrouble troubles = QPersonDayInTrouble.personDayInTrouble;

    build(person, begin, end, orderedDesc, onlyIsTicketAvailable)
        .leftJoin(personDay.troubles, troubles).fetchJoin()
        .fetch();

    final QAbsence absence = QAbsence.absence;
    final QAbsenceType absenceType = QAbsenceType.absenceType;

    return build(person, begin, end, orderedDesc, onlyIsTicketAvailable)
        .leftJoin(personDay.absences, absence).fetchJoin()
        .leftJoin(absence.absenceType, absenceType).fetchJoin()
        .orderBy(personDay.date.asc())
        .fetch();

  }

  private JPQLQuery<PersonDay> build(Person person,
      LocalDate begin, Optional<LocalDate> end,
      boolean orderedDesc, boolean onlyIsTicketAvailable) {

    final QPersonDay personDay = QPersonDay.personDay;

    final BooleanBuilder condition = new BooleanBuilder();

    condition.and(personDay.date.goe(begin));
    if (end.isPresent()) {
      condition.and(personDay.date.loe(end.get()));
    }
    condition.and(personDay.person.eq(person));
    if (onlyIsTicketAvailable) {
      condition.and(personDay.isTicketAvailable.eq(true));
    }
    final JPQLQuery<PersonDay> query = getQueryFactory().selectFrom(personDay).where(condition);

    if (orderedDesc) {
      query.orderBy(personDay.date.desc());
    } else {
      query.orderBy(personDay.date.asc());
    }

    query.distinct();

    return query;

  }


  /**
   * La lista dei personday di una persona tra begin e end.
   *
   * @param person la persona
   * @param begin la data inizio da cui cercare
   * @param end la data fino a cui cercare
   * @return la lista dei personday presenti in un intervallo temporale.
   */
  public List<PersonDay> getPersonDayInPeriod(Person person, LocalDate begin,
      Optional<LocalDate> end) {

    return getPersonDaysFetched(person, begin, end, false, false, false);
  }

  /**
   * La lista dei personday di una persona tra begin e end (opzionale).
   *
   * @param person la persona di cui si vogliono i personday
   * @param begin la data di inizio da cui cercare i personday
   * @param end la data di fine (opzionale)
   * @return la lista dei personday ordinati decrescenti.
   */
  public List<PersonDay> getPersonDayInPeriodDesc(Person person, LocalDate begin,
      Optional<LocalDate> end) {

    return getPersonDaysFetched(person, begin, end,
        false, true, false);
  }


  /**
   * La lista dei PersonDay appartenenti al mese anno. Ordinati in modo crescente.
   */
  public List<PersonDay> getPersonDayInMonth(Person person, YearMonth yearMonth) {

    LocalDate begin = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
    LocalDate end = begin.dayOfMonth().withMaximumValue();

    return getPersonDaysFetched(person, begin, Optional.fromNullable(end),
        false, false, false);
  }

  /**
   * I person day della persona festivi e con ore lavorate. Utilizzo:s Nel mese year.present e
   * month.present Nell'anno year.present e month.absent Sempre year.absent e month.absent
   */
  public List<PersonDay> getHolidayWorkingTime(Person person, Optional<Integer> year,
      Optional<Integer> month) {

    QPersonDay personDay = QPersonDay.personDay;

    final BooleanBuilder condition = new BooleanBuilder();

    condition.and(personDay.person.eq(person));
    condition.and(personDay.timeAtWork.goe(1));
    condition.and(personDay.isHoliday.eq(true));

    if (year.isPresent() && month.isPresent()) {
      LocalDate monthBegin = new LocalDate(year.get(), month.get(), 1);
      LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
      condition.and(personDay.date.between(monthBegin, monthEnd));
    } else if (year.isPresent() && !month.isPresent()) {
      LocalDate yearBegin = new LocalDate(year.get(), 1, 1);
      LocalDate yearEnd = new LocalDate(year.get(), 12, 31);
      condition.and(personDay.date.between(yearBegin, yearEnd));
    }

    return getQueryFactory().selectFrom(personDay).where(condition)
        .orderBy(personDay.person.surname.asc())
        .orderBy(personDay.date.asc())
        .fetch();
  }


  /**
   * La lista dei personday di un singolo giorno di una lista di persone.
   *
   * @return la lista dei personDay relativi a un singolo giorno di tutte le persone presenti nella
   *     lista.
   */
  public List<PersonDay> getPersonDayForPeopleInDay(List<Person> personList, LocalDate date) {
    final QPersonDay personDay = QPersonDay.personDay;
    return getQueryFactory().selectFrom(personDay)
        .where(personDay.date.eq(date).and(personDay.person.in(personList)))
        .orderBy(personDay.person.surname.asc()).fetch();
  }

  /**
   * Il più vecchio personday presente sul db.
   *
   * @return il personday facente riferimento al giorno più vecchio presente sul db.
   */
  public PersonDay getOldestPersonDay() {
    final QPersonDay personDay = QPersonDay.personDay;
    return getQueryFactory().selectFrom(personDay).orderBy(personDay.date.asc()).limit(1)
        .fetchOne();
  }
  
  /**
   * Ritorna il personday più futuro della persona.
   *
   * @param person la persona di cui si ricerca l'ultimo personday
   * @return l'ultimo personday della persona sul db.
   */
  public PersonDay getMoreFuturePersonDay(Person person) {
    final QPersonDay personDay = QPersonDay.personDay;
    return getQueryFactory().selectFrom(personDay)
        .where(personDay.person.eq(person)).orderBy(personDay.date.desc()).limit(1)
        .fetchOne();
  }

  /**
   * Il personday, se esiste, che contiene l'assenza passata come parametro.
   *
   * @param abs l'assenza di cui si cerca il personday che la conteneva
   * @return il personDay che conteneva l'assenza passata come parametro.
   */
  public Optional<PersonDay> getByAbsence(Absence abs) {
    QPersonDay personDay = QPersonDay.personDay;
    final PersonDay result = getQueryFactory().selectFrom(personDay)
        .where(personDay.absences.contains(abs)).fetchOne();
    return Optional.fromNullable(result);
  }
  
  /**
   * Metodo che ritorna la lista dei giorni di lavoro tra begin e date per i dipendenti della 
   * sede office. 
   * Usato nel controllers.rest persondays.
   *
   * @param office la sede per cui si cercano i giorni di lavoro
   * @param begin la data di inizio da cui cercare
   * @param end la data di fine fino a cui cercare
   * @return la lista dei giorni di lavoro dei dipendenti della sede office tra begin e date.
   */
  public List<PersonDay> getPersonDaysByOfficeInPeriod(Office office, 
      LocalDate begin, LocalDate end) {
    QPersonDay personDay = QPersonDay.personDay;
    QPerson person = QPerson.person;
    
    return getQueryFactory().selectFrom(personDay)
        .leftJoin(personDay.person, person)
        .where(person.office.eq(office).and(personDay.date.between(begin, end)))
        .orderBy(personDay.date.asc()).fetch();
  }

  /**
   * Ritorna la lista dei giorni di lavoro di un dipendente con date tra begin e emd
   * e che abbia almeno una timbratura per lavoro fuori sede.
   */
  public List<PersonDay> getOffSitePersonDaysByPersonInPeriod(
      Person person, LocalDate begin, LocalDate end) {
    QPersonDay personDay = QPersonDay.personDay;
    QStamping stamping = QStamping.stamping;
    return getQueryFactory().selectFrom(personDay)
        .leftJoin(personDay.stampings, stamping)
        .where(personDay.person.eq(person),
            personDay.date.between(begin, end),
            stamping.stampType.eq(StampTypes.LAVORO_FUORI_SEDE))
        .distinct()
        .orderBy(personDay.date.asc()).fetch();
  }

  /**
   * Ritorna la lista dei giorni di lavoro di un dipendente con date tra begin e end
   * e che abbia almeno una timbratura per lavoro fuori sede o per motivi di servizio con
   * impostato luogo o motivazione.
   */
  public List<PersonDay> getOffSitePersonDaysByOfficeInPeriod(
      Office office, LocalDate begin, LocalDate end) {
    QPersonDay personDay = QPersonDay.personDay;
    QStamping stamping = QStamping.stamping;
    return getQueryFactory().selectFrom(personDay)
        .leftJoin(personDay.stampings, stamping)
        .where(personDay.person.office.eq(office),
            personDay.date.between(begin, end),
            stamping.stampType.eq(StampTypes.LAVORO_FUORI_SEDE)
              .or(
                  stamping.stampType.eq(StampTypes.MOTIVI_DI_SERVIZIO_FUORI_SEDE)
                    .and(stamping.reason.isNotEmpty()).or(stamping.place.isNotEmpty())))
        .distinct()
        .orderBy(personDay.date.asc()).fetch();
  }

  /**
   * Ritorna la lista dei giorni di lavoro di un dipendente con date tra begin e end
   * e che abbia almeno una timbratura del tipo passato con stampType.
   */
  public List<PersonDay> getStampTypePersonDaysByOFficeInPeriod(
      StampTypes stampType, Office office, LocalDate begin, LocalDate end) {
    QPersonDay personDay = QPersonDay.personDay;
    QStamping stamping = QStamping.stamping;
    return getQueryFactory().selectFrom(personDay)
        .leftJoin(personDay.stampings, stamping)
        .where(personDay.person.office.eq(office),
            personDay.date.between(begin, end),
            stamping.stampType.eq(stampType))
        .distinct()
        .orderBy(personDay.date.asc()).fetch();
  }

  /**
   * Ritorna la lista dei giorni di lavoro di un dipendente con date tra begin e end
   * e che abbia almeno una timbratura del tipo passato con stampType.
   */
  public List<PersonDay> getServiceExitPersonDaysByOFficeInPeriod(Office office, LocalDate begin, LocalDate end) {
    return getStampTypePersonDaysByOFficeInPeriod(StampTypes.MOTIVI_DI_SERVIZIO, office, begin, end);
  }
}