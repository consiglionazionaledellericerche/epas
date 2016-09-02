package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.extern.slf4j.Slf4j;

import manager.PersonDayManager;
import manager.services.absences.AbsenceService.AbsenceRequestType;
import manager.services.absences.AbsencesReport.ReportAbsenceProblem;
import manager.services.absences.AbsencesReport.ReportAbsenceTypeProblem;
import manager.services.absences.AbsencesReport.ReportImplementationProblem;
import manager.services.absences.AbsencesReport.ReportRequestProblem;
import manager.services.absences.InsertResultItem.Operation;
import manager.services.absences.model.AbsenceEngine;
import manager.services.absences.model.AbsencePeriod;
import manager.services.absences.model.AbsencePeriod.ComplationComponent;
import manager.services.absences.model.AbsencePeriod.ComplationComponent.ReplacingDay;
import manager.services.absences.model.AbsencePeriod.TakableComponent;
import manager.services.absences.model.PeriodChain;

import models.Contract;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceTrouble;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceTrouble.AbsenceTypeProblem;
import models.absences.AbsenceTrouble.ImplementationProblem;
import models.absences.AbsenceTrouble.RequestProblem;
import models.absences.AbsenceType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

@Slf4j
public class AbsenceEngineCore {
  
  private final AbsenceEngineUtility absenceEngineUtility;
  private final PersonDayManager personDayManager;
  private final AbsenceComponentDao absenceComponentDao;
  private final PersonChildrenDao personChildrenDao;

  @Inject
  public AbsenceEngineCore(AbsenceComponentDao absenceComponentDao, 
      PersonChildrenDao personChildrenDao, AbsenceEngineUtility absenceEngineUtility, 
      PersonDayManager personDayManager) {
    this.absenceComponentDao = absenceComponentDao;
    this.personChildrenDao = personChildrenDao;
    this.absenceEngineUtility = absenceEngineUtility;
    this.personDayManager = personDayManager;
  }
  
