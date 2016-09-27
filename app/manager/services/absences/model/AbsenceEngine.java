package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.model.AbsencesReport.ReportAbsenceTypeProblem;
import manager.services.absences.model.AbsencesReport.ReportRequestProblem;

import models.Contract;
import models.ContractWorkingTimeType;
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

public class AbsenceEngine {
  
  //Dependencies Injected
  public AbsenceComponentDao absenceComponentDao;
  public PersonChildrenDao personChildrenDao;
  public AbsenceEngineUtility absenceEngineUtility;

  public Person person;
  public List<PersonChildren> childrenAsc = null; //all children   
  public DateInterval childInterval;

  // Richiesta inserimento  
  public AbsenceEngineRequest request;

  // Richiesta scan
  public AbsenceEngineScan scan;
  
  // AbsencePeriod Chain
  public PeriodChain periodChain;

  // Risultato richiesta
  public AbsencesReport report;

  
  //Boh
  //private InitializationGroup initializationGroup = null;
  

  
  public AbsenceEngine(Person person, AbsenceComponentDao absenceComponentDao,
      AbsenceEngineUtility absenceEngineUtility, PersonChildrenDao personChildrenDao) {
    this.person = person;
    this.absenceComponentDao = absenceComponentDao;
    this.absenceEngineUtility = absenceEngineUtility;
    this.personChildrenDao = personChildrenDao;
  }

  public boolean isRequestEngine() {
    return request != null;
  }
  
  public boolean isScanEngine() {
    return request == null;
  }
  
  public GroupAbsenceType engineGroup() {
    if (isRequestEngine()) {
      return this.request.group;
    } 
    if (isScanEngine()) {
      return this.scan.currentGroup;
    }
    return null;
  }
  
  public List<PersonChildren> orderedChildren() {
    if (this.childrenAsc == null) {
      this.childrenAsc = personChildrenDao.getAllPersonChildren(this.person);
    }
    return this.childrenAsc;
  }
  
  public int workingTime(LocalDate date) {
    for (Contract contract : this.periodChain.contracts) {
      for (ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {
        if (DateUtility.isDateIntoInterval(date, cwtt.periodInterval())) {
          if (cwtt.workingTimeType.workingTimeTypeDays.get(date.getDayOfWeek() - 1).holiday) {
            return 0;
          }
          return cwtt.workingTimeType.workingTimeTypeDays.get(date.getDayOfWeek() - 1)
              .workingTime;
        }
      }
    }
    return 0;
  }
  
  /**
   * Costruisce la catena dei periodi per il gruppo e la data passati.
   * Calcola anche la soundness del periodo e mette nel report eventuali errori.
   * @param absenceEngine
   * @param groupAbsenceType
   * @param date
   * @return
   */
  public void buildPeriodChain(GroupAbsenceType groupAbsenceType, LocalDate date) { 
    
    PeriodChain periodChain = this.periodChain;
    
    if (periodChain != null) {
      // TODO: Implementare logica di verifica data 
      // richiesta compatibile col precedente absencePeriod  
    } 
      //prima iterata
    periodChain = new PeriodChain(this);
    this.periodChain = periodChain;
    
    //Primo absencePeriod
    AbsencePeriod currentPeriod = buildAbsencePeriod(periodChain, groupAbsenceType, date);
    if (this.report.containsCriticalProblems()) {
      return ;
    }
    periodChain.periods.add(currentPeriod);
    while (currentPeriod.groupAbsenceType.nextGroupToCheck != null) {
      //successivi
      currentPeriod = buildAbsencePeriod(periodChain, currentPeriod.groupAbsenceType.nextGroupToCheck, date);
      if (this.report.containsCriticalProblems()) {
        return;
      }
      periodChain.periods.add(currentPeriod);
    }
    
    //Altre informazioni da calcolare / reinizializzare una volta ottenuti gli absencePeriods.
    periodChain.success = false;
    periodChain.absencesAsc = null;
    
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
    periodChain.contracts = Lists.newArrayList();
    for (Contract contract : this.person.contracts) {
      if (DateUtility.intervalIntersection(
          contract.periodInterval(), new DateInterval(periodChain.from, periodChain.to)) != null) {
        periodChain.contracts.add(contract);
      }
    }
    
    //fetch delle assenze
    periodChain.fetchPeriodChainAbsencesAsc();

    // Assegnare ad ogni periodo le assenze di competenza (fase da migliorare) e calcoli
    populatePeriodChain();
 
    return;
  }
  
  private AbsencePeriod buildAbsencePeriod(PeriodChain periodChain, GroupAbsenceType groupAbsenceType, LocalDate date) {
    // recuperare l'inizializzazione (questo lo posso fare anche fuori) per i fix sulle date.
    
    AbsencePeriod absencePeriod = new AbsencePeriod(periodChain, groupAbsenceType);
    
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
        this.childInterval = absencePeriod.groupAbsenceType.periodType
            .getChildInterval(this.orderedChildren()
                .get(absencePeriod.groupAbsenceType.periodType.childNumber - 1).bornDate);
        absencePeriod.from = this.childInterval.getBegin();
        absencePeriod.to = this.childInterval.getEnd();

      } catch (Exception e) {
        this.report.addRequestProblem(ReportRequestProblem.builder()
            .requestProblem(RequestProblem.NoChildExists)
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
          this.report.addAbsenceTypeProblem(ReportAbsenceTypeProblem.builder()
              .absenceTypeProblem(AbsenceTypeProblem.IncalcolableReplacingAmount)
              .absenceType(absenceType)
              .build());
          continue;
        }
        if (absencePeriod.replacingCodesDesc.get(amount) != null) {
          this.report.addAbsenceTypeProblem(ReportAbsenceTypeProblem.builder()
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
  private void populatePeriodChain() {
    
    Set<Absence> absencesAlreadyAssigned = Sets.newHashSet();
    
    for (AbsencePeriod absencePeriod : this.periodChain.periods) {
      
      for (Absence absence : this.periodChain.absencesAsc) {
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
          this.report.addAbsenceTypeProblem(ReportAbsenceTypeProblem.builder()
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
              .absenceJustifiedAmount(this, absence, absencePeriod.takeAmountType);
          if (takenAmount <= 0) {
            this.report.addAbsenceTrouble(AbsenceTrouble.builder()
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
          
          int complationAmount = absenceEngineUtility.absenceJustifiedAmount(this, 
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
      
      absencePeriod.computeReplacingStatusInPeriod();

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