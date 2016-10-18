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
  
  private Map<Absence, Map<AbsenceProblem, AbsenceError>> absenceWarningsSuperMap = 
      Maps.newHashMap();
  
  private void addAbsenceErrorIntoMap(Absence absence, AbsenceProblem absenceProblem, 
      Absence conflictingAsbence, Map<Absence, Map<AbsenceProblem, AbsenceError>> map) {
    Map<AbsenceProblem, AbsenceError> absenceErrors = map.get(absence);
    if (absenceErrors == null) {
      absenceErrors = Maps.newHashMap();
      map.put(absence, absenceErrors);
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
    if (map.equals(absenceErrorsSuperMap)) {
      log.info("Aggiunto errore alla mappa {} {}", absence.toString(), absenceProblem);  
    }
    if (map.equals(absenceWarningsSuperMap)) {
      log.info("Aggiunto warning alla mappa {} {}", absence.toString(), absenceProblem);
    }
    
  }
  
  public void addAbsenceError(Absence absence, AbsenceProblem absenceProblem) {
    addAbsenceErrorIntoMap(absence, absenceProblem, null, absenceErrorsSuperMap);
  }
  
  /**
   * Aggiunge l'errore alla scatola.
   * @param absence assenza
   * @param absenceProblem problema
   * @param conflictingAbsence assenza in conflitto
   */
  public void addAbsenceError(Absence absence, AbsenceProblem absenceProblem, 
      Absence conflictingAbsence) {
    addAbsenceErrorIntoMap(absence, absenceProblem, conflictingAbsence, absenceErrorsSuperMap);
    addAbsenceErrorIntoMap(conflictingAbsence, absenceProblem, absence, absenceErrorsSuperMap);
  }
  
  /**
   * Aggiunge l'errore alla scatola.
   * @param absence assenza
   * @param absenceProblem problema
   * @param conflictingAbsences assenze in conflitto
   */
  public void addAbsenceError(Absence absence, AbsenceProblem absenceProblem, 
      List<Absence> conflictingAbsences) {
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
  
  public void addCriticalError(LocalDate date, AbsenceType absenceType, 
      CriticalProblem criticalProblem) {
    addCriticalErrorIntoMap(date, null, absenceType, null, criticalProblem);
  }
  
  public void addCriticalError(LocalDate date, AbsenceType absenceType, 
      AbsenceType conflictingAbsenceType, CriticalProblem criticalProblem) {
    addCriticalErrorIntoMap(date, null, absenceType, conflictingAbsenceType, criticalProblem);
  }
  
  public void addAbsenceWarning(Absence absence, AbsenceProblem absenceProblem) {
    addAbsenceErrorIntoMap(absence, absenceProblem, null, absenceWarningsSuperMap);
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
  
  /**
   * Il problema per quell'assenza.
   * @param absence assenza
   * @param absenceProblem problema 
   * @return absenceError se presente
   */
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
  
  /**
   * I problemi per quell'assenza.
   * @param absence assenza
   * @return gli errori
   */
  public List<AbsenceError> absenceErrors(Absence absence) {
    Map<AbsenceProblem, AbsenceError> absenceErrors = absenceErrorsSuperMap.get(absence);
    if (absenceErrors == null) {
      return Lists.newArrayList();
    }
    return Lists.newArrayList(absenceErrors.values());
  }
  
  /**
   * I warning per quell'assenza.
   * @param absence assenza
   * @return i warning
   */
  public List<AbsenceError> absenceWarnings(Absence absence) {
    Map<AbsenceProblem, AbsenceError> absenceWarnings = absenceWarningsSuperMap.get(absence);
    if (absenceWarnings == null) {
      return Lists.newArrayList();
    }
    return Lists.newArrayList(absenceWarnings.values());
  }
  
  /**
   * Gli errori critici nella box.
   * @return set
   */
  public Set<CriticalError> criticalErrors() {
    Set<CriticalError> list = Sets.newHashSet();
    for (List<CriticalError> errors : criticalErrorsMap.values()) {
      list.addAll(errors);
    }
    return list;
  }
  
  /// STATIC
  
  /**
   * Se le boxes contengono errori critici.
   * @param errorsBoxes boxes
   * @return esito
   */
  public static boolean boxesContainsCriticalErrors(List<ErrorsBox> errorsBoxes) {
    for (ErrorsBox errorsBox : errorsBoxes) {
      if (errorsBox.containsCriticalErrors()) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Tutti gli errori critici nelle boxes.
   * @param errorsBoxes boxes
   * @return set
   */
  public static Set<CriticalError> allCriticalErrors(List<ErrorsBox> errorsBoxes) {
    Set<CriticalError> errors = Sets.newHashSet();
    for (ErrorsBox errorsBox : errorsBoxes) {
      errors.addAll(errorsBox.criticalErrors());
    } 
    return errors;
  }
  
  /**
   * Tutti i problemi per l'assenza nelle boxes.
   * @param errorsBoxes boxes
   * @param absence assenza
   * @return set
   */
  public static Set<AbsenceProblem> allAbsenceProblems(List<ErrorsBox> errorsBoxes, 
      Absence absence) {
    Set<AbsenceProblem> allProblems = Sets.newHashSet();
    for (ErrorsBox errorsBox : errorsBoxes) {
      Map<AbsenceProblem, AbsenceError> absenceErrors = errorsBox
          .absenceErrorsSuperMap.get(absence);
      if (absenceErrors != null) {
        for (AbsenceError absenceError : absenceErrors.values()) {
          allProblems.add(absenceError.absenceProblem);
        }
      }
    }
    return allProblems;
  }


}
