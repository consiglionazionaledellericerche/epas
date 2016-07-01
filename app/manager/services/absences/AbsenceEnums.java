package manager.services.absences;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import manager.services.absences.AbsenceEngine.AmountType;
import manager.services.absences.AbsenceEngine.ComputeAmountRestriction;
import manager.services.absences.AbsenceEngine.CountBehaviour;
import manager.services.absences.AbsenceEngine.PeriodType;

import java.util.List;
import java.util.Set;

public class AbsenceEnums {

  public static final Set<String> codes661 = 
      Sets.newHashSet("661h1", "661h2", "661h3", "661h4", "661h5", "661h6", "661h7", 
          "661h8", "661h9");
  public static final Set<String> codes18 = 
      Sets.newHashSet("18h1", "18h2", "18h3", "18h4", "18h5", "18h6", "18h7", "18h8", "18h9");
  public static final Set<String> codes19 = 
      Sets.newHashSet("19h1", "19h2", "19h3", "19h4", "19h5", "19h6", "19h7", "19h8", "19h9");

  public static final Set<String> codesCompl09 = 
      Sets.newHashSet("09h1", "09h2", "09h3", "09h4", "09h5", "09h6", "09h7");

  public enum TakableAbsenceGroup {

    /* Permesso Orario Permesso Personale 18h anno (no completamenti) */ 
    takable661(
        AmountType.minutes,
        CountBehaviour.period,
        CountBehaviour.period,
        1080,                  //18h
        Lists.newArrayList(ComputeAmountRestriction.workingPeriodPercent, 
            ComputeAmountRestriction.workingTimePercent),
        codes661,
        codes661),

    /* Assistenza Disabile 3gg mese */
    takable18(
        AmountType.units,
        CountBehaviour.period,
        CountBehaviour.period,
        3,
        Lists.newArrayList(),
        codes18,
        codes18),

    /* Dipendente Disabile 3gg mese */
    takable19(
        AmountType.units,
        CountBehaviour.period,
        CountBehaviour.period,
        3,
        Lists.newArrayList(),
        codes19,
        codes19);

    public AmountType takeAmountType;
    public CountBehaviour takableCountBehaviour;
    public CountBehaviour takenCountBehaviour;
    public Integer periodTakableAmount;
    public List<ComputeAmountRestriction> computeTakableAmountBehaviour;
    public Set<String> takenCodes; 
    public Set<String> takableCodes;

    private TakableAbsenceGroup( 
        AmountType takeAmountType,
        CountBehaviour takableCountBehaviour, 
        CountBehaviour takenCountBehaviour,
        Integer periodTakableAmount,
        List<ComputeAmountRestriction> computeTakableAmountBehaviour,
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
    
    complation09(AmountType.minutes, "90B", codesCompl09),
    complation18(AmountType.minutes, "18", codes18);

    public AmountType complationAmountType;
    public String replacingCode;
    public Set<String> complationCodes;

    private ComplationAbsenceGroup(AmountType complationAmountType, 
        String replacingCode, Set<String> complationCodes) {
      this.complationAmountType = complationAmountType;
      this.replacingCode = replacingCode;
      this.complationCodes = complationCodes;
    }
  }

  public enum GroupAbsenceType {

    /* Permesso Orario Permesso Personale 18h anno (no completamenti) */ 
    group661(
        PeriodType.year,
        TakableAbsenceGroup.takable661,
        null),

    /* Assistenza Disabile 3gg mese */
    group18(
        PeriodType.month,
        TakableAbsenceGroup.takable18,
        null),

    /* Dipendente Disabile 3gg mese */
    group19(
        PeriodType.month,
        TakableAbsenceGroup.takable19,
        null),

    /* Completamento giorno visita medica -> Proporzionato Tempo Lavoro e Durata */
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

}
