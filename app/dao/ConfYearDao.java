package dao;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;

import java.util.List;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import models.ConfYear;
import models.Office;
import models.query.QConfYear;

public class ConfYearDao {

	/**
	 * 
	 * @param office
	 * @param year
	 * @return la lista dei conf year relativi a un certo ufficio in un certo anno
	 */
	public static List<ConfYear> getConfByYear(Optional<Office> office, Integer year){
		
		final BooleanBuilder condition = new BooleanBuilder();
		QConfYear confYear = QConfYear.confYear;
		
		if(office.isPresent()){
			condition.and(confYear.office.eq(office.get()));
		}
		condition.and(confYear.year.eq(year));
		return  ModelQuery.queryFactory().from(confYear).where(condition).list(confYear);
		 
				
	}
	
	/**
	 * 
	 * @param office
	 * @param year
	 * @param field
	 * @return il conf year di un certo ufficio in un certo anno rispondente al parametro field
	 */
	public static ConfYear getConfYearField(Optional<Office> office, Integer year, String field){
		final BooleanBuilder condition = new BooleanBuilder();
		QConfYear confYear = QConfYear.confYear;
		final JPQLQuery query = ModelQuery.queryFactory().from(confYear);
		if(office.isPresent()){
			condition.and(confYear.office.eq(office.get()));
		}
		condition.and(confYear.year.eq(year));
		condition.and(confYear.field.eq(field));
		query.where(condition);
		return query.singleResult(confYear);
	}
}
