package manager.recaps.troubles;

import javax.inject.Inject;

import manager.PersonManager;
import models.Person;

import org.joda.time.LocalDate;

import dao.PersonDayInTroubleDao;

public class PersonTroublesInMonthRecapFactory {

	private final PersonDayInTroubleDao personDayInTroubleDao;
	private final PersonManager personManager;

	@Inject
	PersonTroublesInMonthRecapFactory(PersonDayInTroubleDao personDayInTroubleDao,
			PersonManager personManager) {
		this.personDayInTroubleDao = personDayInTroubleDao;
		this.personManager = personManager;
		
	}
	
	/**
	 * 
	 * @param person
	 * @param month
	 * @param year
	 * @return
	 */
	public PersonTroublesInMonthRecap create( Person person, 
			LocalDate monthBegin, LocalDate monthEnd) {
		
		return new PersonTroublesInMonthRecap(personDayInTroubleDao, 
				personManager, person, monthBegin, monthEnd);
	}
	
}
