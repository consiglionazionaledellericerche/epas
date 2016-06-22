package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import dao.AbsenceTypeDao;

import models.Absence;
import models.AbsenceType;
import models.Person;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

public class AbsenceEngine {
  
  public static final Set<String> codes661 = 
      Sets.newHashSet("661H1", "661H2", "661H3", "661H4", "661H5", "661H6", "661H7");
  
  private final AbsenceTypeDao absenceTypeDao;

  @Inject
  public AbsenceEngine(AbsenceTypeDao absenceTypeDao) {
    this.absenceTypeDao = absenceTypeDao;
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
    
    /*Period*/
    if (groupAbsenceType.periodType.equals(PeriodType.year)) {
      absencePeriod.from = new LocalDate(date.getYear(), 1, 1);
      absencePeriod.to = new LocalDate(date.getYear(), 12, 31);
    } 
    // TODO: month, always
    
    /* Takable/taken model*/
    if (groupAbsenceType.fixedTakableAmount.isPresent()) {
      absencePeriod.takableAmount = groupAbsenceType.fixedTakableAmount.get();
    }
   
    absencePeriod.takeAmountType = groupAbsenceType.takeAmountType;
    absencePeriod.takableCountBehaviour = CountBehaviour.period;
    if (groupAbsenceType.particularTakableCountBehaviour.isPresent()) {
      absencePeriod.takableCountBehaviour = groupAbsenceType.particularTakableCountBehaviour.get();
    }
    absencePeriod.takenCountBehaviour = CountBehaviour.period;
    if (groupAbsenceType.particularTakenCountBehaviour.isPresent()) {
      absencePeriod.takenCountBehaviour = groupAbsenceType.particularTakenCountBehaviour.get();
    }
    
    absencePeriod.takenCodes = 
        Sets.newHashSet(absenceTypeDao.absenceTypeCodeSet(groupAbsenceType.takenCodes));
    absencePeriod.takableCodes = 
        Sets.newHashSet(absenceTypeDao.absenceTypeCodeSet(groupAbsenceType.takableCodes));
      
   
    
    return null;

  }
  
  public enum GroupAbsenceType {
    
    group661(
        PeriodType.year, 
        AmountType.minutes, 
        Optional.<CountBehaviour>absent(),
        Optional.<CountBehaviour>absent(),
        Optional.<Integer>fromNullable(1080),
        codes661,
        codes661);
    
    public PeriodType periodType;
    public AmountType takeAmountType;
    public Optional<CountBehaviour> particularTakableCountBehaviour;
    public Optional<CountBehaviour> particularTakenCountBehaviour;
    public Optional<Integer> fixedTakableAmount;
    public Set<String> takenCodes; 
    public Set<String> takableCodes; 
    
    private GroupAbsenceType(PeriodType periodType, AmountType takeAmountType, 
        Optional<CountBehaviour> particularTakableCountBehaviour, 
        Optional<CountBehaviour> particularTakenCountBehaviour, 
        Optional<Integer> fixedTakableAmount,
        Set<String> takenCodes, Set<String> takableCodes) {
      this.periodType = periodType;
      this.takeAmountType = takeAmountType;
      this.particularTakableCountBehaviour = particularTakableCountBehaviour;
      this.particularTakenCountBehaviour = particularTakenCountBehaviour;
      this.fixedTakableAmount = fixedTakableAmount;
      this.takenCodes = takenCodes;
      this.takableCodes = takableCodes;
    }
    
  }
  
  public static class AbsencePeriod {

    /*Period*/
    public LocalDate from;                      // Data inizio
    public LocalDate to;                        // Data fine
    
    /*Takable/taken model*/
    public AmountType takeAmountType;           // Il tipo di tetto / usato 
    public CountBehaviour takableCountBehaviour;// Come contare il tetto totale
    public CountBehaviour takenCountBehaviour;  // Come contare il tetto consumato
    public int takableAmount;                   // Il tetto massimo   
    public int takenAmount;                     // Il tetto consumato
    
    public Set<AbsenceType> takableCodes;       // I tipi assenza prendibili del periodo
    public Set<AbsenceType> takenCodes;         // I tipi di assenza consumati del periodo

    public List<Absence> takenAbsences;         // Le assenze consumate
    
    /*Complation model*/
    public AmountType complationAmountType;     // Tipo di ammontare completamento
    public int complationLimitAmount;           // Limite di completamento
    public int complationConsumedAmount;        // Ammontare completamento attualmente consumato
    
    public AbsenceType replacingCode;           // Codice di rimpiazzamento      
    public Set<AbsenceType> complationCodes;   // Codici di completamento
    
    public List<Absence> replacingAbsences;     // Le assenze di rimpiazzamento (solo l'ultima??)     
    public List<Absence> complationAbsences;    // Le assenze di completamento
    
    /*Next Period*/
    public AbsencePeriod nextAbsencePeriod;     // Puntatore al periodo successivo ->
    public AbsencePeriod previousAbsencePeriod; // <- puntatore al periodo precedente
    
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
  

    
}
