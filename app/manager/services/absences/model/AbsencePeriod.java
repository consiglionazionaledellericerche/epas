package manager.services.absences.model;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import it.cnr.iit.epas.DateInterval;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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

  /*Period*/
  public LocalDate from;                      // Data inizio
  public LocalDate to;                        // Data fine

  public Optional<TakableComponent> takableComponent;
  public Optional<ComplationComponent> complationComponent;

  public AbsencePeriod(GroupAbsenceType groupAbsenceType) {
    this.groupAbsenceType = groupAbsenceType;
  }

  public DateInterval periodInterval() {
    return new DateInterval(from, to);
  }
    
  public static class TakableComponent {

    public AmountType takeAmountType;                // Il tipo di ammontare del periodo

    public TakeCountBehaviour takableCountBehaviour; // Come contare il tetto totale
    private int fixedPeriodTakableAmount = 0;         // Il tetto massimo

    public TakeCountBehaviour takenCountBehaviour;   // Come contare il tetto consumato
    private int periodTakenAmount = 0;                // Il tetto consumato

    public Set<AbsenceType> takableCodes;            // I tipi assenza prendibili del periodo
    public Set<AbsenceType> takenCodes;              // I tipi di assenza consumati del periodo

    // Le assenze consumate
    public List<Absence> takenAbsences = Lists.newArrayList(); 
    
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
      this.takenAbsences.add(absence);
      this.periodTakenAmount += takenAmount;
    }
  }

  public static class ComplationComponent {

    public AmountType complationAmountType;     // Tipo di ammontare completamento

    public int complationConsumedAmount;        // Ammontare completamento attualmente consumato

    // I codici di rimpiazzamento ordinati per il loro tempo di completamento (decrescente)
    public SortedMap<Integer, AbsenceType> replacingCodesDesc = 
        Maps.newTreeMap(Collections.reverseOrder());
    
    //I tempi di rimpiazzamento per ogni assenza
    public Map<AbsenceType, Integer> replacingTimes = Maps.newHashMap();
    
    // Codici di completamento
    public Set<AbsenceType> complationCodes;    

    // Le assenze di rimpiazzamento
    public List<Absence> replacingAbsences = Lists.newArrayList();
    // Le assenze di rimpiazzamento per giorno
    public SortedMap<LocalDate, Absence> replacingAbsencesByDay = Maps.newTreeMap();
    
    
    // Le assenze di completamento
    public List<Absence> complationAbsences = Lists.newArrayList();
    // Le assenze di completamento per giorno
    public SortedMap<LocalDate, Absence> complationAbsencesByDay = Maps.newTreeMap();
    
    public LocalDate compromisedReplacingDate = null;
    
    /**
     * Lo stato dei rimpiazzamenti, data per data, quelli corretti e quelli esistenti.
     * @author alessandro
     *
     */
    @Builder @Getter @Setter
    public static class ReplacingDay {
      private LocalDate date;
      private Absence existentReplacing;
      private AbsenceType correctReplacing;
      
      public boolean wrongType() {
        return correctReplacing != null && existentReplacing != null 
            && !existentReplacing.getAbsenceType().equals(correctReplacing);
      }
      
      public boolean onlyCorrect() {
        return correctReplacing != null && existentReplacing == null;
      }

      public boolean onlyExisting() {
        return correctReplacing == null && existentReplacing != null;
      }

    }

  }
}


