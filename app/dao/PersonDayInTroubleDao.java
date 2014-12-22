package dao;

import helpers.ModelQuery;

import java.util.List;

import org.joda.time.LocalDate;

import com.mysema.query.jpa.JPQLQuery;

import models.Person;
import models.PersonDayInTrouble;
import models.query.QPersonDayInTrouble;

public class PersonDayInTroubleDao {

	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @param fixed
	 * @return la lista dei personDayInTrouble relativi alla persona person nel periodo begin-end. E' possibile specificare se si vuole
	 * ottenere quelli fixati (fixed = true) o no (fixed = false)
	 */
	public static List<PersonDayInTrouble> getPersonDayInTroubleInPeriod(Person person, LocalDate begin, LocalDate end, boolean fixed){
		QPersonDayInTrouble pdit = QPersonDayInTrouble.personDayInTrouble;
		final JPQLQuery query = ModelQuery.queryFactory().from(pdit)
				.where(pdit.personDay.person.eq(person).and(pdit.personDay.date.between(begin, end)).and(pdit.fixed.eq(fixed)));
		return query.list(pdit);
	}
}
