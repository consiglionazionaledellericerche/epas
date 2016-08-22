package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateUtility;

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
import manager.services.absences.model.AbsencePeriod.EnhancedAbsence;
import manager.services.absences.model.AbsencePeriod.TakableComponent;

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
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Map;

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
        person, groupAbsenceType, from, to);
    
    if (groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.vacationsCnr) || 
        groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.compensatoryRestCnr)) {
      
      absenceEngine.report.addImplementationProblem(ReportImplementationProblem.builder()
          .implementationProblem(ImplementationProblem.UnimplementedTakableComplationGroup)
          .build());
      return absenceEngine;
    }
    
    configureNextInsertDate(absenceEngine);
   
    return absenceEngine;
  }
  
  public AbsenceEngine configureNextInsertDate(AbsenceEngine absenceEngine) {
    
    //prima iterata
    if (absenceEngine.requestCurrentDate == null) {
      absenceEngine.requestCurrentDate = absenceEngine.requestFrom;
    } else {
      //iterata successiva
      absenceEngine.requestCurrentDate = absenceEngine.requestCurrentDate.plusDays(1);
      if (absenceEngine.requestTo == null 
          || absenceEngine.requestCurrentDate.isAfter(absenceEngine.requestTo)) {
        absenceEngine.requestCurrentDate = null;
        return absenceEngine;
      }
    }
    
    buildPeriodChain(absenceEngine, null);
    
    return absenceEngine;
  }
  
  
  /**
   * Costruisce le date dell'AbsencePeriod relativo all'istanza. 
   * Se il gruppo è ricorsivo costruisce anche le date dei periodi seguenti.
   * @param absenceEngine
   * @return
   */
  private AbsencePeriod buildPeriodChain(AbsenceEngine absenceEngine, 
      AbsencePeriod previousAbsencePeriod) { 
    
    if (previousAbsencePeriod == null && absenceEngine.periodChain != null) {
      // TODO: Implementare logica di verifica data 
      // richiesta compatibile col precedente absencePeriod
    }
    
    AbsencePeriod currentAbsencePeriod;
    
    if (previousAbsencePeriod == null) {
      //Primo absencePeriod (assegno il gruppo dell'engine)
      currentAbsencePeriod = new AbsencePeriod(absenceEngine.engineGroup());
      absenceEngine.periodChain = currentAbsencePeriod;
    } else {
      //Seguenti
      currentAbsencePeriod = new AbsencePeriod(previousAbsencePeriod.groupAbsenceType.nextGroupToCheck);
      currentAbsencePeriod.previousAbsencePeriod = previousAbsencePeriod; //vedere se serve...
    }

    // recuperare l'inizializzazione (questo lo posso fare anche fuori) per i fix sulle date.

    if (currentAbsencePeriod.groupAbsenceType.periodType.equals(PeriodType.year)) {
      currentAbsencePeriod.from = new LocalDate(absenceEngine.currentDate().getYear(), 1, 1);
      currentAbsencePeriod.to = new LocalDate(absenceEngine.currentDate().getYear(), 12, 31);
    } else if (currentAbsencePeriod.groupAbsenceType.periodType.equals(PeriodType.month)) {
      currentAbsencePeriod.from = absenceEngine.currentDate().dayOfMonth().withMinimumValue();
      currentAbsencePeriod.to = absenceEngine.currentDate().dayOfMonth().withMaximumValue();
    } else if (currentAbsencePeriod.groupAbsenceType.periodType.equals(PeriodType.always)) {
      currentAbsencePeriod.from = null;
      currentAbsencePeriod.to = null;
    }

    // Caso inerente i figli.
    else if (currentAbsencePeriod.groupAbsenceType.periodType.isChildPeriod()) {
      try {
        absenceEngine.childInterval = currentAbsencePeriod.groupAbsenceType.periodType
            .getChildInterval(absenceEngine.orderedChildren()
                .get(currentAbsencePeriod.groupAbsenceType.periodType.childNumber - 1).bornDate);
        currentAbsencePeriod.from = absenceEngine.childInterval.getBegin();
        currentAbsencePeriod.to = absenceEngine.childInterval.getEnd();

      } catch (Exception e) {
        absenceEngine.report.addRequestProblem(ReportRequestProblem.builder()
            .requestProblem(RequestProblem.NoChildExists)
            .date(absenceEngine.currentDate())
            .build());
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
      currentAbsencePeriod.complationComponent = Optional.of(complationComponent);
    }

    //Chiamata ricorsiva
    if (currentAbsencePeriod.groupAbsenceType.nextGroupToCheck != null) {
      currentAbsencePeriod.nextAbsencePeriod = 
          buildPeriodChain(absenceEngine, currentAbsencePeriod);
    }
    
    //Ultimo step del primo absencePeriod
    if (previousAbsencePeriod == null) {
      // Assegnare ad ogni periodo le assenze di competenza (fase da migliorare) e calcoli
      populatePeriodChain(absenceEngine);
    }
 
    return currentAbsencePeriod;
  }
  
  /**
   * Aggiunge ai period tutte le assenze prese e ne calcola l'integrità.
   * Popola il report con tutti gli errori riscontrati.
   * @param periodChain
   * @param absenceEngine
   * @param absence
   */
  private void populatePeriodChain(AbsenceEngine absenceEngine) {

    absenceEngine.resetPeriodChainSupportStructures();
    
    AbsencePeriod absencePeriod = absenceEngine.periodChain;
    while (absencePeriod != null) {
      
      for (EnhancedAbsence enhancedAbsence : absenceEngine.periodChainAbsencesAsc()) {
        if (!DateUtility.isDateIntoInterval(enhancedAbsence.getAbsence().getAbsenceDate(), 
            absencePeriod.periodInterval())) {
          continue;
        }
        
        //Se è una operazione di scan il gruppo lo imposto come scansionato
        if (absenceEngine.isScanEngine()) {
          enhancedAbsence.setGroupScanned(absenceEngine.engineGroup());
        }
        
        //Se il suo tipo ha un errore 
        if (absenceEngine.report.absenceTypeHasProblem(enhancedAbsence.getAbsence().absenceType)) {
          absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
              .absenceProblem(AbsenceProblem.AbsenceTypeProblem)
              .absence(enhancedAbsence.getAbsence())
              .build());
        }
        
        TakableComponent takableComponent = null;
        ComplationComponent complationComponent = null;
        LocalDate date = null;
        
        //Computo il ruolo dell'assenza nel period
        boolean isTaken = false, isComplation = false, isReplacing = false;
        if (absencePeriod.takableComponent.isPresent()) {
          takableComponent = absencePeriod.takableComponent.get();
          isTaken = takableComponent.takenCodes.contains(enhancedAbsence.getAbsence().absenceType);
        }
        if (absencePeriod.complationComponent.isPresent()) {
          complationComponent = absencePeriod.complationComponent.get();
          isReplacing = complationComponent.replacingCodesDesc.values()
              .contains(enhancedAbsence.getAbsence().absenceType);
          isComplation = complationComponent.complationCodes.contains(enhancedAbsence.getAbsence().absenceType);
        }
        
        //una assenza deve avere un ruolo
        if (!isTaken && !isComplation && !isReplacing) {
          absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
              .absenceProblem(AbsenceProblem.UselessAbsenceInPeriod)
              .absence(enhancedAbsence.getAbsence())
              .build());
          continue;
        }
        
        //una assenza può essere assegnata ad un solo period
        if (enhancedAbsence.isAlreadyAssigned()) {
          absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
              .absenceProblem(AbsenceProblem.TwoPeriods)
              .absence(enhancedAbsence.getAbsence())
              .build());
          continue;
        }
        
        //una tipo di assenza può essere di rimpiazzamento e nient'altro
        if (isReplacing && (isComplation || isTaken)) {
          absenceEngine.report.addAbsenceTypeProblem(ReportAbsenceTypeProblem.builder()
              .absenceTypeProblem(AbsenceTypeProblem.OnlyReplacingRuleViolated)
              .absenceType(enhancedAbsence.getAbsence().getAbsenceType())
              .build());
          continue;  
        }
        if (isTaken || isComplation || isReplacing) {
          enhancedAbsence.setAlreadyAssigned(true);
          date = enhancedAbsence.getAbsence().getAbsenceDate();
        }
        
        //controllo assenza taken
        if (isTaken) {
          if (!setJustifiedTime(absenceEngine, takableComponent, enhancedAbsence)) {
            absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
                .absenceProblem(AbsenceProblem.IncalcolableJustifiedAmount)
                .absence(enhancedAbsence.getAbsence())
                .build());
            continue;
          }
          takenSoundness(absenceEngine, takableComponent, enhancedAbsence);
          takableComponent.addAbsenceTaken(enhancedAbsence);
        }
        
        if (isComplation) {
          EnhancedAbsence previous = complationComponent.complationAbsencesByDay.get(date);
          if (previous != null ) {
            //una sola assenza di completamento per quel giorno
            absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
                .absenceProblem(AbsenceProblem.TwoComplationSameDay)
                .absence(enhancedAbsence.getAbsence())
                .conflictingAbsence(previous.getAbsence())
                .build());
            continue;
          }
          if (!setJustifiedTime(absenceEngine, takableComponent, enhancedAbsence)) {
            absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
                .absenceProblem(AbsenceProblem.IncalcolableJustifiedAmount)
                .absence(enhancedAbsence.getAbsence())
                .build());
            continue;
          }
          complationComponent.complationAbsencesByDay.put(date, enhancedAbsence);
        }
        if (isReplacing) {
          EnhancedAbsence previous = complationComponent.replacingAbsencesByDay.get(date);
          if (previous != null) {
            //una sola assenza di rimpiazzamento per quel giorno
            absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
                .absenceProblem(AbsenceProblem.TwoReplacingSameDay)
                .absence(enhancedAbsence.getAbsence())
                .conflictingAbsence(previous.getAbsence())
                .build());
            continue;
          }
          complationComponent.replacingAbsencesByDay.put(date, enhancedAbsence);
        }
        
        complationSoundness(absenceEngine, complationComponent);
      }
      
      absencePeriod = absencePeriod.nextAbsencePeriod;
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
      EnhancedAbsence enhancedAbsence) {
    if (!takableComponent.canAddTakenAmount(enhancedAbsence.getJustifiedTime())) {
      absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
          .absenceProblem(AbsenceProblem.LimitExceeded)
          .absence(enhancedAbsence.getAbsence())
          .build());
    }
    return absenceEngine;
  }
  
  /**
   * Controlla che tutti i rimpiazzamenti siano nella posizione corretta.
   * Aggiunge gli errori al report.
   * TODO: la struttura dati replacingDays potrebbe essere rimossa semplificando
   * di molto il codice.
   * @param periodChain
   * @param absenceEngine
   * @param absence
   */
  private AbsenceEngine complationSoundness(AbsenceEngine absenceEngine, ComplationComponent complationComponent) {

    if (complationComponent == null) {
      return absenceEngine;
    }
    //preparo i giorni con i replacing effettivi
    for (EnhancedAbsence replacingAbsence : complationComponent
        .replacingAbsencesByDay.values()) {
      complationComponent.replacingDays.put(replacingAbsence.getAbsence().getAbsenceDate(), 
          ReplacingDay.builder()
          .existentReplacing(replacingAbsence.getAbsence())
          .date(replacingAbsence.getAbsence().getAbsenceDate())
          .build());
    }

    //Le assenze di completamento ordinate per data. Genero i replacing ipotetici
    int complationAmount = 0;
    for (EnhancedAbsence complationAbsence : complationComponent
        .complationAbsencesByDay.values()) {
      complationAmount += complationAbsence.getJustifiedTime();
      Optional<AbsenceType> replacingCode = absenceEngineUtility
          .whichReplacingCode(complationComponent, complationAmount);
      if (replacingCode.isPresent()) {
        LocalDate replacingDate = complationAbsence.getAbsence().getAbsenceDate();
        ReplacingDay replacingDay = complationComponent.replacingDays.get(replacingDate);
        if (replacingDay == null) {
          replacingDay = ReplacingDay.builder()
              .correctReplacing(replacingCode.get())
              .date(replacingDate)
              .build();
          complationComponent.replacingDays.put(replacingDate, replacingDay);
        } else {
          replacingDay.setCorrectReplacing(replacingCode.get());
        }
        complationAmount -= complationComponent.replacingTimes.get(replacingCode.get());
      }
    }
    complationComponent.complationConsumedAmount = complationAmount;

    //Controllo che i replacing ipotetici collimino con quelli reali
    complationComponent.compromisedReplacingDate = null;

    for (ReplacingDay replacingDay : complationComponent.replacingDays.values()) {

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
                .get(complationComponent.compromisedReplacingDate).getAbsence())
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
      for (EnhancedAbsence complationAbsence : complationComponent
          .complationAbsencesByDay.values()) {
        if (complationAbsence.getAbsence().getAbsenceDate()
            .isAfter(complationComponent.compromisedReplacingDate)) {
          absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
              .absenceProblem(AbsenceProblem.CompromisedReplacing)
              .absence(complationAbsence.getAbsence())
              .build());
        }
      }
      for (EnhancedAbsence replacingAbsence : complationComponent
          .replacingAbsencesByDay.values()) {
        if (replacingAbsence.getAbsence().getAbsenceDate()
            .isAfter(complationComponent.compromisedReplacingDate)) {
          absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
              .absenceProblem(AbsenceProblem.CompromisedReplacing)
              .absence(replacingAbsence.getAbsence())
              .build());
        }
      }
    }
    
    return absenceEngine;
  }


  /**
   * Imposta il justifiedTime della enhancedAbsence se non è già calcolato.
   * In caso di errore ritorna false.
   * @param absenceEngine
   * @param takableComponent
   * @param enhancedAbsence
   * @return
   */
  private boolean setJustifiedTime(AbsenceEngine absenceEngine, TakableComponent takableComponent,
      EnhancedAbsence enhancedAbsence) {
    if (enhancedAbsence.getJustifiedTime() == null) {
      int amount = absenceEngineUtility.absenceJustifiedAmount(absenceEngine, 
          enhancedAbsence.getAbsence(), takableComponent.takeAmountType);
      if (amount < 0) {
        return false;
      }
      enhancedAbsence.setJustifiedTime(amount);
    }
    return true;
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
    List<EnhancedAbsence> enhancedAbsencesToScan = Lists.newArrayList();
    for (Absence absence : absenceComponentDao.orderedAbsences(person, scanFrom, null, Lists.newArrayList())) {
      enhancedAbsencesToScan.add(EnhancedAbsence.builder()
          .absence(absence)
          .notScannedGroups(absenceEngineUtility.involvedGroup(absence.absenceType))
          .build());
    }
    AbsenceEngine absenceEngine = new AbsenceEngine(absenceComponentDao, personChildrenDao, 
        person, scanFrom, enhancedAbsencesToScan);

    // analisi dei requisiti generici (risultati in absenceEngine.report)
    for (EnhancedAbsence enhancedAbsence : absenceEngine.scanEnhancedAbsences) {
      genericConstraints(absenceEngine, enhancedAbsence.getAbsence());
    }
    
    // analisi dei requisiti all'interno di ogni gruppo (risultati in absenceEngine.report)
    while (configureNextGroupToScan(absenceEngine).isConfiguredForNextScan()) {
      buildPeriodChain(absenceEngine, null);
    }
    
    //persistenza degli errori riscontrati
    persistScannerResults(absenceEngine);
    
    return absenceEngine;
  }
  
  /**
   * Configura il prossimo gruppo da analizzare (se esiste).
   * @param absenceEngine
   * @return
   */
  private AbsenceEngine configureNextGroupToScan(AbsenceEngine absenceEngine) {
    // stessa assenza prossimo gruppo
    if (absenceEngine.scanCurrentAbsence != null && absenceEngine.scanCurrentAbsence.hasNextGroupToScan()) {
      absenceEngine.scanCurrentGroup = absenceEngine.scanCurrentAbsence.getNextGroupToScan();
      return absenceEngine;
    }
    
    // prossima assenza primo gruppo
    absenceEngine.scanCurrentGroup = null;
    while (absenceEngine.scanAbsencesIterator.hasNext()) {
      absenceEngine.scanCurrentAbsence = absenceEngine.scanAbsencesIterator.next();
      if (absenceEngine.scanCurrentAbsence.hasNextGroupToScan()) {   
        absenceEngine.scanCurrentGroup = absenceEngine.scanCurrentAbsence.getNextGroupToScan();
        return absenceEngine;
      }
    }
    return absenceEngine;
  }
  
  private void persistScannerResults(AbsenceEngine absenceEngine) {
    
    Map<Absence, List<AbsenceProblem>> remainingProblemsMap = absenceEngine.report.remainingProblemsMap();
    
    for (EnhancedAbsence enhancedAbsence : absenceEngine.scanEnhancedAbsences) {
 
      List<AbsenceTrouble> toDeleteTroubles = Lists.newArrayList();     //problemi da aggiungere
      List<AbsenceTrouble> toAddTroubles = Lists.newArrayList();        //problemi da rimuovere
      
      List<AbsenceProblem> remainingProblems = remainingProblemsMap.get(enhancedAbsence.getAbsence());
      if (remainingProblems == null) {
        remainingProblems = Lists.newArrayList();
      }
      
      //decidere quelli da cancellare
      //   per ogni vecchio absenceTroule verifico se non è presente in remaining
      for (AbsenceTrouble absenceTrouble : enhancedAbsence.getAbsence().troubles) {
        if (!remainingProblems.contains(absenceTrouble.trouble)) {
          toDeleteTroubles.add(absenceTrouble);
        }
      }
      
      //decidere quelli da aggiungere
      //   per ogni remaining verifico se non è presente in vecchi absencetrouble
      for (AbsenceProblem absenceProblem : remainingProblems) {
        boolean toAdd = true;
        for (AbsenceTrouble absenceTrouble : enhancedAbsence.getAbsence().troubles) {
          if (absenceTrouble.trouble.equals(absenceProblem)) {
            toAdd = false;
          }
        }
        if (toAdd) {
          AbsenceTrouble toAddTrouble = new AbsenceTrouble();
          toAddTrouble.absence = enhancedAbsence.getAbsence();
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
      AbsenceRequestType absenceRequestType, EnhancedAbsence enhancedAbsence) {
    
    // Provo a inserire l'assenza in ogni periodo della catena...
    AbsencePeriod currentAbsencePeriod = absenceEngine.periodChain;
    while (currentAbsencePeriod != null) {
      
      //Se la data non appartiene al period vado al successivo
      if (!DateUtility.isDateIntoInterval(absenceEngine.currentDate(), 
          currentAbsencePeriod.periodInterval())) {
        currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
        continue;
      }

      //Inferire il tipo se necessario
      absenceEngineUtility.inferAbsenceType(currentAbsencePeriod, enhancedAbsence);
      if (enhancedAbsence.isAbsenceTypeToInfer() && !enhancedAbsence.isAbsenceTypeInfered()) {
        currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
        continue;
      }

      //Calcolare l'amount
      if (currentAbsencePeriod.takableComponent.isPresent()) {
        enhancedAbsence.setJustifiedTime(absenceEngineUtility
            .absenceJustifiedAmount(absenceEngine, enhancedAbsence.getAbsence(), 
                currentAbsencePeriod.takableComponent.get().takeAmountType));
      }

      Absence absence = enhancedAbsence.getAbsence();  //scorciatoia 

      //Vincoli
      if (requestConstraints(absenceEngine, currentAbsencePeriod, absence)
          .report.containsProblems()) {
        return absenceEngine;          
      }
      if (genericConstraints(absenceEngine, absence).report.containsProblems()) {
        return absenceEngine;          
      }
      if (groupConstraints(absenceEngine, currentAbsencePeriod, enhancedAbsence)
          .report.containsProblems()) {
        return absenceEngine;     
      }

      //Inserimento e riepilogo inserimento
      if (performInsert(absenceEngine, currentAbsencePeriod, absenceRequestType, 
          enhancedAbsence).periodChainSuccess) {
        return absenceEngine;
      }

      currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
    }

    //Esco e non sono mai riuscito a inferire il tipo.
    if (enhancedAbsence.isAbsenceTypeToInfer() && !enhancedAbsence.isAbsenceTypeInfered()) {
      absenceEngine.report.addRequestProblem(ReportRequestProblem.builder()
          .requestProblem(RequestProblem.CantInferAbsenceCode)
          .date(absenceEngine.currentDate())
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
      EnhancedAbsence enhancedAbsence) {

    Absence absence = enhancedAbsence.getAbsence();
    
    //simple grouping
    if (absencePeriod.groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.simpleGrouping)) {
      InsertResultItem responseItem = InsertResultItem.builder()
          .absence(absence)
          .absenceType(absence.getAbsenceType())
          .operation(Operation.insert)
          .date(absenceEngine.currentDate()).build();
      absenceEngine.report.insertResultItems.add(responseItem);
      absenceEngine.requestInserts.add(absence);
      absenceEngine.periodChainSuccess = true;
      return absenceEngine;
    }
    
    InsertResultItem absenceResultItem = InsertResultItem.builder()
        .absence(absence)
        .absenceType(absence.getAbsenceType())
        .operation(Operation.insert)
        .consumedResidualAmount(Lists.newArrayList())
        .date(absenceEngine.currentDate()).build();
    absenceEngine.report.insertResultItems.add(absenceResultItem);
    
    //Takable component
    if (absencePeriod.takableComponent.isPresent()) {

      TakableComponent takableComponent = absencePeriod.takableComponent.get();
      ConsumedResidualAmount consumedResidualAmount = ConsumedResidualAmount.builder()
          .amountType(takableComponent.takeAmountType)
          .totalResidual(takableComponent.getPeriodTakableAmount())
          .usedResidualBefore(takableComponent.getPeriodTakenAmount())
          .amount(enhancedAbsence.getJustifiedTime())
          .workingTime(absenceEngine.workingTime(absenceEngine.requestCurrentDate))
          .build();
      absenceResultItem.getConsumedResidualAmount().add(consumedResidualAmount);

      //Aggiungo l'assenza alle strutture dati per l'eventuale iterata successiva.
      takableComponent.addAbsenceTaken(enhancedAbsence);
      absenceEngine.requestInserts.add(absence);
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
          replacingAbsence.absenceType = replacingCode.get();
          replacingAbsence.date = absence.getAbsenceDate();
          replacingAbsence.justifiedType = replacingCode.get().replacingType; //capire
          
          InsertResultItem replacingResultItem = InsertResultItem.builder()
              .absence(replacingAbsence)
              .absenceType(replacingCode.get())
              .operation(Operation.insertReplacing)
              .date(absenceEngine.requestCurrentDate).build();
          
          absenceEngine.report.insertResultItems.add(replacingResultItem);
          absenceEngine.requestInserts.add(absence);
        }
      }
    }

    //success
    absenceEngine.periodChainSuccess = true;

    return absenceEngine; 
  }
  
  private AbsenceEngine genericConstraints(AbsenceEngine absenceEngine, Absence absence) {
    
    //Codice non prendibile nei giorni di festa ed è festa.
    if (!absence.absenceType.consideredWeekEnd && personDayManager.isHoliday(absenceEngine.requestPerson,
        absence.getAbsenceDate())) {
      absenceEngine.report.addAbsenceProblem(ReportAbsenceProblem.builder()
          .absenceProblem(AbsenceProblem.NotOnHoliday)
          .absence(absence).build());
    }
    
    //Un codice giornaliero già presente
    //TODO: requestIntervalAbsences si può fare meglio..
    for (Absence oldAbsence : absenceEngine.requestIntervalAbsences()) {
      if (oldAbsence.isPersistent() && absence.isPersistent() && oldAbsence.equals(absence)) {
        continue;
      }
      if (oldAbsence.getAbsenceDate().isEqual(absenceEngine.currentDate())) {
        if (oldAbsence.justifiedType.name.equals(JustifiedTypeName.all_day) 
            || oldAbsence.justifiedType.name.equals(JustifiedTypeName.assign_all_day)) {
          InsertResultItem responseItem = InsertResultItem.builder()
              .absence(absence)
              .absenceType(absence.getAbsenceType())
              .operation(Operation.insert)
              .date(absenceEngine.currentDate())
              .absenceProblem(AbsenceProblem.AllDayAlreadyExists).build();
          absenceEngine.report.insertResultItems.add(responseItem);
        }
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
          .date(absenceEngine.currentDate())
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
      AbsencePeriod absencePeriod, EnhancedAbsence enhancedAbsence) {
    
    Absence absence = enhancedAbsence.getAbsence();
    
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
      
      if (!absencePeriod.takableComponent.get()
          .canAddTakenAmount(enhancedAbsence.getJustifiedTime())) {
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
        person, groupAbsenceType, date, null);

    if (groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.vacationsCnr) || 
        groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.compensatoryRestCnr)) {

      absenceEngine.report.addImplementationProblem(ReportImplementationProblem.builder()
          .implementationProblem(ImplementationProblem.UnimplementedTakableComplationGroup)
          .build());
      return absenceEngine;
    }

    buildPeriodChain(absenceEngine, null);

    return absenceEngine;
  }

  
}
