package dao.wrapper;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.ContractDao;

import java.util.ArrayList;
import java.util.List;

import manager.ContractManager;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.WorkingTimeType;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;


/**
 * WrapperWorkingTimeType con alcune funzionalità aggiuntive.
 *
 * @author alessandro
 */
public class WrapperWorkingTimeType implements IWrapperWorkingTimeType {

  private final WorkingTimeType value;
  private final ContractManager contractManager;
  private final ContractDao contractDao;

  @Inject
  WrapperWorkingTimeType(@Assisted WorkingTimeType wtt,
                         ContractManager contractManager, ContractDao contractDao) {
    this.value = wtt;
    this.contractManager = contractManager;
    this.contractDao = contractDao;
  }

  @Override
  public WorkingTimeType getValue() {
    return value;
  }

  /**
   * I contratti attivi che attualmente hanno impostato il WorkingTimeType.
   */
  @Override
  public List<Contract> getAssociatedActiveContract(Office office) {

    LocalDate today = new LocalDate();
    List<Contract> activeContract = contractDao
        .getActiveContractsInPeriod(today, Optional.fromNullable(today), Optional.of(office));

    List<Contract> list = new ArrayList<Contract>();
    for (Contract contract : activeContract) {
      ContractWorkingTimeType current = contractManager
              .getContractWorkingTimeTypeFromDate(contract, today);
      if (current.workingTimeType.equals(this.value)) {
        list.add(contract);
      }
    }

    return list;
  }

  /**
   * Ritorna i periodi con questo tipo orario appartenti a contratti attualmente attivi.
   */
  @Override
  public List<ContractWorkingTimeType> getAssociatedPeriodInActiveContract(Office office) {

    LocalDate today = new LocalDate();
    List<Contract> activeContract = contractDao
        .getActiveContractsInPeriod(today, Optional.fromNullable(today), Optional.of(office));

    List<ContractWorkingTimeType> list = new ArrayList<ContractWorkingTimeType>();
    for (Contract contract : activeContract) {
      for (ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {
        if (cwtt.workingTimeType.equals(this.value)) {
          list.add(cwtt);
        }
      }
    }

    return list;
  }

  @Override
  public List<Contract> getAssociatedContract() {
    List<Contract> contracts = Lists.newArrayList();
    for (ContractWorkingTimeType cwtt : this.value.contractWorkingTimeType) {
      contracts.add(cwtt.contract);
    }
    return contracts;
  }
}
