package dao;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import models.ConfYear;
import models.Office;
import models.query.QConfYear;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;


/**
 * 
 * @author dario
 *
 */

public class ConfYearDao extends DaoBase{

	@Inject
	ConfYearDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	/**
	 * 
	 * @param id
	 * @return la confYear relativa all'id passato come parametro
	 */
	public ConfYear getById(Long id){
		QConfYear confYear = QConfYear.confYear;
		final JPQLQuery query = getQueryFactory().from(confYear)
				.where(confYear.id.eq(id));
		return query.singleResult(confYear);
	}

	/**
	 * 
	 * @param office
	 * @param year
	 * @return la lista dei conf year relativi a un certo ufficio in un certo anno
	 */
	public List<ConfYear> getOfficeConfByYear(Optional<Office> office, Integer year){

		final BooleanBuilder condition = new BooleanBuilder();
		QConfYear confYear = QConfYear.confYear;

		if(office.isPresent()){
			condition.and(confYear.office.eq(office.get()));
		}
		condition.and(confYear.year.eq(year));
		return  getQueryFactory().from(confYear).where(condition).list(confYear);

	}

	/**
	 * 
	 * @param office
	 * @param year
	 * @param field
	 * @return il conf year di un certo ufficio in un certo anno rispondente al parametro field
	 */
	public Optional<ConfYear> getByFieldName(String field, Integer year, Office office) {

		final BooleanBuilder condition = new BooleanBuilder();

		QConfYear confYear = QConfYear.confYear;
		final JPQLQuery query = getQueryFactory().from(confYear);
		condition.and(confYear.year.eq(year));
		condition.and(confYear.field.eq(field));
		query.where(condition);

		return Optional.fromNullable(query.singleResult(confYear));
	}
}