  public AbsenceEngine buildInsertAbsenceEngine(Person person, GroupAbsenceType groupAbsenceType,
      LocalDate from, LocalDate to) {
    
    AbsenceEngine absenceEngine = new AbsenceEngine(absenceComponentDao, personChildrenDao, 
        absenceEngineUtility, person, groupAbsenceType, from, to);
    
    if (groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.vacationsCnr) || 
        groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.compensatoryRestCnr)) {
      
      absenceEngine.report.addImplementationProblem(ReportImplementationProblem.builder()
          .implementationProblem(ImplementationProblem.UnimplementedTakableComplationGroup)
          .build());
      return absenceEngine;
    }
    
    configureNext(absenceEngine);
   
    return absenceEngine;
  }
  
  /**
   * Configura il prossimo inserimento da effettuare.
   * @param absenceEngine
   * @return
   */
  public AbsenceEngine configureNext(AbsenceEngine absenceEngine) {
    
    absenceEngine.request.nextDate(absenceEngine);
    if (absenceEngine.request.currentDate == null) {
      return absenceEngine;
    }
    buildPeriodChain(absenceEngine, absenceEngine.request.group, absenceEngine.request.currentDate);
    return absenceEngine;
  }
  
  /**
   * Costruisce la catena dei periodi per il gruppo e la data passati.
   * Calcola anche la soundness del periodo e mette nel report eventuali errori.
   * @param absenceEngine
   * @param groupAbsenceType
   * @param date
   * @return
   */
  private PeriodChain buildPeriodChain(AbsenceEngine absenceEngine, 
      GroupAbsenceType groupAbsenceType, LocalDate date) { 
    
    PeriodChain periodChain = absenceEngine.periodChain;
    
    if (periodChain != null) {
      // TODO: Implementare logica di verifica data 
      // richiesta compatibile col precedente absencePeriod  
    } 
      //prima iterata
    periodChain = new PeriodChain();
    absenceEngine.periodChain = periodChain;
    
    //Primo absencePeriod
    AbsencePeriod currentPeriod = buildAbsencePeriod(absenceEngine, groupAbsenceType, date);
    if (absenceEngine.report.containsProblems()) {
      return periodChain;
    }
    periodChain.periods.add(currentPeriod);
    while (currentPeriod.groupAbsenceType.nextGroupToCheck != null) {
      //successivi
      currentPeriod = buildAbsencePeriod(absenceEngine, currentPeriod.groupAbsenceType.nextGroupToCheck, date);
      if (absenceEngine.report.containsProblems()) {
        return periodChain;
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
    for (Contract contract : absenceEngine.person.contracts) {
      if (DateUtility.intervalIntersection(
          contract.periodInterval(), new DateInterval(periodChain.from, periodChain.to)) != null) {
        periodChain.contracts.add(contract);
      }
    }
    
    //fetch delle assenze
    periodChain.fetchPeriodChainAbsencesAsc(absenceEngine);

    // Assegnare ad ogni periodo le assenze di competenza (fase da migliorare) e calcoli
    populatePeriodChain(absenceEngine);
 
    return periodChain;
  }
  
  private AbsencePeriod buildAbsencePeriod(AbsenceEngine absenceEngine, 
      GroupAbsenceType groupAbsenceType, LocalDate date) {
    // recuperare l'inizializzazione (questo lo posso fare anche fuori) per i fix sulle date.
    
    AbsencePeriod absencePeriod = new AbsencePeriod(groupAbsenceType);
    
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
        absenceEngine.childInterval = absencePeriod.groupAbsenceType.periodType
            .getChildInterval(absenceEngine.orderedChildren()
                .get(absencePeriod.groupAbsenceType.periodType.childNumber - 1).bornDate);
        absencePeriod.from = absenceEngine.childInterval.getBegin();
        absencePeriod.to = absenceEngine.childInterval.getEnd();

      } catch (Exception e) {
        absenceEngine.report.addRequestProblem(ReportRequestProblem.builder()
            .requestProblem(RequestProblem.NoChildExists)
            .date(date)
            .build());
        return absencePeriod;
      }
    }
    
    absencePeriod.takableComponent = Optional.absent();
    absencePeriod.complationComponent = Optional.absent();
    
    // Parte takable
    if (absencePeriod.groupAbsenceType.takableAbsenceBehaviour != null) {

      TakableAbsenceBehaviour takableBehaviour = 
          absencePeriod.groupAbsenceType.takableAbsenceBehaviour;

      TakableComponent takableComponent = new TakableComponent();
      takableComponent.takeAmountType = takableBehaviour.amountType;

      takableComponent.setFixedPeriodTakableAmount(takableBehaviour.fixedLimit);
      if (takableBehaviour.takableAmountAdjustment != null) {
        // TODO: ex. workingTimePercent
        //bisogna ridurre il limite
        //engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
      }

      takableComponent.takableCountBehaviour = TakeCountBehaviour.period;
      takableComponent.takenCountBehaviour = TakeCountBehaviour.period;

      takableComponent.takenCodes = takableBehaviour.takenCodes;
      takableComponent.takableCodes = takableBehaviour.takableCodes;

      absencePeriod.takableComponent = Optional.of(takableComponent);
    }
    
    if (absencePeriod.groupAbsenceType.complationAbsenceBehaviour != null) {
      
      ComplationAbsenceBehaviour complationBehaviour = 
          absencePeriod.groupAbsenceType.complationAbsenceBehaviour;
      
      ComplationComponent complationComponent = new ComplationComponent();
      complationComponent.complationAmountType = complationBehaviour.amountType;
      complationComponent.complationCodes = complationBehaviour.complationCodes;
      // i codici di rimpiazzamento li preparo ordinati per ammontare di rimpiazzamento
      for (AbsenceType absenceType : complationBehaviour.replacingCodes) {
        int amount = absenceEngineUtility.replacingAmount(absenceEngine, absenceType, 
            complationComponent.complationAmountType);
        if (amount < 1) {
          absenceEngine.report.addAbsenceTypeProblem(ReportAbsenceTypeProblem.builder()
              .absenceTypeProblem(AbsenceTypeProblem.IncalcolableReplacingAmount)
              .absenceType(absenceType)
              .build());
          continue;
        }
        if (complationComponent.replacingCodesDesc.get(amount) != null) {
          absenceEngine.report.addAbsenceTypeProblem(ReportAbsenceTypeProblem.builder()
              .absenceTypeProblem(AbsenceTypeProblem.ConflictingReplacingAmount)
              .absenceType(absenceType)
              .conflictingAbsenceType(complationComponent.replacingCodesDesc.get(amount))
              .build());
          continue;
        }
        complationComponent.replacingCodesDesc.put(amount, absenceType);
        complationComponent.replacingTimes.put(absenceType, amount);
      }
      absencePeriod.complationComponent = Optional.of(complationComponent);
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
  private void populatePeriodChain(AbsenceEngine absenceEngine) {
    
    Set<Absence> absencesAlreadyAssigned = Sets.newHashSet();
    
    for (AbsencePeriod absencePeriod : absenceEngine.periodChain.periods) {
      
      TakableComponent takableComponent = null;
      ComplationComponent complationComponent = null;
      
      for (Absence absence : absenceEngine.periodChain.absencesAsc) {
        if (!DateUtility.isDateIntoInterval(absence.getAbsenceDate(), 
            absencePeriod.periodInterval())) {
          continue;
        }
        
        //Se il suo tipo ha un errore 
        if (absenceEngine.report.absenceTypeHasProblem(absence.absenceType)) {
          absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
              .absenceProblem(AbsenceProblem.AbsenceTypeProblem)
              .absence(absence)
              .build());
        }
        
        LocalDate date = null;
        
        //Computo il ruolo dell'assenza nel period
        boolean isTaken = false, isComplation = false, isReplacing = false;
        if (absencePeriod.takableComponent.isPresent()) {
          takableComponent = absencePeriod.takableComponent.get();
          isTaken = takableComponent.takenCodes.contains(absence.absenceType);
        }
        if (absencePeriod.complationComponent.isPresent()) {
          complationComponent = absencePeriod.complationComponent.get();
          isReplacing = complationComponent.replacingCodesDesc.values()
              .contains(absence.absenceType);
          isComplation = complationComponent.complationCodes.contains(absence.absenceType);
        }
        
        //una assenza deve avere un ruolo
        if (!isTaken && !isComplation && !isReplacing) {
          absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
              .absenceProblem(AbsenceProblem.UselessAbsenceInPeriod)
              .absence(absence)
              .build());
          continue;
        }
        
        //una assenza può essere assegnata ad un solo period
        if (absencesAlreadyAssigned.contains(absence)) {
          absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
              .absenceProblem(AbsenceProblem.TwoPeriods)
              .absence(absence)
              .build());
          continue;
        }
        
        //una tipo di assenza può essere di rimpiazzamento e nient'altro
        if (isReplacing && (isComplation || isTaken)) {
          absenceEngine.report.addAbsenceTypeProblem(ReportAbsenceTypeProblem.builder()
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
              .absenceJustifiedAmount(absenceEngine, absence, takableComponent.takeAmountType);
          if (takenAmount <= 0) {
            absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
                .absenceProblem(AbsenceProblem.IncalcolableJustifiedAmount)
                .absence(absence)
                .build());
            continue;
          }
          takenSoundness(absenceEngine, takableComponent, absence, takenAmount);
          takableComponent.addAbsenceTaken(absence, takenAmount);
        }
        
        if (isComplation) {
          Absence previous = complationComponent.complationAbsencesByDay.get(date);
          if (previous != null ) {
            //una sola assenza di completamento per quel giorno
            absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
                .absenceProblem(AbsenceProblem.TwoComplationSameDay)
                .absence(absence)
                .conflictingAbsence(previous)
                .build());
            continue;
          }
          
          int complationAmount = absenceEngineUtility.absenceJustifiedAmount(absenceEngine, 
              absence, complationComponent.complationAmountType);
          
          if (complationAmount <= 0) {
            absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
                .absenceProblem(AbsenceProblem.IncalcolableJustifiedAmount)
                .absence(absence)
                .build());
            continue;
          }
          complationComponent.complationAbsencesByDay.put(date, absence);
        }
        if (isReplacing) {
          Absence previous = complationComponent.replacingAbsencesByDay.get(date);
          if (previous != null) {
            //una sola assenza di rimpiazzamento per quel giorno
            absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
                .absenceProblem(AbsenceProblem.TwoReplacingSameDay)
                .absence(absence)
                .conflictingAbsence(previous)
                .build());
            continue;
          }
          complationComponent.replacingAbsencesByDay.put(date, absence);
        }
        
      }
      
      complationSoundness(absenceEngine, complationComponent);
    }
  }
  
  /**
   * Controlla se l'assenza eccede il limite. 
   * Aggiunge l'errore al report.
   * @param absenceEngine
   * @param takableComponent
   * @param enhancedAbsence
   * @return
   */
  private AbsenceEngine takenSoundness(AbsenceEngine absenceEngine, TakableComponent takableComponent, 
      Absence absence, int takenAmount) {
    if (!takableComponent.canAddTakenAmount(takenAmount)) {
      absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
          .absenceProblem(AbsenceProblem.LimitExceeded)
          .absence(absence)
          .build());
    }
    return absenceEngine;
  }
  
  private SortedMap<LocalDate, ReplacingDay> complationReplacingDays(AbsenceEngine absenceEngine, 
      ComplationComponent complationComponent) {
    
    //I replacing days per ogni data raccolgo i replacing effettivi e quelli corretti
    SortedMap<LocalDate, ReplacingDay> replacingDays = Maps.newTreeMap();
    
    //preparo i giorni con i replacing effettivi
    for (Absence replacingAbsence : complationComponent.replacingAbsencesByDay.values()) {
      replacingDays.put(replacingAbsence.getAbsenceDate(), 
          ReplacingDay.builder()
          .existentReplacing(replacingAbsence)
          .date(replacingAbsence.getAbsenceDate())
          .build());
    }

    //Le assenze di completamento ordinate per data. Genero i replacing ipotetici
    int complationAmount = 0;
    for (Absence complationAbsence : complationComponent.complationAbsencesByDay.values()) {
      
      int amount = absenceEngineUtility.absenceJustifiedAmount(absenceEngine, 
          complationAbsence, complationComponent.complationAmountType);
      
      complationAmount = complationAmount + amount;
      Optional<AbsenceType> replacingCode = absenceEngineUtility
          .whichReplacingCode(absenceEngine, complationComponent, 
              complationAbsence.getAbsenceDate(), complationAmount);
      if (replacingCode.isPresent()) {
        LocalDate replacingDate = complationAbsence.getAbsenceDate();
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
    
    return replacingDays;
  }
  
  /**
   * Controlla che tutti i rimpiazzamenti siano nella posizione corretta.
   * Aggiunge gli errori al report.
   * TODO: la struttura dati replacingDays potrebbe essere rimossa semplificando
   * di molto il codice.
   * @param first
   * @param absenceEngine
   * @param absence
   */
  private AbsenceEngine complationSoundness(AbsenceEngine absenceEngine, ComplationComponent complationComponent) {

    if (complationComponent == null) {
      return absenceEngine;
    }

    //Controllo che i replacing ipotetici collimino con quelli reali
    complationComponent.compromisedReplacingDate = null;

    for (ReplacingDay replacingDay : complationReplacingDays(absenceEngine, complationComponent).values()) {

      if (replacingDay.wrongType()) {
        absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
            .absenceProblem(AbsenceProblem.WrongReplacing)
            .absence(replacingDay.getExistentReplacing())
            .correctType(replacingDay.getCorrectReplacing()).build());
        complationComponent.compromisedReplacingDate = replacingDay.getDate();
        break;
      }
      if (replacingDay.onlyCorrect()) {
        complationComponent.compromisedReplacingDate = replacingDay.getDate();
        absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
            .absenceProblem(AbsenceProblem.MissingReplacing)
            .absence(complationComponent.complationAbsencesByDay
                .get(complationComponent.compromisedReplacingDate))
            .build());
        break;
      }
      if (replacingDay.onlyExisting()) {
        absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
            .absenceProblem(AbsenceProblem.TooEarlyReplacing)
            .absence(replacingDay.getExistentReplacing())
            .build());
        complationComponent.compromisedReplacingDate = replacingDay.getDate();
        break;
      }
    }

    //Se la catena è compromessa tutte le assenza completamento e replacing
    // successive sono taggate con l'errore
    if (complationComponent.compromisedReplacingDate != null) {
      for (Absence complationAbsence : complationComponent
          .complationAbsencesByDay.values()) {
        if (complationAbsence.getAbsenceDate()
            .isAfter(complationComponent.compromisedReplacingDate)) {
          absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
              .absenceProblem(AbsenceProblem.CompromisedReplacing)
              .absence(complationAbsence)
              .build());
        }
      }
      for (Absence replacingAbsence : complationComponent
          .replacingAbsencesByDay.values()) {
        if (replacingAbsence.getAbsenceDate()
            .isAfter(complationComponent.compromisedReplacingDate)) {
          absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
              .absenceProblem(AbsenceProblem.CompromisedReplacing)
              .absence(replacingAbsence)
              .build());
        }
      }
    }
    
    return absenceEngine;
  }

  /**
   * Effettual lo scan degli errori
   * di tutte le assenze della persona a partire dalla data scanFrom.
   * 
   * @param person
   * @param scanFrom
   * @return
   */
  public AbsenceEngine scannerAbsenceEngine(Person person, LocalDate scanFrom) {

    //OTTIMIZZAZIONI//
    
    //fetch all absenceType
    absenceComponentDao.fetchAbsenceTypes();

    //fetch all groupAbsenceType
    //List<GroupAbsenceType> absenceGroupTypes = absenceComponentDao.allGroupAbsenceType();
    absenceComponentDao.allGroupAbsenceType();
    
    //COSTRUZIONE//
    List<Absence> absencesToScan = absenceComponentDao.orderedAbsences(person, scanFrom, 
        null, Lists.newArrayList());
    AbsenceEngine absenceEngine = new AbsenceEngine(absenceComponentDao, personChildrenDao, 
        absenceEngineUtility, person, scanFrom, absencesToScan);

    // analisi dei requisiti generici (risultati in absenceEngine.report)
    for (Absence absence : absencesToScan) {
      genericConstraints(absenceEngine, absence, absencesToScan);
      log.debug("L'assenza data={}, codice={} è stata aggiunta a quelle da analizzare", 
          absence.getAbsenceDate(), absence.getAbsenceType().code);
    }
    
    // analisi dei requisiti all'interno di ogni gruppo (risultati in absenceEngine.report)
    Iterator<Absence> iterator = absenceEngine.scan.scanAbsences.iterator();
    
    while (absenceEngine.scan.configureNextGroupToScan(iterator).currentGroup != null) {
      log.debug("Inizio lo scan del prossimo gruppo {}", absenceEngine.scan.currentGroup.description);
      buildPeriodChain(absenceEngine, 
          absenceEngine.scan.currentGroup, 
          absenceEngine.scan.currentAbsence.getAbsenceDate());
      //taggare come scansionate le assenze coinvolte nella periodChain
      for (Absence absence : absenceEngine.periodChain.absencesAsc) {
          absenceEngine.scan.setGroupScanned(absence, absenceEngine.scan.currentGroup);
      }
    }
    
    //persistenza degli errori riscontrati
    persistScannerResults(absenceEngine);
    
    return absenceEngine;
  }
  
  private void persistScannerResults(AbsenceEngine absenceEngine) {
    
    Map<Absence, List<AbsenceProblem>> remainingProblemsMap = absenceEngine.report.remainingProblemsMap();
    
    for (Absence absence : absenceEngine.scan.scanAbsences) {
 
      List<AbsenceTrouble> toDeleteTroubles = Lists.newArrayList();     //problemi da aggiungere
      List<AbsenceTrouble> toAddTroubles = Lists.newArrayList();        //problemi da rimuovere
      
      List<AbsenceProblem> remainingProblems = remainingProblemsMap.get(absence);
      if (remainingProblems == null) {
        remainingProblems = Lists.newArrayList();
      }
      
      //decidere quelli da cancellare
      //   per ogni vecchio absenceTroule verifico se non è presente in remaining
      for (AbsenceTrouble absenceTrouble : absence.troubles) {
        if (!remainingProblems.contains(absenceTrouble.trouble)) {
          toDeleteTroubles.add(absenceTrouble);
        }
      }
      
      //decidere quelli da aggiungere
      //   per ogni remaining verifico se non è presente in vecchi absencetrouble
      for (AbsenceProblem absenceProblem : remainingProblems) {
        boolean toAdd = true;
        for (AbsenceTrouble absenceTrouble : absence.troubles) {
          if (absenceTrouble.trouble.equals(absenceProblem)) {
            toAdd = false;
          }
        }
        if (toAdd) {
          AbsenceTrouble toAddTrouble = new AbsenceTrouble();
          toAddTrouble.absence = absence;
          toAddTrouble.trouble = absenceProblem;
          toAddTroubles.add(toAddTrouble);
        }
      }
      
      //eseguire
      for (AbsenceTrouble toDelete : toDeleteTroubles) {
        toDelete.delete();
      }
      for (AbsenceTrouble toAdd : toAddTroubles) {
        toAdd.save();
      }
    }
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
  public AbsenceEngine absenceInsertRequest(AbsenceEngine absenceEngine, 
      AbsenceRequestType absenceRequestType, Absence absence, JustifiedType requestedJustifiedType,
      boolean absenceTypeToInfer) {
    
    // Provo a inserire l'assenza in ogni periodo della catena...
    for (AbsencePeriod currentAbsencePeriod : absenceEngine.periodChain.periods) {
     
      //Se la data non appartiene al period vado al successivo
      if (!DateUtility.isDateIntoInterval(absenceEngine.request.currentDate, 
          currentAbsencePeriod.periodInterval())) {
        continue;
      }

      //Inferire il tipo se necessario
      if (absenceTypeToInfer) {
        absence = absenceEngineUtility
            .inferAbsenceType(currentAbsencePeriod, absence, requestedJustifiedType);
      }
      if (absenceTypeToInfer && absence.absenceType == null) {
        continue;
      }

      //Vincoli
      if (requestConstraints(absenceEngine, currentAbsencePeriod, absence)
          .report.containsProblems()) {
        return absenceEngine;          
      }
      if (genericConstraints(absenceEngine, absence, absenceEngine.periodChain.allCodeAbsencesAsc)
          .report.containsProblems()) {
        return absenceEngine;          
      }
      if (groupConstraints(absenceEngine, currentAbsencePeriod, absence)
          .report.containsProblems()) {
        return absenceEngine;     
      }

      //Inserimento e riepilogo inserimento
      if (performInsert(absenceEngine, currentAbsencePeriod, absenceRequestType, 
          absence).periodChain.success) {
        return absenceEngine;
      }
      
      //Al periodo successivo se dovevevo inferire il tipo resetto
      if (absenceTypeToInfer) {
        absence.absenceType = null;
      }
    }

    //Esco e non sono mai riuscito a inferire il tipo.
    if (absenceTypeToInfer && absence.absenceType == null) {
      absenceEngine.report.addRequestProblem(ReportRequestProblem.builder()
          .requestProblem(RequestProblem.CantInferAbsenceCode)
          .date(absenceEngine.request.currentDate)
          .build());
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
  private AbsenceEngine performInsert(AbsenceEngine absenceEngine, 
      AbsencePeriod absencePeriod, AbsenceRequestType absenceRequestType,
      Absence absence) {
    
    //simple grouping
    if (absencePeriod.groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.simpleGrouping)) {
      InsertResultItem insertResultItem = InsertResultItem.builder()
          .absence(absence)
          .absenceType(absence.getAbsenceType())
          .operation(Operation.insert)
          .date(absenceEngine.request.currentDate).build();
      absenceEngine.report.addInsertResultItem(insertResultItem);
      absenceEngine.request.requestInserts.add(absence);
      absenceEngine.periodChain.success = true;
      return absenceEngine;
    }
    
    InsertResultItem absenceResultItem = InsertResultItem.builder()
        .absence(absence)
        .absenceType(absence.getAbsenceType())
        .operation(Operation.insert)
        .consumedResidualAmount(Lists.newArrayList())
        .date(absenceEngine.request.currentDate).build();
    absenceEngine.report.addInsertResultItem(absenceResultItem);

    //Takable component
    if (absencePeriod.takableComponent.isPresent()) {

      TakableComponent takableComponent = absencePeriod.takableComponent.get();
      
      int takenAmount = absenceEngineUtility.absenceJustifiedAmount(absenceEngine, 
          absence, takableComponent.takeAmountType);
      
      ConsumedResidualAmount consumedResidualAmount = ConsumedResidualAmount.builder()
          .amountType(takableComponent.takeAmountType)
          .expireResidual(absencePeriod.to)
          .totalResidual(takableComponent.getPeriodTakableAmount())
          .usedResidualBefore(takableComponent.getPeriodTakenAmount())
          .amount(takenAmount)
          .workingTime(absenceEngine.workingTime(absenceEngine.request.currentDate))
          .build();
      absenceResultItem.getConsumedResidualAmount().add(consumedResidualAmount);

      //Aggiungo l'assenza alle strutture dati per l'eventuale iterata successiva.
      takableComponent.addAbsenceTaken(absence, takenAmount);
      absenceEngine.request.requestInserts.add(absence);
    }

    //Complation replacing
    if (absencePeriod.complationComponent.isPresent()) {
      ComplationComponent complationComponent = absencePeriod.complationComponent.get();
      if (complationComponent.complationCodes.contains(absence.absenceType)) {

        //aggiungere l'assenza ai completamenti     
        complationComponent.complationAbsencesByDay.put(absence.getAbsenceDate(), absence);

        //creare il missing replacing se c'è.
        for (ReplacingDay replacingDay : complationReplacingDays(absenceEngine, complationComponent).values()) {
          if (replacingDay.onlyCorrect()) {

            Absence replacingAbsence = new Absence();
            replacingAbsence.absenceType = replacingDay.getCorrectReplacing();
            replacingAbsence.date = replacingDay.getDate();
            replacingAbsence.justifiedType = absenceComponentDao
                .getOrBuildJustifiedType(JustifiedTypeName.nothing);
            //todo cercarlo fra quelli permit e se non c'è nothing errore

            InsertResultItem replacingResultItem = InsertResultItem.builder()
                .absence(replacingAbsence)
                .absenceType(replacingAbsence.absenceType)
                .operation(Operation.insertReplacing)
                .date(replacingDay.getDate()).build();

            absenceEngine.report.addInsertResultItem(replacingResultItem);
            absenceEngine.request.requestInserts.add(absence);
          }
        }
      }
    }

    //success
    absenceEngine.periodChain.success = true;

    return absenceEngine; 
  }
  
  private AbsenceEngine genericConstraints(AbsenceEngine absenceEngine, Absence absence, 
      List<Absence> allCodeAbsences) {
    
    //Codice non prendibile nei giorni di festa ed è festa.
    if (!absence.absenceType.consideredWeekEnd && personDayManager.isHoliday(absenceEngine.person,
        absence.getAbsenceDate())) {
      absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
          .absenceProblem(AbsenceProblem.NotOnHoliday)
          .absence(absence).build());
    }
    
    //Un codice giornaliero già presente 
    for (Absence oldAbsence : allCodeAbsences) {
      //altra data
      if (!oldAbsence.getAbsenceDate().isEqual(absence.getAbsenceDate())) {
        continue;
      }
      //tempo giustificato non giornaliero
      if ((oldAbsence.justifiedType.name.equals(JustifiedTypeName.all_day) 
          || oldAbsence.justifiedType.name.equals(JustifiedTypeName.assign_all_day)) == false) {
        continue;
      }

      if (absenceEngine.isRequestEngine()) {
        InsertResultItem insertResuItem = InsertResultItem.builder()
            .absence(absence)
            .absenceType(absence.getAbsenceType())
            .operation(Operation.insert)
            .date(absence.getAbsenceDate())
            .absenceProblem(AbsenceProblem.AllDayAlreadyExists).build();
        absenceEngine.report.addInsertResultItem(insertResuItem);
      }
      else if (absenceEngine.isScanEngine()) {
        if (oldAbsence.isPersistent() && absence.isPersistent() && oldAbsence.equals(absence)) {
          continue;
        }
        absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
            .absenceProblem(AbsenceProblem.AllDayAlreadyExists)
            .absence(absence).build());
      }

    }

     
    return absenceEngine;
  }
  
  /**
   * Verifica di errori che non dovrebbero mai accadere.
   * @param absenceEngine
   * @param absencePeriod
   * @param absence
   * @return
   */
  private AbsenceEngine requestConstraints(AbsenceEngine absenceEngine, AbsencePeriod absencePeriod, 
      Absence absence) {
    
    //Controllo integrità absenceType - justifiedType
    if (!absence.absenceType.justifiedTypesPermitted.contains(absence.justifiedType)) {
      absenceEngine.report.addRequestProblem(ReportRequestProblem.builder()
          .requestProblem(RequestProblem.WrongJustifiedType)
          .date(absenceEngine.request.currentDate)
          .build());
      return absenceEngine;
    }
    
    //Se è presente takableComponent allora deve essere un codice takable
    if (absencePeriod.takableComponent.isPresent()) {
      if (!absencePeriod.takableComponent.get().takableCodes.contains(absence.absenceType)) {
        absenceEngine.report.addRequestProblem(ReportRequestProblem.builder()
            .requestProblem(RequestProblem.CodeNotAllowedInGroup)
            .date(absence.getAbsenceDate()).build());
        return absenceEngine;
      }
    }
    
    //Se è presente solo complationComponent allora deve essere un codice complation
    if (!absencePeriod.takableComponent.isPresent() 
        && absencePeriod.complationComponent.isPresent()) {
      if (!absencePeriod.complationComponent.get().complationCodes.contains(absence.absenceType)) { 
        absenceEngine.report.addRequestProblem(ReportRequestProblem.builder()
            .requestProblem(RequestProblem.CodeNotAllowedInGroup)
            .date(absence.getAbsenceDate())
            .build());
        return absenceEngine;
      }
    }
    
    return absenceEngine;
  }
  
  /**
   * 
   * FIXME: questi controlli si potrebbero eliminare rieseguendo la populate
   * con la nuova assenza!!! 
   * 
   * @param absenceEngine
   * @param absencePeriod
   * @param absence
   * @return
   */
  private AbsenceEngine groupConstraints(AbsenceEngine absenceEngine, 
      AbsencePeriod absencePeriod, Absence absence) {
    
    //Un codice di completamento e ne esiste già uno
    if (absencePeriod.complationComponent.isPresent()) {
      ComplationComponent complationComponent = absencePeriod.complationComponent.get();
      if (complationComponent.complationCodes.contains(absence.absenceType) 
          && complationComponent.complationAbsencesByDay.get(absence.getAbsenceDate()) != null) {
        absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
            .absenceProblem(AbsenceProblem.TwoComplationSameDay)
            .absence(absence)
            .build());
        return absenceEngine;
      }
    }

    //Takable limit
    if (absencePeriod.takableComponent.isPresent()) {
      
      int takenAmount = absenceEngineUtility
          .absenceJustifiedAmount(absenceEngine, absence, 
              absencePeriod.takableComponent.get().takeAmountType);
      
      if (!absencePeriod.takableComponent.get().canAddTakenAmount(takenAmount)) {
        absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
            .absenceProblem(AbsenceProblem.LimitExceeded)
            .absence(absence)
           .build());
      }
    }
    return absenceEngine;
  }
  
  
  /**
   * Costruisce l'istanza per calcolare la situazione residuale del gruppo.
   * @param person
   * @param groupAbsenceType
   * @param date
   * @return
   */
  public AbsenceEngine residualAbsenceEngine(Person person, GroupAbsenceType groupAbsenceType, LocalDate date) {

    AbsenceEngine absenceEngine = new AbsenceEngine(absenceComponentDao, personChildrenDao, 
        absenceEngineUtility, person, groupAbsenceType, date, null);

    if (groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.vacationsCnr) || 
        groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.compensatoryRestCnr)) {

      absenceEngine.report.addImplementationProblem(ReportImplementationProblem.builder()
          .implementationProblem(ImplementationProblem.UnimplementedTakableComplationGroup)
          .build());
      return absenceEngine;
    }

    buildPeriodChain(absenceEngine, groupAbsenceType, date);

    return absenceEngine;
  }

  
}
