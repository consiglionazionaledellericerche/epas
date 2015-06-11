package dao;

import java.util.List;

import javax.persistence.EntityManager;

import models.Person;
import models.PersonDay;
import models.Stamping;
import models.query.QAbsence;
import models.query.QPersonDay;
import models.query.QStamping;

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
	 * @param personDayId
	 * @return 
	 */
	public PersonDay getPersonDayById(Long personDayId){
		
		final QPersonDay personDay = QPersonDay.personDay;
		
		return getQueryFactory()
				.from(personDay)
				.where(personDay.id.eq(personDayId))
				.singleResult(personDay);
	}
	
	/**
	 * 
	 * @param person
	 * @param date
	 * @return il personDay relativo al giorno e alla persona passati come parametro. 
	 * E' optional perchè potrebbe non esistere
	 */
	public Optional<PersonDay> getPersonDay(Person person, LocalDate date) {
		
		final QPersonDay personDay = QPersonDay.personDay;
		
		final JPQLQuery query = getQueryFactory()
				.from(personDay)
				.where(personDay.person.eq(person).and(personDay.date.eq(date)));
		
		return Optional.fromNullable(query.singleResult(personDay));
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
	 * @return tutti i personDay relativi alla persona person passata come parametro
	 */
	public List<PersonDay> getAllPersonDay(Person person){
		
		final QPersonDay personDay = QPersonDay.personDay;
		final JPQLQuery query = getQueryFactory().from(personDay)
				.where(personDay.person.eq(person));
		
		return query.list(personDay);
	}
	
	/**
	 * Supporto alla ricerca dei personday.
	 * Default: fetch delle timbrature e ordinamento crescente per data
	 *  
	 * @param person
	 * @param begin
	 * @param end
	 * @param fetchAbsences true se fetch di absences anzichè stampings
	 * @param orderedDesc true se si vuole ordinamento decrescente
	 * @param onlyIsTicketAvailable
	 * @return
	 */
	private List<PersonDay> getPersonDaysFetched(Person person, 
			LocalDate begin, Optional<LocalDate> end, boolean fetchAbsences, 
			boolean orderedDesc, boolean onlyIsTicketAvailable) {
		
		final QPersonDay personDay = QPersonDay.personDay;
		final QStamping stamping = QStamping.stamping;
		final QAbsence absence = QAbsence.absence;
		
		build(person, begin, end, fetchAbsences, 
				orderedDesc, onlyIsTicketAvailable)
		.leftJoin(personDay.stampings, stamping).fetch()
		.list(personDay);
		
		return build(person, begin, end, fetchAbsences, 
				orderedDesc, onlyIsTicketAvailable)
				.leftJoin(personDay.absences, absence).fetch()
				.list(personDay);
		
	}
	
	private JPQLQuery build(Person person, 
			LocalDate begin, Optional<LocalDate> end, boolean fetchAbsences, 
			boolean orderedDesc, boolean onlyIsTicketAvailable) {

		final QPersonDay personDay = QPersonDay.personDay;
		
		final BooleanBuilder condition = new BooleanBuilder();
		final JPQLQuery query = getQueryFactory().from(personDay);

	
		condition.and(personDay.date.between(begin, end.or(begin)));
		condition.and(personDay.person.eq(person));
		if (onlyIsTicketAvailable) {
			condition.and(personDay.isTicketAvailable.eq(true));
		}
		query.where(condition);
		
		if ( orderedDesc ) {
			query.orderBy(personDay.date.desc());
		} else {
			query.orderBy(personDay.date.asc());
		}
		
		query.distinct();

		return query;
		
	}


	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @return
	 */
	public List<PersonDay> getPersonDayInPeriod(Person person, LocalDate begin, 
			Optional<LocalDate> end){
		
		return getPersonDaysFetched(person, begin, end, 
				false, false, false);
	}
	
	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @param orderedDesc
	 * @return
	 */
	public List<PersonDay> getPersonDayInPeriodDesc(Person person, LocalDate begin, 
			Optional<LocalDate> end){
		
		return getPersonDaysFetched(person, begin, end, 
				false, true, false);
	}
	
//	/**
//	 * 
//	 * @param person
//	 * @param begin
//	 * @param end
//	 * @param ordered
//	 * @return
//	 */
//	public List<PersonDay> getPersonDayInPeriodForAbsences(Person person, LocalDate begin, 
//			Optional<LocalDate> end){
//		
//		return getPersonDaysFetched(person, begin, end, 
//				true, false, false);
//	}
	
	/**
	 * La lista dei PersonDay appartenenti al mese anno. 
	 * Ordinati in modo crescente.
	 * 
	 * @param person
	 * @param yearMonth
	 * @return
	 */
	public List<PersonDay> getPersonDayInMonth(Person person, YearMonth yearMonth) {
	
		LocalDate begin = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
		LocalDate end = begin.dayOfMonth().withMaximumValue();
		
		return getPersonDaysFetched(person, begin, Optional.fromNullable(end), 
				false, false, false);
	}

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
