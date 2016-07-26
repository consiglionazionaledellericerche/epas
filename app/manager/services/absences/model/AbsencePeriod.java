package manager.services.absences.model;

import com.google.common.base.Optional;

import it.cnr.iit.epas.DateInterval;

import lombok.Builder;
import lombok.Getter;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.GroupAbsenceType;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import java.util.List;
import java.util.Set;

public class AbsencePeriod {

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

  public static class TakableComponent {

    public AmountType takeAmountType;                // Il tipo di ammontare del periodo

    public TakeCountBehaviour takableCountBehaviour; // Come contare il tetto totale
    public int fixedPeriodTakableAmount = 0;         // Il tetto massimo

    public TakeCountBehaviour takenCountBehaviour;   // Come contare il tetto consumato
    public int periodTakenAmount = 0;                // Il tetto consumato

    public Set<AbsenceType> takableCodes;            // I tipi assenza prendibili del periodo
    public Set<AbsenceType> takenCodes;              // I tipi di assenza consumati del periodo

    public List<SuperAbsence> takenSuperAbsence = Lists.newArrayList(); // Le assenze consumate

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
  }

  public static class ComplationComponent {

    public AmountType complationAmountType;     // Tipo di ammontare completamento

    //public int complationLimitAmount;           // Limite di completamento
    public int complationConsumedAmount;        // Ammontare completamento attualmente consumato

    public Set<AbsenceType> replacingCodes;     // Codici di rimpiazzamento      
    public Set<AbsenceType> complationCodes;    // Codici di completamento

    public List<SuperAbsence> replacingSuperAbsences = Lists.newArrayList(); // Le assenze di rimpiazzamento     
    public List<SuperAbsence> complationSuperAbsences = Lists.newArrayList();// Le assenze di completamento 
    
    public boolean hasError = false;

  }
  
  @Builder @Getter
  public static class SuperAbsence {
    private final Absence absence;
    private final Integer computedJustifiedTime;
    private final Optional<AbsenceErrorType> errorType;
  }
  
  public enum AbsenceErrorType {
    replacingTooEarly, replacingTooLate, takableLimitExceed;
  }

  public enum AbsenceEngineProblem {
    wrongJustifiedType,   // quando il tipo giustificativo non è supportato o identificabile
    noChildExist,         // quando provo a assegnare una tutela per figlio non inserito
    dateOutOfContract,    // quando provo assengare esempio ferie fuori contratto
    absenceCodeNotAllowed,// se passo un codice di assenza da inserire non prendibile
    cantInferAbsenceCode, // se non posso inferire il codice d'assenza
    unsupportedOperation; // ancora non implementato
  }

  

}


