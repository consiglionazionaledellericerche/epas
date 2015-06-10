package dao;

import java.util.List;

import javax.persistence.EntityManager;

import models.Person;
import models.PersonDay;
import models.query.QPersonDay;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;


/**
 * 
 * @author dario
 *
 */
public class PersonDayDao extends DaoBase {

	@Inject
	PersonDayDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @param ordered
	 * @return la lista dei personday relativi a una persona in un certo periodo di tempo  
	 * Se ordered è 'true' la lista dei personDay viene ordinata per data crescente
	 */
	public List<PersonDay> getPersonDayInPeriod(Person person, LocalDate begin, Optional<LocalDate> end, boolean ordered){
		
		final QPersonDay personDay = QPersonDay.personDay;
		final BooleanBuilder condition = new BooleanBuilder();
		final JPQLQuery query = getQueryFactory().from(personDay);
		
		condition.and(personDay.date.between(begin, end.or(begin)));
		condition.and(personDay.person.eq(person));
		
		query.where(condition);
		if(ordered)
			query.orderBy(personDay.date.asc());
		return query.list(personDay);
	}
	
	/**
	 * La lista dei PersonDay appartenenti al mese anno. Ordinati in modo crescente.
	 * 
	 * @param person
	 * @param yearMonth
	 * @return
	 */
	public List<PersonDay> getPersonDayInMonth(Person person, YearMonth yearMonth) {
	
		LocalDate monthBegin = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
		LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();
		
		final QPersonDay personDay = QPersonDay.personDay;
		final JPQLQuery query = getQueryFactory()
				.from(personDay)
				.where(personDay.person.eq(person)
				.and(personDay.date.between(monthBegin, monthEnd)))
				.orderBy(personDay.date.asc());
		
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
	public List<PersonDay> getPersonDayInPeriodDesc(Person person, LocalDate begin, LocalDate end, boolean ordered){
		QPersonDay personDay = QPersonDay.personDay;
		final BooleanBuilder condition = new BooleanBuilder();
		final JPQLQuery query = getQueryFactory().from(personDay);
		
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
	public PersonDay getPersonDayById(Long personDayId){
		final QPersonDay personDay = QPersonDay.personDay;
		final JPQLQuery query = getQueryFactory().from(personDay)
				.where(personDay.id.eq(personDayId));
		return query.singleResult(personDay);
	}
	
	
	/**
	 * 
	 * @param person
	 * @return tutti i personDay relativi alla persona person passata come parametro
	 */
	public List<PersonDay> getAllPersonDay(Person person){
		final QPersonDay personDay = QPersonDay.personDay;
		final JPQLQuery query = getQueryFactory().from(personDay)
				.where(personDay.person.eq(person));
		return query.list(personDay);
	}
	
	
	/**
	 * 
	 * @param person
	 * @param date
	 * @return il personDay relativo al giorno e alla persona passati come parametro. E' optional perchè potrebbe non esistere
	 */
	public Optional<PersonDay> getSinglePersonDay(Person person, LocalDate date){
		final QPersonDay personDay = QPersonDay.personDay;
		final JPQLQuery query = getQueryFactory().from(personDay)
				.where(personDay.person.eq(person).and(personDay.date.eq(date)));
		return Optional.fromNullable(query.singleResult(personDay));
	}
	/**
	 * 
	 * @param person
	 * @param date
	 * @return il personDay relativo al giorno e alla persona passati come parametro. E' optional perchè potrebbe non esistere
	 */
	@Deprecated
	public Optional<PersonDay> getSinglePersonDayStatic(Person person, LocalDate date){
		final QPersonDay personDay = QPersonDay.personDay;
		final JPQLQuery query = getQueryFactory().from(personDay)
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
	public List<PersonDay> getPersonDayForTicket(Person person, 
			LocalDate begin, LocalDate end, boolean isAvailable){
		QPersonDay personDay = QPersonDay.personDay;
		final JPQLQuery query = getQueryFactory().from(personDay)
				.where(personDay.person.eq(person)
						.and(personDay.date.between(begin, end))
						.and(personDay.isTicketAvailable.eq(isAvailable)));
		return query.orderBy(personDay.date.asc()).list(personDay);
	}
		
	/**
	 * Il primo personDay esistente precedente a date per person.
	 * 
	 * @param person
	 * @param date
	 * @return
	 */
	public PersonDay getPreviousPersonDay(Person person, LocalDate date){
		
		final QPersonDay personDay = QPersonDay.personDay;
		
		final JPQLQuery query = getQueryFactory()
				.from(personDay)
				.where(personDay.person.eq(person).and(personDay.date.lt(date)))
				.orderBy(personDay.date.desc());
		
		return query.singleResult(personDay);
	}
	
	/**
	 * 
	 * @param person
	 * @return
	 */
	
	/**
	 * I person day della persona festivi e con ore lavorate. Utilizzo:s
	 * Nel mese year.present e month.present
	 * Nell'anno year.present e month.absent
	 * Sempre year.absent e month.absent
	 * @param person
	 * @param year	
	 * @param month
	 * @return
	 */
	public List<PersonDay> getHolidayWorkingTime(Person person, Optional<Integer> year, 
			Optional<Integer> month) {
		
		QPersonDay personDay = QPersonDay.personDay;

		final BooleanBuilder condition = new BooleanBuilder();
		
		final JPQLQuery query = getQueryFactory().from(personDay);
		
		condition.and(personDay.person.eq(person));
		condition.and(personDay.timeAtWork.goe(1));
		condition.and(personDay.isHoliday.eq(true));
		
		if(year.isPresent() && month.isPresent()) {
			LocalDate monthBegin = new LocalDate(year.get(),month.get(),1);
			LocalDate monthEnd = monthBegin.monthOfYear().withMaximumValue();
			condition.and(personDay.date.between(monthBegin, monthEnd));
		}
		if(year.isPresent() || !month.isPresent()) {
			LocalDate yearBegin = new LocalDate(year.get(),1,1);
			LocalDate yearEnd = new LocalDate(year.get(), 12, 31);
			condition.and(personDay.date.between(yearBegin, yearEnd));
		}
		
		query.where(condition);
	
		return query.orderBy(personDay.person.surname.asc())
				.orderBy(personDay.date.asc()).list(personDay);
	}
}
