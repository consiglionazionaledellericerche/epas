package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.beust.jcommander.internal.Lists;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.WorkingTimeTypeDao;

import models.Absence;
import models.AbsenceType;
import models.Person;
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

public class AbsenceEngine {
  
  public static final Set<String> codes661 = 
      Sets.newHashSet("661h1", "661h2", "661h3", "661h4", "661h5", "661h6", "661h7", 
          "661h8", "661h9");
  public static final Set<String> codes18 = 
      Sets.newHashSet("18h1", "18h2", "18h3", "18h4", "18h5", "18h6", "18h7", "18h8", "18h9");
  public static final Set<String> codes19 = 
      Sets.newHashSet("19h1", "19h2", "19h3", "19h4", "19h5", "19h6", "19h7", "19h8", "19h9");
  
  public static final Set<String> codesCompl09 = 
      Sets.newHashSet("09h1", "09h2", "09h3", "09h4", "09h5", "09h6", "09h7");
  
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
    
    absencePeriod.periodAmountType = groupAbsenceType.periodAmountType;
    
    //////////////////////////////////////////////////////////////////////////////////////
    /* Takable/taken model */
    
    TakableAbsenceGroup takableAbsenceGroup = groupAbsenceType.takableAbsenceGroup;
    
    absencePeriod.periodTakableAmount = takableAbsenceGroup.periodTakableAmount;
    if (!takableAbsenceGroup.computeTakableAmountBehaviour.equals(ComputeAmountBehaviour.normal)) {
      // TODO: ex. workingTimePercent
    }
    
    absencePeriod.takableCountBehaviour = takableAbsenceGroup.takableCountBehaviour;
    absencePeriod.takenCountBehaviour = takableAbsenceGroup.takenCountBehaviour;
    
    absencePeriod.takenCodes = 
        Sets.newHashSet(absenceTypeDao.absenceTypeCodeSet(takableAbsenceGroup.takenCodes));
    absencePeriod.takableCodes = 
        Sets.newHashSet(absenceTypeDao.absenceTypeCodeSet(takableAbsenceGroup.takableCodes));
    
    //TODO: le other absences vanno aggiunte!
    absencePeriod.takenAbsences = absenceDao.getAbsencesInCodeList(person, 
        absencePeriod.from, absencePeriod.to, Lists.newArrayList(absencePeriod.takenCodes), true);

