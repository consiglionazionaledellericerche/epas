package dao.wrapper;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import dao.ContractDao;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import manager.ContractManager;
import models.Contract;
import models.ContractMandatoryTimeSlot;
import models.Office;
import models.TimeSlot;
import org.joda.time.LocalDate;


/**
 * WrapperTimeSlot con alcune funzionalità aggiuntive.
 *
 * @author cristian
 */
public class WrapperTimeSlot implements IWrapperTimeSlot {

  private final TimeSlot value;
  private final ContractManager contractManager;
  private final ContractDao contractDao;

  @Inject
  WrapperTimeSlot(@Assisted TimeSlot ts, ContractManager contractManager, 
      ContractDao contractDao) {
    this.value = ts;
    this.contractManager = contractManager;
    this.contractDao = contractDao;
  }

  @Override
  public TimeSlot getValue() {
    return value;
  }

  /**
   * I contratti attivi che attualmente hanno impostato il TimeSlot.
   */
  @Override
  public List<Contract> getAssociatedActiveContract(Office office) {

    LocalDate today = new LocalDate();
    List<Contract> activeContract = contractDao
        .getActiveContractsInPeriod(today, Optional.fromNullable(today), Optional.of(office));

    List<Contract> list = new ArrayList<Contract>();

    for (Contract contract : activeContract) {
      Optional<ContractMandatoryTimeSlot> current = contractManager
              .getContractMandatoryTimeSlotFromDate(contract, today);
      if (current.isPresent() && current.get().timeSlot.equals(this.value)) {
        list.add(contract);
      }
    }

    return list;
  }

  /**
   * Ritorna i periodi con questo tipo orario appartenti a contratti attualmente attivi.
   */
  @Override
  public List<ContractMandatoryTimeSlot> getAssociatedPeriodInActiveContract(Office office) {

    LocalDate today = new LocalDate();
    List<Contract> activeContract = contractDao
        .getActiveContractsInPeriod(today, Optional.fromNullable(today), Optional.of(office));

    return activeContract.stream().flatMap(c -> c.contractMandatoryTimeSlots.stream())
      .filter(cmts -> cmts.timeSlot.equals(this.value)).collect(Collectors.toList());
    
  }

  @Override
  public List<Contract> getAssociatedContract() {
    return this.value.contractMandatoryTimeSlots.stream()
        .map(cmts -> cmts.contract).collect(Collectors.toList());
  }
}
