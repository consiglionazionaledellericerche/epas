package dao;

import helpers.ModelQuery;
import java.util.List;
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

	public static List<MealTicket> getMealTicketAssignedToPersonIntoInterval(Person p, LocalDate dateFrom, LocalDate dateTo) {
		
		QMealTicket mealTicket = QMealTicket.mealTicket;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(mealTicket)
				.where(mealTicket.person.id.eq(p.id))
				.where(mealTicket.date.goe(dateFrom))
				.where(mealTicket.date.loe(dateTo));
		
		return query.list(mealTicket);
	}
	
	public static List<MealTicket> getMealTicketInBlock(Person p, Integer codeBlock) {
		
		QMealTicket mealTicket = QMealTicket.mealTicket;
		
		final JPQLQuery query = ModelQuery.queryFactory()
				.from(mealTicket)
				.where(mealTicket.person.id.eq(p.id))
				.where(mealTicket.block.eq(codeBlock));
		
		return query.list(mealTicket);
	}
	
}
