package dao;

import helpers.ModelQuery;
import it.cnr.iit.epas.DateInterval;

import java.util.List;

import models.ConfGeneral;
import models.Contract;
import models.MealTicket;
import models.Office;
import models.enumerate.ConfigurationFields;
import models.query.QContract;
import models.query.QMealTicket;
import models.query.QPerson;

import org.joda.time.LocalDate;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

/**
 * DAO per i MealTicket.
 * 
 * @author alessandro
 *
 */
public class MealTicketDao {

	/**
	 * Ritorna la lista di mealTickt assegnati alla persona nella finestra temporale specificata.
	 * Ordinati per data di scadenza con ordinamento crescente e per codice blocco con ordinamento crescente.
	 * @param c
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 */
	public static List<MealTicket> getMealTicketAssignedToPersonIntoInterval(
			Contract c, DateInterval interval) {
		
		final QMealTicket mealTicket = QMealTicket.mealTicket;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(mealTicket)
				.where(mealTicket.contract.id.eq(c.id))
				.where(mealTicket.date.goe(interval.getBegin()))
				.where(mealTicket.date.loe(interval.getEnd()))
				.orderBy(mealTicket.expireDate.asc())
				.orderBy(mealTicket.block.asc());
		
		return query.list(mealTicket);
	}
	
	/**
	 * La scadenza massima precedentemente assegnata ai buoni pasto inseriti per le persone 
	 * appartenenti all'office passato come argomento.
	 * @param office
	 * @return
	 */
	public static LocalDate getFurtherExpireDateInOffice(Office office) {
		
		final QMealTicket qmt = QMealTicket.mealTicket;
		final QPerson qp = QPerson.person;
		final QContract qc = QContract.contract;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				
				.from(qmt)
				.leftJoin(qmt.contract, qc)
				.leftJoin(qc.person, qp)
				.where(qp.office.id.eq(office.id))
				.groupBy(qmt.expireDate)
				.orderBy(qmt.expireDate.desc());
		
		List<LocalDate> date = query.list(qmt.expireDate);
		
		if(date.size() == 0)
			return null;
		
		return date.get(0);
		
		//FIXME trovare un modo di selezionare solo il primo elemento
	}
	
	/**
	 * Ritorna la lista dei buoni pasto associati al contratto ordinata per data di consegna
	 * in ordine decrescente (da quelli consegnati per ultimi a quelli consegnati per primi).
	 * @param contract
	 * @return
	 */
	public static List<MealTicket> getOrderedMealTicketInContract(Contract contract) {
		final QMealTicket mealTicket = QMealTicket.mealTicket;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(mealTicket)
				.where(mealTicket.contract.eq(contract))
				.orderBy(mealTicket.date.desc());
		
		return query.list(mealTicket);
		
		
	}

	/**
	 * Ritorna la data di inizio di utilizzo dei ticket restaurant per l'office passato
	 * come parametro. 
	 * @param office
	 * @return
	 */
	public static LocalDate getMealTicketStartDate(Office office) {
		
		String confParam = ConfGeneral.getFieldValue(
				ConfigurationFields.DateStartMealTicket.description, office);
		
		if(confParam == null)
			return null;
		
		return new LocalDate(confParam);
		
	}
	
	/**
	 * La lista di buoni pasto inseriti nei blocchi passati per argomento.
	 * @param codeBlockIds
	 * @return
	 */
	public static List<MealTicket> getMealTicketsInCodeBlockIds(List<Integer> codeBlockIds) {
		
	final QMealTicket mealTicket = QMealTicket.mealTicket;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(mealTicket)
				.orderBy(mealTicket.code.asc());
		
		final BooleanBuilder condition = new BooleanBuilder();
		for(Integer block : codeBlockIds) {
			condition.or(mealTicket.block.eq(block));
		}
		
		query.where(condition);
		
		return query.list(mealTicket);
		
	}
	
	
	
	
	
	
	
	
}
