package manager.recaps.troubles;

import javax.inject.Inject;

import models.Person;

import org.joda.time.LocalDate;

import dao.PersonDayInTroubleDao;

public class PersonTroublesInMonthRecapFactory {

	private final PersonDayInTroubleDao personDayInTroubleDao;

	@Inject
	PersonTroublesInMonthRecapFactory(PersonDayInTroubleDao personDayInTroubleDao) {
		this.personDayInTroubleDao = personDayInTroubleDao;
		
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
				person, monthBegin, monthEnd);
	}
	
}
