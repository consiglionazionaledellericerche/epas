package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Lists;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.PersonChildrenDao;
import dao.WorkingTimeTypeDao;

import it.cnr.iit.epas.DateInterval;

import manager.services.absences.AbsenceEngine.ResponseItem.AbsenceProblem;

import models.Contract;
import models.Person;
import models.PersonChildren;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.InitializationGroup;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbsenceEngine {
  
  private final AbsenceTypeDao absenceTypeDao;
  private final AbsenceDao absenceDao;
  private final WorkingTimeTypeDao workingTimeTypeDao;
  private final PersonChildrenDao personChildrenDao;

  @Inject
  public AbsenceEngine(AbsenceTypeDao absenceTypeDao, AbsenceDao absenceDao, 
      WorkingTimeTypeDao workingTimeTypeDao, PersonChildrenDao personChildrenDao) {
    this.absenceTypeDao = absenceTypeDao;
    this.absenceDao = absenceDao;
    this.workingTimeTypeDao = workingTimeTypeDao;
    this.personChildrenDao = personChildrenDao;
  }
  
  public static class AbsenceEngineInstance {

    // Dati della richiesta
    public LocalDate date;
    public GroupAbsenceType groupAbsenceType;
    public Person person;
    
    // Errori
    public Optional<AbsenceEngineProblem> absenceEngineProblem = Optional.absent();

    // Strutture ausiliare lazy
    protected Contract contract = null;
    protected List<PersonChildren> orderedChildren = null;
    protected List<Absence> absences = null;
    protected InitializationGroup initializationGroup = null;
    
    // Ultima richiesta
    protected AbsencePeriod absencePeriod;
    protected List<ResponseItem> responseItems;
    
    protected AbsenceEngineInstance(Person person, GroupAbsenceType groupAbsenceType, 
        LocalDate date) {
      this.person = person;
      this.groupAbsenceType = groupAbsenceType;
      this.date = date;
    }
  }
  
  public void buildAbsenceEngineInstance(Person person, GroupAbsenceType groupAbsenceType,
      LocalDate date) {
    
    AbsenceEngineInstance engineInstance = new AbsenceEngineInstance(person, groupAbsenceType, date);
    
    if (groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.vacationsCnr) || 
        groupAbsenceType.pattern.equals(GroupAbsenceType.GroupAbsenceTypePattern.compensatoryRestCnr)) {
      
      // TODO: Implementare costruzione ferie e riposi compensativi
      engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
      return;
    }
    
    buildDefaultAbsencePeriod(engineInstance);
    
    
    
    //buildPeriod
    
    //buildTakable
    
    //buildComplation
   
  }
  
  /**
   * Costruisce l'AbsencePeriod relativo all'istanza. Se il gruppo è ricorsivo costruisce anche
   * i periodi seguenti.
   * @param engineInstance
   * @return
   */
  public void buildDefaultAbsencePeriod(AbsenceEngineInstance engineInstance) { 
   
    if (engineInstance.absencePeriod != null) {
      // TODO: Implementare logica di verifica data 
      // richiesta compatibile col precedente absencePeriod
      engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
      return;
    }
    
    GroupAbsenceType currentGroupAbsenceType = engineInstance.groupAbsenceType;
    engineInstance.absencePeriod = new AbsencePeriod();
    AbsencePeriod currentAbsencePeriod = engineInstance.absencePeriod;  
    while (true) {

      // recuperare l'inizializzazione (questo lo posso fare anche fuori) per i fix sulle date.

      if (currentGroupAbsenceType.periodType.equals(PeriodType.year)) {
        currentAbsencePeriod.from = new LocalDate(engineInstance.date.getYear(), 1, 1);
        currentAbsencePeriod.to = new LocalDate(engineInstance.date.getYear(), 12, 31);
        return;
      }
      if (currentGroupAbsenceType.periodType.equals(PeriodType.month)) {
        currentAbsencePeriod.from = engineInstance.date.dayOfMonth().withMinimumValue();
        currentAbsencePeriod.to = engineInstance.date.dayOfMonth().withMaximumValue();
        return;
      }
      if (currentGroupAbsenceType.periodType.equals(PeriodType.always)) {
        currentAbsencePeriod.from = null;
        currentAbsencePeriod.to = null;
        return;
      }

      // Caso inerente i figli.
      if (currentGroupAbsenceType.periodType.isChildPeriod()) {
        if (engineInstance.orderedChildren == null) {
          engineInstance.orderedChildren = 
              personChildrenDao.getAllPersonChildren(engineInstance.person);
        }
        try {
          DateInterval childInterval = currentGroupAbsenceType.periodType
              .getChildInterval(engineInstance.orderedChildren
                  .get(currentGroupAbsenceType.periodType.childNumber).bornDate);
          currentAbsencePeriod.from = childInterval.getBegin();
          currentAbsencePeriod.to = childInterval.getEnd();
          return;
        } catch (Exception e) {
          engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.noChildExist);
          return;
        }
      }
      
      if (currentGroupAbsenceType.nextGropToCheck == null) {
        break;
      }
      
      currentGroupAbsenceType = currentGroupAbsenceType.nextGropToCheck;
      currentAbsencePeriod.nextAbsencePeriod = new AbsencePeriod();
      currentAbsencePeriod.nextAbsencePeriod.previousAbsencePeriod = currentAbsencePeriod; //capire se serve.. 
      currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
    }
  }




  public void setComplationComponent(AbsenceEngineInstance engineInstance) {
    
//    //Scorciatoie ..
//    TakableAbsenceBehaviour takableAbsenceGroup = 
//        engineInstance.groupAbsenceType.takableAbsenceBehaviour;
//    AbsencePeriod absencePeriod = engineInstance.absencePeriod;
//    
//    if (groupAbsenceType.complationAbsenceBehaviour != null) {
//      
//      ComplationComponent complationComponent = new ComplationComponent();
//      
//      ComplationAbsenceBehaviour complationAbsenceGroup = groupAbsenceType.complationAbsenceBehaviour;
//      complationComponent.complationAmountType = complationAbsenceGroup.amountType;
//      
//      complationComponent.replacingCodes = complationAbsenceGroup.replacingCodes;
//      
//      complationComponent.complationCodes = complationAbsenceGroup.complationCodes;
//      
//      //TODO: le other absences vanno aggiunte!
//      complationComponent.replacingAbsences = 
//          absenceDao.getAbsencesInCodeList(person, absencePeriod.from, 
//          absencePeriod.to, Lists.newArrayList(complationComponent.replacingCodes), true);
//  
//      complationComponent.complationAbsences = 
//          absenceDao.getAbsencesInCodeList(person, absencePeriod.from, 
//          absencePeriod.to, Lists.newArrayList(complationComponent.complationCodes), true);
//      
//      complationComponent.complationConsumedAmount = 0;
//      for (Absence absence : Stream.concat(complationComponent.complationAbsences.stream(), 
//          complationComponent.replacingAbsences.stream()).collect(Collectors.toList())) {
//        
//          complationComponent.complationConsumedAmount += computeAbsenceAmount(person, date, 
//              absence.absenceType, complationComponent.complationAmountType);
//      }
//      
//      absencePeriod.complationComponent = Optional.of(complationComponent);
//            
//      //Un illegal state è  absencePeriod.complationConsumedAmount < 0 ...
//    }
//    
//    engineInstance.absencePeriod = absencePeriod;
//    
//    return absencePeriod;

  }

  public void setTakableComponent(AbsenceEngineInstance engineInstance) {
    
    if (engineInstance.groupAbsenceType.takableAbsenceBehaviour != null) {

      //Scorciatoie ..
      TakableAbsenceBehaviour takableAbsenceGroup = 
          engineInstance.groupAbsenceType.takableAbsenceBehaviour;
      AbsencePeriod absencePeriod = engineInstance.absencePeriod;
      
      TakableComponent takableComponent = new TakableComponent();
      takableComponent.takeAmountType = takableAbsenceGroup.amountType;

      takableComponent.periodTakableAmount = takableAbsenceGroup.fixedLimit;
      if (takableAbsenceGroup.takableAmountAdjustment != null) {
        // TODO: ex. workingTimePercent
        engineInstance.absenceEngineProblem = Optional.of(AbsenceEngineProblem.unsupportedOperation);
      }

      takableComponent.takableCountBehaviour = TakeCountBehaviour.period;
      takableComponent.takenCountBehaviour = TakeCountBehaviour.period;

      takableComponent.takenCodes = takableAbsenceGroup.takenCodes;
      takableComponent.takableCodes = takableAbsenceGroup.takableCodes;

      if (engineInstance.absences == null) {
        //Carico da db le assenze che mi servono
        // 1) Costruire l'intorno dei periodi (anche quelli ricorsivi)
        
        // 2) Prendere tutti i codici (anche quelli ricorsivi)
      }
      takableComponent.takenAbsences = absenceDao.getAbsencesInCodeList(engineInstance.person, 
          absencePeriod.from, absencePeriod.to, Lists.newArrayList(takableComponent.takenCodes), true);

      takableComponent.periodTakenAmount = 0;
      for (Absence absence : takableComponent.takenAbsences) {
        takableComponent.periodTakenAmount += 
            computeAbsenceAmount(engineInstance.person, engineInstance.date, absence.absenceType, takableComponent.takeAmountType);
      }

      absencePeriod.takableComponent = Optional.of(takableComponent);

    }
  }
  
  public int childNumber(PeriodType type) {
   return 0;
  }
  
  public int computeTakableAmount(TakableComponent takableComponent) {
    int takableAmount = takableComponent.periodTakableAmount;
    if (!takableComponent.takableCountBehaviour.equals(TakeCountBehaviour.period)) {
      // TODO: sumAllPeriod, sumUntilPeriod; 
    }
    return takableAmount;
  }
  
  public int computeTakenAmount(TakableComponent takableComponent) {
    int takenAmount = takableComponent.periodTakenAmount;
    if (!takableComponent.takenCountBehaviour.equals(TakeCountBehaviour.period)) {
      // TODO: sumAllPeriod, sumUntilPeriod; 
    } 
    return takenAmount;
  }

  private int computeAbsenceAmount(Person person, LocalDate date, 
      AbsenceType absenceType, AmountType amountType) {
    
    if (amountType.equals(AmountType.units)) {
      return 1;
    }
    // TODO: trattare anche gli altri enumerati.....
    if (absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay) {
      int dateWorkingMinutes = workingTimeTypeDao
          .getWorkingTimeType(date, person).get()
          .workingTimeTypeDays.get(date.getDayOfWeek() - 1).workingTime;
      return dateWorkingMinutes;
    } else {
      return absenceType.justifiedTimeAtWork.minutes;
    }
  }
  
  /**
   * 
   * @param absencePeriod
   * @param absenceType
   * @param date
   * @return
   */
  public boolean requestForAbsenceInPeriod(AbsencePeriod absencePeriod, 
      AbsenceRequestType absenceRequestType, 
      AbsenceType absenceType, LocalDate date) {

    // Solo Takable component
    if (absencePeriod.takableComponent.isPresent() && !absencePeriod.complationComponent.isPresent()) {

      Preconditions.checkState(absenceRequestType.equals(AbsenceRequestType.insertTakable) 
          || absenceRequestType.equals(AbsenceRequestType.deleteTakable));

      Preconditions.checkState(absencePeriod.takableComponent.get().takableCodes.contains(absenceType));

      int absenceAmount = computeAbsenceAmount(absencePeriod.person, date, absenceType, 
          absencePeriod.takableComponent.get().takeAmountType );
      int takableAmount = computeTakableAmount(absencePeriod.takableComponent.get());
      int takenAmount = computeTakenAmount(absencePeriod.takableComponent.get());

      return takableAmount - takenAmount - absenceAmount > 0;

    }
    // Solo Complation component
    if (!absencePeriod.takableComponent.isPresent() && absencePeriod.complationComponent.isPresent()) {

      Preconditions.checkState(absenceRequestType.equals(AbsenceRequestType.insertComplation) 
          || absenceRequestType.equals(AbsenceRequestType.deleteComplation));

      // Trovare la percentuale di completamento alla 
      
      // Ricostruisco la storia nel periodo per capire quanto ho di residuo.
      int initialPercent = computeInitialComplationPercent();  
      LocalDate initialDate = getInitialComplationDate(absencePeriod);


      // inserisco il codice

      // se supero il limite aggiungo anche il codice di completamento 

    }

    // Entrambi i componenti
    if (absencePeriod.takableComponent.isPresent() && absencePeriod.complationComponent.isPresent()) {

    }

    return false; //illegal state
  }

  /**
   * Il valore già utilizzato da inizializzazione.
   * @return
   */
  private int computeInitialComplationPercent() {
    // TODO: recuperare la percentuale inizializzazione quando ci sarà.
    return 0;
  }
  
  /**
   * La data cui si riferisce la percentuale inizializzazione.
   * @param absencePeriod
   * @return
   */
  private LocalDate getInitialComplationDate(AbsencePeriod absencePeriod) {
    // TODO: utilizzare le strutture dati quando ci saranno.
    return absencePeriod.from;
  }


  public static class AbsencePeriod {

    public Person person;

    /*Period*/
    public LocalDate from;                      // Data inizio
    public LocalDate to;                        // Data fine

    public Optional<TakableComponent> takableComponent;
    public Optional<ComplationComponent> complationComponent;
    
    /*Next Period*/
    public AbsencePeriod nextAbsencePeriod;     // Puntatore al periodo successivo ->
    public AbsencePeriod previousAbsencePeriod; // <- puntatore al periodo precedente
    
  }
  
  public static class TakableComponent {

    public AmountType takeAmountType;           // Il tipo di ammontare del periodo
    
    public TakeCountBehaviour takableCountBehaviour;// Come contare il tetto totale
    public int periodTakableAmount;             // Il tetto massimo
    
    public TakeCountBehaviour takenCountBehaviour;  // Come contare il tetto consumato
    public int periodTakenAmount;               // Il tetto consumato
    
    public Set<AbsenceType> takableCodes;       // I tipi assenza prendibili del periodo
    public Set<AbsenceType> takenCodes;         // I tipi di assenza consumati del periodo

    public List<Absence> takenAbsences;         // Le assenze consumate
  }
  
  public static class ComplationComponent {

    public AmountType complationAmountType;     // Tipo di ammontare completamento
    
    public int complationLimitAmount;           // Limite di completamento
    public int complationConsumedAmount;        // Ammontare completamento attualmente consumato
    
    public Set<AbsenceType> replacingCodes;     // Codici di rimpiazzamento      
    public Set<AbsenceType> complationCodes;    // Codici di completamento
    
    public List<Absence> replacingAbsences;     // Le assenze di rimpiazzamento (solo l'ultima??)     
    public List<Absence> complationAbsences;    // Le assenze di completamento
  }
  

  
  
  public enum ComputeAmountRestriction {
    workingTimePercent, workingPeriodPercent;
  }
  
  public enum AbsenceRequestType {
    insertTakable, insertComplation, deleteTakable, deleteComplation;
  }
  
  public enum AbsenceEngineProblem {
    noChildExist,         // quando provo a assegnare una tutela per figlio non inserito
    dateOutOfContract,    // quando provo assengare esempio ferie fuori contratto
    unsupportedOperation, // ancora non implementato
  }
  
  /**
   * Record di esito inserimento assenza.
   * 
   * @author alessandro
   *
   */
  public static class ResponseItem {

    public enum AbsenceOperation {
      insert, insert_complation, remaing, calcel;
    }
    
    public enum AbsenceProblem {
      limitExceeded, wrongComplationPosition;
    }
    
    public static class ConsumedResidualAmount {
      public AmountType amountType;     // | units | minutes | units |
      public int amount;                // | 02:00 | 01:00   |   1   |
      public int workingTime;           // | 07:12 |
      public int getPercent() {         // | 25%   |
        // TODO: implementare la percentuale
        return 0;
      }
      
      public String residualName;       // | res. anno passato | residuo anno corrente | lim. anno |
      public LocalDate expireResidual;  // |     31/03/2016    |      31/03/2017       | 31/12/2016| 
      public int beforeResidual;        // |     20:00         |      07:00            |    22     |
      public int getAfterResidual() {   // |     13:00         |      07:00            |    21     |         
        return beforeResidual - amount;    
      }
    }
    
    public LocalDate date;
    public AbsenceType absenceType;
    public AbsenceOperation operation;
    public List<ConsumedResidualAmount> consumedResidualAmount;
    public AbsenceProblem absenceProblem;
    
    
  }
  
      
}
