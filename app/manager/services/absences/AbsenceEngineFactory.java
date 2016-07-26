package manager.services.absences;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.services.absences.model.AbsenceEngine;
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.AbsencePeriod.AbsenceEngineProblem;
import manager.services.absences.model.AbsencePeriod.AbsenceErrorType;
import manager.services.absences.model.AbsencePeriod.ComplationComponent;
import manager.services.absences.model.AbsencePeriod.SuperAbsence;
import manager.services.absences.model.AbsencePeriod.TakableComponent;

import models.Person;
import models.absences.Absence;
import models.absences.AmountType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;

public class AbsenceEngineFactory {
  
  private AbsenceComponentDao absenceComponentDao;
  private PersonChildrenDao personChildrenDao;
  private AbsenceEngineUtility absenceEngineUtility;

  @Inject
  public AbsenceEngineFactory(AbsenceComponentDao absenceComponentDao,
      PersonChildrenDao personChildrenDao, AbsenceEngineUtility absenceEngineUtility) {
        this.absenceComponentDao = absenceComponentDao;
        this.personChildrenDao = personChildrenDao;
        this.absenceEngineUtility = absenceEngineUtility;
  }
  
  public AbsenceEngine buildAbsenceEngineInstance(Person person, GroupAbsenceType groupAbsenceType,
      LocalDate date) {
    
    AbsenceEngine absenceEngine = new AbsenceEngine(absenceComponentDao, personChildrenDao, 
        person, groupAbsenceType, date);
    
    if (groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.vacationsCnr) || 
        groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.compensatoryRestCnr)) {
      
      // TODO: Implementare costruzione ferie e riposi compensativi
      absenceEngine.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
      return absenceEngine;
    }
    
    buildEngineAbsencePeriods(absenceEngine, null);
    if (absenceEngine.absenceEngineProblem.isPresent()) {
      return absenceEngine;
    }
    
    // Assegnare ad ogni periodo le assenze di competenza (fase da migliorare) e calcoli
    AbsencePeriod absencePeriod = absenceEngine.absencePeriod;
    while (absencePeriod != null) {
      for (Absence absence : absenceEngine.getAbsences()) {
        if (!DateUtility
            .isDateIntoInterval(absence.personDay.date, absencePeriod.periodInterval())) {
          continue;
        }
        dispatchSuperAbsence(absencePeriod, absenceEngine, absence);
        if (absenceEngine.absenceEngineProblem.isPresent()) {
          return absenceEngine;
        }
      }
      absencePeriod = absencePeriod.nextAbsencePeriod;
    }
    
