package manager.recaps.personstamping;

import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;

import manager.PersonDayManager;
import manager.PersonManager;

import models.Person;

import javax.inject.Inject;

public class PersonStampingRecapFactory {

  private final PersonDayManager personDayManager;
  private final PersonDayDao personDayDao;
  private final PersonManager personManager;
  private final PersonStampingDayRecapFactory stampingDayRecapFactory;
  private final IWrapperFactory wrapperFactory;

  @Inject
  PersonStampingRecapFactory(PersonDayManager personDayManager,
                             PersonDayDao personDayDao,
                             PersonManager personManager,
                             IWrapperFactory wrapperFactory,
                             PersonStampingDayRecapFactory stampingDayRecapFactory) {

    this.personDayManager = personDayManager;
    this.personDayDao = personDayDao;
    this.personManager = personManager;
    this.stampingDayRecapFactory = stampingDayRecapFactory;
    this.wrapperFactory = wrapperFactory;
  }

  /**
   * Costruisce il riepilogo mensile delle timbrature.
   */
  public PersonStampingRecap create(Person person, int year, int month, 
      boolean considerExitingNow) {

    return new PersonStampingRecap(personDayManager, personDayDao,
        personManager, stampingDayRecapFactory,
        wrapperFactory, year, month, person, considerExitingNow);
  }

}