    absencePeriod.periodTakenAmount = 0;
    for (Absence absence : absencePeriod.takenAbsences) {

      if (absencePeriod.periodAmountType.equals(AmountType.units)) {
        absencePeriod.periodTakenAmount++;
        continue;
      } 
      if (absencePeriod.periodAmountType.equals(AmountType.minutes)) {
        // TODO: renderlo efficiente...
        
        absencePeriod.periodTakenAmount += justifiedMinutes(person, date, absence.absenceType);
        continue;
      }

    }
    return absencePeriod;

  }
  
  /**
   * 
   * @param absencePeriod
   * @param absenceType
   * @param date
   * @return
   */
  public boolean canTakeAbsenceInPeriod(AbsencePeriod absencePeriod, AbsenceType absenceType, 
      LocalDate date) {
    
    int absenceAmount = 0;
    if (absencePeriod.periodAmountType.equals(AmountType.minutes)) {
      absenceAmount =  justifiedMinutes(absencePeriod.person, date, absenceType);
    }
    if (absencePeriod.periodAmountType.equals(AmountType.units)) {
      absenceAmount =  1;
    }
    
    int takableAmount = absencePeriod.periodTakableAmount;
    if (!absencePeriod.takableCountBehaviour.equals(CountBehaviour.period)) {
      // TODO: sumAllPeriod, sumUntilPeriod; 
    }
    int takenAmount = absencePeriod.periodTakenAmount;
    if (!absencePeriod.takenCountBehaviour.equals(CountBehaviour.period)) {
      // TODO: sumAllPeriod, sumUntilPeriod; 
    } 
    
    return takableAmount - takenAmount - absenceAmount > 0;
  }
  
  /**
   * TODO: spostarlo in un manager più opportuno ..Calcola i minuti giustificati dal tipo assenza... 
   */
  private int justifiedMinutes(Person person, LocalDate date, AbsenceType absenceType) {
    
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
  
  public enum TakableAbsenceGroup {
    
    takable661(
        CountBehaviour.period,
        CountBehaviour.period,
        ComputeAmountBehaviour.normal,
        1080,
        codes661,
        codes661),
    
    takable18(
        CountBehaviour.period,
        CountBehaviour.period,
        ComputeAmountBehaviour.normal,
        1080,
        codes18,
        codes18),
    
    takable19(
        CountBehaviour.period,
        CountBehaviour.period,
        ComputeAmountBehaviour.normal,
        1080,
        codes19,
        codes19);
    
    public CountBehaviour takableCountBehaviour;
    public CountBehaviour takenCountBehaviour;
    public ComputeAmountBehaviour computeTakableAmountBehaviour;
    public Integer periodTakableAmount;
    public Set<String> takenCodes; 
    public Set<String> takableCodes;
     
    private TakableAbsenceGroup( 
        CountBehaviour takableCountBehaviour, 
        CountBehaviour takenCountBehaviour, 
        ComputeAmountBehaviour computeTakableAmountBehaviour,
        Integer periodTakableAmount,
        Set<String> takenCodes, Set<String> takableCodes) {
      this.takableCountBehaviour = takableCountBehaviour;
      this.takenCountBehaviour = takenCountBehaviour;
      this.computeTakableAmountBehaviour = computeTakableAmountBehaviour;
      this.periodTakableAmount = periodTakableAmount;
      this.takenCodes = takenCodes;
      this.takableCodes = takableCodes;
    }
  }
  
  public enum ComplationAbsenceGroup {
    complation09(432, "90B", codesCompl09);

    public int complationLimitAmount;
    public String replacingCode;
    public Set<String> complationCodes;

    private ComplationAbsenceGroup(int complationLimitAmount, String replacingCode, Set<String> complationCodes) {
      this.complationLimitAmount = complationLimitAmount;
      this.replacingCode = replacingCode;
      this.complationCodes = complationCodes;
      
    }
    
  }
  
  public enum GroupAbsenceType {
    
    group661(
        PeriodType.year,
        AmountType.minutes,
        TakableAbsenceGroup.takable661,
        null),
    
    group18(
        PeriodType.month,
        AmountType.minutes,
        TakableAbsenceGroup.takable18,
        null),
    
    group19(
        PeriodType.month,
        AmountType.minutes,
        TakableAbsenceGroup.takable19,
        null),
    
    group09(
        PeriodType.always,
        AmountType.minutes,
        null,
        ComplationAbsenceGroup.complation09);
    
    public PeriodType periodType;
    public AmountType periodAmountType;
    public TakableAbsenceGroup takableAbsenceGroup;
    public ComplationAbsenceGroup complationAbsenceGroup;
    
     
    private GroupAbsenceType(PeriodType periodType, 
        AmountType periodAmountType, 
        TakableAbsenceGroup takableAbsenceGroup,
        ComplationAbsenceGroup complationAbsenceGroup) {
      this.periodType = periodType;
      this.periodAmountType = periodAmountType;
      this.takableAbsenceGroup = takableAbsenceGroup;
      this.complationAbsenceGroup = complationAbsenceGroup;
    }
    
  }
  
  public static class AbsencePeriod {
    
    public Person person;
    
    /*Period*/
    public LocalDate from;                      // Data inizio
    public LocalDate to;                        // Data fine
    public AmountType periodAmountType;         // Il tipo di ammontare del periodo 
    
    /*Takable/taken model*/
    public CountBehaviour takableCountBehaviour;// Come contare il tetto totale
    public int periodTakableAmount;             // Il tetto massimo   
    
    public CountBehaviour takenCountBehaviour;  // Come contare il tetto consumato
    public int periodTakenAmount;               // Il tetto consumato
    
    public Set<AbsenceType> takableCodes;       // I tipi assenza prendibili del periodo
    public Set<AbsenceType> takenCodes;         // I tipi di assenza consumati del periodo

    public List<Absence> takenAbsences;         // Le assenze consumate
    
    /*Complation model*/
    public AmountType complationAmountType;     // Tipo di ammontare completamento
    public int complationLimitAmount;           // Limite di completamento
    public int complationConsumedAmount;        // Ammontare completamento attualmente consumato
    
    public AbsenceType replacingCode;           // Codice di rimpiazzamento      
    public Set<AbsenceType> complationCodes;    // Codici di completamento
    
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
  
  public enum ComputeAmountBehaviour {
    normal, workingTimePercent;
  }
    
}
