package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

import manager.services.absences.AbsenceEngineUtility;

import models.absences.Absence;
import models.absences.AbsenceTrouble;
import models.absences.GroupAbsenceType;

import org.joda.time.LocalDate;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class AbsenceEngineScan {
  
  public AbsenceEngineUtility absenceEngineUtility;
  public AbsenceEngine absenceEngine;
  
  public LocalDate scanFrom;
  public List<Absence> absencesToScan;
  public Absence currentAbsence;
  public GroupAbsenceType currentGroup;
  public Map<Absence, Set<GroupAbsenceType>> absencesGroupsToScan = Maps.newHashMap();

  /// Metodi
  
  private void setGroupScanned(Absence absence, GroupAbsenceType groupAbsenceType) {
    Set<GroupAbsenceType> absenceGroupsToScan = absencesGroupsToScan.get(absence);
    if (absenceGroupsToScan == null) {
      return;
    }
    absenceGroupsToScan.remove(groupAbsenceType);
  }
  
  public void scan() {
    
    // analisi dei requisiti generici (risultati in absenceEngine.report)
    for (Absence absence : this.absencesToScan) {
      absenceEngineUtility.genericConstraints(absenceEngine, absence, absencesToScan);
      log.debug("L'assenza data={}, codice={} è stata aggiunta a quelle da analizzare", 
          absence.getAbsenceDate(), absence.getAbsenceType().code);
    }
    
    // analisi dei requisiti all'interno di ogni gruppo (risultati in absenceEngine.report)
    Iterator<Absence> iterator = absenceEngine.scan.absencesToScan.iterator();
    this.configureNextGroupToScan(iterator);
    while (this.currentGroup != null) {
      log.debug("Inizio lo scan del prossimo gruppo {}", absenceEngine.scan.currentGroup.description);
      absenceEngine.buildPeriodChain(absenceEngine.scan.currentGroup, 
          absenceEngine.scan.currentAbsence.getAbsenceDate());
      if (absenceEngine.report.containsCriticalProblems()) {
        //ex. manca il figlio
        absenceEngine.scan.setGroupScanned(absenceEngine.scan.currentAbsence, 
            absenceEngine.scan.currentGroup);
      } else {
        //taggare come scansionate le assenze coinvolte nella periodChain
        for (Absence absence : absenceEngine.periodChain.absencesAsc) {
          absenceEngine.scan.setGroupScanned(absence, absenceEngine.scan.currentGroup);
        }
      }
      this.configureNextGroupToScan(iterator);
    }
  }

  public void persistScannerTroubles(AbsenceEngine absenceEngine) {

    for (Absence absence : absenceEngine.scan.absencesToScan) {

      List<AbsenceTrouble> toDeleteTroubles = Lists.newArrayList();     //problemi da aggiungere
      List<AbsenceTrouble> toAddTroubles = Lists.newArrayList();        //problemi da rimuovere

      List<AbsenceTrouble> remainingTroubles = 
          absenceEngine.report.absencesRemainingTroubles.get(absence);
      if (remainingTroubles == null) {
        remainingTroubles = Lists.newArrayList();
      }

      //decidere quelli da cancellare
      //   per ogni vecchio absenceTroule verifico se non è presente in remaining
      for (AbsenceTrouble absenceTrouble : absence.troubles) {
        if (!remainingTroubles.contains(absenceTrouble.trouble)) {
          toDeleteTroubles.add(absenceTrouble);
        }
      }

      //decidere quelli da aggiungere
      //   per ogni remaining verifico se non è presente in vecchi absencetrouble
      for (AbsenceTrouble reportAbsenceTrouble : remainingTroubles) {
        boolean toAdd = true;
        for (AbsenceTrouble absenceTrouble : absence.troubles) {
          if (absenceTrouble.trouble.equals(reportAbsenceTrouble.trouble)) {
            toAdd = false;
          }
        }
        if (toAdd) {
          toAddTroubles.add(AbsenceTrouble.builder()
              .absence(absence)
              .trouble(reportAbsenceTrouble.trouble)
              .build());
        }
      }

      //eseguire
      for (AbsenceTrouble toDelete : toDeleteTroubles) {
        toDelete.delete();
      }
      for (AbsenceTrouble toAdd : toAddTroubles) {
        toAdd.save();
      }
    }
  }
  
  /**
   * Il prossimo gruppo da analizzare per la currentAbsence (se c'è).
   * @return
   */
  private GroupAbsenceType currentAbsenceNextGroup() {
    if (this.currentAbsence == null) {
      return null;
    }
    Set<GroupAbsenceType> groupsToScan = this.absencesGroupsToScan.get(this.currentAbsence);
    if (groupsToScan.isEmpty()) {
      return null;
    }
    GroupAbsenceType group = groupsToScan.iterator().next();
    setGroupScanned(this.currentAbsence, group);
    return group;
  }
  
  /**
   * Configura il prossimo gruppo da analizzare (se esiste).
   * @param absenceEngine
   * @return
   */
  private void configureNextGroupToScan(Iterator<Absence> iterator) {
    
    // stessa assenza prossimo gruppo
    this.currentGroup = currentAbsenceNextGroup();
    if (this.currentGroup != null) {
      return;
    }
    
    // prossima assenza primo gruppo
    while (iterator.hasNext()) {
      this.currentAbsence = iterator.next();
      this.currentGroup = currentAbsenceNextGroup();
      if (this.currentGroup != null) {
        return;
      }
    }
    return;
  }

}