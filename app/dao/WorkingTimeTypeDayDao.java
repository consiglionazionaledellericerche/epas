package dao;

import helpers.ModelQuery;
import models.Person;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.query.QWorkingTimeTypeDay;

import org.joda.time.LocalDate;

import com.mysema.query.jpa.JPQLQuery;

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
	private static WorkingTimeTypeDay getWorkingTimeTypeDayByDayOfWeek(WorkingTimeType wtt, Integer dayOfWeek){
		
		QWorkingTimeTypeDay wttd = QWorkingTimeTypeDay.workingTimeTypeDay;
		
		final JPQLQuery query = ModelQuery.queryFactory().from(wttd)
				.where(wttd.workingTimeType.eq(wtt).and(wttd.dayOfWeek.eq(dayOfWeek)));
		
		return query.singleResult(wttd);
	}
	
	public static WorkingTimeTypeDay getWorkingTimeTypeDay(Person person, LocalDate date) {
		
		//Prendo il WorkingTimeType
		WorkingTimeType wtt = WorkingTimeTypeDao.getWorkingTimeTypeStatic(date, person);
		
		if(wtt == null)	//FIXME optional
			return null;
		
		return getWorkingTimeTypeDayByDayOfWeek(wtt, date.getDayOfWeek());
	}
}
