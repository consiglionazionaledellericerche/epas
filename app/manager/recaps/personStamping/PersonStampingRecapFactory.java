package manager.recaps.personStamping;

import javax.inject.Inject;

import manager.PersonDayManager;
import manager.PersonManager;
import manager.recaps.residual.PersonResidualYearRecapFactory;
import models.Person;

public class PersonStampingRecapFactory {

	private final PersonDayManager personDayManager;
	private final PersonManager personManager;
	private final PersonResidualYearRecapFactory yearFactory;
	private final PersonStampingDayRecapFactory dayRecapFactory;

	@Inject
	PersonStampingRecapFactory(PersonDayManager personDayManager,
			PersonManager personManager,
			PersonResidualYearRecapFactory yearFactory,
			PersonStampingDayRecapFactory dayRecapFactory) {

		this.personDayManager = personDayManager;
		this.personManager = personManager;
		this.yearFactory = yearFactory;
		this.dayRecapFactory = dayRecapFactory;

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
				yearFactory, dayRecapFactory,year, month, person);
	}

}
