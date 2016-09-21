package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import it.cnr.iit.epas.DateInterval;

import manager.services.absences.AbsenceStatus;
import manager.services.absences.ReplacingStatus;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.GroupAbsenceType;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public class AbsencePeriod {

  public GroupAbsenceType groupAbsenceType;

  //
  // Period
  //
  
  public LocalDate from;                      // Data inizio
  public LocalDate to;                        // Data fine

  public AbsencePeriod(GroupAbsenceType groupAbsenceType) {
    this.groupAbsenceType = groupAbsenceType;
  }

  public DateInterval periodInterval() {
    return new DateInterval(from, to);
  }
  
  //
  // Takable
  //
  
  public AmountType takeAmountType;                // Il tipo di ammontare del periodo

  public TakeCountBehaviour takableCountBehaviour; // Come contare il tetto totale
  private int fixedPeriodTakableAmount = 0;         // Il tetto massimo

  public TakeCountBehaviour takenCountBehaviour;   // Come contare il tetto consumato
  private int periodTakenAmount = 0;                // Il tetto consumato

  public Set<AbsenceType> takableCodes;            // I tipi assenza prendibili del periodo
  public Set<AbsenceType> takenCodes;              // I tipi di assenza consumati del periodo

  // Le assenze consumate
  public List<AbsenceStatus> takenAbsencesStatus = Lists.newArrayList(); 
  
  // L'errore
  public Absence overtakenLimitAbsence = null;
  
  public boolean isTakable() {
    return takeAmountType != null; 
  }

  public void setOvertakenLimitAbsence(Absence absence) {
    if (this.overtakenLimitAbsence == null) {
      this.overtakenLimitAbsence = absence;
    }
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
    AbsenceStatus absenceStatus = AbsenceStatus.builder()
        .absence(absence)
        .amountTypeTakable(this.takeAmountType)
        .consumedTakable(takenAmount)
        .residualBeforeTakable(getPeriodTakableAmount() - getPeriodTakenAmount())
        .build();
    this.takenAbsencesStatus.add(absenceStatus);
    this.periodTakenAmount += takenAmount;
  }
  
  //
  // Complation
  //
  
  public AmountType complationAmountType;     // Tipo di ammontare completamento

  public int complationConsumedAmount;        // Ammontare completamento attualmente consumato

  // I codici di rimpiazzamento ordinati per il loro tempo di completamento (decrescente)
  public SortedMap<Integer, AbsenceType> replacingCodesDesc = 
      Maps.newTreeMap(Collections.reverseOrder());
  
  //I tempi di rimpiazzamento per ogni assenza
  public Map<AbsenceType, Integer> replacingTimes = Maps.newHashMap();
  
  // Codici di completamento
  public Set<AbsenceType> complationCodes;    

  // Le assenze di rimpiazzamento per giorno
  public SortedMap<LocalDate, Absence> replacingAbsencesByDay = Maps.newTreeMap();
  
  // Le assenze di completamento per giorno
  public SortedMap<LocalDate, Absence> complationAbsencesByDay = Maps.newTreeMap();

  // I giorni analizzati con i completamenti effettivi e quelli ipotetici.
  public SortedMap<LocalDate, ReplacingStatus> replacingStatus = Maps.newTreeMap();
  
  // Gli errori
  public LocalDate compromisedReplacingDate = null;
  public List<Absence> twoComplationSameDay = Lists.newArrayList();
  public List<Absence> twoReplacingSameDay = Lists.newArrayList();
  public Absence wrongReplacing = null;
  public Absence tooEarlyReplcing = null;
  public Absence missingReplacing = null;
    
  public boolean isComplation() {
    return complationAmountType != null; 
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
  
}


