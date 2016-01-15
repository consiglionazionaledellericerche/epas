package manager.services.wrapperRecap;

import manager.services.mealTickets.MealTicketRecap;
import manager.services.mealTickets.wrapper.IWrapperMealTicketRecap;

/**
 * Interfaccia per generico factory dei recap dei services.
 *
 * @author alessandro
 */
public interface IWrapperRecapFactory {

  IWrapperMealTicketRecap create(MealTicketRecap mealTicketRecap);
}
