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
import manager.services.absences.AbsencesReport.ReportAbsenceTypeProblem;
import manager.services.absences.AbsencesReport.ReportImplementationProblem;
import manager.services.absences.AbsencesReport.ReportRequestProblem;
import manager.services.absences.AbsencesReport.ReportStatus;
import manager.services.absences.InsertResultItem.Operation;
import manager.services.absences.model.AbsenceEngine;
import manager.services.absences.model.AbsencePeriod;
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
    if (absenceEngine.report.containsCriticalProblems()) {
      return periodChain;
    }
    periodChain.periods.add(currentPeriod);
    while (currentPeriod.groupAbsenceType.nextGroupToCheck != null) {
      //successivi
      currentPeriod = buildAbsencePeriod(absenceEngine, currentPeriod.groupAbsenceType.nextGroupToCheck, date);
      if (absenceEngine.report.containsCriticalProblems()) {
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
        int amount = absenceEngineUtility.replacingAmount(absenceEngine, absenceType, 
            absencePeriod.complationAmountType);
        if (amount < 1) {
          absenceEngine.report.addAbsenceTypeProblem(ReportAbsenceTypeProblem.builder()
              .absenceTypeProblem(AbsenceTypeProblem.IncalcolableReplacingAmount)
              .absenceType(absenceType)
              .build());
          continue;
        }
        if (absencePeriod.replacingCodesDesc.get(amount) != null) {
          absenceEngine.report.addAbsenceTypeProblem(ReportAbsenceTypeProblem.builder()
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
  private void populatePeriodChain(AbsenceEngine absenceEngine) {
    
    Set<Absence> absencesAlreadyAssigned = Sets.newHashSet();
    
    for (AbsencePeriod absencePeriod : absenceEngine.periodChain.periods) {
      
      for (Absence absence : absenceEngine.periodChain.absencesAsc) {
        if (!DateUtility.isDateIntoInterval(absence.getAbsenceDate(), 
            absencePeriod.periodInterval())) {
          continue;
        }
        
        //Se il gruppo ha una assenza precedente compromessa (con errori) allora
        // tutte le successive sono compromesse.
        if (absenceEngine.report.containsCriticalProblems()) {
          absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.CompromisedTakableComplationGroup)
              .absence(absence)
              .build());
          continue;
        }
        
        //Se il suo tipo ha un errore 
        if (absenceEngine.report.absenceTypeHasProblem(absence.absenceType)) {
          absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
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
        
        //una assenza deve avere un ruolo
        if (!isTaken && !isComplation && !isReplacing) {
          absenceEngine.report.addAbsenceAndImplementationProblem(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.UselessAbsenceInPeriod)
              .absence(absence)
              .build());
          continue;
        }
        
        //una assenza può essere assegnata ad un solo period
        if (absencesAlreadyAssigned.contains(absence)) {
          absenceEngine.report.addAbsenceAndImplementationProblem(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.TwoPeriods)
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
              .absenceJustifiedAmount(absenceEngine, absence, absencePeriod.takeAmountType);
          if (takenAmount <= 0) {
            absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.IncalcolableJustifiedAmount)
                .absence(absence)
                .build());
            continue;
          }
          if (!absencePeriod.canAddTakenAmount(takenAmount)) {
            absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.LimitExceeded)
                .absence(absence)
                .build());
            absencePeriod.setOvertakenLimitAbsence(absence);
          }
          absencePeriod.addAbsenceTaken(absence, takenAmount);
        }
        
        if (isComplation) {
          Absence previous = absencePeriod.complationAbsencesByDay.get(date);
          if (previous != null ) {
            //una sola assenza di completamento per quel giorno
            absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.TwoComplationSameDay)
                .absence(absence)
                .build());
            absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.TwoComplationSameDay)
                .absence(previous)
                .build());
            absencePeriod.twoComplationSameDay.add(previous);
            absencePeriod.twoComplationSameDay.add(absence);
            absencePeriod.setCompromisedReplacingDate(absence.getAbsenceDate());
            continue;
          }
          
          int complationAmount = absenceEngineUtility.absenceJustifiedAmount(absenceEngine, 
              absence, absencePeriod.complationAmountType);
          
          if (complationAmount <= 0) {
            absenceEngine.report.addAbsenceAndImplementationProblem(AbsenceTrouble.builder()
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
            absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.TwoReplacingSameDay)
                .absence(absence)
                .build());
            absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
                .trouble(AbsenceProblem.TwoReplacingSameDay)
                .absence(previous)
                .build());
            absencePeriod.twoReplacingSameDay.add(previous);
            absencePeriod.twoReplacingSameDay.add(absence);
            absencePeriod.setCompromisedReplacingDate(absence.getAbsenceDate());
            continue;
          }
          absencePeriod.replacingAbsencesByDay.put(date, absence);
        }
      }
      
      //Il controllo sui completamenti/rimpiazzamenti lo eseguo quando ho processato tutte le 
      // assenze del periodo (quindi ora) e se non ci sono stati errori precedenti.
      if (absencePeriod.isComplation() && !absenceEngine.report.containsCriticalProblems()) {
        absencePeriod.replacingStatus = complationReplacingStatus(absenceEngine, absencePeriod);
        complationSoundness(absenceEngine, absencePeriod);
      }
    }
  }
  
  private SortedMap<LocalDate, ReplacingStatus> complationReplacingStatus(
      AbsenceEngine absenceEngine, AbsencePeriod absencePeriod) {
    
    //I replacing days per ogni data raccolgo i replacing effettivi e quelli corretti
    SortedMap<LocalDate, ReplacingStatus> replacingStatuss = Maps.newTreeMap();
    
    //preparo i giorni con i replacing effettivi (
    for (Absence replacingAbsence : absencePeriod.replacingAbsencesByDay.values()) {
      if (absencePeriod.isAbsenceCompromisedReplacing(replacingAbsence)) {
        continue;
      }
      replacingStatuss.put(replacingAbsence.getAbsenceDate(), 
          ReplacingStatus.builder()
          .existentReplacing(replacingAbsence)
          .date(replacingAbsence.getAbsenceDate())
          .build());
    }

    //Le assenze di completamento ordinate per data. Genero i replacing ipotetici
    int complationAmount = 0;
    for (Absence complationAbsence : absencePeriod.complationAbsencesByDay.values()) {
      
      if (absencePeriod.isAbsenceCompromisedReplacing(complationAbsence)) {
        continue;
      }
      
      int amount = absenceEngineUtility.absenceJustifiedAmount(absenceEngine, 
          complationAbsence, absencePeriod.complationAmountType);
      complationAmount = complationAmount + amount;

      LocalDate replacingDate = complationAbsence.getAbsenceDate();
      ReplacingStatus replacingStatus = replacingStatuss.get(replacingDate);
      if (replacingStatus == null) {
        replacingStatus = ReplacingStatus.builder().date(replacingDate).build();
        replacingStatuss.put(replacingDate, replacingStatus);
      }
      replacingStatus.setAmountTypeComplation(absencePeriod.complationAmountType);
      replacingStatus.setComplationAbsence(complationAbsence);
      replacingStatus.setResidualBeforeComplation(complationAmount - amount);
      replacingStatus.setConsumedComplation(amount);
      
      Optional<AbsenceType> replacingCode = absenceEngineUtility
          .whichReplacingCode(absenceEngine, absencePeriod, 
              complationAbsence.getAbsenceDate(), complationAmount);
      
      if (replacingCode.isPresent()) {
        
        replacingStatus.setCorrectReplacing(replacingCode.get());
        complationAmount -= absencePeriod.replacingTimes.get(replacingCode.get());
      }
      
      replacingStatus.setResidualAfterComplation(complationAmount);
    }
    absencePeriod.complationConsumedAmount = complationAmount;
    
    return replacingStatuss;
  }
  
  /**
   * Controlla che tutti i rimpiazzamenti siano nella posizione corretta.
   * Aggiunge gli errori al report.
   * TODO: la struttura dati replacingStatuss potrebbe essere rimossa semplificando
   * di molto il codice.
   * @param first
   * @param absenceEngine
   * @param absence
   */
  private AbsenceEngine complationSoundness(
      AbsenceEngine absenceEngine, AbsencePeriod absencePeriod) {

    //Controllo che i replacing ipotetici collimino con quelli reali
    
    for (ReplacingStatus replacingStatus : absencePeriod.replacingStatus.values()) {

      //Errore nel rimpiazzo
      if (replacingStatus.wrongType()) {
        absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
            .trouble(AbsenceProblem.WrongReplacing)
            .absence(replacingStatus.getExistentReplacing())
            .build());
        absencePeriod.setCompromisedReplacingDate(replacingStatus.getDate());
        break;
      }
      if (replacingStatus.onlyExisting()) {
        absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
            .trouble(AbsenceProblem.TooEarlyReplacing)
            .absence(replacingStatus.getExistentReplacing())
            .build());
        absencePeriod.setCompromisedReplacingDate(replacingStatus.getDate());
        break;
      }
      
      //Errore nel completamento
      if (replacingStatus.onlyCorrect()) {
        absencePeriod.setCompromisedReplacingDate(replacingStatus.getDate());
        absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
            .trouble(AbsenceProblem.MissingReplacing)
            .absence(absencePeriod.complationAbsencesByDay
                .get(replacingStatus.getDate()))
            .build());
        break;
      }
      
    }

    //Se la catena è compromessa tutte le assenza completamento e replacing
    // successive sono taggate con l'errore
    if (absencePeriod.compromisedReplacingDate != null) {
      for (Absence complationAbsence : absencePeriod.complationAbsencesByDay.values()) {
        if (complationAbsence.getAbsenceDate()
            .isAfter(absencePeriod.compromisedReplacingDate)) {
          absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.CompromisedReplacing)
              .absence(complationAbsence)
              .build());
        }
      }
      for (Absence replacingAbsence : absencePeriod.replacingAbsencesByDay.values()) {
        if (replacingAbsence.getAbsenceDate().isAfter(absencePeriod.compromisedReplacingDate)) {
          absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
              .trouble(AbsenceProblem.CompromisedReplacing)
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
    persistScannerTroubles(absenceEngine);
    
    return absenceEngine;
  }
  
  private void persistScannerTroubles(AbsenceEngine absenceEngine) {
    
    for (Absence absence : absenceEngine.scan.scanAbsences) {
 
      List<AbsenceTrouble> toDeleteTroubles = Lists.newArrayList();     //problemi da aggiungere
      List<AbsenceTrouble> toAddTroubles = Lists.newArrayList();        //problemi da rimuovere
      
      List<AbsenceTrouble> remainingTroubles = 
          absenceEngine.report.absenceTroublesMap.get(absence);
      if (remainingTroubles == null) {
        remainingTroubles = Lists.newArrayList();
      }
      
      //decidere quelli da cancellare
      //   per ogni vecchio absenceTroule verifico se non è presente in remaining
      for (AbsenceTrouble absenceTrouble : absence.troubles) {
        if (!remainingTroubles.contains(absenceTrouble.trouble)) {
          toDeleteTroubles.add(absenceTrouble);
        }
      }
      
      //decidere quelli da aggiungere
      //   per ogni remaining verifico se non è presente in vecchi absencetrouble
      for (AbsenceTrouble reportAbsenceTrouble : remainingTroubles) {
        boolean toAdd = true;
        for (AbsenceTrouble absenceTrouble : absence.troubles) {
          if (absenceTrouble.trouble.equals(reportAbsenceTrouble.trouble)) {
            toAdd = false;
          }
        }
        if (toAdd) {
          toAddTroubles.add(AbsenceTrouble.builder()
              .absence(absence)
              .trouble(reportAbsenceTrouble.trouble)
              .build());
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
    if (absencePeriod.isTakable()) {
      
      int takenAmount = absenceEngineUtility.absenceJustifiedAmount(absenceEngine, 
          absence, absencePeriod.takeAmountType);
      
      AbsenceStatus consumedResidualAmount = AbsenceStatus.builder()
          .amountType(absencePeriod.takeAmountType)
          .expireResidual(absencePeriod.to)
          .totalResidual(absencePeriod.getPeriodTakableAmount())
          .usedResidualBefore(absencePeriod.getPeriodTakenAmount())
          .amount(takenAmount)
          .workingTime(absenceEngine.workingTime(absenceEngine.request.currentDate))
          .build();
      absenceResultItem.getConsumedResidualAmount().add(consumedResidualAmount);

      //Aggiungo l'assenza alle strutture dati per l'eventuale iterata successiva.
      absencePeriod.addAbsenceTaken(absence, takenAmount);
      absenceEngine.request.requestInserts.add(absence);
    }

    //Complation replacing
    if (absencePeriod.isComplation()) {
      if (absencePeriod.complationCodes.contains(absence.absenceType)) {

        //aggiungere l'assenza ai completamenti     
        absencePeriod.complationAbsencesByDay.put(absence.getAbsenceDate(), absence);

        //creare il missing replacing se c'è.
        for (ReplacingStatus replacingStatus : 
          complationReplacingStatus(absenceEngine, absencePeriod).values()) {
          if (replacingStatus.onlyCorrect()) {

            Absence replacingAbsence = new Absence();
            replacingAbsence.absenceType = replacingStatus.getCorrectReplacing();
            replacingAbsence.date = replacingStatus.getDate();
            replacingAbsence.justifiedType = absenceComponentDao
                .getOrBuildJustifiedType(JustifiedTypeName.nothing);
            //todo cercarlo fra quelli permit e se non c'è nothing errore

            InsertResultItem replacingResultItem = InsertResultItem.builder()
                .absence(replacingAbsence)
                .absenceType(replacingAbsence.absenceType)
                .operation(Operation.insertReplacing)
                .date(replacingStatus.getDate()).build();

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
      absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
          .trouble(AbsenceProblem.NotOnHoliday)
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
        absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
            .trouble(AbsenceProblem.AllDayAlreadyExists)
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
    if (absencePeriod.isTakable()) {
      if (!absencePeriod.takableCodes.contains(absence.absenceType)) {
        absenceEngine.report.addRequestProblem(ReportRequestProblem.builder()
            .requestProblem(RequestProblem.CodeNotAllowedInGroup)
            .date(absence.getAbsenceDate()).build());
        return absenceEngine;
      }
    }
    
    //Se è presente solo complationComponent allora deve essere un codice complation
    if (!absencePeriod.isTakable() && absencePeriod.isComplation()) {
      
      if (!absencePeriod.complationCodes.contains(absence.absenceType)) { 
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
    if (absencePeriod.isComplation()) {
      if (absencePeriod.complationCodes.contains(absence.absenceType) 
          && absencePeriod.complationAbsencesByDay.get(absence.getAbsenceDate()) != null) {
        absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
            .trouble(AbsenceProblem.TwoComplationSameDay)
            .absence(absence)
            .build());
        return absenceEngine;
      }
    }

    //Takable limit
    if (absencePeriod.isTakable()) {
      
      int takenAmount = absenceEngineUtility.absenceJustifiedAmount(absenceEngine, absence, 
          absencePeriod.takeAmountType);
      
      if (!absencePeriod.canAddTakenAmount(takenAmount)) {
        absenceEngine.report.addAbsenceTrouble(AbsenceTrouble.builder()
            .trouble(AbsenceProblem.LimitExceeded)
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
    
    ReportStatus reportStatus = new ReportStatus();
    reportStatus.periods = absenceEngine.periodChain.periods;
    
    for (AbsencePeriod absencePeriod : absenceEngine.periodChain.periods) {

      
      //Obbiettivi:
      
      
      
      
      
      
      


//      for (AbsenceStatus absenceStatus : absencePeriod.takableComponent.get().takenAbsencesStatus) {
//
//        List<ReportDayItem> takableItems = Lists.newArrayList();
//        List<ReportDayItem> takableAndComplationItems = Lists.newArrayList();
//        List<ReportDayItem> orphanReplacingItems = Lists.newArrayList();
//
//
//
//
//
//        if (!absencePeriod.complationComponent.isPresent() 
//            || !absencePeriod.complationComponent.get().complationAbsencesByDay.values()
//            .contains(absenceStatus.absence)) {
//
//          //1) le assenze solo takable
//          ReportDayItem dayItem = ReportDayItem.builder()
//              .takableStatus(absenceStatus)
//              .build();
//          takableItems.add(dayItem);
//        } else {
//
//          //1) le assenze takable e di completamento
//          ReplacingStatus replacingStatus = null;
//          if (absencePeriod.complationComponent.isPresent()) {
//            replacingStatus = absencePeriod.complationComponent.get()
//                .replacingStatus.get(absenceStatus.absence.getAbsenceDate());
//          }
//          ReportDayItem dayItem = ReportDayItem.builder()
//              .takableStatus(absenceStatus)
//              .takableReplacingStatus(replacingStatus)
//              .build();
//          takableAndComplationItems.add(dayItem);
//        }
//      }


      //reportStatus.addReportDayItem(dayItem, absenceStatus.absence.getAbsenceDate());
    }




    return absenceEngine;
  }


}
