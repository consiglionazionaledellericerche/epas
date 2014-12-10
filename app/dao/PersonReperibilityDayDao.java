package dao;

import helpers.ModelQuery;

import org.joda.time.LocalDate;

import com.mysema.query.jpa.JPQLQuery;

import models.Person;
import models.PersonReperibilityDay;
import models.query.QPersonReperibilityDay;

public class PersonReperibilityDayDao {

	/**
	 * 
	 * @param person
	 * @param date
	 * @return un personReperibilityDay nel caso in cui la persona person in data date fosse reperibile.
	 * Null altrimenti
	 */
	public static PersonReperibilityDay getPersonReperibilityDay(Person person, LocalDate date){
		QPersonReperibilityDay personReperibilityDay = QPersonReperibilityDay.personReperibilityDay;
		JPQLQuery query = ModelQuery.queryFactory().from(personReperibilityDay)
				.where(personReperibilityDay.personReperibility.person.eq(person).and(personReperibilityDay.date.eq(date)));
		if(query.list(personReperibilityDay).size() > 0)
			return query.list(personReperibilityDay).get(0);
		else
			return null;
	}
}
