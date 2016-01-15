package manager.services.mealtickets;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import dao.MealTicketDao;
import dao.PersonDao;

import it.cnr.iit.epas.DateInterval;

import manager.MealTicketManager;

import models.Contract;

import javax.inject.Inject;

/**
 * @author alessandro
 */
public class MealTicketRecapFactory {

  private final MealTicketManager mealTicketManager;
  private final PersonDao personDao;
  private final MealTicketDao mealTicketDao;

  @Inject
  MealTicketRecapFactory(MealTicketManager mealTicketManager,
                         PersonDao personDao, MealTicketDao mealTicketDao) {
    this.mealTicketManager = mealTicketManager;
    this.personDao = personDao;
    this.mealTicketDao = mealTicketDao;
  }

  public Optional<MealTicketRecap> create(Contract contract) {

    Preconditions.checkNotNull(contract);

    Optional<DateInterval> dateInterval = mealTicketManager
            .getContractMealTicketDateInterval(contract);

    if (!dateInterval.isPresent()) {
      return Optional.<MealTicketRecap>absent();
    }

    return Optional.fromNullable(new MealTicketRecap(mealTicketManager,
            personDao, mealTicketDao, contract, dateInterval.get()));
  }

}
