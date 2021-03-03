/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager.services.absences.errors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import manager.services.absences.errors.CriticalError.CriticalProblem;
import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;
import org.joda.time.LocalDate;

/**
 * Contenitore delle varie tipologie di warning, errori ed errori critici sulle assenze.
 */
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
      log.trace("Aggiunto errore alla mappa {} {}", absence.toString(), absenceProblem);
    }
    if (map.equals(absenceWarningsSuperMap)) {
      log.trace("Aggiunto warning alla mappa {} {}", absence.toString(), absenceProblem);
    }

  }

  /**
   * Aggiunge un errore relativo ad un'assenza.
   */
  public void addAbsenceError(Absence absence, AbsenceProblem absenceProblem) {
    addAbsenceErrorIntoMap(absence, absenceProblem, null, absenceErrorsSuperMap);
  }

  /**
   * Aggiunge l'errore alla scatola.
   *
   * @param absence            assenza
   * @param absenceProblem     problema
   * @param conflictingAbsence assenza in conflitto
   */
  public void addAbsenceError(Absence absence, AbsenceProblem absenceProblem,
      Absence conflictingAbsence) {
    addAbsenceErrorIntoMap(absence, absenceProblem, conflictingAbsence, absenceErrorsSuperMap);
    addAbsenceErrorIntoMap(conflictingAbsence, absenceProblem, absence, absenceErrorsSuperMap);
  }

  /**
   * Aggiunge l'errore alla scatola.
   *
   * @param absence             assenza
   * @param absenceProblem      problema
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

    log.trace("Aggiunto errore critico alla mappa {}", criticalProblem);
  }
  
  /**
   * Verifica che un certo absenceProblem sia contenuto nella mappa absenceErrorsSuperMap.
   *
   * @param absenceProblem l'oggetto contenente il tipo di problema sull'assenza
   * @return true se quel problema è contenuto nella mappa absenceErrorsSuperMap, 
   *     false altrimenti.
   */
  public boolean containsAbsenceProblem(AbsenceProblem absenceProblem) {
    for (Map<AbsenceProblem, AbsenceError>  map : absenceErrorsSuperMap.values()) {
      if (map.containsKey(absenceProblem)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Aggiunge un errore critico ad un certa data.
   */
  public void addCriticalError(LocalDate date, CriticalProblem criticalProblem) {
    addCriticalErrorIntoMap(date, null, null, null, criticalProblem);
  }

  /**
   * Aggiunge un errore critico relativo ad un'assenza.
   */
  public void addCriticalError(Absence absence, CriticalProblem criticalProblem) {
    addCriticalErrorIntoMap(absence.getAbsenceDate(), absence, null, null, criticalProblem);
  }

  /**
   * Aggiunge un errore relativo ad una tipologia di assenza e data.
   */
  public void addCriticalError(LocalDate date, AbsenceType absenceType,
      CriticalProblem criticalProblem) {
    addCriticalErrorIntoMap(date, null, absenceType, null, criticalProblem);
  }

  /**
   * Aggiunge un errore relativo ad una tipologia di assenza e data specificando il 
   * tipo di assenza che fa in conflitto.
   */
  public void addCriticalError(LocalDate date, AbsenceType absenceType,
      AbsenceType conflictingAbsenceType, CriticalProblem criticalProblem) {
    addCriticalErrorIntoMap(date, null, absenceType, conflictingAbsenceType, criticalProblem);
  }

  /**
   * Aggiunge un warning relativo ad un'assenza.
   */
  public void addAbsenceWarning(Absence absence, AbsenceProblem absenceProblem) {
    addAbsenceErrorIntoMap(absence, absenceProblem, null, absenceWarningsSuperMap);
  }

  /**
   * Verifica se ci sono errori critici.
   */
  public boolean containsCriticalErrors() {
    return !criticalErrorsMap.keySet().isEmpty();
  }

  /**
   * Verifica se ci sono errori.
   */
  public boolean containsAbsencesErrors() {
    return !absenceErrorsSuperMap.keySet().isEmpty();
  }

  /**
   * Verifica se ci sono errori relativamente ad un'assenza specifica.
   */
  public boolean containAbsenceErrors(Absence absence) {
    return absenceErrorsSuperMap.get(absence) != null;
  }

  /**
   * Verifica se ci sono errori di un certo tipo relativamente ad un'assenza specifica.
   */
  public boolean containAbsenceError(Absence absence, AbsenceProblem absenceProblem) {
    return absenceError(absence, absenceProblem) != null;
  }

  /**
   * Il problema per quell'assenza.
   *
   * @param absence        assenza
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
    return absenceErrors.get(absenceProblem);
  }

  /**
   * I problemi per quell'assenza.
   *
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
   *
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
   *
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
   *
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
   *
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
   *
   * @param errorsBoxes boxes
   * @param absence     assenza
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
      Map<AbsenceProblem, AbsenceError> absenceWarnings = 
          errorsBox.absenceWarningsSuperMap.get(absence);
      if (absenceWarnings != null) {
        for (AbsenceError absenceError : absenceWarnings.values()) {
          allProblems.add(absenceError.absenceProblem);
        }
      }
    }
    return allProblems;
  }


}
