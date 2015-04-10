package dao;

import helpers.ModelQuery;

import java.util.List;

import models.Person;
import models.PersonShift;
import models.PersonShiftDay;
import models.ShiftType;
import models.enumerate.ShiftSlot;
import models.query.QPersonShift;
import models.query.QPersonShiftDay;

import org.joda.time.LocalDate;

import com.mysema.query.jpa.JPQLQuery;

/**
 * 
 * @author dario
 *
 */
public class PersonShiftDayDao {

	public final static QPersonShiftDay personShiftDay = QPersonShiftDay.personShiftDay;
	public final static QPersonShift personShift = QPersonShift.personShift;
	/**
	 * 
	 * @param person
	 * @param date
	 * @return il personShiftDay relativo alla persona person nel caso in cui in data date fosse in turno
	 * Null altrimenti 
	 */
	public static PersonShiftDay getPersonShiftDay(Person person, LocalDate date){
		
		JPQLQuery query = ModelQuery.queryFactory().from(personShiftDay)
				.where(personShiftDay.personShift.person.eq(person).and(personShiftDay.date.eq(date)));
		
		return query.singleResult(personShiftDay);
		
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
	public static List<PersonShiftDay> getPersonShiftDayByTypeAndPeriod(LocalDate from, LocalDate to, ShiftType type){
		JPQLQuery query = ModelQuery.queryFactory().from(personShiftDay).where(personShiftDay.date.between(from, to)
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
	public static PersonShiftDay getPersonShiftDayByTypeDateAndSlot(ShiftType shiftType, LocalDate date, ShiftSlot shiftSlot){
		JPQLQuery query = ModelQuery.queryFactory().from(personShiftDay).where(personShiftDay.date.eq(date)
				.and(personShiftDay.shiftType.eq(shiftType)
						.and(personShiftDay.shiftSlot.eq(shiftSlot))));
		return query.singleResult(personShiftDay);
	}
	
	
	/**
	 * 
	 * @param person
	 * @return il personShift associato alla persona passata come parametro
	 */
	public static PersonShift getPersonShiftByPerson(Person person){
		JPQLQuery query = ModelQuery.queryFactory().from(personShift).where(personShift.person.eq(person));
		return query.singleResult(personShift);
	}
	
}
