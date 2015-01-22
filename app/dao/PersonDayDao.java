package dao;

import helpers.ModelQuery;

import java.util.List;

import models.Person;
import models.PersonDay;
import models.query.QPersonDay;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;


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
	 * Se ordered è 'true' la lista dei personDay viene ordinata per data crescente
	 */
	public static List<PersonDay> getPersonDayInPeriod(Person person, LocalDate begin, Optional<LocalDate> end, boolean ordered){
		QPersonDay personDay = QPersonDay.personDay;
		final BooleanBuilder condition = new BooleanBuilder();
		final JPQLQuery query = ModelQuery.queryFactory().from(personDay);
		
		condition.and(personDay.date.between(begin, end.or(begin)));
		condition.and(personDay.person.eq(person));
		
		query.where(condition);
		if(ordered)
			query.orderBy(personDay.date.asc());
		return query.list(personDay);
	}
	
	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @param ordered
	 * @return la lista dei personDay della persona person nel periodo begin-end ordinata per data in ordine decrescente
	 */
	public static List<PersonDay> getPersonDayInPeriodDesc(Person person, LocalDate begin, LocalDate end, boolean ordered){
		QPersonDay personDay = QPersonDay.personDay;
		final BooleanBuilder condition = new BooleanBuilder();
		final JPQLQuery query = ModelQuery.queryFactory().from(personDay);
		
		condition.and(personDay.date.between(begin, end));
		condition.and(personDay.person.eq(person));
		
		query.where(condition);
		if(ordered)
			query.orderBy(personDay.date.desc());
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
	
	
	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @param isAvailable
	 * @return la lista dei giorni compresi tra begin e end in cui la persona person può usare i ticket 
	 */
	public static List<PersonDay> getPersonDayForTicket(Person person, LocalDate begin, LocalDate end, boolean isAvailable){
		QPersonDay personDay = QPersonDay.personDay;
		final JPQLQuery query = ModelQuery.queryFactory().from(personDay)
				.where(personDay.person.eq(person).and(personDay.date.between(begin, end)).and(personDay.isTicketAvailable.eq(isAvailable)));
		return query.orderBy(personDay.date.asc()).list(personDay);
	}
	
	
	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @return il personDay precedente al giorno in cui viene richiamata questa funzione. Utilizzato per creare il recap della lista
	 * dei personDay
	 */
	public static PersonDay getPersonDayForRecap(Person person, Optional<LocalDate> begin, LocalDate end){
		QPersonDay personDay = QPersonDay.personDay;
		BooleanBuilder condition = new BooleanBuilder();
		if(begin.isPresent())
			condition.and(personDay.date.goe(begin.get()));
		final JPQLQuery query = ModelQuery.queryFactory().from(personDay)
				.where(personDay.person.eq(person).and(condition.and(personDay.date.lt(end))))
				.orderBy(personDay.date.desc());
		return query.singleResult(personDay);
	}
}
