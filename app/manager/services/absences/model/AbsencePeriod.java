package manager.services.absences.model;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.errors.ErrorsBox;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.GroupAbsenceType;
import models.absences.InitializationGroup;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

public class AbsencePeriod {
  
  // Period
  public Person person;
  public GroupAbsenceType groupAbsenceType;
  public LocalDate from;                      // Data inizio
  public LocalDate to;                        // Data fine
  public InitializationGroup initialization;  // Inizializazione period (se presente)
  public SortedMap<LocalDate, DayInPeriod> daysInPeriod = Maps.newTreeMap();
  
  //AllPeriods
  public List<AbsencePeriod> subPeriods;
  
  // Takable
  public AmountType takeAmountType;                // Il tipo di ammontare del periodo
  public TakeCountBehaviour takableCountBehaviour; // Come contare il tetto totale
  private int fixedPeriodTakableAmount = 0;        // Il tetto massimo
  public TakeCountBehaviour takenCountBehaviour;   // Come contare il tetto consumato
  public Set<AbsenceType> takableCodes;            // I tipi assenza prendibili del periodo
  public Set<AbsenceType> takenCodes;              // I tipi di assenza consumati del periodo
  public LocalDate limitExceedDate;
  
  // Complation
  public AmountType complationAmountType;                      // Tipo di ammontare completamento
  //I codici di rimpiazzamento ordinati per il loro tempo di completamento (decrescente)
  public SortedMap<Integer, AbsenceType> replacingCodesDesc = 
      Maps.newTreeMap(Collections.reverseOrder());              
                                                                                    
  //I tempi di rimpiazzamento per ogni assenza
  public Map<AbsenceType, Integer> replacingTimes = Maps.newHashMap();              
  public Set<AbsenceType> complationCodes;                             // Codici di completamento
  public boolean compromisedTwoComplation = false;
  
  //Errori del periodo
  public ErrorsBox errorsBox = new ErrorsBox();
  public boolean ignorePeriod = false;
  
  //Tentativo di inserimento assenza nel periodo
  public Absence attemptedInsertAbsence;
  
  //Assenze che hanno provocato una riduzione della quantità 
  //(utile solo per visualizzazione.. per ora)
  public List<Absence> reducingAbsences = Lists.newArrayList();
  
  AbsencePeriod(Person person, GroupAbsenceType groupAbsenceType) {
    this.person = person;
    this.groupAbsenceType = groupAbsenceType;
  }

  public DateInterval periodInterval() {
    return new DateInterval(from, to);
  }
  
  public boolean isTakable() {
    return takeAmountType != null; 
  }
  
  public boolean isTakableNoLimit() {
    return takeAmountType != null && getPeriodTakableAmount() < 0;
  }
  
  public boolean isTakableWithLimit() {
    return isTakable() && !isTakableNoLimit();
  }
  
  public boolean isTakableUnits() {
    return isTakableWithLimit() && this.takeAmountType == AmountType.units;
  }
  
  public boolean isTakableMinutes() {
    return isTakableWithLimit() && this.takeAmountType == AmountType.minutes;
  }

  /**
   * Imposta l'ammontare fisso del periodo.
   * ex. 150 ore (che possono poi essere decurtate in modo variabile)
   * @param amount ammontare fisso
   */
  public void setFixedPeriodTakableAmount(int amount) {
    if (this.takeAmountType.equals(AmountType.units)) {
      // Per non fare operazioni in virgola mobile...
      this.fixedPeriodTakableAmount = amount * 100;
    } else {
      this.fixedPeriodTakableAmount = amount;  
    }
  }

  private List<TakenAbsence> takenAbsences() {
    List<TakenAbsence> takenAbsences = Lists.newArrayList();
    for (DayInPeriod daysInPeriod : this.daysInPeriod.values()) {
      takenAbsences.addAll(daysInPeriod.getTakenAbsences());
    }
    return takenAbsences;
  }
  
  /**
   * L'ammontare totale prendibile nel periodo.
   * @return int
   */
  public int getPeriodTakableAmount() {
    int computedTakableAmounut = computePeriodTakableAmount(takableCountBehaviour, this.from);
    return computedTakableAmounut;
  }
  
