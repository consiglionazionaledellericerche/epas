package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.Person;
import models.PersonDay;
import models.absences.query.QAbsence;
import models.absences.query.QAbsenceType;
import models.query.QAbsenceTypeGroup;
import models.query.QPersonDay;
import models.query.QPersonDayInTrouble;
import models.query.QStamping;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import java.util.List;

import javax.persistence.EntityManager;


/**
 * @author dario.
 */
public class PersonDayDao extends DaoBase {

  @Inject
  PersonDayDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @param personDayId l'id del personday
   * @return il personday relativo all'id passato come parametro.
   */
  public PersonDay getPersonDayById(Long personDayId) {

    final QPersonDay personDay = QPersonDay.personDay;

    return getQueryFactory()
            .from(personDay)
            .where(personDay.id.eq(personDayId))
            .singleResult(personDay);
  }

  /**
   *  
   * @param person la persona 
   * @param date la data
   * @return un personday se esiste per quella persona in quella data.
   */
  public Optional<PersonDay> getPersonDay(Person person, LocalDate date) {

    final QPersonDay personDay = QPersonDay.personDay;

    final JPQLQuery query = getQueryFactory()
            .from(personDay)
            .where(personDay.person.eq(person).and(personDay.date.eq(date)));

    return Optional.fromNullable(query.singleResult(personDay));
  }

  /**
   * Cerca il personDay se non esiste ne crea una copia.
   */
  public PersonDay getOrBuildPersonDay(Person person, LocalDate date) {

    Optional<PersonDay> optPersonDay = getPersonDay(person, date);
    if (optPersonDay.isPresent()) {
      return optPersonDay.get();
    }
    PersonDay personDay = new PersonDay(person, date);
    return personDay;
  }

  /**
   * Il primo personDay esistente precedente a date per person.
   */
  public PersonDay getPreviousPersonDay(Person person, LocalDate date) {

    final QPersonDay personDay = QPersonDay.personDay;

    final JPQLQuery query = getQueryFactory()
            .from(personDay)
            .where(personDay.person.eq(person).and(personDay.date.lt(date)))
            .orderBy(personDay.date.desc());

    return query.singleResult(personDay);
  }

  /**
   * @return tutti i personDay relativi alla persona person passata come parametro.
   */
  public List<PersonDay> getAllPersonDay(Person person) {

    final QPersonDay personDay = QPersonDay.personDay;
    final JPQLQuery query = getQueryFactory().from(personDay)
            .where(personDay.person.eq(person));

    return query.list(personDay);
  }

  /**
   * Supporto alla ricerca dei personday. Default: fetch delle timbrature e ordinamento crescente
   * per data
   *
   * @param fetchAbsences true se fetch di absences anzichè stampings
   * @param orderedDesc   true se si vuole ordinamento decrescente
   */
  private List<PersonDay> getPersonDaysFetched(Person person,
           LocalDate begin, Optional<LocalDate> end, boolean fetchAbsences,
           boolean orderedDesc, boolean onlyIsTicketAvailable) {

    final QPersonDay personDay = QPersonDay.personDay;
    final QStamping stamping = QStamping.stamping;


    build(person, begin, end, orderedDesc, onlyIsTicketAvailable)
            .leftJoin(personDay.stampings, stamping).fetch()
            .list(personDay);

    final QPersonDayInTrouble troubles = QPersonDayInTrouble.personDayInTrouble;

    build(person, begin, end, orderedDesc, onlyIsTicketAvailable)
            .leftJoin(personDay.troubles, troubles).fetch()
            .list(personDay);

    final QAbsence absence = QAbsence.absence;
    final QAbsenceType absenceType = QAbsenceType.absenceType;
    final QAbsenceTypeGroup absenceTypeGroup = QAbsenceTypeGroup.absenceTypeGroup;


    return build(person, begin, end, orderedDesc, onlyIsTicketAvailable)
            .leftJoin(personDay.absences, absence).fetch()
            .leftJoin(absence.absenceType, absenceType).fetch()
            .leftJoin(absenceType.absenceTypeGroup, absenceTypeGroup).fetch()
            .orderBy(personDay.date.asc())
            .list(personDay);

  }

  private JPQLQuery build(Person person,
                          LocalDate begin, Optional<LocalDate> end,
                          boolean orderedDesc, boolean onlyIsTicketAvailable) {

    final QPersonDay personDay = QPersonDay.personDay;

    final BooleanBuilder condition = new BooleanBuilder();
    final JPQLQuery query = getQueryFactory().from(personDay);


    condition.and(personDay.date.goe(begin));
    if (end.isPresent()) {
      condition.and(personDay.date.loe(end.get()));
    }
    condition.and(personDay.person.eq(person));
    if (onlyIsTicketAvailable) {
      condition.and(personDay.isTicketAvailable.eq(true));
    }
    query.where(condition);

    if (orderedDesc) {
      query.orderBy(personDay.date.desc());
    } else {
      query.orderBy(personDay.date.asc());
    }

    query.distinct();

    return query;

  }


  /**
   * @param person la persona
   * @param begin la data inizio da cui cercare
   * @param end la data fino a cui cercare
   * @return la lista dei personday presenti in un intervallo temporale.
   */
  public List<PersonDay> getPersonDayInPeriod(Person person, LocalDate begin,
                                              Optional<LocalDate> end) {

    return getPersonDaysFetched(person, begin, end,
            false, false, false);
  }

  /**
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

    final JPQLQuery query = getQueryFactory().from(personDay);

    condition.and(personDay.person.eq(person));
    condition.and(personDay.timeAtWork.goe(1));
    condition.and(personDay.isHoliday.eq(true));

    if (year.isPresent() && month.isPresent()) {
      LocalDate monthBegin = new LocalDate(year.get(), month.get(), 1);
      LocalDate monthEnd = monthBegin.monthOfYear().withMaximumValue();
      condition.and(personDay.date.between(monthBegin, monthEnd));
    }
    if (year.isPresent() || !month.isPresent()) {
      LocalDate yearBegin = new LocalDate(year.get(), 1, 1);
      LocalDate yearEnd = new LocalDate(year.get(), 12, 31);
      condition.and(personDay.date.between(yearBegin, yearEnd));
    }

    query.where(condition);

    return query.orderBy(personDay.person.surname.asc())
            .orderBy(personDay.date.asc()).list(personDay);
  }
  
  
  /**
   * 
   * @param personList
   * @param date
   * @return la lista dei personDay relativi a un singolo giorno di tutte le persone presenti nella lista.
   */
  public List<PersonDay> getPersonDayForPeopleInDay(List<Person> personList, LocalDate date) {
    QPersonDay personDay = QPersonDay.personDay;
    final JPQLQuery query = getQueryFactory().from(personDay).where(personDay.date.eq(date).and(personDay.person.in(personList)));
    return query.orderBy(personDay.person.surname.asc()).list(personDay);
  }
}
