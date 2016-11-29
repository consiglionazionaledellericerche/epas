package absences.mocker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lombok.Builder;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Person;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeAmountAdjustment;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

public class AbsencesMocker {

  @Builder
  public static AbsenceType absenceType(String code, Integer justifiedTime, 
      boolean consideredWeekEnd, boolean timeForMealTicket, 
      Set<JustifiedType> justifiedTypePermitted) {
    
    AbsenceType absenceType = mock(AbsenceType.class);
    when(absenceType.getCode()).thenReturn(code);
    when(absenceType.getJustifiedTypesPermitted()).thenReturn(justifiedTypePermitted);
    when(absenceType.isConsideredWeekEnd()).thenReturn(consideredWeekEnd);
    when(absenceType.isTimeForMealTicket()).thenReturn(timeForMealTicket);
    when(absenceType.getJustifiedTime()).thenReturn(justifiedTime);

    return absenceType;
  }
  
  @Builder
  public static JustifiedType justifiedType(JustifiedTypeName name) {
    
    JustifiedType justifiedType = mock(JustifiedType.class);
    when(justifiedType.getName()).thenReturn(name);
    return justifiedType;
  }
  
  @Builder
  public static TakableAbsenceBehaviour takableAbsenceBehaviour(AmountType amountType,
      Set<AbsenceType> takenCodes, Set<AbsenceType> takableCodes, Integer fixedLimit,
      TakeAmountAdjustment takableAmountAdjustment) {
    
    TakableAbsenceBehaviour behaviour = mock(TakableAbsenceBehaviour.class);
    when(behaviour.getAmountType()).thenReturn(amountType);
    when(behaviour.getTakenCodes()).thenReturn(takenCodes);
    when(behaviour.getTakableCodes()).thenReturn(takableCodes);
    when(behaviour.getFixedLimit()).thenReturn(fixedLimit);
    when(behaviour.getTakableAmountAdjustment()).thenReturn(takableAmountAdjustment);
    return behaviour;
  }
  
  @Builder
  public static ComplationAbsenceBehaviour complationAbsenceBehaviour(AmountType amountType,
      Set<AbsenceType> complationCodes, Set<AbsenceType> replacingCodes) {
    
    ComplationAbsenceBehaviour behaviour = mock(ComplationAbsenceBehaviour.class);
    when(behaviour.getAmountType()).thenReturn(amountType);
    when(behaviour.getComplationCodes()).thenReturn(complationCodes);
    when(behaviour.getReplacingCodes()).thenReturn(replacingCodes);
    return behaviour;
  }
  
  @Builder
  public static GroupAbsenceType groupAbsenceType(GroupAbsenceTypePattern pattern, 
      PeriodType periodType, TakableAbsenceBehaviour takableAbsenceBehaviour,
      ComplationAbsenceBehaviour complationAbsenceBehaviour, GroupAbsenceType next) {
    
    GroupAbsenceType group = mock(GroupAbsenceType.class);
    when(group.getPattern()).thenReturn(pattern);
    when(group.getPeriodType()).thenReturn(periodType);
    when(group.getTakableAbsenceBehaviour()).thenReturn(takableAbsenceBehaviour);
    when(group.getComplationAbsenceBehaviour()).thenReturn(complationAbsenceBehaviour);
    when(group.getNextGroupToCheck()).thenReturn(next);
    
    return group;
  }



  @Builder
  public static WorkingTimeTypeDay workingTimeTypeDay(Integer dayOfWeek, Integer workingTime, 
      boolean holiday, Integer mealTicketTime, Integer breakTicketTime, 
      Integer ticketAfternoonThreshold, Integer ticketAfternoonWorkingTime) {
    WorkingTimeTypeDay workingTimeTypeDay = new WorkingTimeTypeDay();
    when(workingTimeTypeDay.getDayOfWeek()).thenReturn(dayOfWeek);
    when(workingTimeTypeDay.getWorkingTime()).thenReturn(workingTime);
    when(workingTimeTypeDay.isHoliday()).thenReturn(holiday);
    when(workingTimeTypeDay.getMealTicketTime()).thenReturn(mealTicketTime);
    when(workingTimeTypeDay.getBreakTicketTime()).thenReturn(breakTicketTime);
    when(workingTimeTypeDay.getTicketAfternoonThreshold()).thenReturn(ticketAfternoonThreshold);
    when(workingTimeTypeDay.getTicketAfternoonWorkingTime()).thenReturn(ticketAfternoonWorkingTime);
    return workingTimeTypeDay;
  }
  
  @Builder
  public static WorkingTimeType workingTimeType(String description, boolean horizontal, 
      List<WorkingTimeTypeDay> orderedWorkingTimeTypeDays) {
    WorkingTimeType workingTimeType = new WorkingTimeType();
    when(workingTimeType.getDescription()).thenReturn(description);
    when(workingTimeType.getHorizontal()).thenReturn(horizontal);
    when(workingTimeType.getWorkingTimeTypeDays()).thenReturn(orderedWorkingTimeTypeDays);
    return workingTimeType;
  }

  @Builder
  public static ContractWorkingTimeType contractWorkingTimeType(LocalDate beginDate, 
      LocalDate endDate, Contract contract, WorkingTimeType workingTimeType) {
    ContractWorkingTimeType contractWorkingTimeType = new ContractWorkingTimeType();
    when(contractWorkingTimeType.getContract()).thenReturn(contract);
    when(contractWorkingTimeType.getWorkingTimeType()).thenReturn(workingTimeType);
    when(contractWorkingTimeType.getBeginDate()).thenReturn(beginDate);
    when(contractWorkingTimeType.getEndDate()).thenReturn(endDate);
    when(contractWorkingTimeType.calculatedEnd()).thenReturn(endDate);
    return contractWorkingTimeType;
  }
  
  @Builder
  public static Contract contract(LocalDate beginDate, LocalDate endDate, LocalDate endContract, 
      Set<ContractWorkingTimeType> contractWorkingTimeType) { 
    Contract contract = new Contract();
    when(contract.getBeginDate()).thenReturn(beginDate);
    when(contract.getEndDate()).thenReturn(endDate);
    when(contract.getEndContract()).thenReturn(endContract);
    when(contract.calculatedEnd()).thenReturn(Contract.computeEnd(endDate, endContract));
    when(contract.calculatedEnd()).thenReturn(Contract.computeEnd(endDate, endContract));
    when(contract.getContractWorkingTimeType()).thenReturn(contractWorkingTimeType);
    return contract;
  }
  
  
  @Builder
  public static Person person(List<Contract> contracts) {
    Person person = mock(Person.class);
    
    when(person.getContracts()).thenReturn(contracts);
    return person;
  }
  

  
  
  
  
  
  
}
