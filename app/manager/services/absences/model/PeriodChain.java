package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import manager.services.absences.errors.CriticalError;
import manager.services.absences.errors.ErrorsBox;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;

import org.joda.time.LocalDate;
import org.testng.collections.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PeriodChain {
  
  public Person person;
  public GroupAbsenceType groupAbsenceType;
  public LocalDate date;
  public LocalDate from = null;                                     
  public LocalDate to = null;                                       
  public List<AbsencePeriod> periods = Lists.newArrayList();
  
  //Assenze coinvolte nella catena (compresi i nuovi inserimenti)
  public Set<Absence> involvedAbsences = Sets.newHashSet() ; //le assenza assegnate ad un periodo
  public Set<Absence> orphanAbsences = Sets.newHashSet();    //le assenze non assegnate ad alcun periodo 
                                                             // perchè sono uscito in modo critico mentre le analizzavo

  //Tutte le assenze coinvolte nella catena - anche quelle di codici diversi (compresi i nuovi inserimenti) 
  public Map<LocalDate, Set<Absence>> allInvolvedAbsences = Maps.newHashMap();    
  
  //Assenza da inserire
  public AbsencePeriod successPeriodInsert;
  public ErrorsBox errorsBox = new ErrorsBox();              //per adesso contiene solo il caso di ins. assenza senza figlio.
  
  //Errori
  private List<ErrorsBox> periodsErrorsBoxes = null;         //errori dei periodi.. lazy quando ho i periodi

  /**
   * 
   * @param person
   * @param groupAbsenceType
   * @param date
   */
  public PeriodChain(Person person, GroupAbsenceType groupAbsenceType, LocalDate date) {
    this.person = person;
    this.date = date;
    this.groupAbsenceType = groupAbsenceType;
  }
  
  public String getChainDescription() {
    if (periods.get(0).groupAbsenceType.chainDescription != null) {
      return periods.get(0).groupAbsenceType.chainDescription;
    }
    return periods.get(0).groupAbsenceType.description;
  }
  
  public AbsencePeriod firstPeriod() {
    if (periods.isEmpty()) {
      return null;
    }
    return periods.get(0);
  }
  
  /**
   * I codici coinvolti nella periodChain
   * @return
   */
  public Set<AbsenceType> periodChainInvolvedCodes() {

    Set<AbsenceType> absenceTypes = Sets.newHashSet();
    for (AbsencePeriod absencePeriod : this.periods) {
      if (absencePeriod.isTakable()) {
        absenceTypes.addAll(absencePeriod.takenCodes);
        //absenceTypes.addAll(currentAbsesncePeriod.takableComponent.get().takableCodes);
      }
      if (absencePeriod.isComplation()) {
        absenceTypes.addAll(absencePeriod.replacingCodesDesc.values());
        absenceTypes.addAll(absencePeriod.complationCodes);
      }
    }
    return absenceTypes;
  }
  
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
    return periods.isEmpty() && groupAbsenceType.periodType.isChildPeriod();
  }
  
  public boolean containsCriticalErrors() {
    return ErrorsBox.containsCriticalErrors(this.allErrorsInPeriods());
  }
  
  public Set<CriticalError> criticalErrors() {
    return ErrorsBox.allCriticalErrors(allErrorsInPeriods());
  }

 
}
