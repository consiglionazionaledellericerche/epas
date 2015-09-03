package dao;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import models.Person;
import models.PersonShift;
import models.PersonShiftDay;
import models.ShiftType;
import models.enumerate.ShiftSlot;
import models.query.QPersonShift;
import models.query.QPersonShiftDay;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

/**
 * 
 * @author dario
 *
 */
public class PersonShiftDayDao extends DaoBase{

	@Inject
	PersonShiftDayDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	/**
	 * 
	 * @param person
	 * @param date
	 * @return il personShiftDay relativo alla persona person nel caso in cui in data date fosse in turno
	 * Null altrimenti 
	 */
	public Optional<PersonShiftDay> getPersonShiftDay(Person person, LocalDate date){
		final QPersonShiftDay personShiftDay = QPersonShiftDay.personShiftDay;
		final QPersonShift personShift = QPersonShift.personShift;

		JPQLQuery query = getQueryFactory().from(personShiftDay)
				.join(personShiftDay.personShift, personShift)
				.where(personShift.person.eq(person).and(personShiftDay.date.eq(date)));
		PersonShiftDay psd = query.singleResult(personShiftDay);

		return Optional.fromNullable(psd);
	}

	/**
	 * 
	 * @param from
	 * @param to
	 * @param type
	 * @return la lista dei personShiftDay presenti nel periodo compreso tra 'from' e 'to' aventi lo shiftType 'type'
	 * 
	 * PersonShiftDay.find("SELECT psd FROM PersonShiftDay psd WHERE date BETWEEN ? AND ? AND psd.shiftType = ? ORDER by date", firstOfMonth, lastOfMonth, shiftType).fetch();
	 */
	public List<PersonShiftDay> getPersonShiftDayByTypeAndPeriod(LocalDate from, LocalDate to, ShiftType type){
		final QPersonShiftDay personShiftDay = QPersonShiftDay.personShiftDay;

		JPQLQuery query = getQueryFactory().from(personShiftDay).where(personShiftDay.date.between(from, to)
				.and(personShiftDay.shiftType.eq(type))).orderBy(personShiftDay.date.asc());
		return query.list(personShiftDay);
	}

	/**
	 * 
	 * @param shiftType
	 * @param date
	 * @param shiftSlot
	 * @return il personShiftDay relativo al tipo 'shiftType' nel giorno 'date' con lo slot 'shiftSlot'
	 */
	public PersonShiftDay getPersonShiftDayByTypeDateAndSlot(ShiftType shiftType, LocalDate date, ShiftSlot shiftSlot){
		final QPersonShiftDay personShiftDay = QPersonShiftDay.personShiftDay;

		JPQLQuery query = getQueryFactory().from(personShiftDay).where(personShiftDay.date.eq(date)
				.and(personShiftDay.shiftType.eq(shiftType)
						.and(personShiftDay.shiftSlot.eq(shiftSlot))));
		return query.singleResult(personShiftDay);
	}


	/**
	 * 
	 * @param person
	 * @return il personShift associato alla persona passata come parametro
	 */
	public PersonShift getPersonShiftByPerson(Person person){
		final QPersonShift personShift = QPersonShift.personShift;
		JPQLQuery query = getQueryFactory().from(personShift).where(personShift.person.eq(person));
		return query.singleResult(personShift);
	}

}
