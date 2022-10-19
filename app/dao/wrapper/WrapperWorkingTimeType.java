/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import dao.ContractDao;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import manager.ContractManager;
import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.WorkingTimeType;
import org.joda.time.LocalDate;


/**
 * WrapperWorkingTimeType con alcune funzionalità aggiuntive.
 *
 * @author Alessandro Martelli
 * @author Cristian Lucchesi
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
   * La lista dei contratti attivi che hanno un periodo attivo con associato
   * il tipo di orario di lavoro indicato.
   */
  public List<Contract> getAllAssociatedActiveContract() {
    return contractDao.getAllAssociatedActiveContracts(getValue());
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
      if (current.getWorkingTimeType().equals(this.value)) {
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
      for (ContractWorkingTimeType cwtt : contract.getContractWorkingTimeType()) {
        if (cwtt.getWorkingTimeType().equals(this.value)) {
          list.add(cwtt);
        }
      }
    }

    return list;
  }

  @Override
  public List<Contract> getAssociatedContract() {
    return value.getContractWorkingTimeType().stream().map(cwtt -> cwtt.getContract())
        .collect(Collectors.toList());
  }
}