  /**
   * Calcola l'ammontare in funzione del tipo di conteggio. 
   * @return int
   */
  public int computePeriodTakableAmount(TakeCountBehaviour countBehaviour, LocalDate date) {
    
    if (countBehaviour.equals(TakeCountBehaviour.period)) {
      return this.fixedPeriodTakableAmount;
    }

    if (countBehaviour.equals(TakeCountBehaviour.sumAllPeriod)) {
      int takableAmount = 0;
      for (AbsencePeriod absencePeriod : this.subPeriods) {
        takableAmount = takableAmount + absencePeriod.fixedPeriodTakableAmount;
      }
      return takableAmount;  
    }

    if (countBehaviour.equals(TakeCountBehaviour.sumUntilPeriod)) {
      int takableAmount = 0;
      for (AbsencePeriod absencePeriod : this.subPeriods) {
        if (absencePeriod.from.isAfter(date)) {
          break;
        }
        takableAmount = takableAmount + absencePeriod.fixedPeriodTakableAmount;
      }
      return takableAmount;  
    }

    return 0;
  }
  
  /**
   * L'ammontare utilizzato nel periodo.
   * @return int
   */
  public int getPeriodTakenAmount(boolean firstIteration) {
    
    int takenInPeriod = getInitializationTakableUsed();
    
    for (TakenAbsence takenAbsence : takenAbsences()) {
      if (!takenAbsence.beforeInitialization) {
        takenInPeriod += takenAbsence.getTakenAmount();
      }
    }
    if (takenCountBehaviour.equals(TakeCountBehaviour.period)) {
      return takenInPeriod;
    }
    
    if (takenCountBehaviour.equals(TakeCountBehaviour.sumAllPeriod) 
        || takenCountBehaviour.equals(TakeCountBehaviour.sumAllPeriod)) {
      if (!firstIteration) {
        return takenInPeriod;
      } 
      return getPeriodTakenAmountSumAll(this.takenCountBehaviour);  
    }
    
    return 0;
    
  }

  private int getPeriodTakenAmountSumAll(TakeCountBehaviour countType) {
    int taken = 0;
    for (AbsencePeriod absencePeriod : this.subPeriods) {
      if (countType.equals(TakeCountBehaviour.sumUntilPeriod) 
          && absencePeriod.from.isAfter(this.from)) {
        break;
      }
      taken = taken + absencePeriod.getPeriodTakenAmount(false);
    }
    return taken;
  }
  
  public int getRemainingAmount() {
    return this.getPeriodTakableAmount() - this.getPeriodTakenAmount(true);
  }

  /**
   * Aggiunge al period l'assenza takable nel periodo.
   * @param absence assenza
   * @param takenAmount ammontare
   * @return l'assenza takable
   */
  public TakenAbsence buildTakenAbsence(Absence absence, int takenAmount) {
    int periodTakableAmount = this.getPeriodTakableAmount();
    int periodTakenAmount = this.getPeriodTakenAmount(true);
    TakenAbsence takenAbsence = TakenAbsence.builder()
        .absence(absence)
        .amountType(this.takeAmountType)
        .periodTakableTotal(periodTakableAmount)
        .periodTakenBefore(periodTakenAmount)
        .takenAmount(takenAmount)
        .build();
    if (this.initialization != null 
        && !absence.getAbsenceDate().isAfter(this.initialization.date)) {
      takenAbsence.beforeInitialization = true;
    }  
    return takenAbsence;
  }
  
  public void addTakenAbsence(TakenAbsence takenAbsence) {
    DayInPeriod dayInPeriod = getDayInPeriod(takenAbsence.absence.getAbsenceDate());
    dayInPeriod.getTakenAbsences().add(takenAbsence);
  }
  
  /**
   * Aggiunge l'assenza di completamento al periodo.
   * @param absence assenza di completamento
   */
  public void addComplationAbsence(Absence absence) {
    DayInPeriod dayInPeriod = getDayInPeriod(absence.getAbsenceDate());
    if (!dayInPeriod.getExistentComplations().isEmpty()) {
      this.compromisedTwoComplation = true;
    }
    dayInPeriod.getExistentComplations().add(absence);
  }
  
  public void addReplacingAbsence(Absence absence) {
    DayInPeriod dayInPeriod = getDayInPeriod(absence.getAbsenceDate());
    dayInPeriod.getExistentReplacings().add(absence);
  }
   
  /**
   * Tagga il periodo come limite superato alla data.
   * @param date data
   */
  public void setLimitExceededDate(LocalDate date) {
    if (this.limitExceedDate == null || this.limitExceedDate.isAfter(date)) {
      this.limitExceedDate = date;
    }
  }
  
  public boolean isComplation() {
    return this.complationAmountType != null;
  }
  
  public boolean isComplationUnits() {
    return isComplation() && this.complationAmountType == AmountType.units; 
  }
  
