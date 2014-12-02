package dao;

import helpers.ModelQuery;

import java.util.List;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import models.ConfYear;
import models.Office;
import models.query.QConfYear;

public class ConfYearDao {

	public static List<ConfYear> getConfByYear(Optional<Office> office, Integer year){
		
		final BooleanBuilder condition = new BooleanBuilder();
		QConfYear confYear = QConfYear.confYear;
		
		if(office.isPresent()){
			condition.and(confYear.office.eq(office.get()));
		}
		condition.and(confYear.year.eq(year));
		return  ModelQuery.queryFactory().from(confYear).where(condition).list(confYear);
		 
				
	}
}
