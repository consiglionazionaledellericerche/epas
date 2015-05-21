package manager.recaps.vacation;

import javax.inject.Inject;

import manager.ConfYearManager;
import manager.ContractManager;
import manager.ContractMonthRecapManager;
import manager.VacationManager;
import models.Contract;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
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

		VacationsRecap vacationRecap = new VacationsRecap(wrapperFactory, 
				absenceDao, absenceTypeDao,	confYearManager, 
				vacationManager,
				year, contract, actualDate, considerExpireLastYear);
		
		if (contract.sourceRequired != null &&
				contract.sourceRequired == true ) {
			return Optional.<VacationsRecap>absent();
		}
		
		return Optional.fromNullable(vacationRecap);
	}

}
