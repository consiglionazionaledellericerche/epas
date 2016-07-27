package manager.services.absences.model;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.services.absences.model.AbsencePeriod.AbsenceEngineProblem;
import manager.services.absences.model.AbsencePeriod.SuperAbsence;
import manager.services.absences.web.AbsenceRequestForm;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Person;
import models.PersonChildren;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.InitializationGroup;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

public class AbsenceEngine {

  //Dependencies Injected
  private final AbsenceComponentDao absenceComponentDao;
  private final PersonChildrenDao personChildrenDao;
  
  // Form della riechiesta
  public AbsenceRequestForm absenceRequestForm;
  
  // Dati della richiesta
  public LocalDate date;
  public GroupAbsenceType groupAbsenceType;
  public Person person;
  
  // Errori
  public Optional<AbsenceEngineProblem> absenceEngineProblem = Optional.absent();

  // Risultato richiesta
  public boolean success = false;
  public List<ResponseItem> responseItems = Lists.newArrayList();
  
  // Strutture ausiliare lazy
  
  private LocalDate from = null;
  private LocalDate to = null;
  private List<Contract> contracts = null;
  private List<PersonChildren> orderedChildren = null;
  private List<SuperAbsence> orderedSuperAbsences = null;
  private InitializationGroup initializationGroup = null;
  
  // AbsencePeriod
  public AbsencePeriod absencePeriod;
  
  public AbsenceEngine(AbsenceComponentDao absenceComponentDao, PersonChildrenDao personChildrenDao, 
      Person person, GroupAbsenceType groupAbsenceType, 
      LocalDate date) {
    this.absenceComponentDao = absenceComponentDao;
    this.personChildrenDao = personChildrenDao;
    this.person = person;
    this.groupAbsenceType = groupAbsenceType;
    this.date = date;
  }
  
  public List<PersonChildren> getOrderedChildren() {
    if (this.orderedChildren == null) {
      this.orderedChildren = 
          personChildrenDao.getAllPersonChildren(this.person);
    }
    return this.orderedChildren;
  }
  
  public int workingTime(LocalDate date) {
    if (this.contracts == null) {
      this.contracts = Lists.newArrayList();
      for (Contract contract : person.contracts) {
        if (DateUtility.intervalIntersection(
            contract.periodInterval(), new DateInterval(getFrom(), getTo())) != null) {
          this.contracts.add(contract);
        }
      }
    }
    for (Contract contract : this.contracts) {
      for (ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {
        if (DateUtility.isDateIntoInterval(date, cwtt.periodInterval())) {
          if (cwtt.workingTimeType.workingTimeTypeDays.get(date.getDayOfWeek() - 1).holiday) {
            return 0;
          }
          return cwtt.workingTimeType.workingTimeTypeDays.get(date.getDayOfWeek() - 1)
              .workingTime;
        }
      }
    }
    return 0;
  }
  
  public LocalDate getFrom() {
    if (this.from == null) {
      buildInterval();
    }
    return this.from;
  }
  
  public LocalDate getTo() {
    if (this.to == null) {
      buildInterval();
    }
    return this.to;
  }
  
  private void buildInterval() {
 
    this.from = this.absencePeriod.from;
    this.to = this.absencePeriod.to;
    AbsencePeriod currentAbsencePeriod = this.absencePeriod;
    while (currentAbsencePeriod.nextAbsencePeriod != null) {
      if (currentAbsencePeriod.nextAbsencePeriod.from.isBefore(this.from)) {
        this.from = currentAbsencePeriod.nextAbsencePeriod.from;
      }
      if (currentAbsencePeriod.nextAbsencePeriod.to.isAfter(this.to)) {
        this.to = currentAbsencePeriod.nextAbsencePeriod.to;
      }
      currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
    }
  }
  
  public List<SuperAbsence> getOrderedAbsences() {
    if (this.orderedSuperAbsences == null) {
      // 1) Prendere tutti i codici (anche quelli ricorsivi)
      Set<AbsenceType> absenceTypes = Sets.newHashSet();
      AbsencePeriod currentAbsencePeriod = this.absencePeriod;
      while (currentAbsencePeriod != null) {
        if (currentAbsencePeriod.takableComponent.isPresent()) {
          absenceTypes.addAll(currentAbsencePeriod.takableComponent.get().takenCodes);
          //absenceTypes.addAll(currentAbsencePeriod.takableComponent.get().takableCodes);
        }
        if (currentAbsencePeriod.complationComponent.isPresent()) {
          absenceTypes.addAll(currentAbsencePeriod.complationComponent.get().replacingCodes);
          absenceTypes.addAll(currentAbsencePeriod.complationComponent.get().complationCodes);
        }
        currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
      }

      // 2) Scaricare le assenze
      List<Absence> orderedAbsences = this.absenceComponentDao.orderedAbsences(this.person, 
          this.getFrom(), this.getTo(), Lists.newArrayList(absenceTypes));

      this.orderedSuperAbsences = Lists.newArrayList();
      for (Absence absence : orderedAbsences) {
        Verify.verifyNotNull(absence.justifiedType == null );
        this.orderedSuperAbsences.add(SuperAbsence.builder().absence(absence).build());
      }
    }
    
    return this.orderedSuperAbsences;
  }
  
}