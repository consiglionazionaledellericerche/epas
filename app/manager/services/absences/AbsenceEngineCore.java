package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import it.cnr.iit.epas.DateUtility;

import manager.PersonDayManager;
import manager.services.absences.AbsenceService.AbsenceRequestType;
import manager.services.absences.ResponseItem.AbsenceOperation;
import manager.services.absences.ResponseItem.AbsenceProblem;
import manager.services.absences.model.AbsenceEngine;
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.AbsencePeriod.ComplationComponent;
import manager.services.absences.model.AbsencePeriod.ProblemType;
import manager.services.absences.model.AbsencePeriod.TakableComponent;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType;

public class AbsenceEngineCore {
  
  private final AbsenceEngineUtility absenceEngineUtility;
  private final PersonDayManager personDayManager;

  @Inject
  public AbsenceEngineCore(AbsenceEngineUtility absenceEngineUtility, 
      PersonDayManager personDayManager) {
    this.absenceEngineUtility = absenceEngineUtility;
    this.personDayManager = personDayManager;
  }
  
  /**  
   * 
   * @param absenceEngine
   * @param absenceRequestType
   * @param absenceType
   * @param justifiedType
   * @param specifiedMinutes
   * @return
   */
  public AbsenceEngine doRequest(AbsenceEngine absenceEngine, 
      AbsenceRequestType absenceRequestType, AbsenceType absenceType, JustifiedType justifiedType, 
      Optional<Integer> specifiedMinutes) {
    
    // Costruzione assenza.
    Absence absence = new Absence();
    absence.date = absenceEngine.date;
    absence.absenceType = absenceType;
    absence.justifiedType = justifiedType;
    if (specifiedMinutes.isPresent()) {
      absence.justifiedMinutes = specifiedMinutes.get();
    }
    
    boolean absenceTypeToInfer = (absenceType == null);
    
    // Provo a inserire l'assenza in ogni periodo della catena...
    AbsencePeriod currentAbsencePeriod = absenceEngine.absencePeriod;
    while (currentAbsencePeriod != null) {
      
      //Inferire l'absenceType se necessario...
      if (absenceTypeToInfer) {
        absenceEngineUtility.inferAbsenceType(currentAbsencePeriod, absence);
        if (absence.absenceType == null) {
          currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
          continue;
        }
      }
      
      //Controllo integrità absenceType - justifiedType
      if (!absence.absenceType.justifiedTypesPermitted.contains(absence.justifiedType)) {
        absenceEngine.setProblem(ProblemType.wrongJustifiedType, absence.getAbsenceDate());
        return absenceEngine;
      }
      
      //TODO: Skip not on Holiday se richiedo in una finestra temporale devo saltare.
      //Codice non prendibile nei giorni di festa ed è festa.
      if (!absence.absenceType.consideredWeekEnd && personDayManager.isHoliday(absenceEngine.person,
          absence.date)) {
        absenceEngine.setProblem(ProblemType.notOnHoliday, absence.getAbsenceDate());
        return absenceEngine;
      }
      
      if (DateUtility.isDateIntoInterval(absenceEngine.date, 
          currentAbsencePeriod.periodInterval())) {
        
        checkRequest(absenceEngine, currentAbsencePeriod, absenceRequestType, absence);
        if (absenceEngine.absenceEngineProblem.isPresent()) {
          return absenceEngine;
        }
        
        if (absenceEngine.success) {
          return absenceEngine;
        }
      }
      
      //Al giro dopo devo inferire nuovamente il tipo quindi resetto l'assenza
      if (absenceTypeToInfer) {
        absence.absenceType = null;
        absence.justifiedType = justifiedType;
      }
      
      currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
    }
    
    return absenceEngine;
  }

  
  /**
   * 
   * @param absencePeriod
   * @param absenceType
   * @param date
   * @return
   */
  public AbsenceEngine checkRequest(AbsenceEngine absenceEngine, 
      AbsencePeriod absencePeriod, AbsenceRequestType absenceRequestType,
      Absence absence) {
    
    //Se è presente takableComponent allora deve essere un codice takable
    if (absencePeriod.takableComponent.isPresent()) {
      if (!absencePeriod.takableComponent.get().takableCodes.contains(absence.absenceType)) {
        absenceEngine.setProblem(ProblemType.absenceCodeNotAllowed, absence.getAbsenceDate());
        return absenceEngine;
      }
    }
    
    //Se è presente solo complationComponent allora deve essere un codice complation
    if (!absencePeriod.takableComponent.isPresent() 
        && absencePeriod.complationComponent.isPresent()) {
      if (!absencePeriod.complationComponent.get().complationCodes.contains(absence.absenceType)) { 
        absenceEngine.setProblem(ProblemType.absenceCodeNotAllowed, absence.getAbsenceDate());
        return absenceEngine;
      }
    }

    //Un codice di completamento e ne esiste già uno.
    if (absencePeriod.complationComponent.isPresent()) {
      ComplationComponent complationComponent = absencePeriod.complationComponent.get();
      if (complationComponent.complationCodes.contains(absence.absenceType) 
          && complationComponent.complationAbsencesByDay.get(absence.getAbsenceDate()) != null) {
        absenceEngine.setProblem(ProblemType.stateErrorTwoComplationSameDay, 
            absence.getAbsenceDate());
        return absenceEngine;
      }
    }
    
    //Esecuzione ...

    //simple grouping
    // TODO: bisogna capire dove inserire i controlli di compatibilità (ex. festivo, assenze lo stesso giorno etc)  
    if (absencePeriod.groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.simpleGrouping)) {
      ResponseItem responseItem = ResponseItem.builder()
          .absence(absence)
          .absenceType(absence.getAbsenceType())
          .operation(AbsenceOperation.insert)
          .date(absenceEngine.date).build();
      absenceEngine.responseItems.add(responseItem);
      return absenceEngine;
    }
    
