package manager.services.absences.errors;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;

import java.util.List;
import java.util.Map;


public class ErrorsBox {
  
  private Map<Absence, Map<AbsenceProblem, AbsenceError>> absenceErrorsSuperMap = Maps.newHashMap();
  
  private void addAbsenceErrorIntoMap(Absence absence, AbsenceProblem absenceProblem, 
      Absence conflictingAsbence) {
    Map<AbsenceProblem, AbsenceError> absenceErrors = absenceErrorsSuperMap.get(absence);
    if (absenceErrors == null) {
      absenceErrors = Maps.newHashMap();
    }
    AbsenceError absenceError = absenceErrors.get(absenceProblem);
    if (absenceError == null) {
      absenceError = AbsenceError.builder()
          .absence(absence)
          .absenceProblem(absenceProblem)
          .conflictingAbsences(Sets.newHashSet())
          .build();
    }
    if (conflictingAsbence != null) {
      absenceError.conflictingAbsences.add(conflictingAsbence);
    }
  }
  
  public void addAbsenceError(Absence absence, AbsenceProblem absenceProblem) {
    addAbsenceErrorIntoMap(absence, absenceProblem, null);
  }
  
  public void addAbsenceError(Absence absence, AbsenceProblem absenceProblem, Absence conflictingAbsence) {
    addAbsenceErrorIntoMap(absence, absenceProblem, conflictingAbsence);
    addAbsenceErrorIntoMap(conflictingAbsence, absenceProblem, absence);
  }
  
  public void addAbsenceError(Absence absence, AbsenceProblem absenceProblem, List<Absence> conflictingAbsences) {
    for (Absence conflictingAbsence : conflictingAbsences) {
      addAbsenceError(absence, absenceProblem, conflictingAbsence);
    }
  }
  

}
