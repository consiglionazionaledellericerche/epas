package manager.services.absences.model;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.errors.ErrorsBox;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.GroupAbsenceType;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public class AbsencePeriod {
  
  // Period
  public Person person;
  public GroupAbsenceType groupAbsenceType;
  public LocalDate from;                      // Data inizio
  public LocalDate to;                        // Data fine
  public SortedMap<LocalDate, DayInPeriod> daysInPeriod = Maps.newTreeMap();
  
  // Takable
  public AmountType takeAmountType;                // Il tipo di ammontare del periodo
  public TakeCountBehaviour takableCountBehaviour; // Come contare il tetto totale
  private int fixedPeriodTakableAmount = 0;        // Il tetto massimo
  public TakeCountBehaviour takenCountBehaviour;   // Come contare il tetto consumato
  public Set<AbsenceType> takableCodes;            // I tipi assenza prendibili del periodo
  public Set<AbsenceType> takenCodes;              // I tipi di assenza consumati del periodo
  public LocalDate limitExceedDate;
  
  // Complation
  public AmountType complationAmountType;                                           // Tipo di ammontare completamento
  public SortedMap<Integer, AbsenceType> replacingCodesDesc =                       // I codici di rimpiazzamento ordinati per il loro
      Maps.newTreeMap(Collections.reverseOrder());                                  // tempo di completamento (decrescente) 
                                                                                     
  public Map<AbsenceType, Integer> replacingTimes = Maps.newHashMap();              //I tempi di rimpiazzamento per ogni assenza
  public Set<AbsenceType> complationCodes;                                          // Codici di completamento     
  public boolean compromisedTwoComplation = false;
  
  //Errori del periodo
  public ErrorsBox errorsBox = new ErrorsBox();
  public boolean ignorePeriod = false;
  
  //Tentativo di inserimento assenza nel periodo
  public Absence attemptedInsertAbsence;
  
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
  
  public int getPeriodTakableAmount() {
    if (!takableCountBehaviour.equals(TakeCountBehaviour.period)) {
      // TODO: sumAllPeriod, sumUntilPeriod;
      return 0;
    }
    return this.fixedPeriodTakableAmount;
  }
  
  public int getPeriodTakenAmount() {
    int takenInPeriod = 0;
    for (TakenAbsence takenAbsence : takenAbsences()) {
      takenInPeriod += takenAbsence.getTakenAmount();
    }
    if (!takenCountBehaviour.equals(TakeCountBehaviour.period)) {
      // TODO: sumAllPeriod, sumUntilPeriod;
      return 0;
    } 
    return takenInPeriod;
  }
  
  public int getRemainingAmount() {
    return this.getPeriodTakableAmount() - this.getPeriodTakenAmount();
  }
  
  /**
   * Se è possibile quell'ulteriore amount.
   * @param amount
   * @return se ho superato il limite
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
    return takenAbsence;
  }
  
  public void addTakenAbsence(TakenAbsence takenAbsence) {
    DayInPeriod dayInPeriod = getDayInPeriod(takenAbsence.absence.getAbsenceDate());
    dayInPeriod.getTakenAbsences().add(takenAbsence);
  }
  
  
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
   
  public void setLimitExceededDate(LocalDate date) {
    if (this.limitExceedDate == null || this.limitExceedDate.isAfter(date)) {
      this.limitExceedDate = date;
    }
  }
  
  public boolean isComplation() {
    return complationAmountType != null; 
  }
  
  /**
   * Requires: no criticalErrors and no twoComplationSameDate
   * @param absenceEngineUtility
   */
  public void computeCorrectReplacingInPeriod(AbsenceEngineUtility absenceEngineUtility, 
      AbsenceComponentDao absenceComponentDao) {

    if (!this.isComplation()) {
      return;
    }

    int complationAmount = 0;
    for (DayInPeriod dayInPeriod : this.daysInPeriod.values()) {
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
          .whichReplacingCode(this, absence.getAbsenceDate(), complationAmount);
      if (replacingCode.isPresent()) {
        dayInPeriod.setCorrectReplacing(replacingCode.get());
        complationAmount -= this.replacingTimes.get(replacingCode.get());
      }
      complationAbsence.residualComplationAfter = complationAmount;
      dayInPeriod.setComplationAbsence(complationAbsence);
    }
    return;
  }
  
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
  
  public DayInPeriod getDayInPeriod(LocalDate date) {
    DayInPeriod dayInPeriod = this.daysInPeriod.get(date);
    if (dayInPeriod == null) {
      dayInPeriod = new DayInPeriod(date, this);
      daysInPeriod.put(date, dayInPeriod);
    }
    return dayInPeriod;
  }
  
  public boolean containsCriticalErrors() {
    return ErrorsBox.containsCriticalErrors(Lists.newArrayList(this.errorsBox));
  }
  
}


