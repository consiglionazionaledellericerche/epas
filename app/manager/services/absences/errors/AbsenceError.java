package manager.services.absences.errors;

import com.google.common.base.MoreObjects;

import lombok.Builder;

import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;

import java.util.List;
import java.util.Set;

@Builder
public class AbsenceError {
  
  public Absence absence;
  public AbsenceProblem absenceProblem;
  public Set<Absence> conflictingAbsences;     //le assenze che conflittano per lo stesso problem
  
  public String toString() {
    return MoreObjects.toStringHelper(AbsenceError.class)
    .add("date", absence.getAbsenceDate())
    .add("code", absence.absenceType.code)
    .add("problem", absenceProblem)
    .toString();
  }
}