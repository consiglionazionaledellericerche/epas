package dao.wrapper;

import java.util.List;

import models.Contract;
import models.ContractWorkingTimeType;
import models.WorkingTimeType;

public interface IWrapperWorkingTimeType extends IWrapperModel<WorkingTimeType> {

  List<Contract> getAssociatedActiveContract(Long officeId);

  List<ContractWorkingTimeType> getAssociatedPeriodInActiveContract(
          Long officeId);

  List<Contract> getAssociatedContract();


}
