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
  
  public List<AbsencePeriod> periods = Lists.newArrayList();
  
  public List<Absence> absencesAsc = null;                          //to reset nextDate
  public List<Absence> allCodeAbsencesAsc = null;                          //to reset nextDate
  public Set<Absence> absenceAlreadyAssigned = Sets.newHashSet();   //to reset nextDate
  
  public List<Contract> contracts = null;           //to reset nextDate
  public LocalDate from = null;                     //to reset nextDate
  public LocalDate to = null;                       //to reset nextDate
  public boolean success = false;                   //to reset nextDate
  
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
      if (absencePeriod.takableComponent.isPresent()) {
        absenceTypes.addAll(absencePeriod.takableComponent.get().takenCodes);
        //absenceTypes.addAll(currentAbsencePeriod.takableComponent.get().takableCodes);
      }
      if (absencePeriod.complationComponent.isPresent()) {
        absenceTypes.addAll(absencePeriod.complationComponent.get()
            .replacingCodesDesc.values());
        absenceTypes.addAll(absencePeriod.complationComponent.get().complationCodes);
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
  public void fetchPeriodChainAbsencesAsc(AbsenceEngine absenceEngine) {
    
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

    //Le assenze precedenti e quelle precedentemente inserite
    List<Absence> periodAbsences = absenceEngine.absenceComponentDao.orderedAbsences(
        absenceEngine.person, this.from, this.to, Lists.newArrayList(absenceTypes));
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
    for (List<Absence> enhancedAbsences : sortedAbsenceMap.values()) {
      this.absencesAsc.addAll(enhancedAbsences);
    }
    
    //Popolo la lista con tutte le assenze coinvolte nel periodo.
    this.allCodeAbsencesAsc = absenceEngine.absenceComponentDao.orderedAbsences(
        absenceEngine.person, this.from, this.to, Lists.newArrayList());
    if (absenceEngine.request != null) {
      periodAbsences.addAll(absenceEngine.request.requestInserts);
    }
    
    return;

  }
  
  /**
   * Le assenze della catena in caso di scan. Vengono processate una sola volta, 
   * la dimensione è statica ma si devono usare quelle già caricate per potervi
   * memorizzare l'effettuato scan, ed eventualmente inserire quelleappartenenti al period 
   * precedente a scanFrom.
   * @return
   */
  public List<Absence> getScanPeriodChainAbsencesAsc(AbsenceEngine absenceEngine) {

    this.absencesAsc = Lists.newArrayList();

    Set<AbsenceType> absenceTypes = periodChainInvolvedCodes();
    if (absenceTypes.isEmpty()) {
      return this.absencesAsc;
    }

    //le assenze del periodo precedenti allo scan le scarico 
    List<Absence> previousAbsences = Lists.newArrayList();
    if (this.from == null || this.from.isBefore(absenceEngine.scan.scanFrom)) {
      previousAbsences = absenceEngine.absenceComponentDao.orderedAbsences(absenceEngine.person, 
          this.from, absenceEngine.scan.scanFrom.minusDays(1), Lists.newArrayList(absenceTypes));
      for (Absence absence : previousAbsences) {
        this.absencesAsc.add(absence);
      }
    }

    //le assenze del periodo appartenenti allo scan le recupero
    for (Absence absence: absenceEngine.scan.scanAbsences) {
      if (absenceTypes.contains(absence.getAbsenceType())) {
        this.absencesAsc.add(absence);
      }
    }
    
    return this.absencesAsc;
  }
  
  public void resetSupportStructures(AbsenceEngine absenceEngine) {
    
    
  }
}
