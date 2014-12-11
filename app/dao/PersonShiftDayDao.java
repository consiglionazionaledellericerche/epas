package dao;

import helpers.ModelQuery;

import org.joda.time.LocalDate;

import com.mysema.query.jpa.JPQLQuery;

import models.Person;
import models.PersonShiftDay;
import models.query.QPersonShiftDay;

public class PersonShiftDayDao {

	/**
	 * 
	 * @param person
	 * @param date
	 * @return il personShiftDay relativo alla persona person nel caso in cui in data date fosse in turno
	 * Null altrimenti 
	 */
	public static PersonShiftDay getPersonShiftDay(Person person, LocalDate date){
		QPersonShiftDay personShiftDay = QPersonShiftDay.personShiftDay;
		JPQLQuery query = ModelQuery.queryFactory().from(personShiftDay)
				.where(personShiftDay.personShift.person.eq(person).and(personShiftDay.date.eq(date)));
		
		return query.singleResult(personShiftDay);
		
	}
}
