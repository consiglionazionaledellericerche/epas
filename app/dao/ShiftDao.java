package dao;

import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.Person;
import models.PersonShift;
import models.PersonShiftDay;
import models.ShiftCancelled;
import models.ShiftCategories;
import models.ShiftType;
import models.query.QPersonShiftDay;
import models.query.QPersonShiftShiftType;
import models.query.QShiftCancelled;
import models.query.QShiftType;

import org.joda.time.LocalDate;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

/**
 * Dao per i turni.
 *
 * @author dario
 */
public class ShiftDao extends DaoBase {

  @Inject
  ShiftDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);

  }

  /**
   * @return lo shiftType corrispondente al tipo type passato come parametro.
   */
  public ShiftType getShiftTypeByType(String type) {
    final QShiftType shiftType = QShiftType.shiftType;
    JPQLQuery query = getQueryFactory().from(shiftType).where(shiftType.type.eq(type));
    return query.singleResult(shiftType);
  }

  /**
   * @return la lista dei personShiftDay con ShiftType 'type' presenti nel periodo tra 'begin' e
   *     'to'.
   */
  public List<PersonShiftDay> getShiftDaysByPeriodAndType(
      LocalDate begin, LocalDate to, ShiftType type) {
    final QPersonShiftDay psd = QPersonShiftDay.personShiftDay;
    JPQLQuery query = getQueryFactory().from(psd)
            .where(psd.date.between(begin, to)
                    .and(psd.shiftType.eq(type))).orderBy(psd.shiftSlot.asc(), psd.date.asc());
    return query.list(psd);
  }

  /**
   * @author arianna
   *
   * @return la lista dei 'personShiftDay' della persona 'person' di tipo 'type' presenti nel
   *     periodo tra 'begin' e 'to'.
   */
  public List<PersonShiftDay> getPersonShiftDaysByPeriodAndType(
      LocalDate begin, LocalDate to, ShiftType type, Person person) {
    final QPersonShiftDay psd = QPersonShiftDay.personShiftDay;
    JPQLQuery query = getQueryFactory().from(psd)
            .where(psd.date.between(begin, to)
                    .and(psd.shiftType.eq(type))
                    .and(psd.personShift.person.eq(person))
            )
            .orderBy(psd.shiftSlot.asc(), psd.date.asc());
    return query.list(psd);
  }


  /**
   * @return la lista dei turni cancellati relativi al tipo 'type' nel periodo compreso tra 'from'
   *     e 'to'.
   */
  public List<ShiftCancelled> getShiftCancelledByPeriodAndType(
      LocalDate from, LocalDate to, ShiftType type) {
    final QShiftCancelled sc = QShiftCancelled.shiftCancelled;
    JPQLQuery query =
        getQueryFactory().from(sc)
          .where(sc.date.between(from, to).and(sc.type.eq(type))).orderBy(sc.date.asc());
    return query.list(sc);
  }

  /**
   * @return il turno cancellato relativo al giorno 'day' e al tipo 'type' passati come parametro.
   */
  public ShiftCancelled getShiftCancelled(LocalDate day, ShiftType type) {
    final QShiftCancelled sc = QShiftCancelled.shiftCancelled;
    JPQLQuery query = getQueryFactory().from(sc).where(sc.date.eq(day).and(sc.type.eq(type)));
    return query.singleResult(sc);
  }

  /**
   * @return il quantitativo di shiftCancelled effettivamente cancellati.
   */
  public Long deleteShiftCancelled(ShiftType type, LocalDate day) {
    final QShiftCancelled sc = QShiftCancelled.shiftCancelled;
    return getQueryFactory().delete(sc).where(sc.date.eq(day).and(sc.type.eq(type))).execute();
  }


  /**
   * @return il PersonShift relativo alla persona person e al tipo type passati come parametro.
   * @author arianna
   */
  public PersonShift getPersonShiftByPersonAndType(Long personId, String type) {
    final QPersonShiftShiftType psst = QPersonShiftShiftType.personShiftShiftType;

    JPQLQuery query = getQueryFactory().from(psst).where(
            psst.personShift.person.id.eq(personId)
                    .and(psst.shiftType.type.eq(type))
                    .and(psst.beginDate.isNull().or(psst.beginDate.loe(LocalDate.now())))
                    .and(psst.endDate.isNull().or(psst.endDate.goe(LocalDate.now())))
    );
    return query.singleResult(psst.personShift);
  }


  /**
   * @author arianna

   * @return la categoria associata al tipo di turno.
   *
   */
  public ShiftCategories getShiftCategoryByType(String type) {
    final QShiftType st = QShiftType.shiftType;

    JPQLQuery query = getQueryFactory().from(st)
            .where(st.type.eq(type));
    return query.singleResult(st.shiftCategories);
  }

}

