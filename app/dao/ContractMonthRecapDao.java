package dao;

import java.util.List;

import javax.persistence.EntityManager;

import models.ContractMonthRecap;
import models.query.QContractMonthRecap;

import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

/**
 * DAO per i riepiloghi mensili.
 * 
 * - situazione residuale minuti anno passato e anno corrente
 * - situazione residuale buoni pasto
 * 
 * - situazione residuale ferie (solo per il mese di dicembre)
 * 
 * @author alessandro
 *
 */
public class ContractMonthRecapDao extends DaoBase {

	@Inject
	ContractMonthRecapDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}
	
	/**
	 * I riepiloghi delle persone con un massimo di buoni pasto passato come 
	 * parametro.
	 * 
	 * TODO: il filtro sugli office delle persone.
	 * 
	 * @param yearMonth
	 * @param max
	 * @return
	 */
	public List<ContractMonthRecap> getPersonMealticket(YearMonth yearMonth,
			Optional<Integer> max) {
		
		final QContractMonthRecap recap = QContractMonthRecap.contractMonthRecap;
		
		final BooleanBuilder condition = new BooleanBuilder();
		if (max.isPresent()) {
			condition.and( recap.remainingMealTickets.loe(max.get()) );
		}
		
		final JPQLQuery query = getQueryFactory().from(recap)
				.where(recap.year.eq(yearMonth.getYear())
				.and(recap.month.eq(yearMonth.getMonthOfYear())
				.and(condition)) );
						
		return query.list(recap);
	}
}
