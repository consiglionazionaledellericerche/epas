package manager.recaps.mealTicket;

import javax.inject.Inject;

import manager.MealTicketManager;
import models.Contract;
import dao.MealTicketDao;
import dao.PersonDao;

public class MealTicketRecapFactory {

	private MealTicketManager mealTicketManager;
	private PersonDao personDao;
	private MealTicketDao mealTicketDao;

	@Inject
	MealTicketRecapFactory(MealTicketManager mealTicketManager, 
			PersonDao personDao, MealTicketDao mealTicketDao) {
				this.mealTicketManager = mealTicketManager;
				this.personDao = personDao;
				this.mealTicketDao = mealTicketDao;
	}
	
	/**
	 * 
	 * @param person
	 * @param month
	 * @param year
	 * @return
	 */
	public MealTicketRecap create(Contract contract) {
		
		return new MealTicketRecap(mealTicketManager,
				personDao, mealTicketDao, contract);
	}
	
}
