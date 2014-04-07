package dao;

import java.util.List;

import helpers.ModelQuery;
import models.Person;
import models.query.QContract;
import models.query.QPerson;

import org.joda.time.LocalDate;

import com.google.common.base.Strings;
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
	
	public List<Person> list(String name) {
		final QPerson qp = QPerson.person;
		final QContract qc = QContract.contract;
		final LocalDate start = new LocalDate();
		final LocalDate end = start;
		
		final JPQLQuery query = ModelQuery.queryFactory().from(qp)
				.join(qp.contracts, qc)
				.orderBy(qp.surname.asc(), qp.name.asc());
		
		final BooleanBuilder condition = new BooleanBuilder();
		if (!Strings.isNullOrEmpty(name)) {
			condition.and(qp.name.startsWithIgnoreCase(name)
					.or(qp.surname.startsWithIgnoreCase(name)));
		}
		condition.and(qc.onCertificate.isTrue());
		condition.and(qc.beginContract.before(end));
		condition.and(qc.endContract.isNull().and(qc.expireContract.isNull())
				.or(qc.expireContract.isNotNull().and(qc.expireContract.goe(start)))
				.or(qc.endContract.isNotNull().and(qc.endContract.goe(start))));
		return query.list(qp);
	}
}
