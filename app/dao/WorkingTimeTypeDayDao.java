package dao;

import helpers.ModelQuery;

import com.mysema.query.jpa.JPQLQuery;

import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.query.QWorkingTimeTypeDay;

/**
 * 
 * @author dario
 *
 */
public class WorkingTimeTypeDayDao {

	
	/**
	 * 
	 * @param wtt
	 * @param dayOfWeek
	 * @return il workingTimeTypeDay relativo al workingTimeType e al giorno passati come parametro
	 */
	public static WorkingTimeTypeDay getWorkingTimeTypeDayByDayOfWeek(WorkingTimeType wtt, Integer dayOfWeek){
		QWorkingTimeTypeDay wttd = QWorkingTimeTypeDay.workingTimeTypeDay;
		final JPQLQuery query = ModelQuery.queryFactory().from(wttd)
				.where(wttd.workingTimeType.eq(wtt).and(wttd.dayOfWeek.eq(dayOfWeek)));
		return query.singleResult(wttd);
	}
}
