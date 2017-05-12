package manager.recaps.troubles;

import dao.PersonDayInTroubleDao;

import javax.inject.Inject;

import manager.PersonManager;

import models.Person;

import org.joda.time.LocalDate;

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
