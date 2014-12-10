package dao;

import helpers.ModelQuery;

import java.util.List;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import models.Person;
import models.PersonDay;
import models.query.QAbsenceType;
import models.query.QPersonDay;

public class PersonDayDao {

	
	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @return la lista dei personday relativi a una persona in un certo periodo di tempo se il parametro end è presente, 
	 * il personDay relativo a un certo giorno specifico (begin) altrimenti
	 */
	public static List<PersonDay> getPersonDayInPeriod(Person person, LocalDate begin, Optional<LocalDate> end){
		QPersonDay personDay = QPersonDay.personDay;
		final BooleanBuilder condition = new BooleanBuilder();
		final JPQLQuery query = ModelQuery.queryFactory().from(personDay);
		if(end.isPresent())
			condition.and(personDay.date.between(begin, end.get()));
		else
			condition.and(personDay.date.eq(begin));
		condition.and(personDay.person.eq(person));
		
		query.where(condition);
		return query.list(personDay);
	}
}
