package manager.services.absences.errors;

import java.util.Set;

import lombok.Builder;

import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;

@Builder
public class AbsenceError {
  
  public Absence absence;
  public AbsenceProblem absenceProblem;
  public Set<Absence> conflictingAbsences;     //le assenze che conflittano per lo stesso problem
  
}