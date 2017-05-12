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
