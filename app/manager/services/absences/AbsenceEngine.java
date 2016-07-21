package manager.services.absences;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.PersonChildrenDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.Builder;

import manager.services.absences.AbsenceEngine.ResponseItem.AbsenceOperation;
import manager.services.absences.AbsenceEngine.ResponseItem.AbsenceProblem;
import manager.services.absences.AbsenceEngine.ResponseItem.ConsumedResidualAmount;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import java.util.List;
import java.util.Set;

public class AbsenceEngine {
  
  private final AbsenceDao absenceDao;
  private final PersonChildrenDao personChildrenDao;
  private final AbsenceMigration absenceMigration;


  @Inject
  public AbsenceEngine(AbsenceDao absenceDao, PersonChildrenDao personChildrenDao,
      AbsenceMigration absenceMigration) {
    this.absenceDao = absenceDao;
    this.personChildrenDao = personChildrenDao;
    this.absenceMigration = absenceMigration;
  }
  
 
  
  public AbsenceEngineInstance buildAbsenceEngineInstance(Person person, GroupAbsenceType groupAbsenceType,
      LocalDate date) {
    
    AbsenceEngineInstance engineInstance = new AbsenceEngineInstance(absenceDao, absenceMigration, 
        personChildrenDao, person, groupAbsenceType, date);
    
    if (groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.vacationsCnr) || 
        groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.compensatoryRestCnr)) {
      
      // TODO: Implementare costruzione ferie e riposi compensativi
      engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
      return engineInstance;
    }
    
    buildEngineAbsencePeriods(engineInstance, null);
    if (engineInstance.absenceEngineProblem.isPresent()) {
      return engineInstance;
    }
    
    // Assegnare ad ogni periodo le assenze di competenza (fase da migliorare) e calcoli
    AbsencePeriod absencePeriod = engineInstance.absencePeriod;
    while (absencePeriod != null) {
      for (Absence absence : engineInstance.getAbsences()) {
        if (!DateUtility
            .isDateIntoInterval(absence.personDay.date, absencePeriod.periodInterval())) {
          continue;
        }
        if (absencePeriod.takableComponent.isPresent()) {
          if (absencePeriod.takableComponent.get().takableCodes.contains(absence.absenceType)) {
            absencePeriod.takableComponent.get().takenAbsences.add(absence);
          }
        }
        if (absencePeriod.complationComponent.isPresent()) {
          if (absencePeriod.complationComponent.get().replacingCodes.contains(absence.absenceType)) {
            absencePeriod.complationComponent.get().replacingAbsences.add(absence);
          }
          if (absencePeriod.complationComponent.get().complationCodes.contains(absence.absenceType)) {
            absencePeriod.complationComponent.get().complationAbsences.add(absence);
          }
        }
      }
      absencePeriod = absencePeriod.nextAbsencePeriod;
    }
    
    // Altre Computazioni di supporto: takenAmount 
    AbsencePeriod currentAbsencePeriod = engineInstance.absencePeriod;
    while (currentAbsencePeriod != null) {
      if (currentAbsencePeriod.takableComponent.isPresent()) {
        TakableComponent takableComponent = currentAbsencePeriod.takableComponent.get();
        takableComponent.periodTakenAmount = 0;
        for (Absence absence : takableComponent.takenAbsences) {
          long amount = computeAbsenceAmount(engineInstance, absence, takableComponent.takeAmountType);
          if (amount < 0) {
            engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
            return engineInstance;
          }
          takableComponent.periodTakenAmount += amount; 
        }
      }
      currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
    }
    
    return engineInstance;
   
  }
  
  /**
   * Costruisce le date dell'AbsencePeriod relativo all'istanza. 
   * Se il gruppo è ricorsivo costruisce anche le date dei periodi seguenti.
   * @param engineInstance
   * @return
   */
  private AbsencePeriod buildEngineAbsencePeriods(AbsenceEngineInstance engineInstance, 
      AbsencePeriod previousAbsencePeriod) { 
   
    if (engineInstance.absencePeriod != null) {
      // TODO: Implementare logica di verifica data 
      // richiesta compatibile col precedente absencePeriod
      engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
      return engineInstance.absencePeriod;
    }
    
    AbsencePeriod currentAbsencePeriod;
    
    if (previousAbsencePeriod == null) {
      //Primo absencePeriod
      currentAbsencePeriod = new AbsencePeriod(engineInstance.groupAbsenceType);
      engineInstance.absencePeriod = currentAbsencePeriod;
    } else {
      //Seguenti
      currentAbsencePeriod = new AbsencePeriod(previousAbsencePeriod.groupAbsenceType.nextGroupToCheck);
      currentAbsencePeriod.previousAbsencePeriod = previousAbsencePeriod; //vedere se serve...
    }

    // recuperare l'inizializzazione (questo lo posso fare anche fuori) per i fix sulle date.

    if (currentAbsencePeriod.groupAbsenceType.periodType.equals(PeriodType.year)) {
      currentAbsencePeriod.from = new LocalDate(engineInstance.date.getYear(), 1, 1);
      currentAbsencePeriod.to = new LocalDate(engineInstance.date.getYear(), 12, 31);
    } else if (currentAbsencePeriod.groupAbsenceType.periodType.equals(PeriodType.month)) {
      currentAbsencePeriod.from = engineInstance.date.dayOfMonth().withMinimumValue();
      currentAbsencePeriod.to = engineInstance.date.dayOfMonth().withMaximumValue();
    } else if (currentAbsencePeriod.groupAbsenceType.periodType.equals(PeriodType.always)) {
      currentAbsencePeriod.from = null;
      currentAbsencePeriod.to = null;
    }

    // Caso inerente i figli.
    else if (currentAbsencePeriod.groupAbsenceType.periodType.isChildPeriod()) {
      try {
        DateInterval childInterval = currentAbsencePeriod.groupAbsenceType.periodType
            .getChildInterval(engineInstance.getOrderedChildren()
                .get(currentAbsencePeriod.groupAbsenceType.periodType.childNumber).bornDate);
        currentAbsencePeriod.from = childInterval.getBegin();
        currentAbsencePeriod.to = childInterval.getEnd();
      } catch (Exception e) {
        engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.noChildExist);
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

      takableComponent.periodTakableAmount = takableBehaviour.fixedLimit * 100;
      if (takableBehaviour.takableAmountAdjustment != null) {
        // TODO: ex. workingTimePercent
        engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
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
          buildEngineAbsencePeriods(engineInstance, currentAbsencePeriod);
    }
 
    return currentAbsencePeriod;
  }

  /**
   * Quanto giustifica l'assenza passata.
   * Se non si riesce a stabilire il tempo giustificato si ritorna un numero negativo.
   * @param person
   * @param date
   * @param absence
   * @param amountType
   * @return
   */
  private int computeAbsenceAmount(AbsenceEngineInstance engineInstance, Absence absence, 
      AmountType amountType) {
    
    int amount = 0;

    if (absence.justifiedType.name.equals(JustifiedTypeName.nothing)) {
      amount = 0;
    } 
    else if (absence.justifiedType.name.equals(JustifiedTypeName.all_day)) {
      amount = engineInstance.workingTime(engineInstance.date);
    } 
    else if (absence.justifiedType.name.equals(JustifiedTypeName.half_day)) {
      amount = engineInstance.workingTime(engineInstance.date) / 2;
    }
    else if (absence.justifiedType.name.equals(JustifiedTypeName.missing_time) ||
        absence.justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
      // TODO: quello che manca va implementato. Occorre persistere la dacisione di quanto manca
      // se non si vogliono fare troppi calcoli.
      if (absence.justifiedMinutes == null) {
        amount = -1;
      } else {
        amount = absence.justifiedMinutes;
      }
    }
    else if (absence.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
      amount = absence.absenceType.justifiedTime;
    }
    else if (absence.justifiedType.name.equals(JustifiedTypeName.assign_all_day)) {
      amount = -1;
    }
    
    if (amountType.equals(AmountType.units)) {
      int work = engineInstance.workingTime(engineInstance.date);
      int result = amount * 100 / work;
      return result;
    } else {
      return amount;
    }
    
  }
  
  /**  
   * 
   * @param engineInstance
   * @param absenceRequestType
   * @param absenceType
   * @param justifiedType
   * @param specifiedMinutes
   * @return
   */
  public AbsenceEngineInstance doRequest(AbsenceEngineInstance engineInstance, 
      AbsenceRequestType absenceRequestType, AbsenceType absenceType, JustifiedType justifiedType, 
      Optional<Integer> specifiedMinutes) {
    
    if (!absenceType.justifiedTypesPermitted.contains(justifiedType)) {
      engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.wrongJustifiedType);
      return engineInstance;
    }
    
    // Costruzione assenza.
    Absence absence = new Absence();
    absence.date = engineInstance.date;
    absence.absenceType = absenceType;
    absence.justifiedType = justifiedType;
    if (specifiedMinutes.isPresent()) {
      absence.justifiedMinutes = specifiedMinutes.get();
    }
    
    // Provo a inserire l'assenza in ogni periodo della catena...
    AbsencePeriod currentAbsencePeriod = engineInstance.absencePeriod;
    while (currentAbsencePeriod != null) {
      
      if (DateUtility.isDateIntoInterval(engineInstance.date, currentAbsencePeriod.periodInterval())) {
        checkRequest(engineInstance, currentAbsencePeriod, absenceRequestType, absence);
        if (engineInstance.absenceEngineProblem.isPresent()) {
          return engineInstance;
        }
      }
      
      currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
    }
    
    return engineInstance;
  }
  
  /**
   * 
   * @param absencePeriod
   * @param absenceType
   * @param date
   * @return
   */
  private AbsenceEngineInstance checkRequest(AbsenceEngineInstance engineInstance, 
      AbsencePeriod absencePeriod, AbsenceRequestType absenceRequestType,
      Absence absence) {
    
    //simple grouping
    // TODO: bisogna capire dove inserire i controlli di compatibilità (ex. festivo, assenze lo stesso giorno etc)  
    if (absencePeriod.groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.simpleGrouping)) {
      ResponseItem responseItem = new ResponseItem(absence.absenceType, 
          AbsenceOperation.insert, engineInstance.date);
      engineInstance.responseItems.add(responseItem);
    }

    //Struttura del caso base di risposta (senza errori di superamento tetto o completamento errato)
    
    // [A] Response item pre operazione 
    //1) Rimanenza completamento precedente 
    //2) Residuo taken/takable 

    // [B] Response item inserimento codice richiesto
    //1) Consumo taken/takable
    
    // [C] Response item inserimento codice completamento
    //1) Nothing
    
    // [D] Response item post operazione
    //1) Rimanenza completamento susseguente
    //2) Residuo taken/takable
    
    // Se c'è un limite di tetto 
    //          -> Calcolare residuo takable alla data e verificare la prendibilità
    if (absencePeriod.takableComponent.isPresent()) {
      
      TakableComponent takableComponent = absencePeriod.takableComponent.get();
      
      if (!takableComponent.takableCodes.contains(absence.absenceType)) {
        engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.absenceCodeNotAllowed);
        return engineInstance;
      }

      ResponseItem responseItem = new ResponseItem(absence.absenceType, 
          AbsenceOperation.insert, engineInstance.date);
      
      ConsumedResidualAmount consumedResidualAmount = ConsumedResidualAmount.builder()
          .amountType(takableComponent.takeAmountType)
          .totalResidual(takableComponent.computeTakableAmount())
          .usedResidualBefore(takableComponent.computeTakenAmount())
          .amount(computeAbsenceAmount(engineInstance, absence, takableComponent.takeAmountType))
          .workingTime(engineInstance.workingTime(engineInstance.date))
          .build();
      if (consumedResidualAmount.canTake()) {
        responseItem.consumedResidualAmount.add(consumedResidualAmount);
        responseItem.absence = absence;
      } else {
        responseItem.consumedResidualAmount.add(consumedResidualAmount);
        responseItem.absenceProblem = AbsenceProblem.limitExceeded;
      }
      
      engineInstance.responseItems.add(responseItem);
    }
    
    // Se il codice da prendere appartiene a complationCodes 
    //          -> Calcolare il residuo di completamento alla data
    
    
    
    //Complation component
    if (absencePeriod.complationComponent.isPresent()) {
    }

    return engineInstance; 
  }

  /**
   * Il valore già utilizzato da inizializzazione.
   * @return
   */
  @SuppressWarnings("unused")
  private int computeInitialComplationPercent() {
    // TODO: recuperare la percentuale inizializzazione quando ci sarà.
    return 0;
  }
  
  /**
   * La data cui si riferisce la percentuale inizializzazione.
   * @param absencePeriod
   * @return
   */
  @SuppressWarnings("unused")
  private LocalDate getInitialComplationDate(AbsencePeriod absencePeriod) {
    // TODO: utilizzare le strutture dati quando ci saranno.
    return absencePeriod.from;
  }


  public static class AbsencePeriod {

    public GroupAbsenceType groupAbsenceType;

    /*Period*/
    public LocalDate from;                      // Data inizio
    public LocalDate to;                        // Data fine

    public Optional<TakableComponent> takableComponent;
    public Optional<ComplationComponent> complationComponent;
    
    /*Next Period*/
    public AbsencePeriod nextAbsencePeriod;     // Puntatore al periodo successivo ->
    public AbsencePeriod previousAbsencePeriod; // <- puntatore al periodo precedente
    
    public AbsencePeriod(GroupAbsenceType groupAbsenceType) {
      this.groupAbsenceType = groupAbsenceType;
    }
    
    public DateInterval periodInterval() {
      return new DateInterval(from, to);
    }
  }
  
  public static class TakableComponent {

    public AmountType takeAmountType;                // Il tipo di ammontare del periodo
    
    public TakeCountBehaviour takableCountBehaviour; // Come contare il tetto totale
    private int periodTakableAmount;                  // Il tetto massimo
    
    public TakeCountBehaviour takenCountBehaviour;   // Come contare il tetto consumato
    private int periodTakenAmount;                    // Il tetto consumato
    
    public Set<AbsenceType> takableCodes;            // I tipi assenza prendibili del periodo
    public Set<AbsenceType> takenCodes;              // I tipi di assenza consumati del periodo

    public List<Absence> takenAbsences = Lists.newArrayList();           // Le assenze consumate
    
    public int computeTakableAmount() {
      if (!takableCountBehaviour.equals(TakeCountBehaviour.period)) {
        // TODO: sumAllPeriod, sumUntilPeriod; 
      }
      return this.periodTakableAmount;
    }
    
    public int computeTakenAmount() {
      if (!takenCountBehaviour.equals(TakeCountBehaviour.period)) {
        // TODO: sumAllPeriod, sumUntilPeriod; 
      } 
      return periodTakenAmount;
    }
  }
  
  public static class ComplationComponent {

    public AmountType complationAmountType;     // Tipo di ammontare completamento
    
    public int complationLimitAmount;           // Limite di completamento
    public int complationConsumedAmount;        // Ammontare completamento attualmente consumato
    
    public Set<AbsenceType> replacingCodes;     // Codici di rimpiazzamento      
    public Set<AbsenceType> complationCodes;    // Codici di completamento
    
    public List<Absence> replacingAbsences = Lists.newArrayList();     // Le assenze di rimpiazzamento (solo l'ultima??)     
    public List<Absence> complationAbsences = Lists.newArrayList();    // Le assenze di completamento
  }
  

  
  
  public enum ComputeAmountRestriction {
    workingTimePercent, workingPeriodPercent;
  }
  
  public enum AbsenceRequestType {
    insert, cancel; // insertSimulated, cancelSimulated;
  }
  
  public enum AbsenceEngineProblem {
    wrongJustifiedType,   // quando il tipo giustificativo non è supportato o identificabile
    noChildExist,         // quando provo a assegnare una tutela per figlio non inserito
    dateOutOfContract,    // quando provo assengare esempio ferie fuori contratto
    absenceCodeNotAllowed,// se passo un codice di assenza da inserire non prendibile
    unsupportedOperation; // ancora non implementato
  }
  
  /**
   * Record di esito inserimento assenza.
   * 
   * @author alessandro
   *
   */
  public static class ResponseItem {

    public enum AbsenceOperation {
      insert, insertComplation, remainingBefore, remainingAfter, cancel;
    }
    
    public enum AbsenceProblem {
      limitExceeded, wrongComplationPosition;
    }
    
    @Builder
    public static class ConsumedResidualAmount {
      
      public AmountType amountType;                     // | units | minutes | units |
      public int amount;                               // | 02:00 | 01:00   |   1   |
      public int workingTime;                           // | 07:12 |
      
      public String printAmount(int amount) {
        if (amountType.equals(AmountType.units)) {
          int units = amount / 100;
          int percent = amount % 100;
          return units + " + " + percent + "%";
        }
        return amount + "";
      }
      
      public String residualName;       // | res. anno passato | residuo anno corrente | lim. anno |
      public LocalDate expireResidual;  // |     31/03/2016    |      31/03/2017       | 31/12/2016| 
      public int totalResidual;         // |     20:00         |      07:00            |    28     |
      public int usedResidualBefore;    // |     00:00         |      00:00            |    6      |
      
      public int residualBefore() {
        return this.totalResidual - this.usedResidualBefore;
      }

      public int residualAfter() {
        return this.residualBefore() - this.amount;
      }
      
      public boolean canTake() {
        return residualAfter() >= 0;
      }
    }
    
    public LocalDate date;
    public Absence absence;
    public AbsenceType absenceType;
    public AbsenceOperation operation;
    public List<ConsumedResidualAmount> consumedResidualAmount = Lists.newArrayList();
    public AbsenceProblem absenceProblem;
    
    public ResponseItem(AbsenceType absenceType, AbsenceOperation operation, LocalDate date) {
      this.absenceType = absenceType;
      this.operation = operation;
      this.date = date;
    }
  }
  
      
}
