package dao.wrapper;

import it.cnr.iit.epas.DateInterval;

import java.util.List;

import models.Contract;
import models.ContractWorkingTimeType;
import models.VacationPeriod;

public interface IWrapperContract extends IWrapperModel<Contract> {

	boolean isLastInMonth(int month, int year);

	List<VacationPeriod> getContractVacationPeriods();
	
	/**
	 * True se il contratto è a tempo determinato.
	 * 
	 * @return
	 */
	boolean isDefined();
	
	public DateInterval getContractDateInterval();
	
	public List<ContractWorkingTimeType> getContractWorkingTimeTypeAsList();

}
