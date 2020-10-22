package models.absences;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Builder;
import models.base.BaseModel;
import org.hibernate.envers.Audited;

@Builder
@Audited
@Entity
@Table(name = "absence_troubles")
public class AbsenceTrouble extends BaseModel {

  private static final long serialVersionUID = -5066077912284859060L;

  @Enumerated(EnumType.STRING)
  public AbsenceProblem trouble;

  @ManyToOne//(fetch = FetchType.LAZY)
  @JoinColumn(name = "absence_id", nullable = false, updatable = false)
  public Absence absence;

 
  public enum AbsenceProblem {
    
    //Ignorata dal controllo
    IgnoredOutOfContract(false),
    IgnoredBeforeInitialization(false),
    
    //Generici
    TwoSameCodeSameDay(false),
    AllDayAlreadyExists(false),
    NotOnHoliday(false),
    IncompatibilyTypeSameDay(false),
    WeekEndContinuityBroken(false),
    UngrantedAbsence(false),
    MinimumTimeViolated(false),
    MaximumTimeExceed(false),
    Expired(false),
    
    //Gruppo
    LimitExceeded(false),
    CompromisedTwoComplation(false),           //due completamenti nello stesso giorno
    CompromisedTakableComplationGroup(false),  //assenze successive
    
    //Figli
    NoChildExist(false),
    
    //Implementazione
    ImplementationProblem(true),
    
    //Warnings
    ForceInsert(false, true),
    InReperibility(false, true),
    InShift(false, true),
    InReperibilityOrShift(false, true),
    Migration661(false, true);

    public boolean isImplementationProblem;
    
    public boolean isWarning = false;
    
    private AbsenceProblem(boolean isImplementationProblem, boolean isWarning) {
      this.isImplementationProblem = isImplementationProblem;
      this.isWarning = isWarning;
    }
    
    private AbsenceProblem(boolean isImplementationProblem) {
      this.isImplementationProblem = isImplementationProblem;
      this.isWarning = false;
    }

  }

}
