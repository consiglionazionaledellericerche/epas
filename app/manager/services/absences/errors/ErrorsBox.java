package manager.services.absences.errors;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.beust.jcommander.internal.Lists;

import manager.services.absences.errors.CriticalError.CriticalProblem;

import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Map;


public class ErrorsBox {
  
  private Map<Absence, Map<AbsenceProblem, AbsenceError>> absenceErrorsSuperMap = Maps.newHashMap();
  private Map<CriticalProblem, List<CriticalError>> criticalErrorsMap = Maps.newHashMap();
  
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
  
  private void addCriticalErrorIntoMap(LocalDate date, Absence absence, 
      AbsenceType absenceType, 
      AbsenceType conflictingAbsenceType,
      CriticalProblem criticalProblem) {
    List<CriticalError> criticalErrors = criticalErrorsMap.get(criticalProblem);
    if (criticalErrors == null) {
      criticalErrors = Lists.newArrayList();
      criticalErrorsMap.put(criticalProblem, criticalErrors);
    }
    criticalErrors.add(CriticalError.builder()
        .date(date)
        .absenceType(absenceType)
        .criticalProblem(criticalProblem).build());
  }
  
  public void addCriticalError(LocalDate date, CriticalProblem criticalProblem) {
    addCriticalErrorIntoMap(date, null, null, null, criticalProblem);
  }
  
  public void addCriticalError(Absence absence, CriticalProblem criticalProblem) {
    addCriticalErrorIntoMap(absence.getAbsenceDate(), absence, null, null, criticalProblem);
  }
  
  public void addCriticalError(LocalDate date, AbsenceType absenceType, CriticalProblem criticalProblem) {
    addCriticalErrorIntoMap(date, null, absenceType, null, criticalProblem);
  }
  
  public void addCriticalError(LocalDate date, AbsenceType absenceType, 
      AbsenceType conflictingAbsenceType, CriticalProblem criticalProblem) {
    addCriticalErrorIntoMap(date, null, absenceType, conflictingAbsenceType, criticalProblem);
  }
  
  public boolean containsCriticalErrors() {
    return !criticalErrorsMap.keySet().isEmpty();
  }
  
  /// STATIC
  
  public static boolean absenceTypeHasErrors(List<ErrorsBox> errorsBoxes, AbsenceType absenceType) {
    return false;
  }
  
  public static boolean containsCriticalErrors(List<ErrorsBox> errorsBoxes) {
    return false;
  }

  
  

}
