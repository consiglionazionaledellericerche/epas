package dao;

import helpers.ModelQuery;

import java.util.List;

import models.Contract;
import models.MealTicket;
import models.Person;
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

	public static List<MealTicket> getMealTicketAssignedToPersonIntoInterval(Contract c, LocalDate dateFrom, LocalDate dateTo) {
		
		QMealTicket mealTicket = QMealTicket.mealTicket;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(mealTicket)
				.where(mealTicket.contract.id.eq(c.id))
				.where(mealTicket.date.goe(dateFrom))
				.where(mealTicket.date.loe(dateTo))
				.orderBy(mealTicket.date.asc());
		
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
	
	
	
	
	
	
}
