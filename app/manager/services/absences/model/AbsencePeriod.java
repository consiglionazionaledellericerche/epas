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
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.GroupAbsenceType;
import models.absences.InitializationGroup;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;
import models.enumerate.VacationCode;
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
  private int fixedPeriodTakableAmount = 0;         // Il tetto massimo
  public TakeCountBehaviour takenCountBehaviour;   // Come contare il tetto consumato
  public Set<AbsenceType> takableCodes;            // I tipi assenza prendibili del periodo
  public Set<AbsenceType> takenCodes;              // I tipi di assenza consumati del periodo
  public LocalDate limitExceedDate;
  
  // Complation
  public AmountType complationAmountType;                      // Tipo di ammontare completamento
  
  // I codici di rimpiazzamento ordinati per il loro tempo di completamento (decrescente)
  public SortedMap<Integer, List<AbsenceType>> replacingCodesDesc = 
      Maps.newTreeMap(Collections.reverseOrder());              
                                                                                    
  //I tempi di rimpiazzamento per ogni assenza
  public Map<AbsenceType, Integer> replacingTimes = Maps.newHashMap();              
  public Set<AbsenceType> complationCodes;                             // Codici di completamento
  
  //Errori del periodo
  public ErrorsBox errorsBox = new ErrorsBox();
  public boolean ignorePeriod = false;
  
  //Tentativo di inserimento assenza nel periodo
  public Absence attemptedInsertAbsence;
  
  //Supporto alla gestione ferie e permessi 
  //Assenze che hanno provocato una riduzione della quantità 
  public List<Absence> reducingAbsences = Lists.newArrayList();
  //Ammontare periodo (per visualizzazione prima della patch fix post partum)
  public int vacationAmountBeforeFixPostPartum = 0;
  //Ammontare periodo (per visualizzazione prima della patch inizializzazione)
  public int vacationAmountBeforeInitializationPatch = 0;
  //VacationPeriod che ha generato il period
  public VacationCode vacationCode;
  // Se il period è stato splittato perchè a cavallo del primo anno contratto
  public AbsencePeriod splittedWith;
  
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
  
  public Integer getFixedPeriodTakableAmount() {
    return this.fixedPeriodTakableAmount;
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
  public int getPeriodTakenAmount() {
    int computedTakenAmounut = computePeriodTakenAmount(takenCountBehaviour, this.from);
    return computedTakenAmounut;
  }
  
  /**
   * Calcola l'ammontare in funzione del tipo di conteggio. 
   */
  public int computePeriodTakenAmount(TakeCountBehaviour countBehaviour, LocalDate date) {
    
    if (countBehaviour.equals(TakeCountBehaviour.period)) {
      int takenInPeriod = getInitializationTakableUsed();
      for (TakenAbsence takenAbsence : takenAbsences()) {
        if (!takenAbsence.beforeInitialization) {
          takenInPeriod += takenAbsence.getTakenAmount();
        }
      }
      return takenInPeriod;
    }
    
    if (countBehaviour.equals(TakeCountBehaviour.sumAllPeriod) 
        || countBehaviour.equals(TakeCountBehaviour.sumUntilPeriod)) {
      int taken = 0;
      for (AbsencePeriod absencePeriod : this.subPeriods) {
        if (countBehaviour.equals(TakeCountBehaviour.sumUntilPeriod) 
            && absencePeriod.from.isAfter(date)) {
          break;
        }
        taken = taken + absencePeriod.getInitializationTakableUsed();
        for (TakenAbsence takenAbsence : absencePeriod.takenAbsences()) {
          if (!takenAbsence.beforeInitialization) {
            taken = taken + takenAbsence.getTakenAmount();
          }
        }
      }
      return taken;
    }
    
    return 0;
    
  }
  
  public int getRemainingAmount() {
    return this.getPeriodTakableAmount() - this.getPeriodTakenAmount();
  }

  /**
   * Aggiunge al period l'assenza takable nel periodo.
   * @param absence assenza
   * @param takenAmount ammontare
   * @return l'assenza takable
   */
  public TakenAbsence buildTakenAbsence(Absence absence, int takenAmount) {
    int periodTakableAmount = this.getPeriodTakableAmount();
    int periodTakenAmount = this.getPeriodTakenAmount();
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
      this.errorsBox.addAbsenceError(absence, AbsenceProblem.CompromisedTwoComplation);
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
  
  public boolean isCompromisedComplation() {
    return errorsBox.containsAbsenceProblem(AbsenceProblem.CompromisedTwoComplation);
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
      int units = (this.initialization.unitsInput * 100);
      if (minutes > 0) {
        units = units + workingTypePercent(minutes, this.initialization.averageWeekTime); 
      }
      return units; 
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
    return from + " " + to + " " + fixedPeriodTakableAmount + takableCodes;  
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AbsencePeriod)) {
      return false;
    }
    
    AbsencePeriod other = (AbsencePeriod) o;
    
    boolean isEqual = false;
    
    if ((from == null && other.from != null) 
        || from != null && other.from == null) {
      return false; 
    }
    if ((to == null && other.to != null) 
        || to != null && other.to == null) {
      return false; 
    }
    
    isEqual = person.id.equals(other.person.id) 
        && groupAbsenceType.id.equals(groupAbsenceType.id);
    
    if (from != null && other.from != null) {
      isEqual = isEqual && from.isEqual(other.from);
    }
    if (to != null && other.to != null) {
      isEqual = isEqual && to.isEqual(other.to);
    }
    
    return isEqual;
  }
  
  @Override
  public int hashCode() {
    return (from != null ? from.hashCode() : 0) 
        +  (to != null ? to.hashCode() : 0) 
        + (person != null ? person.hashCode() : 0) 
        + (groupAbsenceType != null ? groupAbsenceType.hashCode() : 0);
  }

}