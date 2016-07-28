package manager.services.absences.model;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import it.cnr.iit.epas.DateInterval;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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

    // Le assenze consumate potenziate con informazioni aggiuntive
    public List<SuperAbsence> takenSuperAbsence = Lists.newArrayList(); 

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
    // Le assenze di rimpiazzamento potenziate con informazioni aggiuntive
    public SortedMap<LocalDate, SuperAbsence> replacingAbsencesByDay = Maps.newTreeMap();
    
    
    // Le assenze di completamento
    public List<Absence> complationAbsences = Lists.newArrayList();
    // Le assenze di completamento potenziate con informazioni aggiuntive
    public SortedMap<LocalDate, SuperAbsence> complationAbsencesByDay = Maps.newTreeMap(); 

  }
  
  /**
   * Le assenze preesistenti. Con informazioni aggiuntive.
   * @author alessandro
   *
   */
  @Builder @Getter @Setter(AccessLevel.PACKAGE)
  public static class SuperAbsence {
    private final Absence absence;                           //assenza  
    private Integer justifiedTime = null;                    //tempo giustificato
    private boolean isAlreadyAssigned = false;        //ogni assenza deve appartenere 
                                                      //ad uno e uno solo period
  }

  public enum ProblemType {
    
    //Errori della richiesta
    wrongJustifiedType,   // quando il tipo giustificativo non è supportato o identificabile
    dateOutOfContract,    // quando provo assegnare esempio ferie fuori contratto
    absenceCodeNotAllowed,// se passo un codice di assenza da inserire non prendibile
    cantInferAbsenceCode, // se non posso inferire il codice d'assenza
    unsupportedOperation, // ancora non implementato
    
    //Errori di modellazione (sono i più gravi)
    modelErrorTwoPeriods,          //quando una assenza è assegnata a più di un periodo
    modelErrorComplationCode,      //quando un codice è di completamento ma anche t o r. (bootstrap)
    modelErrorReplacingCode,       //quando ex. due codici di rimpiazzamento hanno lo stesso tempo  
    modelErrorAmountType,          //quando provo a calcolare il tempo giustificato / completamento
    
    //Errori di stato
    noChildExist,         // quando provo a assegnare una tutela per figlio non inserito
    notOnHoliday,
    
    stateErrorTwoReplacingSameDay,
    stateErrorTwoComplationSameDay,
    stateErrorLimitlimitExceeded,
    stateErrorMissingReplacing,
    stateErrorWrongReplacing,
    stateErrorTooEarlyReplacing;
  }
  
  @Builder @Getter @Setter
  public static class AbsenceEngineProblem {
    
    private ProblemType problemType;
    
    private LocalDate date;
    private List<AbsenceType> existentReplacing;
    private List<AbsenceType> expectedReplacing;
    
  }

  

}


