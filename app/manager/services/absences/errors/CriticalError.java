package manager.services.absences.errors;

import lombok.Builder;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;

import org.joda.time.LocalDate;

@Builder    
public class CriticalError {
  
  public CriticalProblem criticalProblem;
  public LocalDate date;
  public GroupAbsenceType groupAbsenceType;
  public JustifiedType justifiedType;
  public AbsenceType absenceType;
  public AbsenceType conflictingAbsenceType;
  public Absence absence;
  
  public enum CriticalProblem {

    //Errori 
    UnimplementedTakableComplationGroup,
    
    //Errore generatore form html
    CantInferAbsenceCode,
    WrongJustifiedType,
    CodeNotAllowedInGroup,
    
    //Errori inaspettati
    TwoPeriods,                      //Absence/Group
    IncalcolableJustifiedAmount,     //Absence
    IncalcolableComplationAmount,    //Absence
    OnlyReplacingRuleViolated,       //Group  
    IncalcolableReplacingAmount,     //Group    
    ConflictingReplacingAmount;      //Group

  }
}