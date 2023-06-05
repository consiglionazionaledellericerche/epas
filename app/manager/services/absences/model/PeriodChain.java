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

package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.ToString;
import manager.services.absences.errors.CriticalError;
import manager.services.absences.errors.ErrorsBox;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import org.joda.time.LocalDate;
import org.testng.collections.Maps;

/**
 * Rappresenta una catena di assenze.
 *
 * @author Alessandro Martelli
 *
 */
@ToString
public class PeriodChain {
  
  public Person person;
  public GroupAbsenceType groupAbsenceType;
  public LocalDate date;
  public LocalDate from = null;                                     
  public LocalDate to = null;                                       
  public List<AbsencePeriod> periods = Lists.newArrayList();
  
  //Tutte le assenze coinvolte nella catena 
  // - anche quelle di codici diversi (compresi i nuovi inserimenti) 
  public Map<LocalDate, Set<Absence>> allInvolvedAbsences = Maps.newHashMap();    
  //Le assenze coinvolte nella catena relative al gruppo
  // - di tutti i period, anche non assegnate, comprese le assenze inserite precedentemente
  public List<Absence> involvedAbsencesInGroup = Lists.newArrayList();
  
  public List<Absence> previousInserts = Lists.newArrayList();

  //Assenze coinvolte nella catena (compresi i nuovi inserimenti) assegnate ad un periodo
  public Set<Absence> involvedAbsences = Sets.newHashSet(); 
  //le assenze non assegnate ad alcun periodo perchè sono uscito in modo critico causa errori
  public Set<Absence> orphanAbsences = Sets.newHashSet();  
  
  //Assenza da inserire
  public AbsencePeriod successPeriodInsert;
  //per adesso contiene solo il caso di ins. assenza senza figlio.
  public ErrorsBox errorsBox = new ErrorsBox();               
  
  //Errori
  private List<ErrorsBox> periodsErrorsBoxes = null; //errori dei periodi.. lazy quando ho i periodi
  
  //VacationsSupport 
  //Permette di recuperare gratis i 3 gruppi ferie passate, correnti, permessi
  public List<List<AbsencePeriod>> vacationSupportList = Lists.newArrayList();

  /**
   * Constructor PeriodChain.
   *
   * @param person persona
   * @param groupAbsenceType gruppo
   * @param date data
   */
  public PeriodChain(Person person, GroupAbsenceType groupAbsenceType, LocalDate date) {
    this.person = person;
    this.date = date;
    this.groupAbsenceType = groupAbsenceType;
  }
  
  /**
   * La descrizione della catena.
   *
   * @return string
   */
  public String getChainDescription() {
    return periods.get(0).groupAbsenceType.computeChainDescription();
  }
  
  /**
   * Il primo periodo della catena.
   *
   * @return absencePeriod
   */
  public AbsencePeriod firstPeriod() {
    if (periods.isEmpty()) {
      return null;
    }
    return periods.get(0);
  }
  
  /**
   * L'ultimo periodo della catena.
   *
   * @return absencePeriod
   */
  public AbsencePeriod lastPeriod() {
    if (periods.isEmpty()) {
      return null;
    }
    return periods.get(periods.size() - 1);
  }
  
  /**
   * I codici coinvolti nella periodChain.
   *
   * @return set
   */
  public Set<AbsenceType> periodChainInvolvedCodes() {

    Set<AbsenceType> absenceTypes = Sets.newHashSet();
    for (AbsencePeriod absencePeriod : this.periods) {
      if (absencePeriod.isTakable()) {
        absenceTypes.addAll(absencePeriod.takenCodes);
        absenceTypes.addAll(absencePeriod.takableCodes);
      }
      if (absencePeriod.isComplation()) {
        for (List<AbsenceType> replacings : absencePeriod.replacingCodesDesc.values()) {
          absenceTypes.addAll(replacings);
        }
        absenceTypes.addAll(absencePeriod.complationCodes);
      }
    }
    return absenceTypes;
  }
  
  /**
   * Le assenze coinvolte nella period chain (taken e replacing) 
   * post inizializzazione nella periodChain. Utile per individuare assenze rilevanti epas
   * non in attestati (o altro master).
   */
  public List<Absence> relevantAbsences(boolean onlyOnCertificate) {
    List<Absence> absences = Lists.newArrayList();
    for (AbsencePeriod period : this.periods) {
      for (DayInPeriod day : period.daysInPeriod.values()) {
        if (period.initialization != null && !day.getDate()
            .isAfter(period.initialization.getDate())) {
          continue;
        }
        for (TakenAbsence takenAbsence : day.getTakenAbsences()) { //sia complation che non
          if (onlyOnCertificate && takenAbsence.absence.getAbsenceType().isInternalUse()) {
            continue;
          }
          absences.add(takenAbsence.absence);
        }
        for (Absence absence : day.getExistentReplacings()) {
          if (onlyOnCertificate && absence.getAbsenceType().isInternalUse()) {
            continue;
          }
          absences.add(absence);
        }
      }
    }
    return absences;
  }
  
  /**
   * Tutti gli errori verificatisi nella catena.
   *
   * @return list
   */
  public List<ErrorsBox> allErrorsInPeriods() {
    if (this.periodsErrorsBoxes != null) {
      return this.periodsErrorsBoxes;
    }
    this.periodsErrorsBoxes = Lists.newArrayList(this.errorsBox);
    for (AbsencePeriod absencePeriod : this.periods) {
      this.periodsErrorsBoxes.add(absencePeriod.errorsBox);
    }
    return this.periodsErrorsBoxes;
  }
  
  public boolean childIsMissing() {
    return periods.isEmpty() && groupAbsenceType.getPeriodType().isChildPeriod();
  }
  
  public boolean containsCriticalErrors() {
    return ErrorsBox.boxesContainsCriticalErrors(this.allErrorsInPeriods());
  }
  
  public Set<CriticalError> criticalErrors() {
    return ErrorsBox.allCriticalErrors(allErrorsInPeriods());
  }

 
}
