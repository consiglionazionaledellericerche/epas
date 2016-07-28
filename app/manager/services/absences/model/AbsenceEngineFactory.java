package manager.services.absences.model;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.model.AbsencePeriod.ComplationComponent;
import manager.services.absences.model.AbsencePeriod.ProblemType;
import manager.services.absences.model.AbsencePeriod.SuperAbsence;
import manager.services.absences.model.AbsencePeriod.TakableComponent;

import models.Person;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;

import java.util.SortedMap;

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
      absenceEngine.setProblem(ProblemType.unsupportedOperation);
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
    computeSoundness(absenceEngine);
    
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
      absenceEngine.setProblem(ProblemType.unsupportedOperation);
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
        absenceEngine.setProblem(ProblemType.noChildExist);
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
      complationComponent.complationCodes = complationBehaviour.complationCodes;
      // i codici di rimpiazzamento li preparo ordinati per ammontare di rimpiazzamento
      for (AbsenceType absenceType : complationBehaviour.replacingCodes) {
        int amount = absenceEngineUtility.replacingAmount(absenceEngine, absenceType, 
            complationComponent.complationAmountType);
        if (amount < 1) {
          absenceEngine.setProblem(ProblemType.modelErrorAmountType);
          return currentAbsencePeriod;
        }
        if (complationComponent.replacingCodesDesc.get(amount) != null) {
          absenceEngine.setProblem(ProblemType.modelErrorReplacingCode);
          return currentAbsencePeriod;
        }
        complationComponent.replacingCodesDesc.put(amount, absenceType);
        complationComponent.replacingTimes.put(absenceType, amount);
      }
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
          isReplacing = complationComponent.replacingCodesDesc.values()
              .contains(superAbsence.getAbsence().absenceType);
          isComplation = complationComponent.complationCodes.contains(superAbsence.getAbsence().absenceType);
        }
        if ( (isTaken || isComplation || isReplacing) && superAbsence.isAlreadyAssigned() ) {
          //una assenza può essere assegnata ad un solo period
          absenceEngine.setProblem(ProblemType.modelErrorTwoPeriods);
          return;
        }
        
        if (isComplation && (isReplacing || isTaken)) {
          //una assenza può essere completamento e nient'altro
          absenceEngine.setProblem(ProblemType.modelErrorComplationCode);
          return;  
        }
        if (isTaken || isComplation || isReplacing) {
          superAbsence.setAlreadyAssigned(true);
          date = superAbsence.getAbsence().getAbsenceDate();
        }
        if (isTaken) {
          if (setJustifiedTime(absenceEngine, takableComponent, superAbsence)) {
            absenceEngine.setProblem(ProblemType.modelErrorAmountType, date);
            return;
          }
          absencePeriod.takableComponent.get().takenSuperAbsence.add(superAbsence);
        }
        
        if (isComplation) {
          SuperAbsence previous = complationComponent.complationAbsencesByDay.get(date);
          if (previous != null ) {
            //una sola assenza di completamento per quel giorno
            absenceEngine.setProblem(ProblemType.stateErrorTwoComplationSameDay, date);
            return;
          }
          if (setJustifiedTime(absenceEngine, takableComponent, superAbsence)) {
            absenceEngine.setProblem(ProblemType.modelErrorAmountType, date);
            return;
          }
          complationComponent.complationAbsencesByDay.put(date, superAbsence);
        }
        if (isReplacing) {
          SuperAbsence previous = complationComponent.replacingAbsencesByDay.get(date);
          if (previous != null) {
            //una sola assenza di rimpiazzamento per quel giorno
            absenceEngine.setProblem(ProblemType.stateErrorTwoReplacingSameDay, date);
            return;
          }
          complationComponent.replacingAbsencesByDay.put(date, superAbsence);
        }
      }
      absencePeriod = absencePeriod.nextAbsencePeriod;
    }
    

  }
  
  /**
   * 
   * @param absencePeriod
   * @param absenceEngine
   * @param absence
   */
  private void computeSoundness(AbsenceEngine absenceEngine) {
    
    AbsencePeriod absencePeriod = absenceEngine.absencePeriod;
    while (absencePeriod != null) {
      
      //Takable
      if (absencePeriod.takableComponent.isPresent()) {
        TakableComponent takableComponent = absencePeriod.takableComponent.get();
        for (SuperAbsence superAbsence : takableComponent.takenSuperAbsence) {
          takableComponent.periodTakenAmount += superAbsence.getJustifiedTime();
          if (takableComponent.getPeriodTakableAmount() 
              - takableComponent.getPeriodTakenAmount() < 0) {
            absenceEngine.setProblem(ProblemType.stateErrorLimitlimitExceeded, 
                superAbsence.getAbsence().getAbsenceDate());
          }
        }
        
        //Complation
        if (absencePeriod.complationComponent.isPresent()) {
          ComplationComponent complationComponent = absencePeriod.complationComponent.get();
        
          int complationAmount = 0;
          SortedMap<LocalDate, ReplacingDay> replacingDays = Maps.newTreeMap();
          for (SuperAbsence superAbsence : complationComponent
              .replacingAbsencesByDay.values()) {
            replacingDays.put(superAbsence.getAbsence().getAbsenceDate(), 
                ReplacingDay.builder()
                .existentReplacing(superAbsence.getAbsence().getAbsenceType())
                .date(superAbsence.getAbsence().getAbsenceDate())
                .build());
          }
          
          //Le assenze di completamento ordinate per data. Genero i rimpiazzamenti ipotetici
          for (SuperAbsence superAbsence : complationComponent
              .complationAbsencesByDay.values()) {
            complationAmount += superAbsence.getJustifiedTime();
            Optional<AbsenceType> replacingCode = absenceEngineUtility
                .whichReplacingCode(complationComponent, complationAmount);
            if (replacingCode.isPresent()) {
              LocalDate replacingDate = superAbsence.getAbsence().getAbsenceDate();
              ReplacingDay replacingDay = replacingDays.get(replacingDate);
              if (replacingDay == null) {
                replacingDay = ReplacingDay.builder()
                    .correctReplacing(replacingCode.get())
                    .date(replacingDate)
                    .build();
                replacingDays.put(replacingDate, replacingDay);
              } else {
                replacingDay.setCorrectReplacing(replacingCode.get());
              }
              complationAmount -= complationComponent.replacingTimes.get(replacingCode.get());
            }
          }
          complationComponent.complationConsumedAmount = complationAmount;
          
          //Controllo che i rimpiazzamenti ipotetici collimino con quelli reali
          for (ReplacingDay replacingDay : replacingDays.values()) {
            if (replacingDay.wrongType()) {
              absenceEngine.setProblem(ProblemType.stateErrorWrongReplacing, 
                  replacingDay.getDate()); 
              return;
            }
            if (replacingDay.onlyCorrect()) {
              absenceEngine.setProblem(ProblemType.stateErrorMissingReplacing, 
                  replacingDay.getDate()); 
              return;
            }
            if (replacingDay.onlyExisting()) {
              absenceEngine.setProblem(ProblemType.stateErrorTooEarlyReplacing, 
                  replacingDay.getDate());
              return;
            }
          }
        }
      }
      absencePeriod = absencePeriod.nextAbsencePeriod;
    }
  }
  

  
  /**
   * Imposta il justifiedTime della superAbsence se non è già calcolato.
   * In caso di errore ritorna false.
   * @param absenceEngine
   * @param takableComponent
   * @param superAbsence
   * @return
   */
  private boolean setJustifiedTime(AbsenceEngine absenceEngine, TakableComponent takableComponent,
      SuperAbsence superAbsence) {
    if (superAbsence.getJustifiedTime() == null) {
      int amount = absenceEngineUtility.absenceJustifiedAmount(absenceEngine, 
          superAbsence.getAbsence(), takableComponent.takeAmountType);
      if (amount < 0) {
        return false;
      }
      superAbsence.setJustifiedTime(amount);
    }
    return true;
  }
  
  /**
   * Lo stato dei rimpiazzamenti, data per date, quelli corretti e quelli esistenti.
   * @author alessandro
   *
   */
  @Builder @Getter @Setter(AccessLevel.PRIVATE)
  private static class ReplacingDay {
    private LocalDate date;
    private AbsenceType existentReplacing;
    private AbsenceType correctReplacing;
    
    public boolean wrongType() {
      return correctReplacing != null && existentReplacing != null 
          && !existentReplacing.equals(correctReplacing);
    }
    
    public boolean onlyCorrect() {
      return correctReplacing != null && existentReplacing == null;
    }
    
    public boolean onlyExisting() {
      return correctReplacing == null && existentReplacing != null;
    }

  }
}
