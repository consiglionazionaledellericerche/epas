package models.absences;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Audited
@Entity
@Table(name = "absence_troubles")
public class AbsenceTrouble extends BaseModel {

  @Enumerated(EnumType.STRING)
  public AbsenceProblem trouble;

  @ManyToOne//(fetch = FetchType.LAZY)
  @JoinColumn(name = "absence_id", nullable = false, updatable = false)
  public Absence absence;

 
  public enum AbsenceProblem {
    
    //Ignorata dal controllo
    IgnoredOutOfContract,
    IgnoredBeforeInitialization,
    
    //Incompatibilità all'interno di un giorno
    TwoSameCodeSameDay,
    TwoReplacingSameDay,
    TwoComplationSameDay,
    IncompatibilyTypeSameDay,
    AllDayAlreadyExists,
    NotOnHoliday,
    DailyAmountExceeded,
    
    //Limiti
    LimitExceeded,
    CompromisedLimitExceeded,
    
    //Completamenti
    MissingReplacing,
    WrongReplacing,
    TooEarlyReplacing,
    CompromisedReplacing,
    
    //No diritto
    NoChildExist,
    UngrantedAbsence,
    
    //Continuità fine settimana
    WeekEndContinuityBroken,

    //Errore di implementazione / configurazione
    UselessAbsenceInPeriod,
    TwoPeriods, 
    IncalcolableJustifiedAmount,
    IncalcolableComplationAmount,
    
    //Il suo tipo ha un problema
    AbsenceTypeProblem,
    
    //Il suo gruppo ha un problema
    CompromisedTakableComplationGroup,
  }
  
  public enum AbsenceTypeProblem {
    OnlyReplacingRuleViolated,
    IncalcolableReplacingAmount,
    ConflictingReplacingAmount,
  }

  public enum RequestProblem {
    //Errori nel gestore della form... 
    //sono gravi e non hanno bisogno di grossi dettagli
    CantInferAbsenceCode,
    WrongJustifiedType,
    CodeNotAllowedInGroup,
    
    NoChildExists,  //risolvere la duplicazione con absenceProblem...
  }
  
  public enum ImplementationProblem {
    UnsupportedOperation,
    UnimplementedTakableComplationGroup,
  }
  
}
