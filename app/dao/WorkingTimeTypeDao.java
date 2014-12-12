package dao;

import java.util.List;

import helpers.ModelQuery;

import com.mysema.query.jpa.JPQLQuery;

import models.WorkingTimeType;
import models.query.QWorkingTimeType;

public class WorkingTimeTypeDao {

	/**
	 * 
	 * @param description
	 * @return il workingTimeType relativo alla descrizione passata come parametro
	 */
	public static WorkingTimeType getWorkingTimeTypeByDescription(String description){
		QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
		final JPQLQuery query = ModelQuery.queryFactory().from(wtt)
				.where(wtt.description.eq(description));
		return query.singleResult(wtt);
	}
	
	
	/**
	 * 
	 * @return la lista di tutti gli workingTimeType presenti nel database
	 */
	public static List<WorkingTimeType> getAllWorkingTimeType(){
		QWorkingTimeType wtt = QWorkingTimeType.workingTimeType;
		final JPQLQuery query = ModelQuery.queryFactory().from(wtt);
		return query.list(wtt);
	}
}
