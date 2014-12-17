package dao;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;
import it.cnr.iit.epas.DateInterval;

import java.util.List;
import java.util.Set;

import models.CompetenceCode;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.query.QCompetenceCode;
import models.query.QContract;
import models.query.QPerson;
import models.query.QPersonDay;
import models.query.QPersonHourForOvertime;
import models.query.QPersonReperibility;
import models.query.QPersonShift;
import models.query.QUser;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

/**
 * DAO per le person.
 * 
 * @author marco
 *
 */
public final class PersonDao {

	private PersonDao() {}
	
	/**
	 * @param name
	 * @param offices obbligatorio
	 * @param onlyTechnician
	 * @return la lista delle person corrispondenti
	 */
	public static SimpleResults<Person> list(Optional<String> name, Set<Office> offices, 
			boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {
		
		Preconditions.checkState(!offices.isEmpty());
		
		final QPerson qp = QPerson.person;
		final QContract qc = QContract.contract;
		// TODO: completare con l'intervallo
		//final LocalDate start = new LocalDate();
		//final LocalDate end = start;
				
					
		 final JPQLQuery query = ModelQuery.queryFactory().from(qp)
					.leftJoin(qp.contracts, qc).fetch()
					.leftJoin(qc.contractWorkingTimeType).fetch()
					.leftJoin(qc.contractStampProfile).fetch()
					.leftJoin(qc.vacationPeriods).fetch()
					.leftJoin(qp.personHourForOvertime).fetch()
					.leftJoin(qp.reperibility).fetch()
					.leftJoin(qp.personShift).fetch()
					.orderBy(qp.surname.asc(), qp.name.asc())
					.distinct();
		
		
		final BooleanBuilder condition = new BooleanBuilder();
		condition.and(qp.office.in(offices));
		
		if (onlyTechnician) {
			// i livelli sopra al 3 sono dei tecnici:
			condition.and(qp.qualification.qualification.gt(3));
		}
		
		if (name.isPresent() && !name.get().trim().isEmpty()) {
			condition.andAnyOf(qp.name.startsWithIgnoreCase(name.get()),
					qp.surname.startsWithIgnoreCase(name.get()));
		}
		
		if(onlyOnCertificate)
			condition.and(qc.onCertificate.isTrue());
		
		condition.andAnyOf(
				
				//contratto terminato
				qc.endContract.isNotNull().and(qc.beginContract.loe(end)).and(qc.endContract.goe(start)),

				//contratto non terminato
				qc.endContract.isNull().and(

						//contratto tempo indeterminato
						qc.expireContract.isNull().and(qc.beginContract.loe(end))
						
						.or(
						
						//contratto tempo determinato
						qc.expireContract.isNotNull().and(qc.beginContract.loe(end)).and(qc.expireContract.goe(start))
						
						)
					)
				
				);
		
		/*
		condition.and(qc.beginContract.before(end));
		
		condition.andAnyOf(
				qc.endContract.isNull().and(qc.expireContract.isNull()),											//contratto indeterminato non terminato
				
				qc.endContract.isNull().and( qc.expireContract.isNotNull().and(qc.expireContract.goe(start)) ), 	//contratto determinato non terminato
				
				qc.endContract.isNotNull().and(qc.endContract.goe(start)) );										//contratto terminato 
				
				*/
				

		query.where(condition);
		
		return ModelQuery.simpleResults(query, qp);
	}
	
	
	/**
	 * @param name
	 * @param offices obbligatorio
	 * @param onlyTechnician
	 * @return la lista delle person corrispondenti
	 */
	public static SimpleResults<Person> listForCompetence(CompetenceCode compCode, Optional<String> name, Set<Office> offices, 
			boolean onlyTechnician, LocalDate start, LocalDate end) {
		
		Preconditions.checkState(!offices.isEmpty());
		
		final QPerson qp = QPerson.person;
		final QContract qc = QContract.contract;
		final QCompetenceCode qcc = QCompetenceCode.competenceCode;
		// TODO: completare con l'intervallo
		//final LocalDate start = new LocalDate();
		//final LocalDate end = start;
				
		final JPQLQuery query = ModelQuery.queryFactory().from(qp)
				.leftJoin(qp.contracts, qc)
				.leftJoin(qp.personHourForOvertime, QPersonHourForOvertime.personHourForOvertime).fetch()
				//.leftJoin(qp.location, QLocation.location)
				.leftJoin(qp.reperibility, QPersonReperibility.personReperibility).fetch()
				.leftJoin(qp.personShift, QPersonShift.personShift).fetch()
				.leftJoin(qp.user, QUser.user)
				.leftJoin(qp.competenceCode, qcc)
				.orderBy(qp.surname.asc(), qp.name.asc())
				.distinct();
		
		
		
		final BooleanBuilder condition = new BooleanBuilder();
		condition.and(qp.office.in(offices));
		
		if (onlyTechnician) {
			// i livelli sopra al 3 sono dei tecnici:
			condition.and(qp.qualification.qualification.gt(3));
		}
		
		if (name.isPresent() && !name.get().trim().isEmpty()) {
			condition.andAnyOf(qp.name.startsWithIgnoreCase(name.get()),
					qp.surname.startsWithIgnoreCase(name.get()));
		}
		condition.and(qp.competenceCode.contains(compCode));
		condition.and(qc.onCertificate.isTrue());
		condition.and(qc.beginContract.before(end));
		condition.andAnyOf(qc.endContract.isNull().and(qc.expireContract.isNull()),
				qc.expireContract.isNotNull().and(qc.expireContract.goe(start)),
				qc.endContract.isNotNull().and(qc.endContract.goe(start)));
		
		query.where(condition);
		
		return ModelQuery.simpleResults(query, qp);
	}
	
	/**
	 * L'ultimo contratto inserito in ordine di data inizio. 
	 * (Tendenzialmente quello attuale)
	 * @param person
	 * @return
	 */
	public Contract getLastContract(Person person) {
		
		final QContract qc = QContract.contract;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(qc)
				.where(qc.person.eq(person))
				.orderBy(qc.beginContract.desc());
		
		List<Contract> contracts = query.list(qc);
		if(contracts.size() == 0)
			return null;
		return contracts.get(0);
		
	}
	
	/**
	 * Il contratto precedente in ordine temporale rispetto a quello passato
	 * come argomento.
	 * @param contract
	 * @return
	 */
	public static Contract getPreviousPersonContract(Contract contract) {
	
		final QContract qc = QContract.contract;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(qc)
				.where(qc.person.eq(contract.person))
				.orderBy(qc.beginContract.desc());
		
		List<Contract> contracts = query.list(qc);
		
		final int indexOf = contracts.indexOf(contract);
		if(indexOf + 1 < contracts.size())
			return contracts.get(indexOf + 1);
		else 
			return null;
	}
	
	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @return la lista di contratti che soddisfa le seguenti condizioni:
	 */
	public static List<Contract> getContractList(Person person, LocalDate begin, LocalDate end){
		final QContract qc = QContract.contract;
		final JPQLQuery query = ModelQuery.queryFactory().from(qc)
				.where(qc.person.eq(person)
						.andAnyOf(qc.endContract.isNotNull().and(qc.endContract.between(begin, end))
								,qc.beginContract.after(begin).and(qc.expireContract.isNull())
								,qc.beginContract.after(begin).and(qc.expireContract.after(end)))).orderBy(qc.beginContract.asc());
								
						
		return query.list(qc);
	}

	/**
	 * Ritorna la lista dei person day della persona nella finestra temporale specificata
	 * ordinati per data con ordinimento crescente.
	 * @param person
	 * @param interval
	 * @param onlyWithMealTicket 
	 * @return
	 */
	public static List<PersonDay> getPersonDayIntoInterval(
			Person person, DateInterval interval, boolean onlyWithMealTicket) {
		
		final QPersonDay qpd = QPersonDay.personDay;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(qpd)
				.orderBy(qpd.date.asc());

		final BooleanBuilder condition = new BooleanBuilder();
		condition.and(qpd.person.eq(person)
				.and(qpd.date.goe(interval.getBegin()))
				.and(qpd.date.loe(interval.getEnd())));
		
		if(onlyWithMealTicket) {
			condition.and(qpd.isTicketAvailable.eq(true));
		}
		query.where(condition);
		
		return query.list(qpd);
	}

	/**
	 * 
	 * @param personId
	 * @return la persona corrispondente all'id passato come parametro
	 */
	public static Person getPersonById(Long personId) {
		QPerson person = QPerson.person;
		final JPQLQuery query = ModelQuery.queryFactory().from(person).where(person.id.eq(personId));
		
		return query.singleResult(person);
		
	
	}
	
	/**
	 * 
	 * @param number
	 * @return la persona corrispondente alla matricola passata come parametro
	 */
	public static Person getPersonByNumber(Integer number){
		QPerson person = QPerson.person;
		final JPQLQuery query = ModelQuery.queryFactory().from(person).where(person.number.eq(number));
		
		return query.singleResult(person);
		
	}
	
	/**
	 * 
	 * @return la lista di persone che hanno una matricola associata
	 */
	public static List<Person> getPersonsByNumber(){
		QPerson person = QPerson.person;
		final JPQLQuery query = ModelQuery.queryFactory().from(person)
				.where(person.number.isNotNull().and(person.number.ne(0)));
		query.orderBy(person.number.asc());
		return query.list(person);
	}
	
	/**
	 * 
	 * @param email
	 * @return la persona che ha associata la mail email
	 */
	public static Person getPersonByEmail(String email){
		QPerson person = QPerson.person;
		final JPQLQuery query = ModelQuery.queryFactory().from(person).where(person.email.eq(email));
		
		return query.singleResult(person);
	}
	
	
}
