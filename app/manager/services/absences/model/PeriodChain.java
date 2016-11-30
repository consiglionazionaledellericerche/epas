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
  
  //Tutte le assenze coinvolte nella catena 
  // - anche quelle di codici diversi (compresi i nuovi inserimenti) 
  public Map<LocalDate, Set<Absence>> allInvolvedAbsences = Maps.newHashMap();    
  //Le assenze coinvolte nella catena relative al gruppo
  // - di tutti i period, anche non assegnate, comprese le assenze inserite precedentemente
  public List<Absence> involvedAbsencesInGroup = Lists.newArrayList();
  
  public List<Absence> previousInserts = Lists.newArrayList();

  //Assenze coinvolte nella catena (compresi i nuovi inserimenti) assegnate ad un periodo
  public Set<Absence> involvedAbsences = Sets.newHashSet() ; 
  //le assenze non assegnate ad alcun periodo perchè sono uscito in modo critico causa errori
  public Set<Absence> orphanAbsences = Sets.newHashSet();  
  
  //Assenza da inserire
  public AbsencePeriod successPeriodInsert;
  //per adesso contiene solo il caso di ins. assenza senza figlio.
  public ErrorsBox errorsBox = new ErrorsBox();               
  
  //Errori
  private List<ErrorsBox> periodsErrorsBoxes = null; //errori dei periodi.. lazy quando ho i periodi

  /**
   * Constructor PeriodChain.
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
   * @return string
   */
  public String getChainDescription() {
    return periods.get(0).groupAbsenceType.getChainDescription();
  }
  
  /**
   * Il primo periodo della catena.
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
   * @return set
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
  
  /**
   * Tutti gli errori verificatisi nella catena.
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
    return periods.isEmpty() && groupAbsenceType.periodType.isChildPeriod();
  }
  
  public boolean containsCriticalErrors() {
    return ErrorsBox.boxesContainsCriticalErrors(this.allErrorsInPeriods());
  }
  
  public Set<CriticalError> criticalErrors() {
    return ErrorsBox.allCriticalErrors(allErrorsInPeriods());
  }

 
}
