package db.h2support.base;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import models.absences.AmountType;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.TakableAbsenceBehaviour.TakeAmountAdjustment;

import java.util.List;
import java.util.Set;

public class AbsenceDefinitions {

  public static Integer ONE_HOUR = 60;
  public static Integer TWO_HOUR = 120;
  public static Integer THREE_HOUR = 180;
  public static Integer FOUR_HOUR = 240;
  public static Integer FIVE_HOUR = 300;
  public static Integer SIX_HOUR = 360;
  public static Integer SEVEN_HOUR = 420;

  public static Set<JustifiedTypeName> PERMITTED_NOTHING = 
      ImmutableSet.of(JustifiedTypeName.nothing);
  public static Set<JustifiedTypeName> PERMITTED_SPECIFIED_MINUTES = 
      ImmutableSet.of(JustifiedTypeName.specified_minutes);

  public enum CategoryTabDefinition {
    
    Missione(1, "Missione", 0, true),
    Ferie(2, "Ferie e Festività Soppr.", 1, false),
    Riposi(3, "Riposo Compensativo", 2, false),
    AltreTipologie(4, "Altre Tipologie", 3, false),
    CodiciDipendenti(5, "Codici Dipendenti", 4, false);
    
    public int id;
    public String description;
    public int priority;
    public boolean isDefault;
    
    private CategoryTabDefinition(int id, String description, int priority, boolean isDefault) {
      this.id = id;
      this.description = description;
      this.priority = priority;
      this.isDefault = isDefault;
    }
  }
  
  public enum CategoryDefinition {
    
    Missione(1, "Missione", 0, CategoryTabDefinition.Missione),
    Ferie(13, "Ferie e Festività Soppr.", 1, CategoryTabDefinition.Ferie),
    Riposi(14, "Riposo Compensativo", 2, CategoryTabDefinition.Riposi),
    Permessi(2, "Permessi Vari", 3, CategoryTabDefinition.AltreTipologie),
    Congedi(3, "Congedi Parentali", 4, CategoryTabDefinition.AltreTipologie),
    L104(4, "Disabilità legge 104/92", 5, CategoryTabDefinition.AltreTipologie),
    PubblicaFunz(5, "Pubblica Funzione", 6, CategoryTabDefinition.AltreTipologie),
    MalattiaDipendente(6, "Malattia Dipendente", 7, CategoryTabDefinition.AltreTipologie),
    MalattiaPrimoFiglio(7, "Malattia Primo Figlio", 8, CategoryTabDefinition.AltreTipologie),
    MalattiaSecondoFiglio(8, "Malattia Secondo Figlio", 9, CategoryTabDefinition.AltreTipologie),
    MalattiaTerzoFiglio(9, "Malattia Terzo Figlio", 10, CategoryTabDefinition.AltreTipologie),
    CodiciAutomatici(10, "Codici Automatici", 11, CategoryTabDefinition.AltreTipologie),
    CodiciDipendenti(11, "Codici Dipendenti", 12, CategoryTabDefinition.AltreTipologie),
    AltriCodici(12, "Altri Codici", 12, CategoryTabDefinition.AltreTipologie);
    
    public int id;
    public CategoryTabDefinition tabDefinition;
    public String description;
    public Integer priority;

    private CategoryDefinition(int id, String description, Integer priority, 
        CategoryTabDefinition tabDefinition) {
      this.id = id;
      this.description = description;
      this.priority = priority;
      this.tabDefinition = tabDefinition;
      
    }
    
  }
  
  public enum AbsenceTypeDefinition {

    _661H1(0, false, false, PERMITTED_NOTHING, ONE_HOUR, JustifiedTypeName.absence_type_minutes),
    _661H2(0, false, false, PERMITTED_NOTHING, TWO_HOUR, JustifiedTypeName.absence_type_minutes),
    _661H3(0, false, false, PERMITTED_NOTHING, THREE_HOUR, JustifiedTypeName.absence_type_minutes),
    _661H4(0, false, false, PERMITTED_NOTHING, FOUR_HOUR, JustifiedTypeName.absence_type_minutes),
    _661H5(0, false, false, PERMITTED_NOTHING, FIVE_HOUR, JustifiedTypeName.absence_type_minutes),
    _661H6(0, false, false, PERMITTED_NOTHING, SIX_HOUR, JustifiedTypeName.absence_type_minutes),
    _661H7(0, false, false, PERMITTED_NOTHING, SEVEN_HOUR, JustifiedTypeName.absence_type_minutes),
    _661M(0, false, false, PERMITTED_SPECIFIED_MINUTES, null, 
        JustifiedTypeName.absence_type_minutes);

