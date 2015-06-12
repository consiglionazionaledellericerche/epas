package manager.recaps.personStamping;

import it.cnr.iit.epas.DateUtility;

import javax.inject.Inject;

import manager.ContractMonthRecapManager;
import manager.PersonDayManager;
import manager.PersonManager;
import models.Person;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;

public class PersonStampingRecapFactory {

	private final PersonDayManager personDayManager;
	private final PersonDayDao personDayDao;
	private final ContractMonthRecapManager contractMonthRecapManager;
	private final PersonManager personManager;
	private final PersonStampingDayRecapFactory stampingDayRecapFactory;
	private final IWrapperFactory wrapperFactory;
	private final DateUtility dateUtility;
	
	@Inject
	PersonStampingRecapFactory(PersonDayManager personDayManager,
			PersonDayDao personDayDao,
			PersonManager personManager,
			ContractMonthRecapManager contractMonthRecapManager,
			IWrapperFactory wrapperFactory,
			PersonStampingDayRecapFactory stampingDayRecapFactory,
			DateUtility dateUtility) {

		this.personDayManager = personDayManager;
		this.personDayDao = personDayDao;
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

		return new PersonStampingRecap(personDayManager, personDayDao, personManager,
				contractMonthRecapManager, stampingDayRecapFactory, wrapperFactory, dateUtility,
				year, month, person);
	}

}
