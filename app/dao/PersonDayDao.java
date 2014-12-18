package dao;

import helpers.ModelQuery;

import java.util.List;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.types.Expression;

import models.Person;
import models.PersonDay;
import models.query.QAbsenceType;
import models.query.QPersonDay;

/**
 * 
 * @author dario
 *
 */
public class PersonDayDao {

	
	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @param ordered
	 * @return la lista dei personday relativi a una persona in un certo periodo di tempo  
	 * Se ordered è 'true' la lista dei personDay viene ordinata
	 */
	public static List<PersonDay> getPersonDayInPeriod(Person person, LocalDate begin, LocalDate end, boolean ordered){
		QPersonDay personDay = QPersonDay.personDay;
		final BooleanBuilder condition = new BooleanBuilder();
		final JPQLQuery query = ModelQuery.queryFactory().from(personDay);
		
		condition.and(personDay.date.between(begin, end));
		condition.and(personDay.person.eq(person));
		
		query.where(condition);
		if(ordered)
			query.orderBy(personDay.date.asc());
		return query.list(personDay);
	}
	
	
	/**
	 * 
	 * @param personDayId
	 * @return il personDay associato all'id passato come parametro
	 */
	public static PersonDay getPersonDayById(Long personDayId){
		QPersonDay personDay = QPersonDay.personDay;
		final JPQLQuery query = ModelQuery.queryFactory().from(personDay)
				.where(personDay.id.eq(personDayId));
		return query.singleResult(personDay);
	}
	
	
	/**
	 * 
	 * @param person
	 * @return tutti i personDay relativi alla persona person passata come parametro
	 */
	public static List<PersonDay> getAllPersonDay(Person person){
		QPersonDay personDay = QPersonDay.personDay;
		final JPQLQuery query = ModelQuery.queryFactory().from(personDay)
				.where(personDay.person.eq(person));
		return query.list(personDay);
	}
	
	
	/**
	 * 
	 * @param person
	 * @param date
	 * @return il personDay relativo al giorno e alla persona passati come parametro. E' optional perchè potrebbe non esistere
	 */
	public static Optional<PersonDay> getSinglePersonDay(Person person, LocalDate date){
		QPersonDay personDay = QPersonDay.personDay;
		final JPQLQuery query = ModelQuery.queryFactory().from(personDay)
				.where(personDay.person.eq(person).and(personDay.date.eq(date)));
		return Optional.fromNullable(query.singleResult(personDay));
	}
}
