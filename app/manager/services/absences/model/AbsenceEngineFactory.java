package manager.services.absences.model;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Sets;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.services.absences.AbsenceEngineUtility;
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

import java.util.List;
import java.util.Set;

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
    dispatchAbsence(absenceEngine);
    if (absenceEngine.absenceEngineProblem.isPresent()) {
      return absenceEngine;
    }
    
    // Effettuare i precalcoli sugli ammontare dei periodi e delle specifiche assenze.
    computeAmounts(absenceEngine);
    
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
   * Assegna ogni assenza al period appropriato. 
   * @param absencePeriod
   * @param absenceEngine
   * @param absence
   */
  private void dispatchAbsence(AbsenceEngine absenceEngine) {
    boolean hasError = false;
    AbsencePeriod absencePeriod = absenceEngine.absencePeriod;
    while (absencePeriod != null) {
      for (SuperAbsence superAbsence : absenceEngine.getOrderedAbsences()) {
        if (!DateUtility.isDateIntoInterval(superAbsence.getAbsence().personDay.date, 
            absencePeriod.periodInterval())) {
          return;
        }
        TakableComponent takableComponent = null;
        ComplationComponent complationComponent = null;
        LocalDate date = null;
        boolean isTaken = false, isComplation = false, isReplacing = false;
        if (absencePeriod.takableComponent.isPresent()) {
          takableComponent = absencePeriod.takableComponent.get();
          isTaken = takableComponent.takenCodes.contains(superAbsence.getAbsence().absenceType);
        }
        if (absencePeriod.complationComponent.isPresent()) {
          complationComponent = absencePeriod.complationComponent.get();
          isReplacing = complationComponent.replacingCodes.contains(superAbsence.getAbsence().absenceType);
          isComplation = complationComponent.complationCodes.contains(superAbsence.getAbsence().absenceType);
        }
        if ( (isTaken || isComplation || isReplacing) && superAbsence.isAlreadyAssigned() ) {
          //una assenza può essere assegnata ad un solo period
          absenceEngine.absenceEngineProblem = 
              Optional.of(AbsenceEngineProblem.modelErrorTwoPeriods);
          return;
        }
        
        if (isComplation && (isReplacing || isTaken)) {
          //una assenza può essere completamento e nient'altro
          absenceEngine.absenceEngineProblem = 
              Optional.of(AbsenceEngineProblem.modelErrorComplationCode);
          return;  
        }
        if (isTaken || isComplation || isReplacing) {
          superAbsence.setAlreadyAssigned(true);
          date = superAbsence.getAbsence().getAbsenceDate();
        }
        if (isTaken) {
          absencePeriod.takableComponent.get().takenSuperAbsence.add(superAbsence);
        }
        
        if (isComplation) {
          SuperAbsence previous = complationComponent.complationSuperAbsencesByDay.get(date);
          if (previous != null ) {
            //una sola assenza di completamento per quel giorno
            previous.setErrorType(Optional.of(AbsenceErrorType.twoComplationSameDay));
            superAbsence.setErrorType(Optional.of(AbsenceErrorType.twoComplationSameDay));
            hasError = true;
            return;
          }
          complationComponent.complationSuperAbsencesByDay.put(date, superAbsence);
        }
        if (isReplacing) {
          SuperAbsence previous = complationComponent.replacingSuperAbsencesByDay.get(date);
          if (previous != null) {
            //una sola assenza di rimpiazzamento per quel giorno
            previous.setErrorType(Optional.of(AbsenceErrorType.twoReplacingSameDay));
            superAbsence.setErrorType(Optional.of(AbsenceErrorType.twoReplacingSameDay));
            hasError = true;
            return;
          }
          complationComponent.replacingSuperAbsencesByDay.put(date, superAbsence);
        }
      }
      absencePeriod = absencePeriod.nextAbsencePeriod;
    }
    
    if (hasError) {
      absenceEngine.absenceEngineProblem = 
          Optional.of(AbsenceEngineProblem.compromisedReplacingSequence);
    }

  }
  
  /**
   * 
   * @param absencePeriod
   * @param absenceEngine
   * @param absence
   */
  private void computeAmounts(AbsenceEngine absenceEngine) {
    
    AbsencePeriod absencePeriod = absenceEngine.absencePeriod;
    while (absencePeriod != null) {
      
      //Takable
      if (absencePeriod.takableComponent.isPresent()) {
        TakableComponent takableComponent = absencePeriod.takableComponent.get();
        for (SuperAbsence superAbsence : takableComponent.takenSuperAbsence) {
          int amount = absenceEngineUtility.computeAbsenceAmount(absenceEngine, 
              superAbsence.getAbsence(), takableComponent.takeAmountType);
          if (amount < 0) {
            absenceEngine.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
            return;
          }
          superAbsence.setJustifiedTime(amount);
          takableComponent.periodTakenAmount += amount;
          if (takableComponent.getPeriodTakableAmount() - takableComponent.getPeriodTakenAmount() < 0) {
            superAbsence.setErrorType(Optional.of(AbsenceErrorType.takableLimitExceed));
          }
        }
        
        //Complation
        if (absencePeriod.complationComponent.isPresent()) {
          ComplationComponent complationComponent = absencePeriod.complationComponent.get();
          
          //Prendere le complation absences e quando c'è il replacing potenziale vedere se esiste.
          
          
        }
        
      }
     
      
      absencePeriod = absencePeriod.nextAbsencePeriod;
    }
    
    

//    if (absencePeriod.complationComponent.isPresent()) {
//      ComplationComponent complationComponent = absencePeriod.complationComponent.get();
//      boolean isReplacingCode = complationComponent.replacingCodes.contains(absence.absenceType);
//      boolean isComplationCode = complationComponent.complationCodes.contains(absence.absenceType);
//      if (isReplacingCode || isComplationCode) {
//        int amount = absenceEngineUtility
//            .computeAbsenceAmount(absenceEngine, absence, complationComponent.complationAmountType);
//        int complationAmount = absenceEngineUtility
//            .computeAbsenceComplationAmount(absenceEngine, absence, complationComponent.complationAmountType);
//        Optional<AbsenceErrorType> absenceErrorType = Optional.absent();
//
//        
//        //Se la somma del residuo da completamento e il tempo giustificato della assenza
//        // superano uno dei limiti di completamento dei codici in replacingCodes
//        
//        //L'assenza successiva deve essere
//        
//        SuperAbsence superAbsence = SuperAbsence.builder()
//            .absence(absence)
//            .complationTime(complationAmount)
//            .justifiedTime(amount)
//            .errorType(absenceErrorType)
//            .build();
//        if (isReplacingCode) {
//          complationComponent.replacingSuperAbsences.add(superAbsence);
//        }
//        if (complationComponent.complationCodes.contains(absence.absenceType)) {
//          complationComponent.complationSuperAbsences.add(superAbsence);
//        }
//      }
//      
//    }
  }

}