//    // Altre Computazioni di supporto:
//    //takenAmount 
//    AbsencePeriod currentAbsencePeriod = absenceEngine.absencePeriod;
//    while (currentAbsencePeriod != null) {
//      if (currentAbsencePeriod.takableComponent.isPresent()) {
//        TakableComponent takableComponent = currentAbsencePeriod.takableComponent.get();
//        takableComponent.resetPeriodTakenAmount();
//        for (Absence absence : takableComponent.takenAbsences) {
//          long amount = absenceEngineUtility
//              .computeAbsenceAmount(absenceEngine, absence, takableComponent.takeAmountType);
//          if (amount < 0) {
//            absenceEngine.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
//            return absenceEngine;
//          }
//          takableComponent.periodTakenAmount += amount; 
//        }
//      }
//      currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
//    }
//    
//    //takenAmount
    
    return absenceEngine;
   
  }
  
  /**
   * Costruisce le date dell'AbsencePeriod relativo all'istanza. 
   * Se il gruppo è ricorsivo costruisce anche le date dei periodi seguenti.
   * @param absenceEngine
   * @return
   */
  private AbsencePeriod buildEngineAbsencePeriods(AbsenceEngine absenceEngine, 
      AbsencePeriod previousAbsencePeriod) { 
   
    if (previousAbsencePeriod == null && absenceEngine.absencePeriod != null) {
      // TODO: Implementare logica di verifica data 
      // richiesta compatibile col precedente absencePeriod
      absenceEngine.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
      return absenceEngine.absencePeriod;
    }
    
    AbsencePeriod currentAbsencePeriod;
    
    if (previousAbsencePeriod == null) {
      //Primo absencePeriod
      currentAbsencePeriod = new AbsencePeriod(absenceEngine.groupAbsenceType);
      absenceEngine.absencePeriod = currentAbsencePeriod;
    } else {
      //Seguenti
      currentAbsencePeriod = new AbsencePeriod(previousAbsencePeriod.groupAbsenceType.nextGroupToCheck);
      currentAbsencePeriod.previousAbsencePeriod = previousAbsencePeriod; //vedere se serve...
    }

    // recuperare l'inizializzazione (questo lo posso fare anche fuori) per i fix sulle date.

    if (currentAbsencePeriod.groupAbsenceType.periodType.equals(PeriodType.year)) {
      currentAbsencePeriod.from = new LocalDate(absenceEngine.date.getYear(), 1, 1);
      currentAbsencePeriod.to = new LocalDate(absenceEngine.date.getYear(), 12, 31);
    } else if (currentAbsencePeriod.groupAbsenceType.periodType.equals(PeriodType.month)) {
      currentAbsencePeriod.from = absenceEngine.date.dayOfMonth().withMinimumValue();
      currentAbsencePeriod.to = absenceEngine.date.dayOfMonth().withMaximumValue();
    } else if (currentAbsencePeriod.groupAbsenceType.periodType.equals(PeriodType.always)) {
      currentAbsencePeriod.from = null;
      currentAbsencePeriod.to = null;
    }

    // Caso inerente i figli.
    else if (currentAbsencePeriod.groupAbsenceType.periodType.isChildPeriod()) {
      try {
        DateInterval childInterval = currentAbsencePeriod.groupAbsenceType.periodType
            .getChildInterval(absenceEngine.getOrderedChildren()
                .get(currentAbsencePeriod.groupAbsenceType.periodType.childNumber - 1).bornDate);
        currentAbsencePeriod.from = childInterval.getBegin();
        currentAbsencePeriod.to = childInterval.getEnd();
      } catch (Exception e) {
        absenceEngine.absenceEngineProblem = Optional.of(AbsenceEngineProblem.noChildExist);
        return currentAbsencePeriod;
      }
    }
    
    currentAbsencePeriod.takableComponent = Optional.absent();
    currentAbsencePeriod.complationComponent = Optional.absent();
    
    // Parte takable
    if (currentAbsencePeriod.groupAbsenceType.takableAbsenceBehaviour != null) {

      TakableAbsenceBehaviour takableBehaviour = 
          currentAbsencePeriod.groupAbsenceType.takableAbsenceBehaviour;

      TakableComponent takableComponent = new TakableComponent();
      takableComponent.takeAmountType = takableBehaviour.amountType;

      takableComponent.fixedPeriodTakableAmount = takableBehaviour.fixedLimit;
      if (takableComponent.takeAmountType.equals(AmountType.units)) {
        // Per non fare operazioni in virgola mobile...
        takableComponent.fixedPeriodTakableAmount = takableComponent.fixedPeriodTakableAmount * 100;
      }
      if (takableBehaviour.takableAmountAdjustment != null) {
        // TODO: ex. workingTimePercent
        //bisogna ridurre il limite
        //engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
      }

      takableComponent.takableCountBehaviour = TakeCountBehaviour.period;
      takableComponent.takenCountBehaviour = TakeCountBehaviour.period;

      takableComponent.takenCodes = takableBehaviour.takenCodes;
      takableComponent.takableCodes = takableBehaviour.takableCodes;

      currentAbsencePeriod.takableComponent = Optional.of(takableComponent);
    }
    
    if (currentAbsencePeriod.groupAbsenceType.complationAbsenceBehaviour != null) {
      
      ComplationAbsenceBehaviour complationBehaviour = 
          currentAbsencePeriod.groupAbsenceType.complationAbsenceBehaviour;
      
      ComplationComponent complationComponent = new ComplationComponent();
      complationComponent.complationAmountType = complationBehaviour.amountType;
      complationComponent.replacingCodes = complationBehaviour.replacingCodes;
      complationComponent.complationCodes = complationBehaviour.complationCodes;
      
      currentAbsencePeriod.complationComponent = Optional.of(complationComponent);
    }

    //Chiamata ricorsiva
    if (currentAbsencePeriod.groupAbsenceType.nextGroupToCheck != null) {
      currentAbsencePeriod.nextAbsencePeriod = 
          buildEngineAbsencePeriods(absenceEngine, currentAbsencePeriod);
    }
 
    return currentAbsencePeriod;
  }
  
  /**
   * 
   * @param absencePeriod
   * @param absenceEngine
   * @param absence
   */
  private void dispatchSuperAbsence(AbsencePeriod absencePeriod, AbsenceEngine absenceEngine, 
      Absence absence) {
    
    if (absencePeriod.takableComponent.isPresent()) {
      TakableComponent takableComponent = absencePeriod.takableComponent.get();
      if (takableComponent.takableCodes.contains(absence.absenceType)) {
        int amount = absenceEngineUtility
            .computeAbsenceAmount(absenceEngine, absence, takableComponent.takeAmountType);
        if (amount < 0) {
          absenceEngine.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
          return;
        }
        takableComponent.periodTakenAmount += amount;
        Optional<AbsenceErrorType> absenceErrorType = Optional.absent();
        if (takableComponent.getPeriodTakableAmount() - takableComponent.getPeriodTakenAmount() < 0) {
          absenceErrorType = Optional.of(AbsenceErrorType.takableLimitExceed);
        }
        takableComponent.takenSuperAbsence.add(
            SuperAbsence.builder()
            .absence(absence)
            .computedJustifiedTime(amount)
            .errorType(absenceErrorType)
            .build());
      }
    }
    if (absencePeriod.complationComponent.isPresent()) {
      ComplationComponent complationComponent = absencePeriod.complationComponent.get(); 
      if (complationComponent.replacingCodes.contains(absence.absenceType)) {
        int amount = absenceEngineUtility
            .computeAbsenceAmount(absenceEngine, absence, complationComponent.complationAmountType);
        Optional<AbsenceErrorType> absenceErrorType = Optional.absent();
        complationComponent.replacingSuperAbsences.add(
            SuperAbsence.builder()
            .absence(absence)
            .computedJustifiedTime(amount)
            .errorType(absenceErrorType)
            .build());
      }
      if (complationComponent.complationCodes.contains(absence.absenceType)) {
        int amount = absenceEngineUtility
            .computeAbsenceAmount(absenceEngine, absence, complationComponent.complationAmountType);
        Optional<AbsenceErrorType> absenceErrorType = Optional.absent();
        complationComponent.complationSuperAbsences.add(
            SuperAbsence.builder()
            .absence(absence)
            .computedJustifiedTime(amount)
            .errorType(absenceErrorType)
            .build());
      }
    }
  }

}
