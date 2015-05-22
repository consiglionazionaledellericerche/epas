package dao.wrapper;

import it.cnr.iit.epas.DateInterval;

import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;

import models.Contract;
import models.ContractMonthRecap;
import models.ContractWorkingTimeType;
import models.VacationPeriod;
import models.enumerate.Parameter;

public interface IWrapperContract extends IWrapperModel<Contract> {
	
	/**
	 * 
	 * @param month
	 * @param year
	 * @return
	 */
	boolean isLastInMonth(int month, int year);

	/**
	 * 
	 * @return
	 */
	List<VacationPeriod> getContractVacationPeriods();
	
	/**
	 * True se il contratto è a tempo determinato.
	 * 
	 * @return
	 */
	boolean isDefined();
	
	/**
	 * 
	 * @return
	 */
	DateInterval getContractDateInterval();
	
	/**
	 * 
	 * @return
	 */
	DateInterval getContractDatabaseInterval();
	
	/**
	 * 
	 * @return
	 */
	YearMonth getFirstMonthToRecap();
	
	/**
	 * 
	 * @return
	 */
	YearMonth getLastMonthToRecap();
	
	/**
	 * 
	 * @return
	 */
	List<ContractWorkingTimeType> getContractWorkingTimeTypeAsList();
	
	Optional<ContractMonthRecap> getContractMonthRecap(YearMonth yearMonth);
	
	/**
	 * Diagnostiche sul contratto.
	 * 
	 * @return
	 */
	public boolean noRelevant();
	public boolean initializationMissing();
	public boolean monthRecapMissing(Optional<YearMonth> yearMonth);
	public boolean hasMonthRecapForVacationsRecap(int yearToRecap);
	

}
