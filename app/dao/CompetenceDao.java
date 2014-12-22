package dao;

import java.util.List;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;
import models.Competence;
import models.CompetenceCode;
import models.Office;
import models.Person;
import models.PersonHourForOvertime;
import models.TotalOvertime;
import models.query.QCompetence;
import models.query.QPersonHourForOvertime;
import models.query.QTotalOvertime;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

/**
 * 
 * @author dario
 *
 */
public class CompetenceDao {

	/**
	 * 
	 * @param id
	 * @return la competenza relativa all'id passato come parametro
	 */
	public static Competence getCompetenceById(Long id){
		QCompetence competence = QCompetence.competence;
		final JPQLQuery query = ModelQuery.queryFactory().from(competence)
				.where(competence.id.eq(id));
		
		return query.singleResult(competence);
		
	}
	
	/**
	 * 
	 * @param person
	 * @param code
	 * @param year
	 * @param month
	 * @return
	 */
	public static SimpleResults<Competence> list(
			Optional<Person> person, Optional<String> code, 
			Optional<Integer> year, Optional<Integer> month) {
		
		QCompetence competence = QCompetence.competence;
		final BooleanBuilder condition = new BooleanBuilder();
		if (person.isPresent()) {
			condition.and(competence.person.eq(person.get()));
		}
		if (code.isPresent()) {
			condition.and(competence.competenceCode.code.eq(code.get()));
		}
		if (year.isPresent()) {
			condition.and(competence.year.eq(year.get()));
		}
		if (month.isPresent()) {
			condition.and(competence.month.eq(month.get()));
		}
		final JPQLQuery query = ModelQuery.queryFactory().from(competence).where(condition);
		return ModelQuery.simpleResults(query, competence);
	}
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @param person
	 * @return sulla base dei parametri passati alla funzione ritorna la quantità di ore approvate di straordinario
	 * (sommando i codici S1 S2 e S3) 
	 */
	public static Integer valueOvertimeApprovedByMonthAndYear(Integer year, Optional<Integer> month, Optional<Person> person, 
			List<CompetenceCode> codeList){
		QCompetence competence = QCompetence.competence;
		final BooleanBuilder condition = new BooleanBuilder();
		if(month.isPresent())
			condition.and(competence.month.eq(month.get()));
		if(person.isPresent())
			condition.and(competence.person.eq(person.get()));
		final JPQLQuery query = ModelQuery.queryFactory().from(competence)
				.where(condition.and(competence.year.eq(year).and(competence.competenceCode.in(codeList))));
		if(query.list(competence.valueApproved.sum()).get(0) != null)
			return query.list(competence.valueApproved.sum()).get(0);
		else 
			return 0;
	}
	
	/**
	 * 
	 * @param person
	 * @param year
	 * @param month
	 * @param code
	 * @return la competenza relativa ai parametri passati alla funzione
	 */
	public static Competence getCompetence(Person person, Integer year, Integer month, CompetenceCode code){
		QCompetence competence = QCompetence.competence;
		final JPQLQuery query = ModelQuery.queryFactory().from(competence)
				.where(competence.person.eq(person).
						and(competence.year.eq(year).and(competence.month.eq(month).and(competence.competenceCode.eq(code)))));
		
		return query.singleResult(competence);
		
	}
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @param code
	 * @param office
	 * @param untilThisMonth
	 * @return la lista delle competenze che hanno come validità l'anno year, che sono di persone che appartengono
	 * all'office office e che sono relative ai codici di competenza passati nella lista di stringhe code.
	 * Se il booleano untilThisMonth è true, viene presa la lista delle competenze dall'inizio dell'anno fino a quel mese compreso, se è false
	 * solo quelle del mese specificato
	 */
	public static List<Competence> getCompetences(Integer year, Integer month, List<String> code, Office office, boolean untilThisMonth){
		QCompetence competence = QCompetence.competence;
		final BooleanBuilder condition = new BooleanBuilder();
		if(untilThisMonth)
			condition.and(competence.month.loe(month));
		else
			condition.and(competence.month.eq(month));
		final JPQLQuery query = ModelQuery.queryFactory().from(competence)
				.where(condition.and(competence.year.eq(year)
						.and(competence.competenceCode.code.in(code)
								.and(competence.person.office.eq(office)))));
		return query.list(competence);
	}
	
	
	/**
	 * 
	 * @param year
	 * @return la lista delle competenze presenti nell'anno
	 */
	public static List<Competence> getCompetenceInYear(Integer year){
		QCompetence competence = QCompetence.competence;
		JPQLQuery query = ModelQuery.queryFactory().from(competence)
				.where(competence.year.eq(year));
		query.orderBy(competence.competenceCode.code.asc());
		return query.list(competence);
	}
	
	/*********************************************************************************************************************************/
	/*Parte relativa a query su TotalOvertime per la quale, essendo unica, non si è deciso di creare un Dao ad hoc*/
	
	/**
	 * 
	 * @param year
	 * @param office
	 * @return dei quantitativi di straordinario assegnati per l'ufficio office nell'anno year
	 */
	public static List<TotalOvertime> getTotalOvertime(Integer year, Office office){
		QTotalOvertime totalOvertime = QTotalOvertime.totalOvertime;
		final JPQLQuery query = ModelQuery.queryFactory().from(totalOvertime)
				.where(totalOvertime.year.eq(year).and(totalOvertime.office.eq(office)));
		return query.list(totalOvertime);
	}
	
	/**********************************************************************************************************************************/
	/*Parte relativa a query su PersonHourForOvertime per la quale, essendo unica, non si è deciso di creare un Dao ad hoc*/
	
	/**
	 * 
	 * @param person
	 * @return il personHourForOvertime relativo alla persona person passata come parametro
	 */
	public static PersonHourForOvertime getPersonHourForOvertime(Person person){
		QPersonHourForOvertime personHourForOvertime = QPersonHourForOvertime.personHourForOvertime;
		final JPQLQuery query = ModelQuery.queryFactory().from(personHourForOvertime)
				.where(personHourForOvertime.person.eq(person));
		return query.singleResult(personHourForOvertime);
	}
	
}
