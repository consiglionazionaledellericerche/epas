package dao;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;

import java.util.Set;

import models.CompetenceCode;
import models.Office;
import models.Person;
import models.query.QCompetenceCode;
import models.query.QContract;

import models.query.QPerson;
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
			boolean onlyTechnician, LocalDate start, LocalDate end) {
		
		Preconditions.checkState(!offices.isEmpty());
		
		final QPerson qp = QPerson.person;
		final QContract qc = QContract.contract;
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
}
