/*
 * Copyright (C) 2022  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import models.MealTicket;
import models.MealTicketCard;
import models.query.QMealTicket;
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
  
  public List<MealTicket> getMealTicketByCard(MealTicketCard card) {
    final QMealTicket mealTicket = QMealTicket.mealTicket;
    
    return getQueryFactory().selectFrom(mealTicket)
        .where(mealTicket.mealTicketCard.isNotNull().and(mealTicket.mealTicketCard.eq(card))).fetch();
  }
  
  
}
