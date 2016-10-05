package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.errors.ErrorsBox;

import models.Contract;
import models.Person;
import models.PersonChildren;
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
  
  public PeriodChainFactory periodChainFactory;
  public AbsenceEngineUtility absenceEngineUtility;
  
  public Person person;
  public List<PersonChildren> orderedChildren;
  public List<Contract> fetchedContracts;
  
  //Puntatori scan
  public LocalDate scanFrom;
  public List<Absence> absencesToScan;
  public Absence currentAbsence;
  public GroupAbsenceType nextGroupToScan;
  public Map<Absence, Set<GroupAbsenceType>> absencesGroupsToScan = Maps.newHashMap();
  
  //Report scan
  ErrorsBox genericErrors = new ErrorsBox(); 
  List<PeriodChain> chainScanned = Lists.newArrayList();
  
  public AbsenceEngineScan(Person person, LocalDate scanFrom, 
      List<PersonChildren> orderedChildren, List<Contract> fetchedContracts, 
      PeriodChainFactory periodChainFactory, AbsenceEngineUtility absenceEngineUtility) {
   this.person = person;
   this.scanFrom = scanFrom;
   this.orderedChildren = orderedChildren;
   this.fetchedContracts = fetchedContracts;
   this.periodChainFactory = periodChainFactory;
   this.absenceEngineUtility = absenceEngineUtility;
  }

  private void setGroupScanned(Absence absence, GroupAbsenceType groupAbsenceType) {
    Set<GroupAbsenceType> absenceGroupsToScan = absencesGroupsToScan.get(absence);
    if (absenceGroupsToScan == null) {
      return;
    }
    absenceGroupsToScan.remove(groupAbsenceType);
  }
  
  public void scan() {
    
    // analisi dei requisiti generici
    for (Absence absence : this.absencesToScan) {
      this.genericErrors = absenceEngineUtility
          .genericConstraints(genericErrors, person, absence, absencesToScan);
    }
    
    // analisi dei requisiti all'interno di ogni gruppo (risultati in absenceEngine.report)
    Iterator<Absence> iterator = this.absencesToScan.iterator();
    this.configureNextGroupToScan(iterator);
    while (this.nextGroupToScan != null) {
     
      log.debug("Inizio lo scan del prossimo gruppo {}", this.nextGroupToScan.description);
      
      periodChainFactory.buildPeriodChain(person, this.nextGroupToScan, 
          this.currentAbsence.getAbsenceDate(), orderedChildren, fetchedContracts);
      if (absenceEngine.report.containsCriticalProblems()) {
        //ex. manca il figlio
        this.setGroupScanned(this.currentAbsence, this.nextGroupToScan);
      } else {
        //taggare come scansionate le assenze coinvolte nella periodChain
        for (Absence absence : absenceEngine.periodChain.absencesAsc) {
          this.setGroupScanned(absence, this.nextGroupToScan);
        }
      }
      this.configureNextGroupToScan(iterator);
    }
  }

  public void persistScannerTroubles() {

    for (Absence absence : this.absencesToScan) {

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
    this.nextGroupToScan = currentAbsenceNextGroup();
    if (this.nextGroupToScan != null) {
      return;
    }
    
    // prossima assenza primo gruppo
    while (iterator.hasNext()) {
      this.currentAbsence = iterator.next();
      this.nextGroupToScan = currentAbsenceNextGroup();
      if (this.nextGroupToScan != null) {
        return;
      }
    }
    return;
  }

}