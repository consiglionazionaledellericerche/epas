package manager.services.absences.model;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.errors.AbsenceTypeError;
import manager.services.absences.errors.CriticalError;

import models.Contract;
import models.Person;
import models.PersonChildren;
import models.absences.Absence;
import models.absences.AbsenceTrouble;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceTrouble.AbsenceTypeProblem;
import models.absences.AbsenceTrouble.RequestProblem;
import models.absences.AbsenceType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import javax.inject.Inject;

public class PeriodChainFactory {

  private AbsenceEngineUtility absenceEngineUtility;
  private AbsenceComponentDao absenceComponentDao;

  @Inject
  PeriodChainFactory(AbsenceEngineUtility absenceEngineUtility, 
      AbsenceComponentDao absenceComponentDao) {
    this.absenceEngineUtility = absenceEngineUtility;
    this.absenceComponentDao = absenceComponentDao;
  }
  
  
//  public AbsenceEngine buildInsertAbsenceEngine(Person person, GroupAbsenceType groupAbsenceType,
//      LocalDate from, LocalDate to) {
//    
//    AbsenceEngine absenceEngine = new AbsenceEngine(person, absenceComponentDao, this, personChildrenDao);
//
//    AbsenceEngineRequest request = new AbsenceEngineRequest();
//    request.absenceEngine = absenceEngine;
//    request.absenceEngineUtility = this;
//    request.group = groupAbsenceType;
//    request.from = from;
//    request.to = to;
//    absenceEngine.request = request;
//    
//    absenceEngine.report = new AbsencesReport();
//    
//    if (groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.vacationsCnr) || 
//        groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.compensatoryRestCnr)) {
//      
//      absenceEngine.report.addImplementationProblem(ImplementationError.builder()
//          .implementationProblem(ImplementationProblem.UnimplementedTakableComplationGroup)
//          .build());
//      return absenceEngine;
//    }
//    
//    absenceEngine.request.configureNextInsert();
//   
//    return absenceEngine;
//  }
  
///**
//* 
//* @param person
//* @param groupAbsenceType
//* @param from
//* @return
//*/
//public PeriodChain buildResidualInstance(Person person, GroupAbsenceType groupAbsenceType, LocalDate date, 
//   List<PersonChildren> orderedChildren, 
//   List<Contract> fetchedContracts) {
// 
// AbsenceEngine absenceEngine = buildInsertAbsenceEngine(person, groupAbsenceType, date, null);
// 
// if (groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.vacationsCnr) || 
//     groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.compensatoryRestCnr)) {
//
//   absenceEngine.report.addImplementationProblem(ImplementationError.builder()
//       .implementationProblem(ImplementationProblem.UnimplementedTakableComplationGroup)
//       .build());
//   return null; //??
// }
//
// return buildPeriodChain(person, groupAbsenceType, date, null, orderedChildren, fetchedContracts);
// 
//}
  
  
  /**
   * Costruttore per richiesta di scan.
   * @param person
   * @param scanFrom
   * @param absencesToScan
   * @return
   */
  public AbsenceEngineScan buildScanInstance(Person person, LocalDate scanFrom, 
      List<Absence> absencesToScan, List<PersonChildren> orderedChildren, 
      List<Contract> fetchedContracts) {
    AbsenceEngineScan absenceEngineScan = new AbsenceEngineScan(person, scanFrom, 
        orderedChildren, fetchedContracts, 
        this, absenceEngineUtility);
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
      List<PersonChildren> orderedChildren, List<Contract> fetchedContracts) { 
    
    PeriodChain periodChain = new PeriodChain(person, date);
    
    //Primo absencePeriod
    AbsencePeriod currentPeriod = buildAbsencePeriod(person, groupAbsenceType, date, 
        orderedChildren, fetchedContracts);
    if (this.report.containsCriticalProblems()) {
      return periodChain;
    }
    periodChain.periods.add(currentPeriod);
    while (currentPeriod.groupAbsenceType.nextGroupToCheck != null) {
      //successivi
      currentPeriod = buildAbsencePeriod(person, currentPeriod.groupAbsenceType.nextGroupToCheck, date, 
          orderedChildren, fetchedContracts);
      if (this.report.containsCriticalProblems()) {
        return periodChain;
      }
      periodChain.periods.add(currentPeriod);
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
    // Fix caso in cui l'intervallo dipende da un figlio e passo una data fuori intervallo.
    if (!DateUtility.isDateIntoInterval(date, new DateInterval(periodChain.from, periodChain.to))) {
      if (groupAbsenceType.periodType.isChildPeriod()) { 
        this.report.addRequestProblem(CriticalError.builder()
            .requestProblem(CriticalProblem.NoChildExists)
            .date(date)
            .build());
        return periodChain;
      }
    }
    
    //fetch delle assenze
    fetchPeriodChainAbsencesAsc(periodChain, previousInserts);

    // Assegnare ad ogni periodo le assenze di competenza e calcoli
    populatePeriodChain(periodChain);
 
    return periodChain;
  }
  
  /**
   * Le assenze della catena in caso di richiesta inserimento (sono dinamiche
   * in quanto contengono anche le assenza inserite fino all'iterata 
   * precedente).
   * @return
   */
  private PeriodChain fetchPeriodChainAbsencesAsc(PeriodChain periodChain, List<Absence> previousInserts) {
    
    //I tipi coinvolti...
    Set<AbsenceType> absenceTypes = periodChain.periodChainInvolvedCodes();
    if (absenceTypes.isEmpty()) {
      periodChain.absencesAsc = Lists.newArrayList();
      periodChain.allCodeAbsencesAsc = Lists.newArrayList();
      return periodChain;
    }

    //Le assenze preesistenti 
    List<Absence> periodAbsences = absenceComponentDao.orderedAbsences(
       periodChain.person, periodChain.from, periodChain.to, Lists.newArrayList(absenceTypes));
    // e quelle precedentemente inserite
    if (previousInserts != null) {
      periodAbsences.addAll(previousInserts);
    }
    
    //Le ordino tutte per data...
    SortedMap<LocalDate, List<Absence>> sortedAbsenceMap = Maps.newTreeMap();
    for (Absence absence : periodAbsences) {
      Verify.verifyNotNull(absence.justifiedType == null );     //rimuovere..
      List<Absence> absences = sortedAbsenceMap.get(absence.getAbsenceDate());
      if (absences == null) {
        absences = Lists.newArrayList();
        sortedAbsenceMap.put(absence.getAbsenceDate(), absences);
      }
      absences.add(absence);
    }
    
    //Popolo la lista definitiva
    periodChain.absencesAsc = Lists.newArrayList();
    for (List<Absence> absences : sortedAbsenceMap.values()) {
      periodChain.absencesAsc.addAll(absences);
    }
    
    //Popolo la lista con tutte le assenze coinvolte nel periodo.
    periodChain.allCodeAbsencesAsc = absenceComponentDao.orderedAbsences(
        periodChain.person, periodChain.from, periodChain.to, Lists.newArrayList());
    if (previousInserts != null) {
      periodAbsences.addAll(previousInserts);
    }
    
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
    }

    // Caso inerente i figli.
    else if (absencePeriod.groupAbsenceType.periodType.isChildPeriod()) {
      try {
        DateInterval childInterval = absencePeriod.groupAbsenceType.periodType
            .getChildInterval(orderedChildren.get(absencePeriod.groupAbsenceType.periodType.childNumber - 1).bornDate);
        absencePeriod.from = childInterval.getBegin();
        absencePeriod.to = childInterval.getEnd();

      } catch (Exception e) {
        this.report.addRequestProblem(CriticalError.builder()
            .requestProblem(CriticalProblem.NoChildExists)
            .date(date)
            .build());
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
          this.report.addAbsenceTypeProblem(AbsenceTypeError.builder()
              .absenceTypeProblem(AbsenceTypeProblem.IncalcolableReplacingAmount)
              .absenceType(absenceType)
              .build());
          continue;
        }
        if (absencePeriod.replacingCodesDesc.get(amount) != null) {
          this.report.addAbsenceTypeProblem(AbsenceTypeError.builder()
              .absenceTypeProblem(AbsenceTypeProblem.ConflictingReplacingAmount)
              .absenceType(absenceType)
              .conflictingAbsenceType(absencePeriod.replacingCodesDesc.get(amount))
              .build());
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
  private void populatePeriodChain(PeriodChain periodChain) {
    
    Set<Absence> absencesAlreadyAssigned = Sets.newHashSet();
    
    for (AbsencePeriod absencePeriod : periodChain.periods) {
      
      for (Absence absence : periodChain.absencesAsc) {
        if (!DateUtility.isDateIntoInterval(absence.getAbsenceDate(), 
            absencePeriod.periodInterval())) {
          continue;
        }
        
        //Se il gruppo ha una assenza precedente compromessa (con errori) allora
        // tutte le successive sono compromesse.
        if (this.report.containsCriticalProblems()) {
          this.report.addAbsenceTrouble(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.CompromisedTakableComplationGroup)
              .absence(absence)
              .build());
          continue;
        }
        
        //Se il suo tipo ha un errore 
        if (this.report.absenceTypeHasProblem(absence.absenceType)) {
          this.report.addAbsenceTrouble(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.AbsenceTypeProblem)
              .absence(absence)
              .build());
        }
        
        LocalDate date = null;
        
        //Computo il ruolo dell'assenza nel period
        boolean isTaken = false, isComplation = false, isReplacing = false;
        if (absencePeriod.isTakable()) {
          isTaken = absencePeriod.takenCodes.contains(absence.absenceType);
        }
        if (absencePeriod.isComplation()) {
          isReplacing = absencePeriod.replacingCodesDesc.values()
              .contains(absence.absenceType);
          isComplation = absencePeriod.complationCodes.contains(absence.absenceType);
        }
        
        //una assenza senza ruolo nel gruppo la ignoro
        if (!isTaken && !isComplation && !isReplacing) {
//          this.report.addAbsenceAndImplementationProblem(AbsenceTrouble.builder()
//              .trouble(AbsenceProblem.UselessAbsenceInPeriod)
//              .absence(absence)
//              .build());
          continue;
        }
        
        //una assenza può essere assegnata ad un solo period
        if (absencesAlreadyAssigned.contains(absence)) {
          this.report.addAbsenceAndImplementationProblem(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.TwoPeriods)
              .absence(absence)
              .build());
          continue;
        }
        
        //una tipo di assenza può essere di rimpiazzamento e nient'altro
        if (isReplacing && (isComplation || isTaken)) {
          this.report.addAbsenceTypeProblem(AbsenceTypeError.builder()
              .absenceTypeProblem(AbsenceTypeProblem.OnlyReplacingRuleViolated)
              .absenceType(absence.getAbsenceType())
              .build());
          continue;  
        }
        if (isTaken || isComplation || isReplacing) {
          absencesAlreadyAssigned.add(absence);
          date = absence.getAbsenceDate();
        }
        
        //controllo assenza taken
        if (isTaken) {
          int takenAmount = absenceEngineUtility
              .absenceJustifiedAmount(absencePeriod.person, absence, absencePeriod.takeAmountType);
          if (takenAmount < 0) {
            this.report.addAbsenceAndImplementationProblem(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.IncalcolableJustifiedAmount)
                .absence(absence)
                .build());
            continue;
          }
          if (!absencePeriod.canAddTakenAmount(takenAmount)) {
            this.report.addAbsenceTrouble(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.LimitExceeded)
                .absence(absence)
                .build());
          }
          absencePeriod.addAbsenceTaken(absence, takenAmount);
        }
        
        if (isComplation) {
          Absence previous = absencePeriod.complationAbsencesByDay.get(date);
          if (previous != null ) {
            //una sola assenza di completamento per quel giorno
            this.report.addAbsenceTrouble(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.TwoComplationSameDay)
                .absence(absence)
                .build());
            this.report.addAbsenceTrouble(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.TwoComplationSameDay)
                .absence(previous)
                .build());
            absencePeriod.setCompromisedReplacingDate(absence.getAbsenceDate());
            absencePeriod.addComplationSameDay(previous);
            absencePeriod.addComplationSameDay(absence);
            continue;
          }
          
