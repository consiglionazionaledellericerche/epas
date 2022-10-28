package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.Optional;
import javax.persistence.EntityManager;
import models.MealTicketCard;
import models.query.QMealTicketCard;

public class MealTicketCardDao extends DaoBase {

  @Inject
  MealTicketCardDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }
  
  public Optional<MealTicketCard> getMealTicketCardById(Long id) {
    final QMealTicketCard mealTicketCard = QMealTicketCard.mealTicketCard;
    
    return Optional.ofNullable(getQueryFactory().selectFrom(mealTicketCard)
        .where(mealTicketCard.id.eq(id)).fetchFirst());
  }
}
