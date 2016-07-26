package manager.services.absences;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import it.cnr.iit.epas.DateUtility;

import manager.PersonDayManager;
import manager.services.absences.AbsenceService.AbsenceRequestType;
import manager.services.absences.model.AbsenceEngine;
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.AbsencePeriod.AbsenceEngineProblem;
import manager.services.absences.model.AbsencePeriod.ComplationComponent;
import manager.services.absences.model.AbsencePeriod.SuperAbsence;
import manager.services.absences.model.AbsencePeriod.TakableComponent;
import manager.services.absences.model.ResponseItem;
import manager.services.absences.model.ResponseItem.AbsenceOperation;
import manager.services.absences.model.ResponseItem.AbsenceProblem;
import manager.services.absences.model.ResponseItem.AbsenceWarning;
import manager.services.absences.model.ResponseItem.ConsumedResidualAmount;

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
   * @param engineInstance
   * @param absenceRequestType
   * @param absenceType
   * @param justifiedType
   * @param specifiedMinutes
   * @return
   */
  public AbsenceEngine doRequest(AbsenceEngine engineInstance, 
      AbsenceRequestType absenceRequestType, AbsenceType absenceType, JustifiedType justifiedType, 
      Optional<Integer> specifiedMinutes) {
    
    // Costruzione assenza.
    Absence absence = new Absence();
    absence.date = engineInstance.date;
    absence.absenceType = absenceType;
    absence.justifiedType = justifiedType;
    if (specifiedMinutes.isPresent()) {
      absence.justifiedMinutes = specifiedMinutes.get();
    }
    
    boolean absenceTypeToInfer = (absenceType == null);
    
    // Provo a inserire l'assenza in ogni periodo della catena...
    AbsencePeriod currentAbsencePeriod = engineInstance.absencePeriod;
    while (currentAbsencePeriod != null) {
      
      //Inferire l'absenceType se necessario...
      if (absenceTypeToInfer) {
        absenceEngineUtility.inferAbsenceType(currentAbsencePeriod, absence);
        if (absence.absenceType == null) {
          currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
          continue;
        }
      }
      
      if (DateUtility.isDateIntoInterval(engineInstance.date, currentAbsencePeriod.periodInterval())) {
        checkRequest(engineInstance, currentAbsencePeriod, absenceRequestType, absence);
        if (engineInstance.absenceEngineProblem.isPresent()) {
          return engineInstance;
        }
        if (engineInstance.success) {
          return engineInstance;
        }
      }
      
      //Al giro dopo devo inferire nuovamente il tipo quindi resetto l'assenza
      if (absenceTypeToInfer) {
        absence.absenceType = null;
        absence.justifiedType = justifiedType;
      }
      
      currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
    }
    
    return engineInstance;
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
    
    //Controllo integrità absenceType - justifiedType
    if (!absence.absenceType.justifiedTypesPermitted.contains(absence.justifiedType)) {
      absenceEngine.absenceEngineProblem = Optional.of(AbsenceEngineProblem.wrongJustifiedType);
      return absenceEngine;
    }
    
    //Codice non prendibile nei giorni di festa ed è festa.
    if (!absence.absenceType.consideredWeekEnd && personDayManager.isHoliday(absenceEngine.person,
        absence.date)) {
      ResponseItem responseItem = new ResponseItem(absence.absenceType, 
          AbsenceOperation.insert, absenceEngine.date);
      responseItem.absenceWarning = AbsenceWarning.notOnHoliday;
      absenceEngine.responseItems.add(responseItem);
      return absenceEngine;
    }
    
    //simple grouping
    // TODO: bisogna capire dove inserire i controlli di compatibilità (ex. festivo, assenze lo stesso giorno etc)  
    if (absencePeriod.groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.simpleGrouping)) {
      ResponseItem responseItem = new ResponseItem(absence.absenceType, 
          AbsenceOperation.insert, absenceEngine.date);
      absenceEngine.responseItems.add(responseItem);
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
    
    // Se c'è un limite di tetto 
    //          -> Calcolare residuo takable alla data e verificare la prendibilità
    if (absencePeriod.takableComponent.isPresent()) {
      
      TakableComponent takableComponent = absencePeriod.takableComponent.get();
      
      if (!takableComponent.takableCodes.contains(absence.absenceType)) {
        absenceEngine.absenceEngineProblem = Optional.of(AbsenceEngineProblem.absenceCodeNotAllowed);
        return absenceEngine;
      }

//    ResponseItem responseItem = new ResponseItem(absence.absenceType, 
//    AbsenceOperation.remainingBefore, engineInstance.date);
//ConsumedResidualAmount consumedResidualAmount = ConsumedResidualAmount.builder()
//    .amountType(takableComponent.takeAmountType)
//    .totalResidual(takableComponent.computeTakableAmount())
//    .usedResidualBefore(takableComponent.computeTakenAmount())
//    .build();
//responseItem.consumedResidualAmount.add(consumedResidualAmount);
//engineInstance.responseItems.add(responseItem);
      
      ResponseItem responseItem = new ResponseItem(absence.absenceType, 
          AbsenceOperation.insert, absenceEngine.date);
      
      ConsumedResidualAmount consumedResidualAmount = ConsumedResidualAmount.builder()
          .amountType(takableComponent.takeAmountType)
          .totalResidual(takableComponent.getPeriodTakableAmount())
          .usedResidualBefore(takableComponent.getPeriodTakenAmount())
          .amount(absenceEngineUtility
              .computeAbsenceAmount(absenceEngine, absence, takableComponent.takeAmountType))
          .workingTime(absenceEngine.workingTime(absenceEngine.date))
          .build();
      if (consumedResidualAmount.canTake()) {
        responseItem.consumedResidualAmount.add(consumedResidualAmount);
        responseItem.absence = absence;
      } else {
        responseItem.consumedResidualAmount.add(consumedResidualAmount);
        responseItem.absenceProblem = AbsenceProblem.limitExceeded;
      }
      
      absenceEngine.responseItems.add(responseItem);
      absenceEngine.success = true;
      
    }
    
    // Se il codice da prendere appartiene a complationCodes 
    //          -> Calcolare il residuo di completamento alla data
    //Complation component
    if (absencePeriod.complationComponent.isPresent()) {
      
    }

    return absenceEngine; 
  }
 
}
