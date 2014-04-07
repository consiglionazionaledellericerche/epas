package dao;

import java.util.List;
import java.util.Set;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;
import models.Office;
import models.Person;
import models.query.QContract;
import models.query.QPerson;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.SearchResults;
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
			boolean onlyTechnician) {
		
		Preconditions.checkState(!offices.isEmpty());
		
		final QPerson qp = QPerson.person;
		final QContract qc = QContract.contract;
		// TODO: completare con l'intervallo
		final LocalDate start = new LocalDate();
		final LocalDate end = start;
		
		final JPQLQuery query = ModelQuery.queryFactory().from(qp)
				.join(qp.contracts, qc)
				.orderBy(qp.surname.asc(), qp.name.asc());
		
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
		condition.and(qc.beginContract.before(end));
		condition.andAnyOf(qc.endContract.isNull().and(qc.expireContract.isNull()),
				qc.expireContract.isNotNull().and(qc.expireContract.goe(start)),
				qc.endContract.isNotNull().and(qc.endContract.goe(start)));
		query.where(condition);
		
		return ModelQuery.simpleResults(query, qp);
	}
}
