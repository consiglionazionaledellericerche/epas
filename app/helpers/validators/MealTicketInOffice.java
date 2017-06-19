package helpers.validators;

import dao.MealTicketDao;

import injection.StaticInject;

import javax.inject.Inject;

import models.MealTicket;

import play.data.validation.Check;


@StaticInject
public class MealTicketInOffice extends Check {
   
  
  @Inject
  static MealTicketDao mealTicketDao;
  

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {

    if (!(validatedObject instanceof MealTicket)) {
      return false;
    }
    final MealTicket mealTicket = (MealTicket) validatedObject;
    final String code = (String) value;
    MealTicket exist = mealTicketDao.getMealTicketByCodeAndOffice(code, mealTicket.admin.office);
    if (exist != null) {
      return false;
    }
    return true;
  }
}
