package manager.recaps.vacation;

import javax.inject.Inject;

import manager.ConfYearManager;
import manager.ContractManager;
import manager.VacationManager;
import models.Contract;

import org.joda.time.LocalDate;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.wrapper.IWrapperFactory;

public class VacationsRecapFactory {

	private ContractManager contractManager;
	private AbsenceDao absenceDao;
	private AbsenceTypeDao absenceTypeDao;
	private ConfYearManager confYearManager;
	private VacationManager vacationManager;
	private IWrapperFactory wrapperFactory;

	@Inject
	VacationsRecapFactory(IWrapperFactory wrapperFactory, AbsenceDao absenceDao, 
			AbsenceTypeDao absenceTypeDao, ConfYearManager confYearManager, VacationManager vacationManager) {
				this.wrapperFactory = wrapperFactory;
				this.contractManager = contractManager;
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
	public VacationsRecap create(int year, Contract contract,
			LocalDate actualDate, boolean considerExpireLastYear) {
		
		return new VacationsRecap(wrapperFactory, absenceDao, absenceTypeDao,
				confYearManager, vacationManager, year, contract,
				actualDate, considerExpireLastYear);
	}
	
}
