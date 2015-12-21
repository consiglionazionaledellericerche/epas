package manager.recaps.troubles;

import dao.PersonDayInTroubleDao;

import manager.PersonManager;

import models.Person;

import org.joda.time.LocalDate;

import javax.inject.Inject;

public class PersonTroublesInMonthRecapFactory {

  private final PersonDayInTroubleDao personDayInTroubleDao;
  private final PersonManager personManager;

  @Inject
  PersonTroublesInMonthRecapFactory(PersonDayInTroubleDao personDayInTroubleDao,
                                    PersonManager personManager) {
    this.personDayInTroubleDao = personDayInTroubleDao;
    this.personManager = personManager;

  }

  public PersonTroublesInMonthRecap create(Person person,
                                           LocalDate monthBegin, LocalDate monthEnd) {

    return new PersonTroublesInMonthRecap(personDayInTroubleDao,
            personManager, person, monthBegin, monthEnd);
  }

}
