package absences;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;

import com.beust.jcommander.internal.Lists;

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
import java.util.Map;
import java.util.Set;

import absences.AbsenceDefinitions.AbsenceTypeDefinition;
import absences.AbsenceDefinitions.ComplationBehaviourDefinition;
import absences.AbsenceDefinitions.GroupAbsenceTypeDefinition;
import absences.AbsenceDefinitions.TakableBehaviourDefinition;
import absences.AbsenceDefinitions.WorkingDayDefinition;
import absences.AbsenceDefinitions.WorkingDefinition;

public class AbsencesMocker {
  
  private static AbsenceType absenceType(String code, Integer justifiedTime, 
      boolean consideredWeekEnd, boolean timeForMealTicket, 
      Set<JustifiedType> justifiedTypePermitted, 
      JustifiedType replacingType, Integer replacingTime) {
    
    AbsenceType absenceType = mock(AbsenceType.class);
    when(absenceType.getCode()).thenReturn(code);
    when(absenceType.getJustifiedTypesPermitted()).thenReturn(justifiedTypePermitted);
    when(absenceType.isConsideredWeekEnd()).thenReturn(consideredWeekEnd);
    when(absenceType.isTimeForMealTicket()).thenReturn(timeForMealTicket);
    when(absenceType.getJustifiedTime()).thenReturn(justifiedTime);
    when(absenceType.getReplacingType()).thenReturn(replacingType);
    when(absenceType.getReplacingTime()).thenReturn(replacingTime);
    return absenceType;
  }
  
  private static JustifiedType justifiedType(JustifiedTypeName name) {
    
    JustifiedType justifiedType = mock(JustifiedType.class);
    when(justifiedType.getName()).thenReturn(name);
    return justifiedType;
  }
  
