package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import models.Competence;
import models.CompetenceCode;
import models.Office;
import models.Person;
import models.PersonHourForOvertime;
import models.PersonReperibilityType;
import models.TotalOvertime;
import models.query.QCompetence;
import models.query.QCompetenceCode;
import models.query.QPerson;
import models.query.QPersonHourForOvertime;
import models.query.QPersonReperibilityType;
import models.query.QTotalOvertime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * 
 * @author dario
 *
 */
public class CompetenceDao extends DaoBase{

	private final static Logger log = LoggerFactory.getLogger(CompetenceDao.class);
	
	@Inject
	CompetenceDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp
			, IWrapperFactory wrapperFactory) {
		super(queryFactory, emp);
	}
	
	/**
	 * 
	 * @param id
	 * @return la competenza relativa all'id passato come parametro
	 */
	public Competence getCompetenceById(Long id){
		
		final QCompetence competence = QCompetence.competence;
		
		return getQueryFactory().from(competence)
				.where(competence.id.eq(id)).singleResult(competence);		
	}
	
	/**
	 * La lista dei CompetenceCode abilitati ad almeno una persona appartenente
	 * all'office.
	 * @param office
	 * @return
	 */
	public List<CompetenceCode> activeCompetenceCode(Office office) {
		
		final QCompetenceCode competenceCode = QCompetenceCode.competenceCode;
		
		return getQueryFactory().from(competenceCode)
				.where(competenceCode.persons.any().office.eq(office))
				.distinct().list(competenceCode);
	}
	
	/**
	 * @param person
	 * @param year
	 * @param month
	 * @param code
	 * @return 
	 */
	public List<Competence> getCompetences(Person person, Integer year,
			Integer month, List<CompetenceCode> codes){
		
		final QCompetence competence = QCompetence.competence;
		
		final JPQLQuery query = getQueryFactory().from(competence)
				.leftJoin(competence.competenceCode).fetch()
				.where(competence.person.eq(person)
				.and(competence.year.eq(year)
				.and(competence.month.eq(month)
				.and(competence.competenceCode.in(codes)))));
		
		return query.list(competence);
	}
	
	/**
	 * @param person
	 * @param year
	 * @param month
	 * @param code
	 * @return 
	 */
	public Optional<Competence> getCompetence(Person person, Integer year,
			Integer month, CompetenceCode code){
		
		final QCompetence competence = QCompetence.competence;
		
		final JPQLQuery query = getQueryFactory().from(competence)
				.where(competence.person.eq(person)
				.and(competence.year.eq(year)
				.and(competence.month.eq(month)
				.and(competence.competenceCode.eq(code)))));
		
		return Optional.fromNullable(query.singleResult(competence));
		
	}
	
	/**
	 * La lista delle competence assegnate nell'office.
	 * 
	 * Se untilThisMonth è true, viene presa la lista delle competenze 
	 * dall'inizio dell'anno fino a quel mese compreso, se è false
	 * solo quelle del mese specificato
	 * 
	 * @param year
	 * @param month
	 * @param code filtra i codici di competenza 
	 * @param office filtra per persone dell'office
	 * @param untilThisMonth 
	 * @return
	 * 
	 */
	public List<Competence> getCompetencesInOffice(Integer year, Integer month, 
			List<String> codes, Office office, boolean untilThisMonth){
		
		final QCompetence competence = QCompetence.competence;
		final BooleanBuilder condition = new BooleanBuilder();
		
		condition.and(competence.year.eq(year))
				 .and(competence.competenceCode.code.in(codes))
				 .and(competence.person.office.eq(office));
		
		if(untilThisMonth)
			condition.and(competence.month.loe(month));
		else
			condition.and(competence.month.eq(month));
		
		return getQueryFactory().from(competence)
				.where(condition).list(competence);
	}

	/**
	 * Le competenze nell'anno year. Se office è present filtra sulle sole competenze
	 * assegnate alle persone nell'office.
	 * 
	 * @param year
	 * @param office
	 * @return
	 */
	public List<Competence> getCompetenceInYear(Integer year, Optional<Office> office){
		
		final QCompetence competence = QCompetence.competence;
		final BooleanBuilder condition = new BooleanBuilder();
		
		condition.and(competence.year.eq(year));
		
		if(office.isPresent())
			condition.and(competence.person.office.eq(office.get()));
		
		return getQueryFactory().from(competence)
				.where(condition).orderBy(competence.competenceCode.code.asc())
				.list(competence);
	}
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @param person
	 * @return sulla base dei parametri passati alla funzione ritorna la quantità di ore approvate di straordinario
	 * (sommando i codici S1 S2 e S3) 
	 */
	public Optional<Integer> valueOvertimeApprovedByMonthAndYear(Integer year, Optional<Integer> month, Optional<Person> person, 
			List<CompetenceCode> codeList){
		
		final QCompetence competence = QCompetence.competence;
		final BooleanBuilder condition = new BooleanBuilder();
		
		if(month.isPresent())
			condition.and(competence.month.eq(month.get()));
		if(person.isPresent())
			condition.and(competence.person.eq(person.get()));
		final JPQLQuery query = getQueryFactory().from(competence)
				.where(condition.and(competence.year.eq(year).and(competence.competenceCode.in(codeList))));
		
		return Optional.fromNullable(query.singleResult(competence.valueApproved.sum()));
		
	}
	

	
	/**
	 * 
	 * @param person
	 * @param year
	 * @param month
	 * @return la lista di tutte le competenze di una persona nel mese month e nell'anno year che abbiano un valore approvato > 0
	 */
	public List<Competence> getAllCompetenceForPerson(Person person, Integer year, Integer month){
		return competenceInMonth(person, year, month, Optional.<List<String>>absent());
	}

	
	public List<Competence> competenceInMonth(Person person, Integer year, Integer month,Optional<List<String>> codes){
		
		final QCompetence competence = QCompetence.competence;
		final BooleanBuilder condition = new BooleanBuilder();
		
		condition.and(competence.year.eq(year))
				 .and(competence.person.eq(person))
			     .and(competence.month.eq(month).and(competence.valueApproved.gt(0)));
		
		if(codes.isPresent()){
			condition.and(competence.competenceCode.code.in(codes.get()));
		}
		
		return getQueryFactory().from(competence)
				.where(condition).list(competence);
	}
	
	/**
	 * metodo di utilità per il controller UploadSituation
	 * @return la lista delle competenze del dipendente in questione per quel mese in quell'anno
	 */
	public List<Competence> getCompetenceInMonthForUploadSituation(Person person, Integer year, Integer month){
		List<Competence> competenceList = getAllCompetenceForPerson(person, year, month);
		
		log.trace("Per la persona {} trovate {} competenze approvate nei mesi di {}/{}", 
				new Object[]{person.getFullname(),competenceList.size(),month,year});
		
		return competenceList;
	}
	
	/**
	 * 
	 * @param type
	 * @param year
	 * @param month
	 * @param code
	 * @return la lista di competenze relative all'anno year, al mese month e al codice code di persone che hanno reperibilità 
	 * di tipo type associata
	 */
	public List<Competence> getCompetenceInReperibility(PersonReperibilityType type, int year, int month, CompetenceCode code){
	       final QCompetence competence = QCompetence.competence;
	       final QPerson person = QPerson.person;
	       final QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;
	       
	       JPQLQuery query = getQueryFactory().from(competence)
	               .leftJoin(competence.person, person)
	               .leftJoin(person.reperibility.personReperibilityType, prt)
					.where(prt.eq(type)
					.and(competence.year.eq(year)
					.and(competence.month.eq(month)
					.and(competence.competenceCode.eq(code)))))
					.orderBy(competence.person.surname.asc());

	       return query.list(competence);
	   }

	
	/* 
	 * @param person 
	 * @param year
	 * @param month
	 * @param competenceCode
	 * @return l'ultima competenza assegnata din un certo typo in un determinato anno
	 */
	public Competence getLastPersonCompetenceInYear(Person person, int year, int month, CompetenceCode competenceCode) {
		final QCompetence com = new QCompetence("competence");
		final JPQLQuery query = getQueryFactory().query();
		final Competence myCompetence = query
				.from(com)
				.where(
						com.person.eq(person)
						.and(com.year.eq(year))
						.and(com.month.lt(month))
						.and(com.competenceCode.eq(competenceCode))		
						)
						.orderBy(com.month.desc())
						.limit(1)
						.uniqueResult(com);
		
		return myCompetence;
	}
	

	/**
	 * @param year
	 * @param office
	 * @return dei quantitativi di straordinario assegnati per l'ufficio office nell'anno year
	 */
	public List<TotalOvertime> getTotalOvertime(Integer year, Office office){
		final QTotalOvertime totalOvertime = QTotalOvertime.totalOvertime;
		
		return getQueryFactory().from(totalOvertime)
				.where(totalOvertime.year.eq(year).and(totalOvertime.office.eq(office)))
				.list(totalOvertime);
	}
	
	/**
	 * 
	 * @param person
	 * @return il personHourForOvertime relativo alla persona person passata come parametro
	 */
	public PersonHourForOvertime getPersonHourForOvertime(Person person){
		
		final QPersonHourForOvertime personHourForOvertime = QPersonHourForOvertime.personHourForOvertime;
		
		return getQueryFactory().from(personHourForOvertime)
				.where(personHourForOvertime.person.eq(person))
				.singleResult(personHourForOvertime);
	}
	
}
