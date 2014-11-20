package dao;

import helpers.ModelQuery;
import it.cnr.iit.epas.DateInterval;

import java.util.List;

import models.ConfGeneral;
import models.Contract;
import models.MealTicket;
import models.Office;
import models.PersonDay;
import models.enumerate.ConfigurationFields;
import models.query.QMealTicket;

import org.joda.time.LocalDate;

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
	 * Ordinati per data di scadenza con ordinamento crescente.
	 * @param c
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 */
	public static List<MealTicket> getMealTicketAssignedToPersonIntoInterval(
			Contract c, DateInterval interval) {
		
		QMealTicket mealTicket = QMealTicket.mealTicket;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(mealTicket)
				.where(mealTicket.contract.id.eq(c.id))
				.where(mealTicket.date.goe(interval.getBegin()))
				.where(mealTicket.date.loe(interval.getEnd()))
				.orderBy(mealTicket.expireDate.asc());
		
		return query.list(mealTicket);
	}
	
	//TODO rivederla dopo la rifattorizzazione delle relazioni
	public static List<MealTicket> getMealTicketInBlock(Integer codeBlock) {
		
		QMealTicket mealTicket = QMealTicket.mealTicket;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(mealTicket)
				.where(mealTicket.block.eq(codeBlock));
		
		return query.list(mealTicket);
	}
	
	/**
	 * Ritorna la lista dei buoni pasto associati al contratto ordinata per data di consegna
	 * in ordine decrescente (da quelli consegnati per ultimi a quelli consegnati per primi).
	 * @param contract
	 * @return
	 */
	public static List<MealTicket> getOrderedMealTicketInContract(Contract contract) {
		QMealTicket mealTicket = QMealTicket.mealTicket;
		
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
	
	
	
	
	
	
	
	
}
