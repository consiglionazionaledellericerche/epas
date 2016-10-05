package models.absences;

import lombok.Builder;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Builder
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
    IgnoredOutOfContract(false),
    IgnoredBeforeInitialization(false),
    
    //Generici
    TwoSameCodeSameDay(false),
    AllDayAlreadyExists(false),
    NotOnHoliday(false),
    DailyAmountExceeded(false),
    IncompatibilyTypeSameDay(false),
    WeekEndContinuityBroken(false),
    UngrantedAbsence(false),
    
    //Gruppo
    LimitExceeded(false),
    CompromisedLimitExceeded(false),
    
    TwoReplacingSameDay(false),
    TwoComplationSameDay(false),
    MissingReplacing(false),
    WrongReplacing(false),
    TooEarlyReplacing(false),
    CompromisedReplacing(false),               //data compromessa
    CompromisedTakableComplationGroup(false),  //assenze successive
    
    //Figli
    NoChildExist(false),
    
    //Implementazione
    ImplementationProblem(true);

    
    public boolean isImplementationProblem;
    
    private AbsenceProblem(boolean isImplementationProblem) {
      this.isImplementationProblem = isImplementationProblem;
    }

  }
  
  

  
}
