package manager.recaps;

import javax.inject.Inject;

import manager.PersonDayManager;
import manager.PersonManager;
import models.Person;

public class PersonStampingRecapFactory {
	
	private final PersonDayManager personDayManager;
	private final PersonManager personManager;
	private final PersonResidualYearRecapFactory yearFactory;
	//private final PersonStampingDayRecapFactory dayRecapFactory;
	
	@Inject
	PersonStampingRecapFactory(PersonDayManager personDayManager,
			PersonManager personManager,
			PersonResidualYearRecapFactory yearFactory) {
		
		this.personDayManager = personDayManager;
		this.personManager = personManager;
		this.yearFactory = yearFactory;
		
	}
	
	/**
	 * Costruisce il riepilogo mensile delle timbrature. 
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public PersonStampingRecap create(Person person, int year, int month) {
		return new PersonStampingRecap(personDayManager, personManager,
				yearFactory, year, month, person);
	}
	
}
