package dao.wrapper;

import models.Contract;
import models.ContractWorkingTimeType;
import models.WorkingTimeType;

import java.util.List;

public interface IWrapperWorkingTimeType extends IWrapperModel<WorkingTimeType> {

	List<Contract> getAssociatedActiveContract(Long officeId);

	List<ContractWorkingTimeType> getAssociatedPeriodInActiveContract(
			Long officeId);

	List<Contract> getAssociatedContract();
	

}
