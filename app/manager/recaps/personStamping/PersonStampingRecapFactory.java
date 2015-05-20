package manager.recaps.personStamping;

import it.cnr.iit.epas.DateUtility;

import javax.inject.Inject;

import dao.wrapper.IWrapperFactory;
import manager.ContractManager;
import manager.PersonDayManager;
import manager.PersonManager;
import manager.recaps.residual.PersonResidualYearRecapFactory;
import models.Person;

public class PersonStampingRecapFactory {

	private final PersonDayManager personDayManager;
	private final ContractManager contractManager;
	private final PersonManager personManager;
	private final PersonStampingDayRecapFactory stampingDayRecapFactory;
	private final IWrapperFactory wrapperFactory;
	private final DateUtility dateUtility;
	
	@Inject
	PersonStampingRecapFactory(PersonDayManager personDayManager,
			PersonManager personManager,
			ContractManager contractManager,
			PersonResidualYearRecapFactory yearFactory,
			IWrapperFactory wrapperFactory,
			PersonStampingDayRecapFactory stampingDayRecapFactory,
			DateUtility dateUtility) {

		this.personDayManager = personDayManager;
		this.contractManager = contractManager;
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

		return new PersonStampingRecap(personDayManager,  personManager,
				contractManager, stampingDayRecapFactory, wrapperFactory, dateUtility,
				year, month, person);
	}

}
