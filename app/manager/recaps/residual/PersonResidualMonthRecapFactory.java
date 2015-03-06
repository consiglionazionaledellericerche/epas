package manager.recaps.residual;

import it.cnr.iit.epas.DateInterval;

import javax.inject.Inject;

import models.Contract;
import dao.AbsenceDao;
import dao.MealTicketDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperFactory;

/**
 * @author dario
 * 
 * Factory da utilizzare nei controller/dao/manager da injectare per poter
 * creare i PersonResidualMonthRecap.
 * TODO: farla fare al guice assistedinject.
 *
 */
public class PersonResidualMonthRecapFactory {
	
	private final AbsenceDao absenceDao;
	private final PersonDayDao personDayDao;
	private final IWrapperFactory wrapperFactory;
	private final MealTicketDao mealTicketDao;

	@Inject
	PersonResidualMonthRecapFactory(AbsenceDao ad, PersonDayDao pd, 
			MealTicketDao mealTicketDao, IWrapperFactory wrapperFactory) {
		this.absenceDao = ad;
		this.personDayDao = pd;
		this.mealTicketDao = mealTicketDao;
		this.wrapperFactory = wrapperFactory;
	}
	
	/**
	 * @param mesePrecedente
	 * @param year
	 * @param month
	 * @param contract
	 * @param initMonteOreAnnoPassato
	 * @param initMonteOreAnnoCorrente
	 * @param initMealTickets
	 * @param validDataForPersonDay
	 * @param validDataForCompensatoryRest
	 * @param validDataForMealTickets
	 * @return un nuovo personresidualmonthrecap costruito con i parametri
	 * forniti, con vita legata alla richiesta.
	 */
	public PersonResidualMonthRecap create(PersonResidualMonthRecap mesePrecedente,
			int year, int month, Contract contract, 
			int initMonteOreAnnoPassato, int initMonteOreAnnoCorrente, 
			int initMealTickets, DateInterval validDataForPersonDay, 
			DateInterval validDataForCompensatoryRest, 
			DateInterval validDataForMealTickets) {
		
		return new PersonResidualMonthRecap(absenceDao, personDayDao, 
				mealTicketDao, wrapperFactory, mesePrecedente,
				contract, year, month, initMonteOreAnnoPassato,
				initMonteOreAnnoCorrente, initMealTickets,
				validDataForPersonDay, validDataForCompensatoryRest,
				validDataForMealTickets);
		
	}
}