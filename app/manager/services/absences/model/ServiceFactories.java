package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dao.PersonReperibilityDayDao;
import dao.PersonShiftDayDao;
import dao.absences.AbsenceComponentDao;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayManager;
import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.errors.CriticalError.CriticalProblem;
import manager.services.absences.errors.ErrorsBox;
import models.Contract;
import models.Person;
import models.PersonChildren;
import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.InitializationGroup;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;
import models.absences.definitions.DefaultAbsenceType;
import org.joda.time.LocalDate;

@Slf4j
public class ServiceFactories {

  private AbsenceEngineUtility absenceEngineUtility;
  private AbsenceComponentDao absenceComponentDao;
  private PersonDayManager personDayManager;
  private final PersonReperibilityDayDao personReperibilityDayDao;
  private final PersonShiftDayDao personShiftDayDao;
  private final VacationFactory vacationFactory;

  /**
   * Constructor.
   * @param absenceEngineUtility inj
   * @param absenceComponentDao inj
   * @param personDayManager inj
   */
  @Inject
  public ServiceFactories(AbsenceEngineUtility absenceEngineUtility, 
      AbsenceComponentDao absenceComponentDao, PersonDayManager personDayManager, 
      PersonReperibilityDayDao personReperibilityDayDao, 
      PersonShiftDayDao personShiftDayDao, VacationFactory vacationFactory) {
    this.absenceEngineUtility = absenceEngineUtility;
    this.absenceComponentDao = absenceComponentDao;
    this.personDayManager = personDayManager;
    this.personReperibilityDayDao = personReperibilityDayDao;
    this.personShiftDayDao = personShiftDayDao;
    this.vacationFactory = vacationFactory;
    
  }
  
  /**
   * Costruttore per richiesta di scan.
   * @param person persona
   * @param scanFrom scanFrom
   * @param absencesToScan le assenze da scannerizzare
   * @param orderedChildren i figli ordinati per data di nascita
   * @param fetchedContracts i contratti
   * @return scanner
   */
  public Scanner buildScanInstance(Person person, LocalDate scanFrom, 
      List<Absence> absencesToScan, List<PersonChildren> orderedChildren, 
      List<Contract> fetchedContracts, List<InitializationGroup> initializationGroups) {
    Scanner absenceEngineScan = new Scanner(person, scanFrom, 
        absencesToScan, orderedChildren, fetchedContracts, initializationGroups,
        this, absenceEngineUtility, personDayManager);
    for (Absence absence : absenceEngineScan.absencesToScan) {
      Set<GroupAbsenceType> groupsToScan = absenceEngineUtility.involvedGroup(absence.absenceType); 
      absenceEngineScan.absencesGroupsToScan.put(absence, groupsToScan);
    }
    return absenceEngineScan;
  }
  

  /**
   * Costruisce lo stato del gruppo (la periodChain). <br>
   * absenceToInsert se presente viene inserita nella computazione del gruppo per la valutazione
   * della soundness.
   * @param person persona
   * @param groupAbsenceType gruppo
   * @param date data
   * @param previousInserts gli inserimenti di successo precedenti (optional)
   * @param absenceToInsert la nuova assenza da inserire (optional)
   * @param orderedChildren la lista dei figli ordinati per data di nascita
   * @param fetchedContracts i contratti
   * @return periodChain
   */
  public PeriodChain buildPeriodChain(
      Person person, GroupAbsenceType groupAbsenceType, LocalDate date,
      List<Absence> previousInserts,
      Absence absenceToInsert,
      List<PersonChildren> orderedChildren, List<Contract> fetchedContracts, 
      List<InitializationGroup> initializationGroups) { 
    
    //1 costruire i periods
    PeriodChain periodChain = buildPeriodChainPhase1(person, groupAbsenceType, date, 
        orderedChildren, fetchedContracts, initializationGroups, previousInserts);

    List<Absence> allPersistedAbsences = Lists.newArrayList();
    List<Absence> groupPersistedAbsences = Lists.newArrayList();

    //DAO fetch delle assenze (una volta ottenuti i limiti temporali della catena)
    // separato per inject nei test
    if (groupAbsenceType.pattern == GroupAbsenceTypePattern.simpleGrouping) {
      if (absenceToInsert == null) {
        return periodChain;
      }
      allPersistedAbsences = absenceComponentDao.orderedAbsences(periodChain.person, 
          absenceToInsert.getAbsenceDate().minusDays(7),    //costante da inserire nel vincolo
          absenceToInsert.getAbsenceDate().plusDays(7),     //del week end 
          Sets.newHashSet());
    } else {
      allPersistedAbsences = absenceComponentDao.orderedAbsences(periodChain.person, 
          periodChain.from, periodChain.to, Sets.newHashSet());
      groupPersistedAbsences = absenceComponentDao.orderedAbsences(periodChain.person, 
          periodChain.from, periodChain.to, 
          periodChain.periodChainInvolvedCodes());
    }

    //2 assegnare ad ogni periodo le assenze di competenza e calcoli
    if (groupAbsenceType.pattern == GroupAbsenceTypePattern.simpleGrouping) {

      insertAbsenceInSimpleGroup(periodChain, allPersistedAbsences, absenceToInsert);

    } else {
      buildPeriodChainPhase2(periodChain, absenceToInsert, 
          allPersistedAbsences, groupPersistedAbsences, fetchedContracts);
    }

    completePeriodChain(periodChain);
    
    return periodChain;
  }

