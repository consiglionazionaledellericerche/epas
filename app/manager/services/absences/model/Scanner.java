package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dao.absences.AbsenceComponentDao;

import lombok.extern.slf4j.Slf4j;

import manager.PersonDayManager;
import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.errors.ErrorsBox;

import models.Contract;
import models.Person;
import models.PersonChildren;
import models.PersonDay;
import models.absences.Absence;
import models.absences.AbsenceTrouble;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;

import org.joda.time.LocalDate;

import play.db.jpa.JPA;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class Scanner {
  
  public ServiceFactories serviceFactories;
  public AbsenceEngineUtility absenceEngineUtility;
  public PersonDayManager personDayManager;
  public AbsenceComponentDao absenceComponentDao;
  
  public Person person;
  public List<PersonChildren> orderedChildren;
  public List<Contract> fetchedContracts;
  
  //Puntatori scan
  public LocalDate scanFrom;
  public List<Absence> absencesToScan;
  public Absence currentAbsence;
  public GroupAbsenceType nextGroupToScan;
  public Map<Absence, Set<GroupAbsenceType>> absencesGroupsToScan = Maps.newHashMap();
  
  //Report Scan
  ErrorsBox genericErrors = new ErrorsBox(); 

  //PeriodChains
  List<PeriodChain> periodChainScanned = Lists.newArrayList();
  
  public Scanner(Person person, LocalDate scanFrom, List<Absence> absencesToScan,
      List<PersonChildren> orderedChildren, List<Contract> fetchedContracts, 
      ServiceFactories serviceFactories, AbsenceEngineUtility absenceEngineUtility, 
      PersonDayManager personDayManager, AbsenceComponentDao absenceComponentDao) {
   this.person = person;
   this.scanFrom = scanFrom;
   this.absencesToScan = absencesToScan;
   this.orderedChildren = orderedChildren;
   this.fetchedContracts = fetchedContracts;
   this.serviceFactories = serviceFactories;
   this.absenceEngineUtility = absenceEngineUtility;
   this.personDayManager = personDayManager;
   this.absenceComponentDao = absenceComponentDao;
  }


  
  public void scan() {
    
    //mappa di utilità
    Map<LocalDate, Set<Absence>> absencesToScanMap = absenceComponentDao
        .mapAbsences(this.absencesToScan, null);
    
    // analisi dei requisiti generici
    for (Absence absence : this.absencesToScan) {
      this.genericErrors = absenceEngineUtility
          .genericConstraints(genericErrors, person, absence, absencesToScanMap);
    }
    
    // analisi dei requisiti all'interno di ogni gruppo (risultati in absenceEngine.report)
    Iterator<Absence> iterator = this.absencesToScan.iterator();
    this.configureNextGroupToScan(iterator);
    while (this.nextGroupToScan != null) {
     
      log.debug("Inizio lo scan del prossimo gruppo {}", this.nextGroupToScan.description);
      
      PeriodChain periodChain = serviceFactories.buildPeriodChain(person, this.nextGroupToScan, 
          this.currentAbsence.getAbsenceDate(), Lists.newArrayList(), null, 
          orderedChildren, fetchedContracts);
      this.periodChainScanned.add(periodChain);
      
      //caso eccezionale non esiste figlio
      if (periodChain.childIsMissing()) {
        this.genericErrors.addAbsenceError(this.currentAbsence, AbsenceProblem.NoChildExist);
        this.configureNextGroupToScan(iterator);
        continue;
      }
      
      //fix dei completamenti
      fixReplacing(periodChain);

      //taggare come scansionate le assenze coinvolte nella periodChain
      for (Absence absence : periodChain.involvedAbsences) {
        this.setGroupScanned(absence, this.nextGroupToScan);
      }
      
      //prossimo gruppo
      this.configureNextGroupToScan(iterator);
    }
    
    //persistenza degli errori riscontrati
    persistScannerTroubles();
  }

  private void persistScannerTroubles() {
    
    List<ErrorsBox> allErrorsScanned = allErrorsScanned();
    for (Absence absence : this.absencesToScan) {
      List<AbsenceTrouble> toDeleteTroubles = Lists.newArrayList();     //problemi da aggiungere
      List<AbsenceTrouble> toAddTroubles = Lists.newArrayList();        //problemi da rimuovere
      Set<AbsenceProblem> remainingProblems = ErrorsBox.allAbsenceProblems(allErrorsScanned, absence);
      //decidere quelli da cancellare
      //   per ogni vecchio absenceTroule verifico se non è presente in remaining
      for (AbsenceTrouble absenceTrouble : absence.troubles) {
        if (!remainingProblems.contains(absenceTrouble.trouble)) {
          toDeleteTroubles.add(absenceTrouble);
        }
      }
      //decidere quelli da aggiungere
      //   per ogni remaining verifico se non è presente in vecchi absencetrouble
      for (AbsenceProblem remainingProblem : remainingProblems) {
        boolean toAdd = true;
        for (AbsenceTrouble absenceTrouble : absence.troubles) {
          if (absenceTrouble.trouble.equals(remainingProblem)) {
            toAdd = false;
          }
        }
        if (toAdd) {
          toAddTroubles.add(AbsenceTrouble.builder().absence(absence)
              .trouble(remainingProblem).build());
        }
      }
      //eseguire
      for (AbsenceTrouble toDelete : toDeleteTroubles) {
        log.info("Rimuovo problem {} {}", absence.toString(), toDelete.trouble);
        toDelete.delete();
      }
      for (AbsenceTrouble toAdd : toAddTroubles) {
        log.info("Aggiungo problem {} {}", absence.toString(), toAdd.trouble);
        toAdd.save();
      }
    }
  }
  
  private void setGroupScanned(Absence absence, GroupAbsenceType groupAbsenceType) {
    Set<GroupAbsenceType> absenceGroupsToScan = absencesGroupsToScan.get(absence);
    if (absenceGroupsToScan == null) {
      return;
    }
    absenceGroupsToScan.remove(groupAbsenceType);
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
  
  private List<ErrorsBox> allErrorsScanned() {
    List<ErrorsBox> allErrors = Lists.newArrayList();
    allErrors.add(genericErrors);
    for (PeriodChain periodChain : periodChainScanned) {
      allErrors.addAll(periodChain.allErrorsInPeriods());
    }
    return allErrors;
  }
  
  private void fixReplacing(PeriodChain periodChain) {
    
    if (periodChain.containsCriticalErrors()) {
      return;
    }
    
    for (AbsencePeriod absencePeriod : periodChain.periods) {
      if (absencePeriod.compromisedTwoComplation) {
        continue;
      }
      
      for (DayInPeriod dayInPeriod : absencePeriod.daysInPeriod.values()) {
        
        //eliminare i rimpiazzamenti sbagliati
        for (Absence wrongReplacing : dayInPeriod.existentWrongReplacing()) {
          wrongReplacing.delete();
          dayInPeriod.getExistentReplacings().remove(wrongReplacing);
          log.info("Rimosso il rimpiazzamento errato {}", wrongReplacing.toString());
        }
        
        //creare il rimpiazzamento corretto
        if (dayInPeriod.isReplacingMissing()) {
          PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(person, dayInPeriod.getDate());
          Absence replacingAbsence = new Absence();
          replacingAbsence.absenceType = dayInPeriod.getCorrectReplacing();
          replacingAbsence.date = dayInPeriod.getDate();
          replacingAbsence.personDay = personDay;
          replacingAbsence.justifiedType = absenceComponentDao
              .getOrBuildJustifiedType(JustifiedTypeName.nothing);
          personDay.absences.add(replacingAbsence);
          replacingAbsence.save(); 
          personDay.save();
          JPA.em().flush();
          dayInPeriod.getExistentReplacings().add(replacingAbsence);
          log.info("Aggiunto il rimpiazzamento corretto {}", replacingAbsence.toString());
        }
      }
    }
  }

}