  public boolean isComplationMinutes() {
    return isComplation() && this.complationAmountType == AmountType.minutes; 
  }
  
  
  /**
   * Calcola i rimpiazzamenti corretti nel periodo.
   * @param absenceEngineUtility inject dep
   */
  public void computeCorrectReplacingInPeriod(AbsenceEngineUtility absenceEngineUtility) {

    if (!this.isComplation()) {
      return;
    }

    int complationAmount = getInitializationComplationUsed(absenceEngineUtility);
    for (DayInPeriod dayInPeriod : this.daysInPeriod.values()) {
      if (this.initialization != null && !dayInPeriod.getDate().isAfter(this.initialization.date)) {
        continue;
      }
      if (dayInPeriod.getExistentComplations().isEmpty()) {
        continue;
      }
      Preconditions.checkState(dayInPeriod.getExistentComplations().size() == 1);
      Absence absence = dayInPeriod.getExistentComplations().iterator().next();
      int amount = absenceEngineUtility.absenceJustifiedAmount(person, 
          absence, this.complationAmountType);
      
      complationAmount = complationAmount + amount;
      ComplationAbsence complationAbsence = ComplationAbsence.builder()
          .absence(absence)
          .amountType(this.complationAmountType)
          .residualComplationBefore(complationAmount - amount)
          .consumedComplation(amount).build();
      Optional<AbsenceType> replacingCode = absenceEngineUtility
          .whichReplacingCode(this.replacingCodesDesc, absence.getAbsenceDate(), complationAmount);
      if (replacingCode.isPresent()) {
        dayInPeriod.setCorrectReplacing(replacingCode.get());
        complationAmount -= this.replacingTimes.get(replacingCode.get());
      }
      complationAbsence.residualComplationAfter = complationAmount;
      dayInPeriod.setComplationAbsence(complationAbsence);
    }
    return;
  }
  
  /**
   * Seleziona dalla lista le assenze appartenenti al period.
   * @param absences assenze
   * @return list
   */
  public List<Absence> filterAbsencesInPeriod(List<Absence> absences) {
    DateInterval interval = this.periodInterval();
    List<Absence> filtered = Lists.newArrayList();
    for (Absence absence : absences) {
      if (DateUtility.isDateIntoInterval(absence.getAbsenceDate(), interval)) {
        filtered.add(absence);
      }
    }
    return filtered;
  }
  
  /**
   * La struttura dati DayInPeriod per quella data. Se non esiste la crea.
   * @param date data
   * @return il dayInPeriod
   */
  public DayInPeriod getDayInPeriod(LocalDate date) {
    DayInPeriod dayInPeriod = this.daysInPeriod.get(date);
    if (dayInPeriod == null) {
      dayInPeriod = new DayInPeriod(date, this);
      daysInPeriod.put(date, dayInPeriod);
    }
    return dayInPeriod;
  }
  
  public boolean containsCriticalErrors() {
    return ErrorsBox.boxesContainsCriticalErrors(Lists.newArrayList(this.errorsBox));
  }
  
  /**
   * L'inizializzazione nella parte takable.
   * @return int
   */
  public int getInitializationTakableUsed() {
    
    //TODO: si può instanziare una variabile lazy
    
    if (this.initialization == null) {
      return 0;
    }
    
    int minutes = this.initialization.hoursInput * 60 + this.initialization.minutesInput;
    //Takable used
    if (this.isTakableMinutes()) {
      return minutes;
    } else if (this.isTakableUnits()) {
      return (this.initialization.unitsInput * 100) 
          + workingTypePercent(minutes, this.initialization.averageWeekTime);
    }
    
    return 0;
  }
  
  /**
   * L'inizializzazione nella parte completamento.
   * @param absenceEngineUtility inject
   * @return int
   */
  public int getInitializationComplationUsed(AbsenceEngineUtility absenceEngineUtility) {
    
    //TODO: si può instanziare una variabile lazy
    
    if (this.initialization == null) {
      return 0;
    }
    
    int minutes = this.initialization.hoursInput * 60 + this.initialization.minutesInput;
    
    //Complation used
    if (this.isComplationUnits()) {
      return workingTypePercentModule(minutes, this.initialization.averageWeekTime);
    } else if (this.isComplationMinutes()) {
      
      //completare finchè si può minutes
      while (true) {
        Optional<AbsenceType> absenceType = absenceEngineUtility
            .whichReplacingCode(this.replacingCodesDesc, this.initialization.date, minutes);
        if (!absenceType.isPresent()) {
          break;
        }
        minutes -= this.replacingTimes.get(absenceType.get());
      }
      return minutes;
    }
    
    return 0;
  }
  
  private int workingTypePercent(int minutes, int workTime) {
    int time = minutes * 100; 
    int percent = (time) / workTime;
    return percent;
  }
  
  private int workingTypePercentModule(int minutes, int workTime) {
    int workTimePercent = workingTypePercent(minutes, workTime); 
    return workTimePercent % 100;
  }
  
  public String toString() {
    return from + " " + to + " " + fixedPeriodTakableAmount;  
  }
}


