package dao.wrapper;

import it.cnr.iit.epas.DateInterval;

import java.util.List;

import models.Contract;
import models.ContractMonthRecap;
import models.ContractWorkingTimeType;

import org.joda.time.YearMonth;

import com.google.common.base.Optional;

public interface IWrapperContract extends IWrapperModel<Contract> {
	
	/**
	 * 
	 * @param month
	 * @param year
	 * @return
	 */
	boolean isLastInMonth(int month, int year);

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
	Optional<YearMonth> getFirstMonthToRecap();
	
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
	public boolean monthRecapMissing(YearMonth yearMonth);
	public boolean monthRecapMissing();
	public boolean hasMonthRecapForVacationsRecap(int yearToRecap);

}