  /**
   * Prima fase di costruzione (generazione dei periodi).
   * @param person person
   * @param groupAbsenceType group
   * @param date date
   * @param orderedChildren orderedChildren
   * @param initializationGroups initializationGroups
   * @param previousInserts previousInserts
   * @return periodChain phase1
   */
  public PeriodChain buildPeriodChainPhase1(Person person, GroupAbsenceType groupAbsenceType, 
      LocalDate date, List<PersonChildren> orderedChildren, List<Contract> fetchedContracts,
      List<InitializationGroup> initializationGroups, List<Absence> previousInserts) {
    
    PeriodChain periodChain = new PeriodChain(person, groupAbsenceType, date);

    if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.vacationsCnr)) {
      periodChain = vacationFactory
          .buildVacationChain(person, groupAbsenceType, fetchedContracts, date);
    } else if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.compensatoryRestCnr)) {
      //TODO: implementare la migrazione riposi compensativi. Una volta completata 
      // riattivare lo scan del gruppo 
      throw new IllegalStateException();
    } else {
      GroupAbsenceType currentGroup = groupAbsenceType;
      while (currentGroup != null) {
        AbsencePeriod currentPeriod = buildAbsencePeriod(person, currentGroup, date, 
            orderedChildren, initializationGroups);
        if (!currentPeriod.ignorePeriod) { 
          periodChain.periods.add(currentPeriod);  
        }

        if (currentPeriod.errorsBox.containsCriticalErrors()) {
          return periodChain;
        }
        currentGroup = currentGroup.nextGroupToCheck;
      }
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
    
    periodChain.previousInserts = previousInserts;
    
    return periodChain;
  }
  
  /**
   * Seconda fase di costruzione dispatch ed analisi delle assenze (esistenti, inserite e
   * da inserire). 
   * @param periodChain periodChain
   * @param absenceToInsert assenza da inserire
   * @param allPersistedAbsences tutte le assenze persistite nel periodo (tutti i tipi)
   * @param groupPersistedAbsences le assenze persistite nel periodo (relative al gruppo)
   */
  public void buildPeriodChainPhase2(PeriodChain periodChain, 
      Absence absenceToInsert, List<Absence> allPersistedAbsences, 
      List<Absence> groupPersistedAbsences, List<Contract> fetchedContracts) {
    
    periodChain.allInvolvedAbsences = absenceEngineUtility
        .mapAbsences(allPersistedAbsences, null);
    periodChain.allInvolvedAbsences = absenceEngineUtility
        .mapAbsences(periodChain.previousInserts, periodChain.allInvolvedAbsences);
    
    periodChain.involvedAbsencesInGroup = absenceEngineUtility
        .orderAbsences(groupPersistedAbsences, periodChain.previousInserts);
    
    boolean typeToInfer = (absenceToInsert != null && absenceToInsert.getAbsenceType() == null);

    //Dispatch di tutte le assenze coinvolte gruppo e inserimenti precedenti
    for (AbsencePeriod absencePeriod : periodChain.periods) {
      for (Absence absence : absencePeriod
          .filterAbsencesInPeriod(periodChain.involvedAbsencesInGroup)) {
        dispatchAbsenceInPeriod(periodChain, absencePeriod, absence, fetchedContracts);
      }
    }

    //Gestione assenza da inserire (per ultima)
    for (AbsencePeriod absencePeriod : periodChain.periods) {
      
      boolean successInsertInPeriod = 
          insertAbsenceInPeriod(periodChain, absencePeriod, absenceToInsert, fetchedContracts);

      // Se la situazione non è compromessa eseguo lo scan dei rimpiazzamenti
      if (!absencePeriod.containsCriticalErrors() && !absencePeriod.isCompromisedComplation()) {
        absencePeriod.computeCorrectReplacingInPeriod(absenceEngineUtility);
      }
      
      if (successInsertInPeriod) {
        periodChain.previousInserts.add(absenceToInsert);
        periodChain.successPeriodInsert = absencePeriod;
        return;
      }
      if (typeToInfer) {
        //Preparare l'assenza da inserire nel prossimo gruppo (generalmente si resetta absenceType
        // nuovo oggetto identico per avere una istanza nuova indipendente da inserire nei 
        //report)
        Absence nextAbsenceToInsert = new Absence();
        nextAbsenceToInsert.date = absenceToInsert.getAbsenceDate();
        nextAbsenceToInsert.justifiedType = absenceToInsert.getJustifiedType();
        nextAbsenceToInsert.justifiedMinutes = absenceToInsert.getJustifiedMinutes();
        absenceToInsert = nextAbsenceToInsert;
      }
    } 
    
    //Le assenze non assegnate
    for (Absence absence : periodChain.involvedAbsencesInGroup) {
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
  
  private AbsencePeriod buildAbsencePeriod(Person person, GroupAbsenceType groupAbsenceType, 
      LocalDate date, 
      List<PersonChildren> orderedChildren, 
      List<InitializationGroup> initializationGroup) {
    
    AbsencePeriod absencePeriod = new AbsencePeriod(person, groupAbsenceType);
    
    if (absencePeriod.groupAbsenceType.getPeriodType().equals(PeriodType.year)) {
      absencePeriod.from = new LocalDate(date.getYear(), 1, 1);
      absencePeriod.to = new LocalDate(date.getYear(), 12, 31);
    } else if (absencePeriod.groupAbsenceType.getPeriodType().equals(PeriodType.month)) {
      absencePeriod.from = date.dayOfMonth().withMinimumValue();
      absencePeriod.to = date.dayOfMonth().withMaximumValue();
    } else if (absencePeriod.groupAbsenceType.getPeriodType().equals(PeriodType.always)) {
      absencePeriod.from = null;
      absencePeriod.to = null;
    } else if (absencePeriod.groupAbsenceType.getPeriodType().isChildPeriod()) {
      // Caso inerente i figli.
      try {
        DateInterval childInterval = absencePeriod.groupAbsenceType.getPeriodType()
            .getChildInterval(orderedChildren
                .get(absencePeriod.groupAbsenceType.getPeriodType().childNumber - 1).bornDate);
        absencePeriod.from = childInterval.getBegin();
        absencePeriod.to = childInterval.getEnd();
        if (!DateUtility.isDateIntoInterval(date, childInterval)) {
          absencePeriod.ignorePeriod = true;
          return absencePeriod; 
        }
      } catch (Exception ex) {
        absencePeriod.ignorePeriod = true;
        return absencePeriod;
      }
    }
    
    // recuperare l'inizializzazione
    for (InitializationGroup initialization : initializationGroup) {
      if (initialization.groupAbsenceType.equals(groupAbsenceType) 
          && DateUtility.isDateIntoInterval(initialization.date, 
              absencePeriod.periodInterval())) {
        absencePeriod.initialization = initialization;
      }
    }
    
    // Parte takable
    if (absencePeriod.groupAbsenceType.getTakableAbsenceBehaviour() != null) {

      TakableAbsenceBehaviour takableBehaviour = 
          absencePeriod.groupAbsenceType.getTakableAbsenceBehaviour();

      absencePeriod.takeAmountType = takableBehaviour.getAmountType();

      absencePeriod.setFixedPeriodTakableAmount(takableBehaviour.getFixedLimit());
      if (takableBehaviour.getTakableAmountAdjustment() != null) {
        
        Integer adjustment = absenceEngineUtility.takableAmountAdjustment(absencePeriod, date,
            takableBehaviour.getFixedLimit(), takableBehaviour.getTakableAmountAdjustment(), 
            absencePeriod.periodInterval(), person.getContracts());
        
        if (absencePeriod.containsCriticalErrors()) {
          return absencePeriod;
        }
      
        absencePeriod.setFixedPeriodTakableAmount(adjustment);

      }
      if (absencePeriod.initialization != null 
          && absencePeriod.initialization.takableTotal != null) {
        absencePeriod.setFixedPeriodTakableAmount(absencePeriod.initialization.takableTotal);
      }

      absencePeriod.takableCountBehaviour = TakeCountBehaviour.period;
      absencePeriod.takenCountBehaviour = TakeCountBehaviour.period;

      absencePeriod.takenCodes = takableBehaviour.getTakenCodes();
      absencePeriod.takableCodes = takableBehaviour.getTakableCodes();

    }
    
    if (absencePeriod.groupAbsenceType.getComplationAbsenceBehaviour() != null) {
      
      ComplationAbsenceBehaviour complationBehaviour = 
          absencePeriod.groupAbsenceType.getComplationAbsenceBehaviour();
      
      absencePeriod.complationAmountType = complationBehaviour.getAmountType();
      absencePeriod.complationCodes = complationBehaviour.getComplationCodes();
      
      // i codici di rimpiazzamento li preparo ordinati per ammontare di rimpiazzamento
      absenceEngineUtility.setReplacingCodesDesc(
          absencePeriod.complationAmountType, complationBehaviour.getReplacingCodes(), date, //final
          absencePeriod.replacingCodesDesc, absencePeriod.errorsBox);                        //edit
      // genero la mappa inversa
      for (Integer amount : absencePeriod.replacingCodesDesc.keySet()) {
        List<AbsenceType> absenceTypes = absencePeriod.replacingCodesDesc.get(amount);
        for (AbsenceType absenceType : absenceTypes) {
          absencePeriod.replacingTimes.put(absenceType, amount);          
        }
      }
    }
    
    return absencePeriod;
  }

  private void dispatchAbsenceInPeriod(PeriodChain periodChain, 
      AbsencePeriod absencePeriod, 
      Absence absence, List<Contract> fetchedContracts) {
    
    //se il period ha problemi critici esco
    if (absencePeriod.containsCriticalErrors()) {
      return;
    }
    
    //computo il ruolo dell'assenza nel period
    boolean isTaken = false;
    boolean isComplation = false;
    boolean isReplacing = false;
    if (absencePeriod.isTakable()) {
      isTaken = absencePeriod.takenCodes.contains(absence.absenceType) 
        || absencePeriod.takableCodes.contains(absence.getAbsenceType());
    }
    if (absencePeriod.isComplation()) {
      isReplacing = absencePeriod.replacingTimes.keySet().contains(absence.getAbsenceType());
      isComplation = absencePeriod.complationCodes.contains(absence.getAbsenceType());
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
      
      TakableAbsenceBehaviour takableBehaviour = 
          absencePeriod.groupAbsenceType.getTakableAbsenceBehaviour();
      
      int takenAmount = absenceEngineUtility.absenceJustifiedAmount(absencePeriod.person,
          absence, absencePeriod.takeAmountType);
      
      if (takenAmount < 0) {
        absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.ImplementationProblem);
        absencePeriod.errorsBox.addCriticalError(absence, 
            CriticalProblem.IncalcolableJustifiedAmount);
        return;
      }
      
      takenAmount = absenceEngineUtility.takenBehaviouralFixes(absence, takenAmount, 
          fetchedContracts, absencePeriod.periodInterval(), 
          takableBehaviour.takableAmountAdjustment);
      
      TakenAbsence takenAbsence = absencePeriod.buildTakenAbsence(absence, takenAmount);
      if (!takenAbsence.canAddTakenAbsence()) {
        absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.LimitExceeded);
        absencePeriod.setLimitExceededDate(absence.getAbsenceDate());
      }
      if (takenAbsence.isBeforeInitialization()) {
        absencePeriod.errorsBox.addAbsenceWarning(absence, 
            AbsenceProblem.IgnoredBeforeInitialization);
      }
      absencePeriod.addTakenAbsence(takenAbsence);
    }

    if (isComplation) {

      if (absencePeriod.isCompromisedComplation()) {
        // TODO: devo aggiungere all'assenza gli errori di completamento riscontrati nel periodo
        // fino a quel punto
        absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.CompromisedTwoComplation);
      }
      int complationAmount = absenceEngineUtility.absenceJustifiedAmount(absencePeriod.person, 
          absence, absencePeriod.complationAmountType);
      if (complationAmount <= 0) {
        absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.ImplementationProblem);
        absencePeriod.errorsBox.addCriticalError(absence, 
            CriticalProblem.IncalcolableJustifiedAmount);
        return;
      }
      absencePeriod.addComplationAbsence(absence);
    }

    if (isReplacing) {
      if (absencePeriod.isCompromisedComplation()) {
        // TODO: devo aggiungere all'assenza gli errori di completamento riscontrati nel periodo
        // fino a quel punto
        absencePeriod.errorsBox.addAbsenceError(absence, AbsenceProblem.CompromisedTwoComplation);
      }
      absencePeriod.addReplacingAbsence(absence);
    }
  }
  
  private boolean insertAbsenceInPeriod(PeriodChain periodChain, AbsencePeriod absencePeriod, 
      Absence absenceToInsert, List<Contract> fetchedContracts) {
    if (absenceToInsert == null) {
      return false;
    }
    if (!DateUtility.isDateIntoInterval(absenceToInsert.getAbsenceDate(), 
        absencePeriod.periodInterval())) {
      return false;
    }
    
    if (absenceToInsert.getAbsenceType() == null) {
      absenceToInsert = absenceEngineUtility.inferAbsenceType(absencePeriod, absenceToInsert);
      if (absenceToInsert.getAbsenceType() == null) {
        return false;
      }
    }
    
    // i vincoli generici
    genericConstraints(absencePeriod.errorsBox, 
        periodChain.person, absenceToInsert, 
        periodChain.allInvolvedAbsences);
    
    // i vincoli dentro il periodo
    absencePeriod.attemptedInsertAbsence = absenceToInsert;
    dispatchAbsenceInPeriod(periodChain, absencePeriod, absenceToInsert, fetchedContracts);
    
    // sono riuscito a inserirla
    if (!absencePeriod.getDayInPeriod(absenceToInsert.getAbsenceDate())
        .containTakenAbsence(absenceToInsert)) {
      return false;
    }
    
    // gli esiti
    if (absencePeriod.errorsBox.containsCriticalErrors()) {
      return false;
    }
    if (absencePeriod.errorsBox.containAbsenceErrors(absenceToInsert)) {
      return false;
    }
    return true;

  }


  /**
   * Inserimento nel caso di GroupAbsenceTypePattern.simpleGrouping.
   * @param periodChain catena
   * @param absenceToInsert assenza da inserire
   * @return la catena con l'esito
   */
  private PeriodChain insertAbsenceInSimpleGroup(PeriodChain periodChain, 
      List<Absence> allPersistedAbsences, 
      Absence absenceToInsert) {

    //nel caso simple grouping controllo solo i vincoli generici per la nuova assenza da inserire
    if (absenceToInsert == null) { 
      return periodChain;
    }
    
    if (absenceToInsert.getAbsenceType() == null) {
      absenceToInsert = absenceEngineUtility
          .inferAbsenceType(periodChain.firstPeriod(), absenceToInsert);
      if (absenceToInsert.getAbsenceType() == null) {
        periodChain.errorsBox.addCriticalError(absenceToInsert, 
            CriticalProblem.CantInferAbsenceCode);
        return periodChain;
      }
    }

    periodChain.allInvolvedAbsences = absenceEngineUtility
        .mapAbsences(allPersistedAbsences, null);
    periodChain.allInvolvedAbsences = absenceEngineUtility
        .mapAbsences(periodChain.previousInserts, periodChain.allInvolvedAbsences);

    AbsencePeriod absencePeriod = periodChain.firstPeriod(); 
    absencePeriod.attemptedInsertAbsence = absenceToInsert;

    // i vincoli generici
    genericConstraints(absencePeriod.errorsBox, 
        periodChain.person, absenceToInsert, 
        periodChain.allInvolvedAbsences);
    TakenAbsence takenAbsence = absencePeriod.buildTakenAbsence(absenceToInsert, 0);
    absencePeriod.addTakenAbsence(takenAbsence);
    if (!absencePeriod.errorsBox.containsCriticalErrors() 
        && !absencePeriod.errorsBox.containAbsenceErrors(absenceToInsert)) {
      // successo
      periodChain.successPeriodInsert = absencePeriod;
    }

    return periodChain;
  }

  /**
   * I vincoli generici assenza.
   * @param genericErrors box errori
   * @param person persona
   * @param absence assenza
   * @param allCodeAbsences tutti i codici che potrebbero conflittuare.
   * @return error box
   */
  public ErrorsBox genericConstraints(ErrorsBox genericErrors, 
      Person person, Absence absence, 
      Map<LocalDate, Set<Absence>> allCodeAbsences) {

    final boolean isHoliday = personDayManager.isHoliday(person, absence.getAbsenceDate());

    //Codice non prendibile nei giorni di festa ed è festa.
    if (!absence.getAbsenceType().isConsideredWeekEnd() && isHoliday) {
      genericErrors.addAbsenceError(absence, AbsenceProblem.NotOnHoliday);
    } else {
      log.info("Controllo la reperibilità per {} nel giorno {}", person, absence.getAbsenceDate());
      //check sulla reperibilità
      if (!absence.absenceType.reperibilityCompatible && personReperibilityDayDao
          .getPersonReperibilityDay(person, absence.getAbsenceDate()).isPresent() 
          && !absence.getAbsenceType().reperibilityCompatible) {
        log.info("Aggiungere warning di reperibilità per {} in data {}", person, 
            absence.getAbsenceDate());
        genericErrors.addAbsenceWarning(absence, AbsenceProblem.InReperibility); 
      }
      log.info("Controllo i turni per {} nel giorno {}", person, absence.getAbsenceDate());
      if (personShiftDayDao.getPersonShiftDay(person, absence.getAbsenceDate()).isPresent()) {
        log.info("Aggiungere warning di turno per {} in data {}", person, absence.getAbsenceDate());
        genericErrors.addAbsenceWarning(absence, AbsenceProblem.InShift); 
      }
    }

    //Un codice giornaliero già presente 
    Set<Absence> dayAbsences = allCodeAbsences.get(absence.getAbsenceDate());
    if (dayAbsences == null) {
      dayAbsences = Sets.newHashSet();
    }
    for (Absence oldAbsence : dayAbsences) {
      //stessa entità
      if (/*oldAbsence.isPersistent() && absence.isPersistent() && */oldAbsence.equals(absence)) {
        continue;
      }
      //altra data
      if (!oldAbsence.getAbsenceDate().isEqual(absence.getAbsenceDate())) {
        continue;
      }
      //tempo giustificato non giornaliero
      if ((oldAbsence.getJustifiedType().getName().equals(JustifiedTypeName.all_day) 
          || oldAbsence.getJustifiedType().getName()
          .equals(JustifiedTypeName.assign_all_day)) == false) {
        continue;
      }
      genericErrors.addAbsenceError(absence, AbsenceProblem.AllDayAlreadyExists, oldAbsence);
    }

    // Violazione mimino e massimo
    if (absence.justifiedMinutes != null) {
      if (absence.violateMinimumTime()) {
        genericErrors.addAbsenceError(absence, AbsenceProblem.MinimumTimeViolated, absence);
      }
      if (absence.violateMaximumTime()) {
        genericErrors.addAbsenceError(absence, AbsenceProblem.MaximumTimeExceed, absence);
      }
    }
    
    if (absence.absenceType.code.equals(DefaultAbsenceType.A_661MO.getCode())
        && DateUtility.isDateIntoInterval(absence.getAbsenceDate(), 
              DateUtility.getYearInterval(2018)) 
        && absence.justifiedMinutes != null 
        && absence.justifiedMinutes >= 360) {
      genericErrors.addAbsenceWarning(absence, AbsenceProblem.Migration661);
    }
    
    if (absence.absenceType.isExpired(absence.getAbsenceDate())) {
      genericErrors.addAbsenceError(absence, AbsenceProblem.Expired, absence);
    }

    //TODO:
    // Strange weekend

    // Configuration qualification grant

    // DayLimitGroupCode


    return genericErrors;
  }

  /**
   * Azioni da compiere alla fine della costruzione della periodChain.
   */
  private void completePeriodChain(PeriodChain periodChain) {
    
    if (periodChain.groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.vacationsCnr)) {
      vacationFactory.removeAccruedFirstYear(periodChain);
    }  
    
  }


}