    ResponseItem responseItem = ResponseItem.builder()
        .absence(absence)
        .absenceType(absence.getAbsenceType())
        .operation(AbsenceOperation.insert)
        .consumedResidualAmount(Lists.newArrayList())
        .date(absenceEngine.date).build();
    absenceEngine.responseItems.add(responseItem);
    
    //Takable limit
    if (absencePeriod.takableComponent.isPresent()) {
      
      TakableComponent takableComponent = absencePeriod.takableComponent.get();
      
      if (!takableComponent.takableCodes.contains(absence.absenceType)) {
        absenceEngine.setProblem(ProblemType.absenceCodeNotAllowed);
        return absenceEngine;
      }
      ConsumedResidualAmount consumedResidualAmount = ConsumedResidualAmount.builder()
          .amountType(takableComponent.takeAmountType)
          .totalResidual(takableComponent.getPeriodTakableAmount())
          .usedResidualBefore(takableComponent.getPeriodTakenAmount())
          .amount(absenceEngineUtility
              .absenceJustifiedAmount(absenceEngine, absence, takableComponent.takeAmountType))
          .workingTime(absenceEngine.workingTime(absenceEngine.date))
          .build();
      responseItem.getConsumedResidualAmount().add(consumedResidualAmount);
      
      //limite superato?
      if (!consumedResidualAmount.canTake()) {
        responseItem.setAbsenceProblem(AbsenceProblem.limitExceeded);
        return absenceEngine;
      }
    }
    
    //Complation replacing
    if (absencePeriod.complationComponent.isPresent()) {
      
      ComplationComponent complationComponent = absencePeriod.complationComponent.get();
      
      if (complationComponent.complationCodes.contains(absence.absenceType)) {
        
        int complationAmount = complationComponent.complationConsumedAmount + absenceEngineUtility
            .absenceJustifiedAmount(absenceEngine, absence, 
                complationComponent.complationAmountType);
        
        Optional<AbsenceType> replacingCode = absenceEngineUtility
            .whichReplacingCode(complationComponent, complationAmount);
        if (replacingCode.isPresent()) {
          
          Absence replacingAbsence = new Absence();
          absence.absenceType = replacingCode.get();
          absence.date = absence.getAbsenceDate();
          absence.justifiedType = replacingCode.get().replacingType; //capire
          
          ResponseItem replacingResponseItem = ResponseItem.builder()
              .absence(replacingAbsence)
              .absenceType(replacingCode.get())
              .operation(AbsenceOperation.insertReplacing)
              .date(absenceEngine.date).build();
          
          absenceEngine.responseItems.add(replacingResponseItem);
        }
      }
    }
    
    //success
    absenceEngine.success = true;

    return absenceEngine; 
  }
  

 
  
  //Struttura del caso base di risposta (senza errori di superamento tetto o completamento errato)
  
  // [A] Response item pre operazione 
  //1) Rimanenza completamento precedente 
  //2) Residuo taken/takable 

  // [B] Response item inserimento codice richiesto
  //1) Consumo taken/takable
  
  // [C] Response item inserimento codice completamento
  //1) Nothing
  
  // [D] Response item post operazione
  //1) Rimanenza completamento susseguente
  //2) Residuo taken/takable
  
}
