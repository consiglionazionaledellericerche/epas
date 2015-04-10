package manager;

import manager.recaps.vacation.VacationsRecap;
import models.ConfYear;
import models.Contract;
import models.Office;
import models.Person;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.wrapper.IWrapperFactory;
import exceptions.EpasExceptionNoSourceData;

public class VacationManager {

	@Inject 
	public IWrapperFactory wrapperFactory;
	
	@Inject
	public AbsenceDao absenceDao;
	
	@Inject 
	public AbsenceTypeDao absenceTypeDao;
	
	@Inject
	public ConfYearManager confYearManager;
	
	
	/**
	 * Il numero di giorni di ferie dell'anno passato non ancora utilizzati (senza considerare l'expire limit di utilizzo)
	 * Il valore ritornato contiene i giorni ferie maturati previsti dal contratto nell'anno passato meno 
	 * i 32 utilizzati in past year
	 * i 31 utilizzati in current year
	 * i 37 utilizzati in current year
	 * @param year
	 * @param person
	 * @param abt
	 * @return
	 * @throws EpasExceptionNoSourceData 
	 */
	public int remainingPastVacationsAs37(int year, Person person) throws EpasExceptionNoSourceData{

		Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
		Preconditions.checkState(contract.isPresent());
		
		return new VacationsRecap(wrapperFactory, absenceDao, absenceTypeDao,
				confYearManager, this, year, contract.get(), new LocalDate(), false)
					.vacationDaysLastYearNotYetUsed;
		
	}
	
	/**
	 * La data di scadenza delle ferie anno passato per l'office passato come argomento, 
	 * nell'anno year.
	 * @param year
	 * @param office
	 * @return
	 */
	public LocalDate vacationsLastYearExpireDate(int year, Office office) {
		
		Integer monthExpiryVacationPastYear = ConfYearManager.getIntegerFieldValue(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, year);
				
		Integer dayExpiryVacationPastYear = ConfYearManager.getIntegerFieldValue(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, year); 
						
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
	public boolean isVacationsLastYearExpired(int year, LocalDate expireDate)
	{
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
