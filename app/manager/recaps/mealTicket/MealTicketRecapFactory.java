package manager.recaps.mealTicket;

import it.cnr.iit.epas.DateInterval;

import javax.inject.Inject;

import manager.MealTicketManager;
import models.Contract;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import dao.MealTicketDao;
import dao.PersonDao;

/**
 * @author alessandro
 *
 */
public class MealTicketRecapFactory {

	private final MealTicketManager mealTicketManager;
	private final PersonDao personDao;
	private final MealTicketDao mealTicketDao;

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
	public Optional<MealTicketRecap> create(Contract contract) {

		Preconditions.checkNotNull(contract);
		
		Optional<DateInterval> dateInterval = mealTicketManager
				.getContractMealTicketDateInterval(contract);
		
		if (!dateInterval.isPresent()) {
			return Optional.<MealTicketRecap>absent();
		}
		
		return Optional.fromNullable( new MealTicketRecap(mealTicketManager,
				personDao, mealTicketDao, contract, dateInterval.get()) );
	}

}
