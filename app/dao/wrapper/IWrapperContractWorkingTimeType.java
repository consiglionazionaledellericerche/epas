package dao.wrapper;

import it.cnr.iit.epas.DateInterval;
import models.ContractWorkingTimeType;

public interface IWrapperContractWorkingTimeType extends IWrapperModel<ContractWorkingTimeType> {

  public DateInterval getDateInverval();

}
