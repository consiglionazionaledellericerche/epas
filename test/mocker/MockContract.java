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

package mocker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import lombok.Builder;
import manager.ContractManager;
import models.Contract;
import models.VacationPeriod;
import org.joda.time.LocalDate;

public class MockContract {
  
  @Builder
  private static Contract contract(
      LocalDate beginDate,
      LocalDate endDate,
      LocalDate endContract,
      
      LocalDate sourceDateResidual, 
      int sourceVacationLastYearUsed, 
      int sourceVacationCurrentYearUsed, 
      int sourcePermissionUsed, 
      
      ContractManager contractManager) {
    
    Contract contract = mock(Contract.class);
    when(contract.getBeginDate()).thenReturn(beginDate);
    when(contract.getEndDate()).thenReturn(endDate);
    when(contract.getEndContract()).thenReturn(endContract);
    when(contract.getSourceDateResidual()).thenReturn(sourceDateResidual);
    when(contract.getSourceDateVacation()).thenReturn(sourceDateResidual);
    when(contract.getSourceVacationLastYearUsed()).thenReturn(sourceVacationCurrentYearUsed);
    when(contract.getSourceVacationCurrentYearUsed()).thenReturn(sourceVacationCurrentYearUsed);
    when(contract.getSourcePermissionUsed()).thenReturn(sourcePermissionUsed);
    when(contract.calculatedEnd()).thenReturn(Contract.computeEnd(endDate, endContract));

    
    List<VacationPeriod> vacationPeriods = contractManager.contractVacationPeriods(contract);
    when(contract.getVacationPeriods()).thenReturn(vacationPeriods);
    
    return contract;
  }
  
 
}
