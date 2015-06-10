package manager.recaps.vacation;

import java.util.List;

import javax.inject.Inject;

import manager.ConfYearManager;
import manager.VacationManager;
import models.Contract;
import models.VacationPeriod;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

public class VacationsRecapFactory {

	private final AbsenceDao absenceDao;
	private final AbsenceTypeDao absenceTypeDao;
	private final ConfYearManager confYearManager;
	private final VacationManager vacationManager;
	private final IWrapperFactory wrapperFactory;

	@Inject
	VacationsRecapFactory(IWrapperFactory wrapperFactory, AbsenceDao absenceDao, 
			AbsenceTypeDao absenceTypeDao, ConfYearManager confYearManager,
			VacationManager vacationManager) {
		this.wrapperFactory = wrapperFactory;
		this.absenceDao = absenceDao;
		this.absenceTypeDao = absenceTypeDao;
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
		
		List<VacationPeriod> vacationPeriodList = c.getContractVacationPeriods();
		if ( vacationPeriodList == null || vacationPeriodList.isEmpty() ) {
			return Optional.<VacationsRecap>absent();
		}
		
		// Controllo della dipendenza con i riepiloghi
		if ( !c.hasMonthRecapForVacationsRecap( year )) {
			return Optional.<VacationsRecap>absent();
		}
	
		VacationsRecap vacationRecap = new VacationsRecap(wrapperFactory, 
				absenceDao, absenceTypeDao,	confYearManager, 
				vacationManager,
				year, contract, actualDate, considerExpireLastYear);
	
		return Optional.fromNullable(vacationRecap);
	}

}
