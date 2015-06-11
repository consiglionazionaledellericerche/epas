package dao;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;
import it.cnr.iit.epas.DateInterval;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

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
import models.query.QPersonShiftShiftType;
import models.query.QUser;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.types.Projections;

/**
 * DAO per le person.
 *
 * @author marco
 *
 */
public final class PersonDao extends DaoBase{
	
	@Inject
	PersonDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	/**
	 * Modella il Dto contenente le sole informazioni della persona
	 * richieste dalla select nel template menu.
	 * 
	 * @author alessandro
	 *
	 */
	public class PersonLiteDto {
		
		public Long id;
		public String name;
		public String surname;

		public PersonLiteDto(Long id, String name, String surname) {
			this.id = id;
			this.name = name;
			this.surname = surname;
		}
	}

	private final static QPerson person = QPerson.person;
	private final static QContract contract = QContract.contract;
	
	@Inject
	public OfficeDao officeDao;

	/**
	 * La query effettiva per la ricerca delle persone.
	 * FIXME Renderla parametrica ed applicarla all'interno di:
	 *  PersonDao.listForCompetence
	 *  PersonDao.list
	 *  PersonDao.liteList
	 * 
	 * FIXME Sistemare JPA adesso effettua una successiva query per ogni persona trovata.
	 * 
	 * @param name
	 * @param offices
	 * @param onlyTechnician
	 * @param start
	 * @param end
	 * @param onlyOnCertificate
	 * @return
	 */
	private JPQLQuery queryList(Optional<String> name, Set<Office> offices,
			boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {
		
		 final JPQLQuery query = getQueryFactory().from(person)
					.leftJoin(person.contracts, contract)
					.orderBy(person.surname.asc(), person.name.asc())
					
					.distinct();


		final BooleanBuilder condition = new BooleanBuilder();
		condition.and(person.office.in(offices));

		if (onlyTechnician) {
			// i livelli sopra al 3 sono dei tecnici:
			condition.and(person.qualification.qualification.gt(3));
		}

		if (name.isPresent() && !name.get().trim().isEmpty()) {
			condition.andAnyOf(person.name.startsWithIgnoreCase(name.get()),
					person.surname.startsWithIgnoreCase(name.get()));
		}

		if(onlyOnCertificate)
			condition.and(contract.onCertificate.isTrue());
		
		if(start != null && end!= null){
		condition.andAnyOf(

				//contratto terminato
				contract.endContract.isNotNull().and(contract.beginContract.loe(end)).and(contract.endContract.goe(start)),

				//contratto non terminato
				contract.endContract.isNull().and(

						//contratto tempo indeterminato
						contract.expireContract.isNull().and(contract.beginContract.loe(end))

						.or(

						//contratto tempo determinato
						contract.expireContract.isNotNull().and(contract.beginContract.loe(end)).and(contract.expireContract.goe(start))

						)
					)
				);
		}
		
		return query.where(condition);
		
	}
	
	/**
	 * Genera la lista di PersonLite contenente le persone attive nel mese specificato
	 * appartenenti ad un office in offices.
	 * 
	 * @param offices
	 * @param year
	 * @param month
	 * @return
	 */
	public List<PersonLiteDto> liteList(Set<Office> offices, int year, int month) {
		
		final QPerson person = QPerson.person;
		
		LocalDate beginMonth = new LocalDate(year,month,1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		
		return queryList(Optional.<String>absent(), offices, false, beginMonth, endMonth, true)
				.list((Projections.bean(PersonLiteDto.class, person.id, person.name, person.surname)));
	}
	
	/**
	 * La lista di persone una volta applicati i filtri dei parametri. 
	 * 
	 * @param name
	 * @param offices
	 * @param onlyTechnician
	 * @param start
	 * @param end
	 * @param onlyOnCertificate
	 * @return
	 */
	public SimpleResults<Person> list(Optional<String> name, Set<Office> offices,
			boolean onlyTechnician, LocalDate start, LocalDate end, boolean onlyOnCertificate) {
		
		return ModelQuery.simpleResults(queryList(name, offices, onlyTechnician, start, end, onlyOnCertificate), 
				person);
	}

	/**
	 * La lista delle persone abilitate alla competenza compCode.
	 * E che superano i filtri dei parametri.
	 * 
	 * @param compCode
	 * @param name
	 * @param offices
	 * @param onlyTechnician
	 * @param start
	 * @param end
	 * @return
	 */
	public SimpleResults<Person> listForCompetence(CompetenceCode compCode, Optional<String> name, Set<Office> offices,
			boolean onlyTechnician, LocalDate start, LocalDate end) {

		Preconditions.checkState(!offices.isEmpty());

		final QCompetenceCode qcc = QCompetenceCode.competenceCode;
		// TODO: completare con l'intervallo
		//final LocalDate start = new LocalDate();
		//final LocalDate end = start;

		final JPQLQuery query = getQueryFactory().from(person)
				.leftJoin(person.contracts, contract)
				.leftJoin(person.personHourForOvertime, QPersonHourForOvertime.personHourForOvertime).fetch()
				//.leftJoin(qp.location, QLocation.location)
				.leftJoin(person.reperibility, QPersonReperibility.personReperibility).fetch()
				.leftJoin(person.personShift, QPersonShift.personShift).fetch()
				.leftJoin(person.user, QUser.user)
				.leftJoin(person.competenceCode, qcc)
				.orderBy(person.surname.asc(), person.name.asc())
				.distinct();



		final BooleanBuilder condition = new BooleanBuilder();
		condition.and(person.office.in(offices));

		if (onlyTechnician) {
			// i livelli sopra al 3 sono dei tecnici:
			condition.and(person.qualification.qualification.gt(3));
		}

		if (name.isPresent() && !name.get().trim().isEmpty()) {
			condition.andAnyOf(person.name.startsWithIgnoreCase(name.get()),
					person.surname.startsWithIgnoreCase(name.get()));
		}
		condition.and(person.competenceCode.contains(compCode));
		condition.and(contract.onCertificate.isTrue());
		condition.and(contract.beginContract.before(end));
		condition.andAnyOf(contract.endContract.isNull().and(contract.expireContract.isNull()),
				contract.expireContract.isNotNull().and(contract.expireContract.goe(start)),
				contract.endContract.isNotNull().and(contract.endContract.goe(start)));

		query.where(condition);

		return ModelQuery.simpleResults(query, person);
	}

	/**
	 * L'ultimo contratto inserito in ordine di data inizio.
	 * (Tendenzialmente quello attuale)
	 * @param person
	 * @return
	 */
	public Optional<Contract> getLastContract(Person person) {

		final JPQLQuery query = getQueryFactory()
				.from(contract)
				.where(contract.person.eq(person))
				.orderBy(contract.beginContract.desc());

		List<Contract> contracts = query.list(contract);
		if(contracts.size() == 0) {
			return Optional.<Contract>absent();
		}
		return Optional.fromNullable(contracts.get(0));

	}

	/**
	 * Il contratto precedente in ordine temporale rispetto a quello passato
	 * come argomento.
	 * @param c
	 * @return
	 */
	public Contract getPreviousPersonContract(Contract c) {

		final JPQLQuery query = getQueryFactory()
				.from(contract)
				.where(contract.person.eq(c.person))
				.orderBy(contract.beginContract.desc());

		List<Contract> contracts = query.list(contract);

		final int indexOf = contracts.indexOf(c);
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
	public List<Contract> getContractList(Person person,LocalDate fromDate,LocalDate toDate){

		BooleanBuilder conditions = new BooleanBuilder(contract.person.eq(person).and(contract.beginContract.loe(toDate)));

		conditions.andAnyOf(
				contract.endContract.isNull().and(contract.expireContract.isNull()),
				contract.endContract.isNull().and(contract.expireContract.goe(fromDate)),
				contract.endContract.isNotNull().and(contract.endContract.goe(fromDate))
				);

		return getQueryFactory().from(contract).where(conditions).orderBy(contract.beginContract.asc()).list(contract);
	}

	/**
	 * Ritorna la lista dei person day della persona nella finestra temporale specificata
	 * ordinati per data con ordinimento crescente.
	 * @param person
	 * @param interval
	 * @param onlyWithMealTicket
	 * @return
	 */
	public List<PersonDay> getPersonDayIntoInterval(
			Person person, DateInterval interval, boolean onlyWithMealTicket) {

		final QPersonDay qpd = QPersonDay.personDay;

		final JPQLQuery query = getQueryFactory()
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
	public Person getPersonById(Long personId) {

		final JPQLQuery query = getQueryFactory().from(person).where(person.id.eq(personId));

		return query.singleResult(person);


	}

	/**
	 *
	 * @param number
	 * @return la persona corrispondente alla matricola passata come parametro
	 */
	public Person getPersonByNumber(Integer number){

		final JPQLQuery query = getQueryFactory().from(person).where(person.number.eq(number));

		return query.singleResult(person);

	}

	/**
	 *
	 * @return la lista di persone che hanno una matricola associata
	 */
	public List<Person> getPersonsByNumber(){

		final JPQLQuery query = getQueryFactory().from(person)
				.where(person.number.isNotNull().and(person.number.ne(0)));
		query.orderBy(person.number.asc());
		return query.list(person);
	}

	/**
	 *
	 * @param email
	 * @return la persona che ha associata la mail email
	 */
	public Optional<Person> byEmail(String email){

		final JPQLQuery query = getQueryFactory().from(person)
				.where(person.email.eq(email).or(person.cnr_email.eq(email)));

		return Optional.fromNullable(query.singleResult(person));
	}

	/**
	 * 
	 * @param perseoId
	 * @return la persona identificata dall'id con cui è salvata sul db di perseo
	 */
	public Person byPerseoId(Integer perseoId){
		final JPQLQuery query = getQueryFactory().from(person)
				.where(person.iId.eq(perseoId));
			
		return query.singleResult(person);
	}
	/**
	 *
	 * @param oldId
	 * @return la persona associata al vecchio id (se presente in anagrafica) passato come parametro
	 */
	public Person getPersonByOldID(Long oldId){

		final JPQLQuery query = getQueryFactory().from(person).where(person.oldId.eq(oldId));

		return query.singleResult(person);
	}

	/**
	 *
	 * @param badgeNumber
	 * @return la persona associata al badgeNumber passato come parametro
	 */
	public Person getPersonByBadgeNumber(String badgeNumber){

		final JPQLQuery query = getQueryFactory().from(person).where(person.badgeNumber.eq(badgeNumber));

		return query.singleResult(person);
	}

	/**
	 * 
	 * @param type
	 * @return la lista di persone in reperibilità con tipo type 
	 */
	public List<Person> getPersonForReperibility(Long type){
		final JPQLQuery query = getQueryFactory().from(person)
				.where(person.reperibility.personReperibilityType.id.eq(type).and(person.reperibility.startDate.isNull().or(person.reperibility.startDate.loe(LocalDate.now())
						.and(person.reperibility.endDate.isNull().or(person.reperibility.endDate.goe(LocalDate.now()))))));
		return query.list(person);
		
	}

	/**
	 * 
	 * @param type
	 * @return la lista di persone che hanno come tipo turno quello passato come parametro
	 */
	public List<Person> getPersonForShift(String type){
		QPersonShiftShiftType psst = QPersonShiftShiftType.personShiftShiftType;
		QPersonShift ps = QPersonShift.personShift;
		final JPQLQuery query = getQueryFactory().from(person)
				.leftJoin(person.personShift, ps)
				.leftJoin(ps.personShiftShiftTypes, psst).where(psst.shiftType.type.eq(type)
						.and(psst.beginDate.isNull().or(psst.beginDate.loe(LocalDate.now()))
								.and(psst.endDate.isNull().or(psst.endDate.goe(LocalDate.now())))));
		return query.list(person);
	}
	
	/**
	 * 
	 * @return quante sono le persone in anagrafica che hanno valorizzato il campo
	 * email_cnr, campo utile per poter fare la sincronizzazione con gli altri sistemi
	 */
	public long checkCnrEmailForEmployee(){
		final QPerson person = QPerson.person;
		final JPQLQuery query = getQueryFactory().from(person).where(person.cnr_email.isNotNull());
		return query.count();
	}

}
