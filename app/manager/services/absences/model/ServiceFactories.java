package manager.services.absences.model;

import com.google.common.collect.Lists;

import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.PersonDayManager;
import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.errors.CriticalError.CriticalProblem;

import models.Contract;
import models.Person;
import models.PersonChildren;
import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class ServiceFactories {

  private AbsenceEngineUtility absenceEngineUtility;
  private AbsenceComponentDao absenceComponentDao;
  private PersonDayManager personDayManager;

  @Inject
  ServiceFactories(AbsenceEngineUtility absenceEngineUtility, 
      AbsenceComponentDao absenceComponentDao, PersonDayManager personDayManager) {
    this.absenceEngineUtility = absenceEngineUtility;
    this.absenceComponentDao = absenceComponentDao;
    this.personDayManager = personDayManager;
  }
  
  
  /**
   * Costruttore per richiesta di scan.
   * @param person
   * @param scanFrom
   * @param absencesToScan
   * @return
   */
  public Scanner buildScanInstance(Person person, LocalDate scanFrom, 
      List<Absence> absencesToScan, List<PersonChildren> orderedChildren, 
      List<Contract> fetchedContracts) {
    Scanner absenceEngineScan = new Scanner(person, scanFrom, 
        absencesToScan, orderedChildren, fetchedContracts, 
        this, absenceEngineUtility, personDayManager, absenceComponentDao);
    for (Absence absence : absenceEngineScan.absencesToScan) {
      Set<GroupAbsenceType> groupsToScan = absenceEngineUtility.involvedGroup(absence.absenceType); 
      absenceEngineScan.absencesGroupsToScan.put(absence, groupsToScan);
    }
    return absenceEngineScan;
  }
  

  
  /**
   * Costruisce la catena dei periodi per il gruppo e la data passati.
   * Calcola anche la soundness del periodo e mette nel report eventuali errori.
   * @param absenceEngine
   * @param groupAbsenceType
   * @param date
   * @return
   */
  public PeriodChain buildPeriodChain(Person person, GroupAbsenceType groupAbsenceType, LocalDate date,
      List<Absence> previousInserts,
      Absence absenceToInsert,
      List<PersonChildren> orderedChildren, List<Contract> fetchedContracts) { 
    
    PeriodChain periodChain = new PeriodChain(person, groupAbsenceType, date);
    
    GroupAbsenceType currentGroup = groupAbsenceType;
    while (currentGroup != null) {
      AbsencePeriod currentPeriod = buildAbsencePeriod(person, currentGroup, date, 
          orderedChildren, fetchedContracts);
      if (!currentPeriod.ignorePeriod) { 
        periodChain.periods.add(currentPeriod);  
      }
      if (currentPeriod.errorsBox.containsCriticalErrors()) {
        return periodChain;
      }
      currentGroup = currentGroup.nextGroupToCheck;
    }

    if (periodChain.periods.isEmpty()) {
      return periodChain;
    }
    
    //le date
    periodChain.from = periodChain.firstPeriod().from;
    periodChain.to = periodChain.firstPeriod().to;
    for (AbsencePeriod absencePeriod : periodChain.periods) {
      if (absencePeriod.from == null && absencePeriod.to == null) {
        //always
        periodChain.from = null;
        periodChain.to = null;
        break;
      }
      if (absencePeriod.from.isBefore(periodChain.from)) {
        periodChain.from = absencePeriod.from;
      }
      if (absencePeriod.to.isAfter(periodChain.to)) {
        periodChain.to = absencePeriod.to;
      }
    }
    
    // fetch di tutte le assenze nel periodo (comprese previous inserts)
    //TODO: qui si può efficientare molto se il periodChain ha le stesse date
    // di quello precedente.......
    periodChain.allInvolvedAbsences = absenceComponentDao
        .mapAbsences(absenceComponentDao
            .orderedAbsences(
            periodChain.person, 
            periodChain.from, periodChain.to, 
            Lists.newArrayList()), null);
    periodChain.allInvolvedAbsences = absenceComponentDao
        .mapAbsences(previousInserts, periodChain.allInvolvedAbsences);
    
    // fetch delle assenze della catena (comprese previous inserts)
    List<Absence> involvedAbsencesInGroup = absenceComponentDao.orderAbsences(
        absenceComponentDao.orderedAbsences(
            periodChain.person, 
            periodChain.from, periodChain.to, 
            Lists.newArrayList(periodChain.periodChainInvolvedCodes())), 
        previousInserts);
 
    // assegnare ad ogni periodo le assenze di competenza e calcoli
    populatePeriodChain(periodChain, involvedAbsencesInGroup, absenceToInsert, previousInserts);
 
    return periodChain;
  }
  
  
  private AbsencePeriod buildAbsencePeriod(Person person, GroupAbsenceType groupAbsenceType, 
      LocalDate date, 
      List<PersonChildren> orderedChildren,
      List<Contract> fetchedContracts) {
    
    // recuperare l'inizializzazione (questo lo posso fare anche fuori) per i fix sulle date.
    
    AbsencePeriod absencePeriod = new AbsencePeriod(person, groupAbsenceType);
    
    if (absencePeriod.groupAbsenceType.periodType.equals(PeriodType.year)) {
      absencePeriod.from = new LocalDate(date.getYear(), 1, 1);
      absencePeriod.to = new LocalDate(date.getYear(), 12, 31);
    } else if (absencePeriod.groupAbsenceType.periodType.equals(PeriodType.month)) {
      absencePeriod.from = date.dayOfMonth().withMinimumValue();
      absencePeriod.to = date.dayOfMonth().withMaximumValue();
    } else if (absencePeriod.groupAbsenceType.periodType.equals(PeriodType.always)) {
      absencePeriod.from = null;
      absencePeriod.to = null;
    } else if (absencePeriod.groupAbsenceType.periodType.isChildPeriod()) {
      // Caso inerente i figli.
      try {
        DateInterval childInterval = absencePeriod.groupAbsenceType.periodType
            .getChildInterval(orderedChildren
                .get(absencePeriod.groupAbsenceType.periodType.childNumber - 1).bornDate);
        absencePeriod.from = childInterval.getBegin();
        absencePeriod.to = childInterval.getEnd();

      } catch (Exception e) {
        absencePeriod.ignorePeriod = true;
        return absencePeriod;
      }
    }
    
    // Parte takable
    if (absencePeriod.groupAbsenceType.takableAbsenceBehaviour != null) {

      TakableAbsenceBehaviour takableBehaviour = 
          absencePeriod.groupAbsenceType.takableAbsenceBehaviour;

      absencePeriod.takeAmountType = takableBehaviour.amountType;

      absencePeriod.setFixedPeriodTakableAmount(takableBehaviour.fixedLimit);
      if (takableBehaviour.takableAmountAdjustment != null) {
        // TODO: ex. workingTimePercent
        //bisogna ridurre il limite
        //engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
      }

      absencePeriod.takableCountBehaviour = TakeCountBehaviour.period;
      absencePeriod.takenCountBehaviour = TakeCountBehaviour.period;

      absencePeriod.takenCodes = takableBehaviour.takenCodes;
      absencePeriod.takableCodes = takableBehaviour.takableCodes;

    }
    
    if (absencePeriod.groupAbsenceType.complationAbsenceBehaviour != null) {
      
      ComplationAbsenceBehaviour complationBehaviour = 
          absencePeriod.groupAbsenceType.complationAbsenceBehaviour;
      
      absencePeriod.complationAmountType = complationBehaviour.amountType;
      absencePeriod.complationCodes = complationBehaviour.complationCodes;
      // i codici di rimpiazzamento li preparo ordinati per ammontare di rimpiazzamento
      for (AbsenceType absenceType : complationBehaviour.replacingCodes) {
        int amount = absenceEngineUtility.replacingAmount(absenceType, 
            absencePeriod.complationAmountType);
        if (amount < 1) {
          absencePeriod.errorsBox.addCriticalError(date, absenceType, 
              CriticalProblem.IncalcolableReplacingAmount);
          continue;
        }
        if (absencePeriod.replacingCodesDesc.get(amount) != null) {
          AbsenceType conflictingType = absencePeriod.replacingCodesDesc.get(amount);
          absencePeriod.errorsBox.addCriticalError(date, absenceType, conflictingType, 
              CriticalProblem.ConflictingReplacingAmount);
          continue;
        }
        absencePeriod.replacingCodesDesc.put(amount, absenceType);
        absencePeriod.replacingTimes.put(absenceType, amount);
      }
    }
    
    return absencePeriod;
  }
  
  /**
   * Aggiunge ai period tutte le assenze prese e ne calcola l'integrità.
   * Popola il report con tutti gli errori riscontrati.
   * @param first
   * @param absenceEngine
   * @param absence
   */
  private void populatePeriodChain(PeriodChain periodChain, 
      List<Absence> involvedAbsencesInGroup, Absence absenceToInsert,
      List<Absence> previousInsert) {
    
    for (AbsencePeriod absencePeriod : periodChain.periods) {
      
      //Dispatch di tutte le assenze coinvolte gruppo e inserimenti precedenti
      for (Absence absence : absencePeriod
          .filterAbsencesInPeriod(involvedAbsencesInGroup)) {
        dispatchAbsenceInPeriod(periodChain, absencePeriod, absence);
      }
      
      //Gestione assenza da inserire (per ultima)
      boolean successInsertInPeriod = 
          insertAbsenceInPeriod(periodChain, absencePeriod, absenceToInsert, previousInsert);

      // Se la situazione non è compromessa eseguo lo scan dei rimpiazzamenti
      if (!absencePeriod.containsCriticalErrors() && !absencePeriod.compromisedTwoComplation) {
        absencePeriod.computeCorrectReplacingInPeriod(absenceEngineUtility, absenceComponentDao);
      }
      
      if (successInsertInPeriod) {
        previousInsert.add(absenceToInsert);
        periodChain.successPeriodInsert = absencePeriod;
        return;
      }
    } 
    
    //Le assenze non assegnate
    for (Absence absence : involvedAbsencesInGroup) {
      if (!periodChain.involvedAbsences.contains(absence)) {
        periodChain.orphanAbsences.add(absence);
      }
    }
    
    // Se il gruppo è compromesso tutte le sue assenze sono compromesse
    if (periodChain.containsCriticalErrors()) {
      for (AbsencePeriod absencePeriod : periodChain.periods) {
        for (Absence absence : absencePeriod
            .filterAbsencesInPeriod(Lists.newArrayList(periodChain.involvedAbsences))) {
          absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.ImplementationProblem);
        }
      }
    }
   
  }

  private void dispatchAbsenceInPeriod(PeriodChain periodChain, 
      AbsencePeriod absencePeriod, 
      Absence absence) {
    
    //se il period ha problemi critici esco
    if (absencePeriod.containsCriticalErrors()) {
      return;
    }
    
    //computo il ruolo dell'assenza nel period
    boolean isTaken = false, isComplation = false, isReplacing = false;
    if (absencePeriod.isTakable()) {
      //isTaken = absencePeriod.takenCodes.contains(absence.absenceType);   // no insert
      isTaken = absencePeriod.takableCodes.contains(absence.absenceType); // insert
    }
    if (absencePeriod.isComplation()) {
      isReplacing = absencePeriod.replacingCodesDesc.values()
          .contains(absence.absenceType);
      isComplation = absencePeriod.complationCodes.contains(absence.absenceType);
    }
    if (!isTaken && !isComplation && !isReplacing) {
      return; //ex 23 appartiene al 100% ma non al 30% quindi non ha ruolo
    }

    //una tipo di assenza può essere di rimpiazzamento e nient'altro
    if (isReplacing && (isComplation || isTaken)) {
      absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.ImplementationProblem);
      absencePeriod.errorsBox.addCriticalError(absence, CriticalProblem.OnlyReplacingRuleViolated);
      return;  
    }

    //una assenza con un ruolo può essere assegnata ad un solo period
    if (isTaken || isComplation) {
      if (periodChain.involvedAbsences.contains(absence)) {
        absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.ImplementationProblem);
        absencePeriod.errorsBox.addCriticalError(absence, CriticalProblem.TwoPeriods);
        return;            
      } else {
        periodChain.involvedAbsences.add(absence);  
      }
    } 

    if (isTaken) {
      int takenAmount = absenceEngineUtility
          .absenceJustifiedAmount(absencePeriod.person, absence, absencePeriod.takeAmountType);
      if (takenAmount < 0) {
        absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.ImplementationProblem);
        absencePeriod.errorsBox.addCriticalError(absence, CriticalProblem.IncalcolableJustifiedAmount);
        return;
      }
      TakenAbsence takenAbsence = absencePeriod.buildTakenAbsence(absence, takenAmount);
      if (!takenAbsence.canAddTakenAbsence()) {
        absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.LimitExceeded);
        absencePeriod.setLimitExceededDate(absence.getAbsenceDate());
      }  
      absencePeriod.addTakenAbsence(takenAbsence);
    }

    if (isComplation) {
      if (absencePeriod.compromisedTwoComplation) {
        absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.CompromisedTwoComplation);
      }
      int complationAmount = absenceEngineUtility.absenceJustifiedAmount(absencePeriod.person, 
          absence, absencePeriod.complationAmountType);
      if (complationAmount <= 0) {
        absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.ImplementationProblem);
        absencePeriod.errorsBox.addCriticalError(absence, CriticalProblem.IncalcolableJustifiedAmount);
        return;
      }
      absencePeriod.addComplationAbsence(absence);
    }

    if (isReplacing) {
      if (absencePeriod.compromisedTwoComplation) {
        absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.CompromisedTwoComplation);
      }
      absencePeriod.addReplacingAbsence(absence);
    }
  }
  
  /**
   * Ritorna true se sono riuscito ad inserire l'assenza nel periodo senza alcun errore.
   * @param periodChain
   * @param absencePeriod
   * @param absenceToInsert
   * @param previousInserts
   * @return
   */
  private boolean insertAbsenceInPeriod(PeriodChain periodChain, AbsencePeriod absencePeriod, 
      Absence absenceToInsert, List<Absence> previousInserts) {
    if (absenceToInsert == null) {
      return false;
    }
    if (!DateUtility.isDateIntoInterval(absenceToInsert.getAbsenceDate(), 
        absencePeriod.periodInterval())) {
      return false;
    }
      
    if (absenceToInsert.absenceType == null) {
      absenceToInsert = absenceEngineUtility.inferAbsenceType(absencePeriod, absenceToInsert);
      if (absenceToInsert.absenceType == null) {
        return false;
      }
    }
    
    // i vincoli generici
    absenceEngineUtility.genericConstraints(absencePeriod.errorsBox, 
        periodChain.person, absenceToInsert, 
        periodChain.allInvolvedAbsences);
    
    // i vincoli dentro il periodo
    absencePeriod.attemptedInsertAbsence = absenceToInsert;
    dispatchAbsenceInPeriod(periodChain, absencePeriod, absenceToInsert);
    
    // gli esiti
    if (absencePeriod.errorsBox.containsCriticalErrors()) {
      return false;
    }
    if (absencePeriod.errorsBox.containAbsenceErrors(absenceToInsert)) {
      return false;
    }
    return true;

  }
  
 
  
}