    public Integer justifiedTime;
    public boolean consideredWeekEnd;
    public boolean timeForMealTicket;
    public Set<JustifiedTypeName> justifiedTypeNamesPermitted;
    public Integer replacingTime;
    public JustifiedTypeName replacingType;

    private AbsenceTypeDefinition(Integer justifiedTime, 
        boolean consideredWeekEnd, boolean timeForMealTicket, 
        Set<JustifiedTypeName> justifiedTypeNamesPermitted, 
        Integer replacingTime, JustifiedTypeName replacingType) {
      this.justifiedTime = justifiedTime;
      this.consideredWeekEnd = consideredWeekEnd;
      this.timeForMealTicket = timeForMealTicket;
      this.justifiedTypeNamesPermitted = justifiedTypeNamesPermitted;
      this.replacingTime = replacingTime;
      this.replacingType = replacingType;

    }
  }

  public enum TakableBehaviourDefinition {

    Takable_661(AmountType.minutes, 
        ImmutableSet.of(AbsenceTypeDefinition._661M),
        ImmutableSet.of(AbsenceTypeDefinition._661M),
        1080, TakeAmountAdjustment.workingTimeAndWorkingPeriodPercent);

    public AmountType amountType;
    public Set<AbsenceTypeDefinition> takenCodes;
    public Set<AbsenceTypeDefinition> takableCodes;
    public Integer fixedLimit;
    public TakeAmountAdjustment takableAmountAdjustment;

    private TakableBehaviourDefinition(AmountType amountType,
        Set<AbsenceTypeDefinition> takenCodes, Set<AbsenceTypeDefinition> takableCodes, 
        Integer fixedLimit, TakeAmountAdjustment takableAmountAdjustment) {
      this.amountType = amountType;
      this.takenCodes = takenCodes;
      this.takenCodes = takenCodes;
      this.takableCodes = takableCodes;
      this.fixedLimit = fixedLimit;
      this.takableAmountAdjustment = takableAmountAdjustment;

    }
  }

  public enum ComplationBehaviourDefinition {

    Complation_661(AmountType.minutes, 
        ImmutableSet.of(AbsenceTypeDefinition._661M),
        ImmutableSet.of(AbsenceTypeDefinition._661H1, AbsenceTypeDefinition._661H2,
            AbsenceTypeDefinition._661H3, AbsenceTypeDefinition._661H4, 
            AbsenceTypeDefinition._661H5, AbsenceTypeDefinition._661H6,
            AbsenceTypeDefinition._661H7));

    public AmountType amountType;
    public Set<AbsenceTypeDefinition> complationCodes;
    public Set<AbsenceTypeDefinition> replacingCodes;

    private ComplationBehaviourDefinition(AmountType amountType,
        Set<AbsenceTypeDefinition> complationCodes, Set<AbsenceTypeDefinition> replacingCodes) {
      this.amountType = amountType;
      this.complationCodes = complationCodes;
      this.replacingCodes = replacingCodes;
    }
  }

  public enum GroupAbsenceTypeDefinition {

    Group_661(GroupAbsenceTypePattern.programmed, PeriodType.year, 
        TakableBehaviourDefinition.Takable_661, ComplationBehaviourDefinition.Complation_661,
        null);

    public GroupAbsenceTypePattern pattern;
    public PeriodType periodType;
    public TakableBehaviourDefinition takableAbsenceBehaviour;
    public ComplationBehaviourDefinition complationAbsenceBehaviour;
    public GroupAbsenceTypeDefinition next;

    private GroupAbsenceTypeDefinition(GroupAbsenceTypePattern pattern, 
        PeriodType periodType, TakableBehaviourDefinition takableAbsenceBehaviour,
        ComplationBehaviourDefinition complationAbsenceBehaviour, GroupAbsenceTypeDefinition next) {
      this.pattern = pattern;
      this.periodType = periodType;
      this.takableAbsenceBehaviour = takableAbsenceBehaviour;
      this.complationAbsenceBehaviour = complationAbsenceBehaviour;
      this.next = next;
    }
  }
  
}
