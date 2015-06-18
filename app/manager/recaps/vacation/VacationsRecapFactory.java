package manager.recaps.vacation;

import javax.inject.Inject;

import manager.ConfYearManager;
import manager.VacationManager;
import manager.cache.AbsenceTypeManager;
import models.Contract;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

public class VacationsRecapFactory {

	private final AbsenceDao absenceDao;
	private final AbsenceTypeDao absenceTypeDao;
	private final AbsenceTypeManager absenceTypeManager;
	private final ConfYearManager confYearManager;
	private final VacationManager vacationManager;
	private final IWrapperFactory wrapperFactory;

	@Inject
	VacationsRecapFactory(IWrapperFactory wrapperFactory, AbsenceDao absenceDao, 
			AbsenceTypeDao absenceTypeDao, AbsenceTypeManager absenceTypeManager, 
			ConfYearManager confYearManager,
			VacationManager vacationManager) {
		this.wrapperFactory = wrapperFactory;
		this.absenceDao = absenceDao;
		this.absenceTypeDao = absenceTypeDao;
		this.absenceTypeManager = absenceTypeManager;
		this.confYearManager = confYearManager;
		this.vacationManager = vacationManager;
	}

	/**
	 * 
	 * @param person
	 * @param month
	 * @param year
	 * @return
	 */
	public Optional<VacationsRecap> create(int year, Contract contract,
			LocalDate actualDate, boolean considerExpireLastYear) {

		IWrapperContract c = wrapperFactory.create(contract);

		if( contract == null || actualDate == null) {
			return Optional.<VacationsRecap>absent();
		}
		
		if ( c.getValue().vacationPeriods == null || 
				c.getValue().vacationPeriods.isEmpty() ) {
			return Optional.<VacationsRecap>absent();
		}
		
		// Controllo della dipendenza con i riepiloghi
		if ( !c.hasMonthRecapForVacationsRecap( year )) {
			return Optional.<VacationsRecap>absent();
		}
		
		if(actualDate.getYear() > year) {
			// FIXME: deve essere il chiamante a non passare la data di oggi
			// e qui la inizializzo in modo appropriato.
			actualDate = new LocalDate(year, 12, 31);
		}
	
		VacationsRecap vacationRecap = new VacationsRecap(wrapperFactory, 
				absenceDao, absenceTypeDao,	absenceTypeManager, confYearManager, 
				vacationManager,
				year, contract, Optional.fromNullable(actualDate), considerExpireLastYear);
	
		return Optional.fromNullable(vacationRecap);
	}

}
