package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

public class PeriodChain {
  
  public Person person;
  public LocalDate date;
  public LocalDate from = null;                                     
  public LocalDate to = null;                                       
  public List<AbsencePeriod> periods = Lists.newArrayList();        

  //Supporto
  public List<Absence> absencesAsc = null;         //Tutte le assenze dei tipi coinvolti nel gruppo nel periodo                          
  public List<Absence> allCodeAbsencesAsc = null;  //Tutte le assenze presenti nel periodo.                   
  
  //In caso di inserimenti
  public boolean success = false;
  // ...le assenze inserite fino a quel punto
  
  public PeriodChain(Person person, LocalDate date) {
    this.person = person;
    this.date = date;
    this.success = false;
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
  
 
}
