package manager.services.absences.errors;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.beust.jcommander.internal.Lists;

import lombok.extern.slf4j.Slf4j;

import manager.services.absences.errors.CriticalError.CriticalProblem;

import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ErrorsBox {
  
  private Map<Absence, Map<AbsenceProblem, AbsenceError>> absenceErrorsSuperMap = Maps.newHashMap();
  private Map<CriticalProblem, List<CriticalError>> criticalErrorsMap = Maps.newHashMap();
  
  private void addAbsenceErrorIntoMap(Absence absence, AbsenceProblem absenceProblem, 
      Absence conflictingAsbence) {
    Map<AbsenceProblem, AbsenceError> absenceErrors = absenceErrorsSuperMap.get(absence);
    if (absenceErrors == null) {
      absenceErrors = Maps.newHashMap();
      absenceErrorsSuperMap.put(absence, absenceErrors);
    }
    AbsenceError absenceError = absenceErrors.get(absenceProblem);
    if (absenceError == null) {
      absenceError = AbsenceError.builder()
          .absence(absence)
          .absenceProblem(absenceProblem)
          .conflictingAbsences(Sets.newHashSet())
          .build();
      absenceErrors.put(absenceProblem, absenceError);
    }
    if (conflictingAsbence != null) {
      absenceError.conflictingAbsences.add(conflictingAsbence);
    }
    log.info("Aggiunto errore alla mappa {} {}", absence.toString(), absenceProblem);
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
    
    log.info("Aggiunto errore critico alla mappa {}", criticalProblem);
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
  
  public boolean containsAbsencesErrors() {
    return !absenceErrorsSuperMap.keySet().isEmpty();
  }
  
  public boolean containAbsenceErrors(Absence absence) {
    return absenceErrorsSuperMap.get(absence) != null;
  }
  
  public boolean containAbsenceError(Absence absence, AbsenceProblem absenceProblem) {
    return absenceError(absence, absenceProblem) != null;
  }
  
  public AbsenceError absenceError(Absence absence, AbsenceProblem absenceProblem) {
    Map<AbsenceProblem, AbsenceError> absenceErrors = absenceErrorsSuperMap.get(absence);
    if (absenceErrors == null) {
      return null;
    }
    if (absenceErrors.get(absenceProblem) == null) {
      return null;
    }
    return null;
  }
  
  public List<AbsenceError> absenceErrors(Absence absence) {
    Map<AbsenceProblem, AbsenceError> absenceErrors = absenceErrorsSuperMap.get(absence);
    if (absenceErrors == null) {
      return Lists.newArrayList();
    }
    return Lists.newArrayList(absenceErrors.values());
  }
  
  public Set<CriticalError> criticalErrors() {
    Set<CriticalError> list = Sets.newHashSet();
    for (List<CriticalError> errors : criticalErrorsMap.values()) {
      list.addAll(errors);
    }
    return list;
  }
  
  /// STATIC
  
  public static boolean absenceTypeHasErrors(List<ErrorsBox> errorsBoxes, AbsenceType absenceType) {
    return false;
  }
  
  public static boolean containsCriticalErrors(List<ErrorsBox> errorsBoxes) {
    for (ErrorsBox errorsBox : errorsBoxes) {
      if (errorsBox.containsCriticalErrors()) {
        return true;
      }
    }
    return false;
  }
  
 
  
//  public static List<AbsenceError> instancesOfProblem(ErrorsBox errorsBox, AbsenceProblem absenceProblem) {
//    List<AbsenceError> absencesErrors = Lists.newArrayList();
//    for (Map<AbsenceProblem, AbsenceError> map : errorsBox.absenceErrorsSuperMap.values()) {
//      absencesErrors.addAll(map.values());
//    }
//    return absencesErrors;
//  }
//  
//  public static List<AbsenceError> orderedInstancesOfProblem(ErrorsBox errorsBox, AbsenceProblem absenceProblem) {
//    SortedMap<LocalDate, List<AbsenceError>> sortedErrors = Maps.newTreeMap();
//    List<AbsenceError> instancesOfProblem = instancesOfProblem(errorsBox, absenceProblem); 
//    for (AbsenceError absenceError : instancesOfProblem) {
//      List<AbsenceError> dateErrors = sortedErrors.get(absenceError.absence.getAbsenceDate());
//      if (dateErrors == null) {
//        dateErrors = Lists.newArrayList();
//        sortedErrors.put(absenceError.absence.getAbsenceDate(), dateErrors);
//      }
//      sortedErrors.put(absenceError.absence.getAbsenceDate(), dateErrors);
//    }
//    return sortedErrors.values().iterator().next();     
//  }
//  
//  public static AbsenceError firstInstanceOfProblem(ErrorsBox errorsBox, AbsenceProblem absenceProblem) {
//    List<AbsenceError> absencesErrors = orderedInstancesOfProblem(errorsBox, absenceProblem);
//    if (absencesErrors.isEmpty()) {
//      return null;
//    }
//    return absencesErrors.iterator().next();
//  }
  
  public static Set<CriticalError> allCriticalErrors(List<ErrorsBox> errorsBoxes) {
    Set<CriticalError> errors = Sets.newHashSet();
    for (ErrorsBox errorsBox : errorsBoxes) {
      errors.addAll(errorsBox.criticalErrors());
    } 
    return errors;
  }
  
  public static Set<AbsenceProblem> allAbsenceProblems(List<ErrorsBox> errorsBoxes, Absence absence) {
    Set<AbsenceProblem> allProblems = Sets.newHashSet();
    for (ErrorsBox errorsBox : errorsBoxes) {
      Map<AbsenceProblem, AbsenceError> absenceErrors = errorsBox.absenceErrorsSuperMap.get(absence);
      if (absenceErrors != null) {
        for (AbsenceError absenceError : absenceErrors.values()) {
          allProblems.add(absenceError.absenceProblem);
        }
      }
    }
    return allProblems;
  }


}
