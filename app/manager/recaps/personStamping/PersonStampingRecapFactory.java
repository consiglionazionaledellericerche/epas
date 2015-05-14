package manager.recaps.personStamping;

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
	private final PersonResidualYearRecapFactory yearFactory;
	private final PersonStampingDayRecapFactory dayRecapFactory;
	private final IWrapperFactory wrapperFactory;
	
	@Inject
	PersonStampingRecapFactory(PersonDayManager personDayManager,
			PersonManager personManager,
			ContractManager contractManager,
			PersonResidualYearRecapFactory yearFactory,
			PersonStampingDayRecapFactory dayRecapFactory,
			IWrapperFactory wrapperFactory) {
		
		this.personDayManager = personDayManager;
		this.contractManager = contractManager;
		this.personManager = personManager;
		this.yearFactory = yearFactory;
		this.dayRecapFactory = dayRecapFactory;
		this.wrapperFactory = wrapperFactory;
		
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
				yearFactory, contractManager, dayRecapFactory, wrapperFactory,
				year, month, person);
	}
	
}
