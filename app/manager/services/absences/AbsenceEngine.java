package manager.services.absences;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    
    //////////////////////////////////////////////////////////////////////////////////////
    /* Build Takable Component */
    
    if (groupAbsenceType.takableAbsenceGroup != null) {
      
      TakableComponent takableComponent = new TakableComponent();
      
      TakableAbsenceGroup takableAbsenceGroup = groupAbsenceType.takableAbsenceGroup;

      takableComponent.takeAmountType = takableAbsenceGroup.takeAmountType;
      
      takableComponent.periodTakableAmount = takableAbsenceGroup.periodTakableAmount;
      if (!takableAbsenceGroup.computeTakableAmountBehaviour
          .equals(ComputeAmountBehaviour.normal)) {
        // TODO: ex. workingTimePercent
      }

      takableComponent.takableCountBehaviour = takableAbsenceGroup.takableCountBehaviour;
      takableComponent.takenCountBehaviour = takableAbsenceGroup.takenCountBehaviour;

      takableComponent.takenCodes = 
          Sets.newHashSet(absenceTypeDao.absenceTypeCodeSet(takableAbsenceGroup.takenCodes));
      takableComponent.takableCodes = 
          Sets.newHashSet(absenceTypeDao.absenceTypeCodeSet(takableAbsenceGroup.takableCodes));

      //TODO: le other absences vanno aggiunte!
      takableComponent.takenAbsences = absenceDao.getAbsencesInCodeList(person, 
          absencePeriod.from, absencePeriod.to, Lists.newArrayList(takableComponent.takenCodes), true);

      takableComponent.periodTakenAmount = 0;
      for (Absence absence : takableComponent.takenAbsences) {
        takableComponent.periodTakenAmount += 
            computeAbsenceAmount(person, date, absence.absenceType, takableComponent.takeAmountType);
      }
      
      absencePeriod.takableComponent = Optional.of(takableComponent);

    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    /* Build Complation component */
    
    if (groupAbsenceType.complationAbsenceGroup != null) {
      
      ComplationComponent complationComponent = new ComplationComponent();
      
      ComplationAbsenceGroup complationAbsenceGroup = groupAbsenceType.complationAbsenceGroup;
      complationComponent.complationAmountType = complationAbsenceGroup.complationAmountType;
      
      complationComponent.complationLimitAmount = complationAbsenceGroup.complationLimitAmount;
      
      complationComponent.replacingCode = 
          absenceTypeDao.getAbsenceTypeByCode(complationAbsenceGroup.replacingCode).get();
      
      complationComponent.complationCodes = 
          Sets.newHashSet(absenceTypeDao.absenceTypeCodeSet(complationAbsenceGroup.complationCodes));
      
      //TODO: le other absences vanno aggiunte!
      complationComponent.replacingAbsences = 
          absenceDao.getAbsencesInCodeList(person, absencePeriod.from, 
          absencePeriod.to, Lists.newArrayList(complationComponent.replacingCode), true);
  
      complationComponent.complationAbsences = 
          absenceDao.getAbsencesInCodeList(person, absencePeriod.from, 
          absencePeriod.to, Lists.newArrayList(complationComponent.complationCodes), true);
      
      complationComponent.complationConsumedAmount = 0;
      for (Absence absence : Stream.concat(complationComponent.complationAbsences.stream(), 
          complationComponent.replacingAbsences.stream()).collect(Collectors.toList())) {
        
          complationComponent.complationConsumedAmount += computeAbsenceAmount(person, date, 
              absence.absenceType, complationComponent.complationAmountType);
      }
      
      absencePeriod.complationComponent = Optional.of(complationComponent);
            
      //Un illegal state è  absencePeriod.complationConsumedAmount < 0 ...
    }
    
    return absencePeriod;

  }
  
  public int computeTakableAmount(TakableComponent takableComponent) {
    int takableAmount = takableComponent.periodTakableAmount;
    if (!takableComponent.takableCountBehaviour.equals(CountBehaviour.period)) {
      // TODO: sumAllPeriod, sumUntilPeriod; 
    }
    return takableAmount;
  }
  
  public int computeTakenAmount(TakableComponent takableComponent) {
    int takenAmount = takableComponent.periodTakenAmount;
    if (!takableComponent.takenCountBehaviour.equals(CountBehaviour.period)) {
      // TODO: sumAllPeriod, sumUntilPeriod; 
    } 
    return takenAmount;
  }

  private int computeAbsenceAmount(Person person, LocalDate date, 
      AbsenceType absenceType, AmountType amountType) {
    
    if (amountType.equals(AmountType.units)) {
      return 1;
    }
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
  
  /**
   * 
   * @param absencePeriod
   * @param absenceType
   * @param date
   * @return
   */
  public boolean requestForAbsenceInPeriod(AbsencePeriod absencePeriod, 
      AbsenceRequestType absenceRequestType, 
      AbsenceType absenceType, LocalDate date) {
    
    // Solo Takable component
    if (absencePeriod.takableComponent.isPresent() && !absencePeriod.complationComponent.isPresent()) {
      
      Preconditions.checkState(absenceRequestType.equals(AbsenceRequestType.insertTakable) 
          || absenceRequestType.equals(AbsenceRequestType.deleteTakable));
      
      Preconditions.checkState(absencePeriod.takableComponent.get().takableCodes.contains(absenceType));
      
      int absenceAmount = computeAbsenceAmount(absencePeriod.person, date, absenceType, 
          absencePeriod.takableComponent.get().takeAmountType );
      int takableAmount = computeTakableAmount(absencePeriod.takableComponent.get());
      int takenAmount = computeTakenAmount(absencePeriod.takableComponent.get());
      
      return takableAmount - takenAmount - absenceAmount > 0;

      
    }
    // Solo Complation component
    if (!absencePeriod.takableComponent.isPresent() && absencePeriod.complationComponent.isPresent()) {

      Preconditions.checkState(absenceRequestType.equals(AbsenceRequestType.insertComplation) 
          || absenceRequestType.equals(AbsenceRequestType.deleteComplation));

      // inserisco il codice
      
      // se supero il limite aggiungo anche il codice di completamento 
      
    }
    
    // Entrambi i componenti
    if (absencePeriod.takableComponent.isPresent() && absencePeriod.complationComponent.isPresent()) {
      
    }

    return false; //illegal state
  }
  
 
  public enum TakableAbsenceGroup {
    
    takable661(
        AmountType.minutes,
        CountBehaviour.period,
        CountBehaviour.period,
        ComputeAmountBehaviour.normal,
        1080,
        codes661,
        codes661),
    
    takable18(
        AmountType.minutes,
        CountBehaviour.period,
        CountBehaviour.period,
        ComputeAmountBehaviour.normal,
        1080,
        codes18,
        codes18),
    
    takable19(
        AmountType.minutes,
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
    public AmountType takeAmountType;
     
    private TakableAbsenceGroup( 
        AmountType takeAmountType,
        CountBehaviour takableCountBehaviour, 
        CountBehaviour takenCountBehaviour, 
        ComputeAmountBehaviour computeTakableAmountBehaviour,
        Integer periodTakableAmount,
        Set<String> takenCodes, Set<String> takableCodes) {
      this.takeAmountType = takeAmountType;
      this.takableCountBehaviour = takableCountBehaviour;
      this.takenCountBehaviour = takenCountBehaviour;
      this.computeTakableAmountBehaviour = computeTakableAmountBehaviour;
      this.periodTakableAmount = periodTakableAmount;
      this.takenCodes = takenCodes;
      this.takableCodes = takableCodes;
    }
  }
  
  public enum ComplationAbsenceGroup {
    complation09(AmountType.minutes, 432, "90B", codesCompl09);

    public int complationLimitAmount;
    public String replacingCode;
    public Set<String> complationCodes;
    public AmountType complationAmountType;

    private ComplationAbsenceGroup(AmountType complationAmountType, 
        int complationLimitAmount, String replacingCode, Set<String> complationCodes) {
      this.complationAmountType = complationAmountType;
      this.complationLimitAmount = complationLimitAmount;
      this.replacingCode = replacingCode;
      this.complationCodes = complationCodes;
      
    }
    
  }
  
  public enum GroupAbsenceType {
    
    group661(
        PeriodType.year,
        TakableAbsenceGroup.takable661,
        null),
    
    group18(
        PeriodType.month,
        TakableAbsenceGroup.takable18,
        null),
    
    group19(
        PeriodType.month,
        TakableAbsenceGroup.takable19,
        null),
    
    group09(
        PeriodType.always,
        null,
        ComplationAbsenceGroup.complation09);
    
    public PeriodType periodType;
    public TakableAbsenceGroup takableAbsenceGroup;
    public ComplationAbsenceGroup complationAbsenceGroup;
    
     
    private GroupAbsenceType(PeriodType periodType, 
        TakableAbsenceGroup takableAbsenceGroup,
        ComplationAbsenceGroup complationAbsenceGroup) {
      this.periodType = periodType;
      this.takableAbsenceGroup = takableAbsenceGroup;
      this.complationAbsenceGroup = complationAbsenceGroup;
    }
    
  }
  
  public static class AbsencePeriod {
    
    public Person person;
    
    /*Period*/
    public LocalDate from;                      // Data inizio
    public LocalDate to;                        // Data fine
    
    public Optional<TakableComponent> takableComponent;
    public Optional<ComplationComponent> complationComponent;
    
    /*Next Period*/
    public AbsencePeriod nextAbsencePeriod;     // Puntatore al periodo successivo ->
    public AbsencePeriod previousAbsencePeriod; // <- puntatore al periodo precedente
    
  }
  
  public static class TakableComponent {

    public AmountType takeAmountType;         // Il tipo di ammontare del periodo
    
    public CountBehaviour takableCountBehaviour;// Come contare il tetto totale
    public int periodTakableAmount;             // Il tetto massimo
    
    public CountBehaviour takenCountBehaviour;  // Come contare il tetto consumato
    public int periodTakenAmount;               // Il tetto consumato
    
    public Set<AbsenceType> takableCodes;       // I tipi assenza prendibili del periodo
    public Set<AbsenceType> takenCodes;         // I tipi di assenza consumati del periodo

    public List<Absence> takenAbsences;         // Le assenze consumate
  }
  
  public static class ComplationComponent {

    public AmountType complationAmountType;     // Tipo di ammontare completamento
    
    public int complationLimitAmount;           // Limite di completamento
    public int complationConsumedAmount;        // Ammontare completamento attualmente consumato
    
    public AbsenceType replacingCode;           // Codice di rimpiazzamento      
    public Set<AbsenceType> complationCodes;    // Codici di completamento
    
    public List<Absence> replacingAbsences;     // Le assenze di rimpiazzamento (solo l'ultima??)     
    public List<Absence> complationAbsences;    // Le assenze di completamento
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
  
  public enum AbsenceRequestType {
    insertTakable, insertComplation, deleteTakable, deleteComplation;
  }
    
}
