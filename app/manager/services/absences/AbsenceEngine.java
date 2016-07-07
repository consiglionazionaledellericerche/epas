package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Lists;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.WorkingTimeTypeDao;

import manager.services.absences.AbsenceEnums.ComplationAbsenceGroup;
import manager.services.absences.AbsenceEnums.GroupAbsenceType;
import manager.services.absences.AbsenceEnums.TakableAbsenceGroup;

import models.Absence;
import models.AbsenceType;
import models.Person;
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

  @Inject
  public AbsenceEngine(AbsenceTypeDao absenceTypeDao, AbsenceDao absenceDao, 
      WorkingTimeTypeDao workingTimeTypeDao) {
    this.absenceTypeDao = absenceTypeDao;
    this.absenceDao = absenceDao;
    this.workingTimeTypeDao = workingTimeTypeDao;
  }
  
  /**
   * Costruisce la situazione periodica per il gruppo / persona e alla data richiesta!
   * @param person
   * @param groupAbsenceType
   * @param date
   * @return
   */
  public AbsencePeriod buildAbsencePeriod(Person person, GroupAbsenceType groupAbsenceType,
      LocalDate date) { 
   
    // TODO: gli absenceGroupParticolari devono essere costruiti in modo puntuale...
       // Congedi
       // Ferie

    AbsencePeriod absencePeriod = new AbsencePeriod();
    
    //////////////////////////////////////////////////////////////////////////////////////
    /* Period */
    
    if (groupAbsenceType.periodType.equals(PeriodType.year)) {
      absencePeriod.from = new LocalDate(date.getYear(), 1, 1);
      absencePeriod.to = new LocalDate(date.getYear(), 12, 31);
    }
    if (groupAbsenceType.periodType.equals(PeriodType.month)) {
      absencePeriod.from = date.dayOfMonth().withMinimumValue();
      absencePeriod.to = date.dayOfMonth().withMaximumValue();
    }
    if (groupAbsenceType.periodType.equals(PeriodType.always)) {

    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    /* Build Takable Component */
    
    if (groupAbsenceType.takableAbsenceGroup != null) {
      
      TakableComponent takableComponent = new TakableComponent();
      
      TakableAbsenceGroup takableAbsenceGroup = groupAbsenceType.takableAbsenceGroup;

      takableComponent.takeAmountType = takableAbsenceGroup.takeAmountType;
      
      takableComponent.periodTakableAmount = takableAbsenceGroup.periodTakableAmount;
      if (!takableAbsenceGroup.computeTakableAmountBehaviour.isEmpty()) {
        // TODO: ex. workingTimePercent
      }

      takableComponent.takableCountBehaviour = takableAbsenceGroup.takableCountBehaviour;
      takableComponent.takenCountBehaviour = takableAbsenceGroup.takenCountBehaviour;

      takableComponent.takenCodes = 
          Sets.newHashSet(absenceTypeDao.absenceTypeCodeSet(takableAbsenceGroup.takenCodes));
      takableComponent.takableCodes = 
          Sets.newHashSet(absenceTypeDao.absenceTypeCodeSet(takableAbsenceGroup.takableCodes));

      //TODO: le other absences vanno aggiunte!
      takableComponent.takenAbsences = absenceDao.getAbsencesInCodeList(person, 
          absencePeriod.from, absencePeriod.to, Lists.newArrayList(takableComponent.takenCodes), true);

      takableComponent.periodTakenAmount = 0;
      for (Absence absence : takableComponent.takenAbsences) {
        takableComponent.periodTakenAmount += 
            computeAbsenceAmount(person, date, absence.absenceType, takableComponent.takeAmountType);
      }
      
      absencePeriod.takableComponent = Optional.of(takableComponent);

    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    /* Build Complation component */
    
    if (groupAbsenceType.complationAbsenceGroup != null) {
      
      ComplationComponent complationComponent = new ComplationComponent();
      
      ComplationAbsenceGroup complationAbsenceGroup = groupAbsenceType.complationAbsenceGroup;
      complationComponent.complationAmountType = complationAbsenceGroup.complationAmountType;
      
      complationComponent.replacingCodes = Sets.newHashSet(absenceTypeDao
          .absenceTypeCodeSet(complationAbsenceGroup.replacingCodes));
      
      complationComponent.complationCodes = 
          Sets.newHashSet(absenceTypeDao.absenceTypeCodeSet(complationAbsenceGroup.complationCodes));
      
      //TODO: le other absences vanno aggiunte!
      complationComponent.replacingAbsences = 
          absenceDao.getAbsencesInCodeList(person, absencePeriod.from, 
          absencePeriod.to, Lists.newArrayList(complationComponent.replacingCodes), true);
  
      complationComponent.complationAbsences = 
          absenceDao.getAbsencesInCodeList(person, absencePeriod.from, 
          absencePeriod.to, Lists.newArrayList(complationComponent.complationCodes), true);
      
      complationComponent.complationConsumedAmount = 0;
      for (Absence absence : Stream.concat(complationComponent.complationAbsences.stream(), 
          complationComponent.replacingAbsences.stream()).collect(Collectors.toList())) {
        
          complationComponent.complationConsumedAmount += computeAbsenceAmount(person, date, 
              absence.absenceType, complationComponent.complationAmountType);
      }
      
      absencePeriod.complationComponent = Optional.of(complationComponent);
            
      //Un illegal state è  absencePeriod.complationConsumedAmount < 0 ...
    }
    
    return absencePeriod;

  }
  
  public int computeTakableAmount(TakableComponent takableComponent) {
    int takableAmount = takableComponent.periodTakableAmount;
    if (!takableComponent.takableCountBehaviour.equals(CountBehaviour.period)) {
      // TODO: sumAllPeriod, sumUntilPeriod; 
    }
    return takableAmount;
  }
  
  public int computeTakenAmount(TakableComponent takableComponent) {
    int takenAmount = takableComponent.periodTakenAmount;
    if (!takableComponent.takenCountBehaviour.equals(CountBehaviour.period)) {
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
    
    public CountBehaviour takableCountBehaviour;// Come contare il tetto totale
    public int periodTakableAmount;             // Il tetto massimo
    
    public CountBehaviour takenCountBehaviour;  // Come contare il tetto consumato
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
  
  public enum CountBehaviour {
    period, sumAllPeriod, sumUntilPeriod; 
  }
  
  public enum AmountType {
    minutes, units;
  }
  
  public enum PeriodType {
    always, year, month;
  }
  
  public enum ComputeAmountRestriction {
    workingTimePercent, workingPeriodPercent;
  }
  
  public enum AbsenceRequestType {
    insertTakable, insertComplation, deleteTakable, deleteComplation;
  }
  
      
}
