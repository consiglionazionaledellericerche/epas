package dao;

import helpers.ModelQuery;

import java.util.List;

import models.Contract;
import models.VacationPeriod;
import models.query.QVacationPeriod;

import com.mysema.query.jpa.JPQLQuery;

/**
 * 
 * @author dario
 *
 */
public class VacationPeriodDao {

	/**
	 * 
	 * @param contract
	 * @return la lista dei vacationPeriod associati al contratto passato come parametro
	 */
	public static List<VacationPeriod> getVacationPeriodByContract(Contract contract){
		QVacationPeriod vacationPeriod = QVacationPeriod.vacationPeriod;
		final JPQLQuery query = ModelQuery.queryFactory().from(vacationPeriod)
				.where(vacationPeriod.contract.eq(contract));
		return query.orderBy(vacationPeriod.beginFrom.asc()).list(vacationPeriod);
	}
}