  private static TakableAbsenceBehaviour takableAbsenceBehaviour(AmountType amountType,
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
  
  private static ComplationAbsenceBehaviour complationAbsenceBehaviour(AmountType amountType,
      Set<AbsenceType> complationCodes, Set<AbsenceType> replacingCodes) {
    
    ComplationAbsenceBehaviour behaviour = mock(ComplationAbsenceBehaviour.class);
    when(behaviour.getAmountType()).thenReturn(amountType);
    when(behaviour.getComplationCodes()).thenReturn(complationCodes);
    when(behaviour.getReplacingCodes()).thenReturn(replacingCodes);
    return behaviour;
  }
  
  private static GroupAbsenceType groupAbsenceType(GroupAbsenceTypePattern pattern, 
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
  

  /**
   * Istanza mockata del gruppo. 
   * @param mapAbsenceTypes mocked absenceType già creati
   * @param groupDefinition definizione grupp
   * @return entity mockata
   */
  public static GroupAbsenceType mockedGroup(Map<String, AbsenceType> mapAbsenceTypes, 
      GroupAbsenceTypeDefinition groupDefinition) {
    
    if (groupDefinition == null) {
      return null;
    }
    
    GroupAbsenceType groupAbsenceType = groupAbsenceType( 
        groupDefinition.pattern, 
        groupDefinition.periodType, 
        mockedTakable(mapAbsenceTypes, groupDefinition.takableAbsenceBehaviour), 
        mockedComplation(mapAbsenceTypes, groupDefinition.complationAbsenceBehaviour),
        mockedGroup(mapAbsenceTypes, groupDefinition.next));
    return groupAbsenceType;
  }
  
  /**
   * Istanza mockata del takable behaviour.
   * @param mapAbsenceTypes mocked absenceType già creati
   * @param takableDefinition definizione
   * @return entity mockata
   */
  public static TakableAbsenceBehaviour mockedTakable(Map<String, AbsenceType> mapAbsenceTypes,
      TakableBehaviourDefinition takableDefinition) {
    
    return takableAbsenceBehaviour(takableDefinition.amountType, 
        mockedAbsenceTypes(mapAbsenceTypes, takableDefinition.takenCodes),
        mockedAbsenceTypes(mapAbsenceTypes, takableDefinition.takableCodes),
        takableDefinition.fixedLimit, takableDefinition.takableAmountAdjustment);
  }
  
  /**
   * Istanza mockata del complation behaviour.
   * @param mapAbsenceTypes mocked absenceType già creati
   * @param complationDefinition definition
   * @return entity mockata
   */
  public static ComplationAbsenceBehaviour mockedComplation(
      Map<String, AbsenceType> mapAbsenceTypes,
      ComplationBehaviourDefinition complationDefinition) {
    return complationAbsenceBehaviour(complationDefinition.amountType, 
        mockedAbsenceTypes(mapAbsenceTypes, complationDefinition.replacingCodes),
        mockedAbsenceTypes(mapAbsenceTypes, complationDefinition.complationCodes));
  }
  
  /**
   * Istanze mockate di absenceType.
   * @param mapAbsenceTypes mocked absenceType già creati
   * @param absenceTypeDefinitions definition
   * @return set entity mockate
   */
  public static Set<AbsenceType> mockedAbsenceTypes(Map<String, AbsenceType> mapAbsenceTypes, 
      Set<AbsenceTypeDefinition>  absenceTypeDefinitions) {
    Set<AbsenceType> absenceTypes = Sets.newHashSet();
    for (AbsenceTypeDefinition definition : absenceTypeDefinitions) {
      if (mapAbsenceTypes.get(definition.name()) != null) {
        absenceTypes.add(mapAbsenceTypes.get(definition.name()));
      } else {
        absenceTypes.add(mockedAbsenceType(mapAbsenceTypes, definition));
      }
    }
    return absenceTypes;
  }
  
  /**
   * Istanza mockata del absenceType. Preleva da map se presente.  
   * @param mapAbsenceTypes mocked absenceType già creati
   * @param absenceTypeDefinition definition
   * @return entity mockata
   */
  public static AbsenceType mockedAbsenceType(Map<String, AbsenceType> mapAbsenceTypes, 
      AbsenceTypeDefinition absenceTypeDefinition) {
    AbsenceType mocked = mapAbsenceTypes.get(absenceTypeDefinition.name());
    if (mocked != null) {
      return mocked;
    }
    mocked = absenceType(absenceTypeDefinition.name(), 
        absenceTypeDefinition.justifiedTime, 
        absenceTypeDefinition.consideredWeekEnd, 
        absenceTypeDefinition.timeForMealTicket, 
        mockedJustifiedTypes(absenceTypeDefinition.justifiedTypeNamesPermitted),
        mockedJustifiedType(absenceTypeDefinition.replacingType),
        absenceTypeDefinition.replacingTime);
    
    mapAbsenceTypes.put(absenceTypeDefinition.name(), mocked);
    return mocked;
  }
  
  /**
   * Istanze mockate di justifiedType.
   * @param justifiedTypeNames definition
   * @return set entity mockate
   */
  public static Set<JustifiedType> mockedJustifiedTypes(Set<JustifiedTypeName> justifiedTypeNames) {
    Set<JustifiedType> justifiedTypes = Sets.newHashSet();
    for (JustifiedTypeName name : justifiedTypeNames) {
      justifiedTypes.add(mockedJustifiedType(name));
    }
    return justifiedTypes;
  }
  
  /**
   * Istanza mockata di justifiedType.
   * @param justifiedTypeName definition
   * @return entity mockata
   */
  public static JustifiedType mockedJustifiedType(JustifiedTypeName justifiedTypeName) {
    JustifiedType justifiedType = justifiedType(justifiedTypeName);
    return justifiedType;
  }
  
  
  
  private static WorkingTimeTypeDay workingTimeTypeDay(Integer dayOfWeek, Integer workingTime, 
      boolean holiday, Integer mealTicketTime, Integer breakTicketTime, 
      Integer ticketAfternoonThreshold, Integer ticketAfternoonWorkingTime) {
    WorkingTimeTypeDay workingTimeTypeDay = mock(WorkingTimeTypeDay.class);
    when(workingTimeTypeDay.getDayOfWeek()).thenReturn(dayOfWeek);
    when(workingTimeTypeDay.getWorkingTime()).thenReturn(workingTime);
    when(workingTimeTypeDay.isHoliday()).thenReturn(holiday);
    when(workingTimeTypeDay.getMealTicketTime()).thenReturn(mealTicketTime);
    when(workingTimeTypeDay.getBreakTicketTime()).thenReturn(breakTicketTime);
    when(workingTimeTypeDay.getTicketAfternoonThreshold()).thenReturn(ticketAfternoonThreshold);
    when(workingTimeTypeDay.getTicketAfternoonWorkingTime()).thenReturn(ticketAfternoonWorkingTime);
    return workingTimeTypeDay;
  }

  private static WorkingTimeType workingTimeType(String description, boolean horizontal, 
      List<WorkingTimeTypeDay> orderedWorkingTimeTypeDays) {
    WorkingTimeType workingTimeType = mock(WorkingTimeType.class);
    when(workingTimeType.getDescription()).thenReturn(description);
    when(workingTimeType.getHorizontal()).thenReturn(horizontal);
    when(workingTimeType.getWorkingTimeTypeDays()).thenReturn(orderedWorkingTimeTypeDays);
    return workingTimeType;
  }

  private static ContractWorkingTimeType contractWorkingTimeType(LocalDate beginDate, 
      LocalDate endDate, Contract contract, WorkingTimeType workingTimeType) {
    ContractWorkingTimeType contractWorkingTimeType = mock(ContractWorkingTimeType.class);
    when(contractWorkingTimeType.getContract()).thenReturn(contract); //circulary
    when(contractWorkingTimeType.getWorkingTimeType()).thenReturn(workingTimeType);
    when(contractWorkingTimeType.getBeginDate()).thenReturn(beginDate);
    when(contractWorkingTimeType.getEndDate()).thenReturn(endDate);
    when(contractWorkingTimeType.calculatedEnd()).thenReturn(endDate);
    return contractWorkingTimeType;
  }
  
  private static Contract contract(LocalDate beginDate, LocalDate endDate, LocalDate endContract, 
      Set<ContractWorkingTimeType> contractWorkingTimeType, 
      List<ContractWorkingTimeType> orderedContractWorkingTimeType) { 
    Contract contract = mock(Contract.class);
    when(contract.getBeginDate()).thenReturn(beginDate);
    when(contract.getEndDate()).thenReturn(endDate);
    when(contract.getEndContract()).thenReturn(endContract);
    when(contract.calculatedEnd()).thenReturn(Contract.computeEnd(endDate, endContract));
    when(contract.calculatedEnd()).thenReturn(Contract.computeEnd(endDate, endContract));
    when(contract.getContractWorkingTimeType()).thenReturn(contractWorkingTimeType);
    when(contract.getContractWorkingTimeTypeOrderedList())
      .thenReturn(orderedContractWorkingTimeType);
    return contract;
  }
  
  private static Person person(List<Contract> contracts) {
    Person person = mock(Person.class);
    
    when(person.getContracts()).thenReturn(contracts);
    return person;
  }
  
  /**
   * Istanza mockata di un workingTimeTypeDay.
   * @param workingDayDefinition definition
   * @return mocked entity
   */
  public static WorkingTimeTypeDay mockedWorkingTimeTypeDay(
      WorkingDayDefinition workingDayDefinition) {
    
    WorkingTimeTypeDay wttd = workingTimeTypeDay(workingDayDefinition.dayOfWeek, 
        workingDayDefinition.workingTime, workingDayDefinition.holiday, 
        workingDayDefinition.mealTicketTime, workingDayDefinition.breakTicketTime, 
        workingDayDefinition.ticketAfternoonThreshold, 
        workingDayDefinition.ticketAfternoonWorkingTime);
    
    return wttd;
  }
  
  /**
   * Lista di istanze mockate di workingTimeTypeDays.
   * @param workingDayDefinitions definitions
   * @return list mocked entities
   */
  public static List<WorkingTimeTypeDay> mockedWorkingTimeTypeDays(
      List<WorkingDayDefinition> workingDayDefinitions) {
    List<WorkingTimeTypeDay> list = Lists.newArrayList();
    for (WorkingDayDefinition definition : workingDayDefinitions) {
      list.add(mockedWorkingTimeTypeDay(definition));
    }
    return list;
  }
  
  /**
   * Istanza mockata di workingTimeType.
   * @param workingDefinition definition
   * @return mocked entity
   */
  public static WorkingTimeType mockedWorkingTimeType(WorkingDefinition workingDefinition) {
    
    WorkingTimeType wtt = workingTimeType(workingDefinition.name(), 
        workingDefinition.horizontal, 
        mockedWorkingTimeTypeDays(workingDefinition.orderedWorkingDayDefinition));
    return wtt;
  }
  
  /**
   * Istanza mockata di contractWorkingTimeType.
   * @param beginDate data inzio
   * @param endDate data fine
   * @param workingTimeType workingTimeType
   * @return mocked entity
   */
  public static ContractWorkingTimeType mockContractWorkingTimeType(LocalDate beginDate, 
      LocalDate endDate, WorkingTimeType workingTimeType) {
    ContractWorkingTimeType cwtt = contractWorkingTimeType(beginDate, endDate, null, 
        workingTimeType);
    return cwtt;
  }
  
  /**
   * Istanza mockata di Contract.
   * @param beginDate data inizio
   * @param endDate data fine
   * @param endContract endContract
   * @param orderedContractWorkingTimeType lista ordinata
   * @return mocked entity
   */
  public static Contract mockContract(LocalDate beginDate, LocalDate endDate, 
      LocalDate endContract, 
      List<ContractWorkingTimeType> orderedContractWorkingTimeType) {
    
    Contract contract = contract(beginDate, endDate, endContract, 
        Sets.newHashSet(orderedContractWorkingTimeType), 
        orderedContractWorkingTimeType);
    return contract;
  }
  
  public static Person mockPerson(List<Contract> contracts) {
    Person person = person(contracts);
    return person;
  }
  
  /**
   * Istanza mockata di un dipendente con orario Normale a tempo indeterminato.
   * @param beginContract inizio contratto
   * @return mocked entity
   */
  public static Person mockNormalUndefinedEmployee(LocalDate beginContract) {
    
    WorkingTimeType normal = mockedWorkingTimeType(WorkingDefinition.Normal);
    ContractWorkingTimeType cwtt = mockContractWorkingTimeType(beginContract, null, normal);
    Contract contract = mockContract(beginContract, null, null, Lists.newArrayList(cwtt));
    Person person = mockPerson(Lists.newArrayList(contract));
    return person;
  }
  
}