          int complationAmount = absenceEngineUtility.absenceJustifiedAmount(absencePeriod.person, 
              absence, absencePeriod.complationAmountType);
          
          if (complationAmount <= 0) {
            this.report.addAbsenceAndImplementationProblem(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.IncalcolableJustifiedAmount)
                .absence(absence)
                .build());
            continue;
          }
          absencePeriod.complationAbsencesByDay.put(date, absence);
        }
        if (isReplacing) {
          Absence previous = absencePeriod.replacingAbsencesByDay.get(date);
          if (previous != null) {
            //una sola assenza di rimpiazzamento per quel giorno
            this.report.addAbsenceTrouble(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.TwoReplacingSameDay)
                .absence(absence)
                .build());
            this.report.addAbsenceTrouble(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.TwoReplacingSameDay)
                .absence(previous)
                .build());
            absencePeriod.setCompromisedReplacingDate(absence.getAbsenceDate());
            absencePeriod.addReplacingSameDay(previous);
            absencePeriod.addReplacingSameDay(absence);
            continue;
          }
          absencePeriod.replacingAbsencesByDay.put(date, absence);
        }
      }
      
      absencePeriod.computeReplacingStatusInPeriod(absenceEngineUtility);

      //Una volta calcolati i rimpiazzamenti esistenti e ipotetici aggiungo i report
      for (DayStatus dayStatus : absencePeriod.daysStatus.values()) {
        //Errore nel rimpiazzo
        if (dayStatus.wrongTypeOfReplacing()) {
          this.report.addAbsenceTrouble(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.WrongReplacing)
              .absence(dayStatus.getExistentReplacing())
              .build());
          absencePeriod.setCompromisedReplacingDate(dayStatus.getDate());
          break;
        }
        if (dayStatus.tooEarlyReplacing()) {
          this.report.addAbsenceTrouble(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.TooEarlyReplacing)
              .absence(dayStatus.getExistentReplacing())
              .build());
          absencePeriod.setCompromisedReplacingDate(dayStatus.getDate());
          break;
        }
        //Errore nel completamento
        if (dayStatus.missingReplacing()) {
          absencePeriod.setCompromisedReplacingDate(dayStatus.getDate());
          this.report.addAbsenceTrouble(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.MissingReplacing)
              .absence(absencePeriod.complationAbsencesByDay
                  .get(dayStatus.getDate()))
              .build());
          break;
        }

      }

      //Se la catena è compromessa tutte le assenza completamento e replacing
      // successive sono taggate con l'errore
      for (Absence complationAbsence : absencePeriod.complationAbsencesByDay.values()) {
        if (absencePeriod.isAbsenceCompromisedReplacing(complationAbsence)) {
          this.report.addAbsenceTrouble(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.CompromisedReplacing)
              .absence(complationAbsence)
              .build());
        }
      }
      for (Absence replacingAbsence : absencePeriod.replacingAbsencesByDay.values()) {
        if (absencePeriod.isAbsenceCompromisedReplacing(replacingAbsence)) {
          this.report.addAbsenceTrouble(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.CompromisedReplacing)
              .absence(replacingAbsence)
              .build());
        }
      }
      
    } //ends absencePeriod
  }
  
 
  
}
