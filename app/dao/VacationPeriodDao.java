package dao;

import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import models.Contract;
import models.VacationPeriod;
import models.query.QVacationPeriod;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * 
 * @author dario
 *
 */
public class VacationPeriodDao extends DaoBase{
	
	@Inject
	VacationPeriodDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 * @param contract
	 * @return la lista dei vacationPeriod associati al contratto passato come parametro
	 */
	public List<VacationPeriod> getVacationPeriodByContract(Contract contract){
		final QVacationPeriod vacationPeriod = QVacationPeriod.vacationPeriod;
		final JPQLQuery query = getQueryFactory().from(vacationPeriod)
				.where(vacationPeriod.contract.eq(contract));
		return query.orderBy(vacationPeriod.beginFrom.asc()).list(vacationPeriod);
	}
}
