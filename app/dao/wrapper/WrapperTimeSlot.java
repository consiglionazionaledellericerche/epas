/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dao.wrapper;

import com.google.common.base.Optional;
import com.google.inject.assistedinject.Assisted;
import dao.ContractDao;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import manager.ContractManager;
import models.Contract;
import models.ContractMandatoryTimeSlot;
import models.Office;
import models.TimeSlot;
import org.joda.time.LocalDate;


/**
 * WrapperTimeSlot con alcune funzionalità aggiuntive.
 *
 * @author Cristian Lucchesi
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
      if (current.isPresent() && current.get().getTimeSlot().equals(this.value)) {
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

    return activeContract.stream().flatMap(c -> c.getContractMandatoryTimeSlots().stream())
      .filter(cmts -> cmts.getTimeSlot().equals(this.value)).collect(Collectors.toList());
    
  }

  @Override
  public List<Contract> getAssociatedContract() {
    return this.value.getContractMandatoryTimeSlots().stream()
        .map(cmts -> cmts.getContract()).collect(Collectors.toList());
  }
}
