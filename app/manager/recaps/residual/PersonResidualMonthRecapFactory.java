package manager.recaps.residual;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import javax.inject.Inject;

import models.Contract;
import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.ConfYearDao;
import dao.MealTicketDao;
import dao.PersonDayDao;
import dao.WorkingTimeTypeDayDao;
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
	private final CompetenceDao competenceDao;
	private final ConfYearDao confYearDao;
	private final CompetenceCodeDao competenceCodeDao;
	private final WorkingTimeTypeDayDao workingTimeTypeDayDao;
	private final DateUtility dateUtility;

	@Inject
	PersonResidualMonthRecapFactory(AbsenceDao ad, PersonDayDao pd, 
			MealTicketDao mealTicketDao,CompetenceDao competenceDao,
			IWrapperFactory wrapperFactory,
			ConfYearDao confYearDao,CompetenceCodeDao competenceCodeDao,
			WorkingTimeTypeDayDao workingTimeTypeDayDao,
			DateUtility dateUtility) {
		
		this.competenceDao = competenceDao;
		this.absenceDao = ad;
		this.personDayDao = pd;
		this.mealTicketDao = mealTicketDao;
		this.wrapperFactory = wrapperFactory;
		this.confYearDao = confYearDao;
		this.competenceCodeDao = competenceCodeDao;
		this.workingTimeTypeDayDao = workingTimeTypeDayDao;
		this.dateUtility = dateUtility;
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
				mealTicketDao,competenceDao, wrapperFactory, mesePrecedente,
				contract, year, month, initMonteOreAnnoPassato,
				initMonteOreAnnoCorrente, initMealTickets,
				validDataForPersonDay, validDataForCompensatoryRest,
				validDataForMealTickets,confYearDao,competenceCodeDao,workingTimeTypeDayDao,
				dateUtility);
		
	}
}