package manager.services.absences.model;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import models.Contract;
import models.absences.Absence;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;

public class PeriodChain {
  
  public final AbsenceEngine absenceEngine;
  
  public List<AbsencePeriod> periods = Lists.newArrayList();        
  
  //Tutte le assenze dei tipi coinvolti nel gruppo nel periodo
  public List<Absence> absencesAsc = null;                          //to reset nextDate
  
  //Tutte le assenze presenti nel periodo.
  public List<Absence> allCodeAbsencesAsc = null;                   //to reset nextDate
  
  public List<Contract> contracts = null;                           //to reset nextDate
  public LocalDate from = null;                                     //to reset nextDate
  public LocalDate to = null;                                       //to reset nextDate
  public boolean success = false;                                   //to reset nextDate

  public PeriodChain(AbsenceEngine absenceEngine) {
    this.absenceEngine = absenceEngine;
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
  
  /**
   * Le assenze della catena in caso di richiesta inserimento (sono dinamiche
   * in quanto contengono anche le assenza inserite fino all'iterata 
   * precedente).
   * @return
   */
  public void fetchPeriodChainAbsencesAsc() {
    
    //TODO: quando avremo il mantenimento dei period riattivare 
    //la funzionalità lazy
    //    if (this.absencesAsc != null) {
    //      return this.periodChainAbsencesAsc;
    //    }

    //I tipi coinvolti...
    Set<AbsenceType> absenceTypes = this.periodChainInvolvedCodes();
    if (absenceTypes.isEmpty()) {
      this.absencesAsc = Lists.newArrayList();
      this.allCodeAbsencesAsc = Lists.newArrayList();
      return;
    }

    //Le assenze preesistenti 
    List<Absence> periodAbsences = absenceEngine.absenceComponentDao.orderedAbsences(
        absenceEngine.person, this.from, this.to, Lists.newArrayList(absenceTypes));
    // e quelle precedentemente inserite
    if (absenceEngine.request != null) {
      periodAbsences.addAll(absenceEngine.request.requestInserts);
    }
    
    //Le ordino tutte per data...
    SortedMap<LocalDate, List<Absence>> sortedAbsenceMap = Maps.newTreeMap();
    for (Absence absence : periodAbsences) {
      Verify.verifyNotNull(absence.justifiedType == null );     //rimuovere..
      List<Absence> absences = sortedAbsenceMap.get(absence.getAbsenceDate());
      if (absences == null) {
        absences = Lists.newArrayList();
        sortedAbsenceMap.put(absence.getAbsenceDate(), absences);
      }
      absences.add(absence);
    }
    
    //Popolo la lista definitiva
    this.absencesAsc = Lists.newArrayList();
    for (List<Absence> absences : sortedAbsenceMap.values()) {
      this.absencesAsc.addAll(absences);
    }
    
    //Popolo la lista con tutte le assenze coinvolte nel periodo.
    this.allCodeAbsencesAsc = absenceEngine.absenceComponentDao.orderedAbsences(
        absenceEngine.person, this.from, this.to, Lists.newArrayList());
    if (absenceEngine.request != null) {
      periodAbsences.addAll(absenceEngine.request.requestInserts);
    }
    
    return;

  }
}
