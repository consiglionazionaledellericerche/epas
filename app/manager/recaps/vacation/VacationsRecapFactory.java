package manager.recaps.vacation;

import javax.inject.Inject;

import manager.ConfYearManager;
import manager.ContractManager;
import manager.ContractMonthRecapManager;
import manager.VacationManager;
import models.Contract;

import org.joda.time.LocalDate;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.wrapper.IWrapperFactory;

public class VacationsRecapFactory {

	private final AbsenceDao absenceDao;
	private final AbsenceTypeDao absenceTypeDao;
	private final ConfYearManager confYearManager;
	private final VacationManager vacationManager;
	private final IWrapperFactory wrapperFactory;
	private final ContractManager contractManager;
	private final ContractMonthRecapManager contractMonthRecapManager;

	@Inject
	VacationsRecapFactory(IWrapperFactory wrapperFactory, AbsenceDao absenceDao, 
			AbsenceTypeDao absenceTypeDao, ConfYearManager confYearManager,
			ContractManager contractManager, VacationManager vacationManager,
			ContractMonthRecapManager contractMonthRecapManager) {
		this.wrapperFactory = wrapperFactory;
		this.absenceDao = absenceDao;
		this.absenceTypeDao = absenceTypeDao;
		this.confYearManager = confYearManager;
		this.contractManager = contractManager;
		this.vacationManager = vacationManager;
		this.contractMonthRecapManager = contractMonthRecapManager;
	}

	/**
	 * 
	 * @param person
	 * @param month
	 * @param year
	 * @return
	 */
	public VacationsRecap create(int year, Contract contract,
			LocalDate actualDate, boolean considerExpireLastYear) {

		return new VacationsRecap(wrapperFactory, absenceDao, absenceTypeDao, 
				confYearManager, contractManager, vacationManager,
				contractMonthRecapManager,
				year, contract, actualDate, considerExpireLastYear);

	}

}
