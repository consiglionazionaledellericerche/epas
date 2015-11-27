package dao.wrapper;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import it.cnr.iit.epas.DateInterval;

import models.ContractWorkingTimeType;

public class WrapperContractWorkingTimeType implements IWrapperContractWorkingTimeType {

  private final ContractWorkingTimeType value;

  @Inject
  WrapperContractWorkingTimeType(@Assisted ContractWorkingTimeType cwtt) {
    value = cwtt;
  }

  @Override
  public ContractWorkingTimeType getValue() {
    return value;
  }

  /**
   * L'intervallo temporale del periodo
   */
  @Override
  public DateInterval getDateInverval() {
    return new DateInterval(value.beginDate, value.endDate);
  }

}
