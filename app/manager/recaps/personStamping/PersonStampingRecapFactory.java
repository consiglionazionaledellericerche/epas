package manager.recaps.personStamping;

import dao.MealTicketDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;
import it.cnr.iit.epas.DateUtility;
import manager.ContractMonthRecapManager;
import manager.PersonDayManager;
import manager.PersonManager;
import models.Person;

import javax.inject.Inject;

public class PersonStampingRecapFactory {

	private final PersonDayManager personDayManager;
	private final PersonDayDao personDayDao;
	private final MealTicketDao mealTicketDao;
	private final ContractMonthRecapManager contractMonthRecapManager;
	private final PersonManager personManager;
	private final PersonStampingDayRecapFactory stampingDayRecapFactory;
	private final IWrapperFactory wrapperFactory;
	private final DateUtility dateUtility;
	
	@Inject
	PersonStampingRecapFactory(PersonDayManager personDayManager,
			PersonDayDao personDayDao,
			PersonManager personManager,
			MealTicketDao mealTicketDao,
			ContractMonthRecapManager contractMonthRecapManager,
			IWrapperFactory wrapperFactory,
			PersonStampingDayRecapFactory stampingDayRecapFactory,
			DateUtility dateUtility) {

		this.personDayManager = personDayManager;
		this.personDayDao = personDayDao;
		this.mealTicketDao = mealTicketDao;
		this.contractMonthRecapManager = contractMonthRecapManager;
		this.personManager = personManager;
		this.stampingDayRecapFactory = stampingDayRecapFactory;
		this.wrapperFactory = wrapperFactory;
		this.dateUtility = dateUtility;
	}

	/**
	 * Costruisce il riepilogo mensile delle timbrature. 
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public PersonStampingRecap create(Person person, int year, int month) {

		return new PersonStampingRecap(personDayManager, personDayDao, mealTicketDao,
				personManager, contractMonthRecapManager, stampingDayRecapFactory, 
				wrapperFactory, dateUtility, year, month, person);
	}

}
