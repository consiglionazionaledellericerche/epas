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

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.InitializationGroup;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultGroup;
import org.joda.time.LocalDate;
import play.db.jpa.JPA;

/**
 * Scan delle assenze per applicare i vari controlli e fix.
 *
 * @author Alessandro Martelli
 */
@Slf4j
public class Scanner {
  
  public ServiceFactories serviceFactories;
  public AbsenceEngineUtility absenceEngineUtility;
  public PersonDayManager personDayManager;
  
  public Person person;
  public List<PersonChildren> orderedChildren;
  public List<Contract> fetchedContracts;
  public List<InitializationGroup> initializationGroups;
  
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
  
  /**
   * Constructor scanner.
   *
   * @param person persona
   * @param scanFrom scanFrom
   * @param absencesToScan le assenze da scannerizzare
   * @param orderedChildren la lista dei figli ordinati per data di nascita
   * @param fetchedContracts i contratti
   * @param serviceFactories injection
   * @param absenceEngineUtility injection
   * @param personDayManager injection
   */
  public Scanner(Person person, LocalDate scanFrom, List<Absence> absencesToScan,
      List<PersonChildren> orderedChildren, List<Contract> fetchedContracts, 
      List<InitializationGroup> initializationGroups,
      ServiceFactories serviceFactories, AbsenceEngineUtility absenceEngineUtility, 
      PersonDayManager personDayManager) {
    this.person = person;
    this.scanFrom = scanFrom;
    this.absencesToScan = absencesToScan;
    this.orderedChildren = orderedChildren;
    this.fetchedContracts = fetchedContracts;
    this.initializationGroups = initializationGroups;
    this.serviceFactories = serviceFactories;
    this.absenceEngineUtility = absenceEngineUtility;
    this.personDayManager = personDayManager;
  }


  /**
   * Metodo di scan.
   */
  public void scan() {
    
    //mappa di utilità
    Map<LocalDate, Set<Absence>> absencesToScanMap = absenceEngineUtility
        .mapAbsences(this.absencesToScan, null);
    
    // analisi dei requisiti generici
    for (Absence absence : this.absencesToScan) {
      this.genericErrors = serviceFactories
          .genericConstraints(genericErrors, person, absence, absencesToScanMap);
    }
    
    // analisi dei requisiti all'interno di ogni gruppo
    Iterator<Absence> iterator = this.absencesToScan.iterator();
    this.configureNextGroupToScan(iterator);
    while (this.nextGroupToScan != null) {
     
      log.debug("Inizio lo scan del prossimo gruppo {}", this.nextGroupToScan.getDescription());
      
      //TODO: FIXME: quando sarà migrata anche la parte dei riposi, togliere questa eccezione.
      // Oppure taggare quelli che non devono partecipare allo scan, per rendere l'algoritmo
      // generico.
      if (this.nextGroupToScan.getPattern().equals(GroupAbsenceTypePattern.compensatoryRestCnr) 
          || this.nextGroupToScan.getName().equals(DefaultGroup.RIDUCE_FERIE_CNR.name())) {
        //prossimo gruppo
        this.configureNextGroupToScan(iterator);
        continue;
      }
      
      PeriodChain periodChain = serviceFactories.buildPeriodChain(person, this.nextGroupToScan, 
          this.currentAbsence.getAbsenceDate(), Lists.newArrayList(), null, 
          orderedChildren, fetchedContracts, initializationGroups);
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

  /**
   * Persiste gli errori riscontrati. Cancella quelli risolti.
   */
  private void persistScannerTroubles() {
    
    List<ErrorsBox> allErrorsScanned = allErrorsScanned();
    for (Absence absence : this.absencesToScan) {
      List<AbsenceTrouble> toDeleteTroubles = Lists.newArrayList();     //problemi da aggiungere
      List<AbsenceTrouble> toAddTroubles = Lists.newArrayList();        //problemi da rimuovere
      Set<AbsenceProblem> remainingProblems = ErrorsBox
          .allAbsenceProblems(allErrorsScanned, absence);
      //decidere quelli da cancellare
      //   per ogni vecchio absenceTroule verifico se non è presente in remaining
      for (AbsenceTrouble absenceTrouble : absence.getTroubles()) {
        if (!remainingProblems.contains(absenceTrouble.getTrouble())) {
          toDeleteTroubles.add(absenceTrouble);
        }
      }
      //decidere quelli da aggiungere
      //   per ogni remaining verifico se non è presente in vecchi absencetrouble
      for (AbsenceProblem remainingProblem : remainingProblems) {
        boolean toAdd = true;
        for (AbsenceTrouble absenceTrouble : absence.getTroubles()) {
          if (absenceTrouble.getTrouble().equals(remainingProblem)) {
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
        if (toDelete.isPersistent()) { //FIXME Issue #324
          toDelete.refresh();
          log.info("Rimuovo problem {} {}", absence.toString(), toDelete.getTrouble());
          toDelete.delete();  
        }
        
      }
      for (AbsenceTrouble toAdd : toAddTroubles) {
        if (Absence.findById(toAdd.getAbsence().getId()) == null) {
          // FIXME l'assenza di questo trouble è stata cancellata (probabilmente dall'algoritmo
          // che sistema i completamenti) pertanto non risulta più da persistere. 
          // Gestire questo caso all'origine.
          continue;
        }
        log.info("Aggiungo problem {} {}", absence.toString(), toAdd.getTrouble());
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
   *
   * @return gruppo
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
   *
   * @param iterator iteratore sulla lista di assenze da scannerizzare.
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
      if (absencePeriod.isCompromisedComplation()) {
        continue;
      }
      
      for (DayInPeriod dayInPeriod : absencePeriod.daysInPeriod.values()) {
        
        //eliminare i rimpiazzamenti sbagliati
        List<Absence> existentWrongReplacing = dayInPeriod.existentWrongReplacing();
        List<Absence> toDelete = Lists.newArrayList();
        for (Absence wrongReplacing : existentWrongReplacing) {
          toDelete.add(wrongReplacing);
          log.info("Rimosso il rimpiazzamento errato {}", wrongReplacing.toString());
        }
        for (Absence absence : toDelete) {
          dayInPeriod.getExistentReplacings().remove(absence);
          absence.delete();
        }
        
        //creare il rimpiazzamento corretto
        if (dayInPeriod.isReplacingMissing()) {
          //TODO: per la testabilità conviene disaccoppiare. 
          // Questo metodo crea le assenze da aggiungere ed un metodo che le aggiunge ai personDays
          PersonDay personDay = personDayManager
              .getOrCreateAndPersistPersonDay(person, dayInPeriod.getDate());
          
          Absence replacingAbsence = new Absence();
          replacingAbsence.setAbsenceType(dayInPeriod.getCorrectReplacing());
          replacingAbsence.date = dayInPeriod.getDate();
          replacingAbsence.setPersonDay(personDay);
          //justified type nothin (deve essere permitted per il tipo)
          for (JustifiedType justifiedType : replacingAbsence.getAbsenceType()
              .getJustifiedTypesPermitted()) {
            if (justifiedType.getName() == JustifiedTypeName.nothing) {
              replacingAbsence.setJustifiedType(justifiedType);
              break;
            }
          }
          Verify.verifyNotNull(replacingAbsence.getJustifiedType());
          personDay.getAbsences().add(replacingAbsence);
          replacingAbsence.save();
          personDay.refresh();
          personDay.save();
          JPA.em().flush();
          dayInPeriod.getExistentReplacings().add(replacingAbsence);
          log.info("Aggiunto il rimpiazzamento corretto {}", replacingAbsence.toString());
        }
      }
    }
  }

}