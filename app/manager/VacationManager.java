package manager;

import models.Office;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;

import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;

public class VacationManager {

	@Inject
	public VacationManager(AbsenceDao absenceDao, AbsenceTypeDao absenceTypeDao,
			ConfYearManager confYearManager) {
		this.confYearManager = confYearManager;
	}

	private final ConfYearManager confYearManager;

	/**
	 * La data di scadenza delle ferie anno passato per l'office passato come argomento, 
	 * nell'anno year.
	 * @param year
	 * @param office
	 * @return
	 */
	public LocalDate vacationsLastYearExpireDate(int year, Office office) {

		Integer monthExpiryVacationPastYear = confYearManager.getIntegerFieldValue(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, year);

		Integer dayExpiryVacationPastYear = confYearManager.getIntegerFieldValue(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, year); 

		LocalDate expireDate = LocalDate.now()
				.withMonthOfYear(monthExpiryVacationPastYear)
				.withDayOfMonth(dayExpiryVacationPastYear);
		return expireDate;
	}

	/**
	 * 
	 * @param year l'anno per il quale vogliamo capire se le ferie dell'anno precedente sono scadute
	 * @param expireDate l'ultimo giorno utile per usufruire delle ferie dell'anno precedente
	 * @return
	 */
	public boolean isVacationsLastYearExpired(int year, LocalDate expireDate) {
		LocalDate today = LocalDate.now();

		if( year < today.getYear() ) {		//query anni passati 
			return true;
		}
		else if( year == today.getYear() && today.isAfter(expireDate)) {	//query anno attuale
			return true;
		}
		return false;
	}
	
	

}
