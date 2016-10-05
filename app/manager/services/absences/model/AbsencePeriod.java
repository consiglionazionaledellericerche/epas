package manager.services.absences.model;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import it.cnr.iit.epas.DateInterval;

import manager.services.absences.AbsenceEngineUtility;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.GroupAbsenceType;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public class AbsencePeriod {
  
  // Period
  public Person person;
  public GroupAbsenceType groupAbsenceType;
  public LocalDate from;                      // Data inizio
  public LocalDate to;                        // Data fine
  public SortedMap<LocalDate, DayStatus> daysStatus = Maps.newTreeMap();
  
  // Takable
  public AmountType takeAmountType;                // Il tipo di ammontare del periodo
  public TakeCountBehaviour takableCountBehaviour; // Come contare il tetto totale
  private int fixedPeriodTakableAmount = 0;         // Il tetto massimo
  public TakeCountBehaviour takenCountBehaviour;   // Come contare il tetto consumato
  private int periodTakenAmount = 0;                // Il tetto consumato
  public Set<AbsenceType> takableCodes;            // I tipi assenza prendibili del periodo
  public Set<AbsenceType> takenCodes;              // I tipi di assenza consumati del periodo
  
  // Complation
  
  public AmountType complationAmountType;                                           // Tipo di ammontare completamento
  public int complationConsumedAmount;                                              // Ammontare completamento attualmente consumato
  public SortedMap<Integer, AbsenceType> replacingCodesDesc =                       // I codici di rimpiazzamento ordinati per il loro
      Maps.newTreeMap(Collections.reverseOrder());                                  // tempo di completamento (decrescente) 
                                                                                     
  public Map<AbsenceType, Integer> replacingTimes = Maps.newHashMap();              //I tempi di rimpiazzamento per ogni assenza
  public Set<AbsenceType> complationCodes;                                          // Codici di completamento     
  public SortedMap<LocalDate, Absence> replacingAbsencesByDay = Maps.newTreeMap();  // Le assenze di rimpiazzamento per giorno
  public SortedMap<LocalDate, Absence> complationAbsencesByDay = Maps.newTreeMap(); // Le assenze di completamento per giorno
  public LocalDate compromisedReplacingDate = null;                                 // Data di errore complation 
                                                                                    // (non si possono più fare i calcoli sui completamenti). 
  
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

  public void setFixedPeriodTakableAmount(int amount) {
    if (this.takeAmountType.equals(AmountType.units)) {
      // Per non fare operazioni in virgola mobile...
      this.fixedPeriodTakableAmount = amount * 100;
    } else {
      this.fixedPeriodTakableAmount = amount;  
    }
  }
  
  public int getPeriodTakableAmount() {
    if (!takableCountBehaviour.equals(TakeCountBehaviour.period)) {
      // TODO: sumAllPeriod, sumUntilPeriod;
      return 0;
    }
    return this.fixedPeriodTakableAmount;
  }
  
  public int getPeriodTakenAmount() {
    if (!takenCountBehaviour.equals(TakeCountBehaviour.period)) {
      // TODO: sumAllPeriod, sumUntilPeriod;
      return 0;
    } 
    return periodTakenAmount;
  }
  
  public int getRemainingAmount() {
    return this.getPeriodTakableAmount() - this.getPeriodTakenAmount();
  }
  
  /**
   * Se è possibile quell'ulteriore amount.
   * @param amount
   * @return
   */
  public boolean canAddTakenAmount(int amount) {
    //TODO: se non c'è limite programmarlo in un booleano
    if (this.getPeriodTakableAmount() < 0) {
      return true;
    }
    if (this.getPeriodTakableAmount() - this.getPeriodTakenAmount() - amount >= 0) {
      return true;
    }
    return false;
  }
  
  /**
   * Aggiunge l'enhancedAbsene al period e aggiorna il limite consumato.
   * @param enhancedAbsence
   */
  public void addAbsenceTaken(Absence absence, int takenAmount) {
    TakenAbsence takenAbsence = TakenAbsence.builder()
        .absence(absence)
        .amountTypeTakable(this.takeAmountType)
        .consumedTakable(takenAmount)
        .residualBeforeTakable(getPeriodTakableAmount() - getPeriodTakenAmount())
        .build();
    getDayStatus(absence.getAbsenceDate()).takenAbsences.add(takenAbsence);
    
    this.periodTakenAmount += takenAmount;
  }
  
 
  
  public boolean isComplation() {
    return complationAmountType != null; 
  }
  
  public void computeReplacingStatusInPeriod(AbsenceEngineUtility absenceEngineUtility) {

    //TODO: questi errori devono essere messi nel period!!!
    if (periodChain.absenceEngine.report.containsCriticalProblems()) {
      return;
    }
    if (!this.isComplation()) {
      return;
    }

    //preparo i giorni con i replacing effettivi (
    for (Absence replacingAbsence : this.replacingAbsencesByDay.values()) {
      DayStatus dayStatus = this.getDayStatus(replacingAbsence.getAbsenceDate());
      if (this.isAbsenceCompromisedReplacing(replacingAbsence)) {
        dayStatus.setCompromisedReplacing(replacingAbsence);
      } else {
        dayStatus.setExistentReplacing(replacingAbsence);
      }
    }

    //Le assenze di completamento ordinate per data. Genero i replacing ipotetici
    int complationAmount = 0;
    for (Absence complationAbsence : this.complationAbsencesByDay.values()) {

      DayStatus dayStatus = this.getDayStatus(complationAbsence.getAbsenceDate());
      if (this.isAbsenceCompromisedReplacing(complationAbsence)) {
        dayStatus.setCompromisedComplation(complationAbsence);
        continue;
      }

      int amount = absenceEngineUtility.absenceJustifiedAmount(person, 
          complationAbsence, this.complationAmountType);
      complationAmount = complationAmount + amount;

      dayStatus.setAmountTypeComplation(this.complationAmountType);
      dayStatus.setComplationAbsence(complationAbsence);
      dayStatus.setResidualBeforeComplation(complationAmount - amount);
      dayStatus.setConsumedComplation(amount);

      Optional<AbsenceType> replacingCode = absenceEngineUtility
          .whichReplacingCode(this, complationAbsence.getAbsenceDate(), complationAmount);

      if (replacingCode.isPresent()) {

        dayStatus.setCorrectReplacing(replacingCode.get());
        complationAmount -= this.replacingTimes.get(replacingCode.get());
      }

      dayStatus.setResidualAfterComplation(complationAmount);
    }
    this.complationConsumedAmount = complationAmount;

    return;
  }
  
  public void addComplationSameDay(Absence absence) {
    getDayStatus(absence.getAbsenceDate()).complationSameDay.add(absence);
  }
  
  public void addReplacingSameDay(Absence absence) {
    getDayStatus(absence.getAbsenceDate()).replacingSameDay.add(absence);
  }
  
  public void setCompromisedReplacingDate(LocalDate date) {
    if (this.compromisedReplacingDate == null) {
      this.compromisedReplacingDate = date;
    } else if (this.compromisedReplacingDate.isAfter(date)) {
      this.compromisedReplacingDate = date;
    }
  }
  
  public boolean isAbsenceCompromisedReplacing(Absence absence) {
    if (this.compromisedReplacingDate == null) {
      return false;
    }
    if (this.compromisedReplacingDate.isAfter(absence.getAbsenceDate())) {
      return false;
    }
    return true;
  }
  
  public DayStatus getDayStatus(LocalDate date) {
    DayStatus dayStatus = this.daysStatus.get(date);
    if (dayStatus == null) {
      dayStatus = DayStatus.builder().date(date)
          .absencePeriod(this)
          .takenAbsences(Lists.newArrayList())
          .complationSameDay(Sets.newHashSet())
          .replacingSameDay(Sets.newHashSet()).build();
      this.daysStatus.put(date, dayStatus);
    }
    
    return dayStatus;
  }
  
}


