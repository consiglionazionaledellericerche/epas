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
 * @author dario
 */
public class PersonShiftDayDao extends DaoBase {


  @Inject
  PersonShiftDayDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @return il personShiftDay relativo alla persona person nel caso in cui in data date fosse in
   * turno Null altrimenti.
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
   * @return la lista dei personShiftDay presenti nel periodo compreso tra 'from' e 'to' aventi lo
   * shiftType 'type'. Se specificato filtra sulla persona richiesta.
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
   * @return il personShiftDay relativo al tipo 'shiftType' nel giorno 'date' con lo slot
   * 'shiftSlot'.
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
   * @return la lista dei personShift disabilitati.
   */
  public List<PersonShift> getDisabled() {
    final QPersonShift personShift = QPersonShift.personShift;
    return getQueryFactory().selectFrom(personShift).where(personShift.disabled.eq(true)).fetch();
  }

  /**
   * Tutti i PersonReperibilityType.
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

  public long countByPersonAndDate(Person person, LocalDate date) {

    final QPersonShiftDay shiftDay = QPersonShiftDay.personShiftDay;
    return getQueryFactory().from(shiftDay)
        .where(shiftDay.personShift.person.eq(person).and(shiftDay.date.eq(date))).fetchCount();
  }

  public List<PersonShiftDay> listByDateAndActivity(LocalDate date, ShiftType activity) {
    final QPersonShiftDay shiftDay = QPersonShiftDay.personShiftDay;
    return getQueryFactory().selectFrom(shiftDay)
        .where(shiftDay.date.eq(date).and(shiftDay.shiftType.eq(activity))).fetch();
  }

}
