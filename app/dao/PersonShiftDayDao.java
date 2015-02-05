package dao;

import helpers.ModelQuery;

import org.joda.time.LocalDate;

import com.mysema.query.jpa.JPQLQuery;

import models.Person;
import models.PersonShiftDay;
import models.query.QPersonShiftDay;

/**
 * 
 * @author dario
 *
 */
public class PersonShiftDayDao {

	public final static QPersonShiftDay personShiftDay = QPersonShiftDay.personShiftDay;
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
	
	
	
}
