package manager.services.absences.model;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.services.absences.AbsencesReport;
import manager.services.absences.model.AbsencePeriod.EnhancedAbsence;
import manager.services.absences.web.AbsenceRequestForm;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Person;
import models.PersonChildren;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;

import org.joda.time.LocalDate;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

public class AbsenceEngine {

  //Dependencies Injected
  private final AbsenceComponentDao absenceComponentDao;
  private final PersonChildrenDao personChildrenDao;
  
  // --------------------------------------------------------------------------------------
  // Richiesta inserimento
  public AbsenceRequestForm absenceRequestForm;
  public LocalDate requestFrom;
  public LocalDate requestTo;
  public LocalDate requestCurrentDate;
  public GroupAbsenceType requestGroup;
  public Person requestPerson;
  public DateInterval childInterval;                      
  private List<PersonChildren> requestChildrenAsc = null; //all children         
  private List<Absence> requestOldAbsences = null;        //old requestFrom -> requestTo
  public List<Absence> requestInserts = Lists.newArrayList();

  // --------------------------------------------------------------------------------------
  // Richiesta scan
  public LocalDate scanFrom;
  public List<EnhancedAbsence> scanEnhancedAbsences;
  public Iterator<EnhancedAbsence> scanAbsencesIterator;
  public EnhancedAbsence scanCurrentAbsence;
  public GroupAbsenceType scanCurrentGroup;
  
  // --------------------------------------------------------------------------------------
  // AbsencePeriod Chain
  public AbsencePeriod periodChain;
  public List<EnhancedAbsence> periodChainAbsencesAsc = null;
  private List<Contract> periodChainContracts = null;          //to reset nextDate
  private LocalDate periodChainFrom = null;                    //to reset nextDate
  private LocalDate periodChainTo = null;                      //to reset nextDate
  public boolean periodChainSuccess = false;                   //to reset nextDate

  // --------------------------------------------------------------------------------------
  // Risultato richiesta
  public AbsencesReport report;


  
  //Boh
  //private InitializationGroup initializationGroup = null;
  
  /**
   * Costrutture per richiesta di inserimento.
   * @param absenceComponentDao
   * @param personChildrenDao
   * @param person
   * @param groupAbsenceType
   * @param from
   * @param to
   */
  public AbsenceEngine(AbsenceComponentDao absenceComponentDao, PersonChildrenDao personChildrenDao, 
      Person person, GroupAbsenceType groupAbsenceType, 
      LocalDate from, LocalDate to) {
    this.absenceComponentDao = absenceComponentDao;
    this.personChildrenDao = personChildrenDao;
    this.requestPerson = person;
    this.requestGroup = groupAbsenceType;
    this.requestFrom = from;
    this.requestTo = to;
    this.report = new AbsencesReport();
  }
  
  public AbsenceEngine(AbsenceComponentDao absenceComponentDao, PersonChildrenDao personChildrenDao, 
      Person person, LocalDate scanFrom, List<EnhancedAbsence> enhancedAbsencesToScan) {
    this.absenceComponentDao = absenceComponentDao;
    this.personChildrenDao = personChildrenDao;
    this.requestPerson = person;
    this.scanFrom = scanFrom;
    this.scanEnhancedAbsences = enhancedAbsencesToScan;
    this.scanAbsencesIterator = this.scanEnhancedAbsences.iterator();
    this.report = new AbsencesReport();
  }
  
  public boolean isRequestEngine() {
    Verify.verify(!(this.requestCurrentDate != null && this.scanFrom != null));
    return this.requestCurrentDate != null;
  }
  
  public boolean isScanEngine() {
    Verify.verify(!(this.requestCurrentDate != null && this.scanFrom != null));
    return this.scanFrom != null;
  }
  
  public GroupAbsenceType engineGroup() {
    if (isRequestEngine()) {
      return this.requestGroup;
    } 
    if (isScanEngine()) {
      return this.scanCurrentGroup;
    }
    return null;
  }
  
  public LocalDate currentDate() {
    if (isRequestEngine()) {
      return this.requestCurrentDate;
    } 
    if (isScanEngine()) {
      return this.scanCurrentAbsence.getAbsence().getAbsenceDate();
    }
    return null;
  }
  
  public List<PersonChildren> orderedChildren() {
    if (this.requestChildrenAsc == null) {
      this.requestChildrenAsc = 
          personChildrenDao.getAllPersonChildren(this.requestPerson);
    }
    return this.requestChildrenAsc;
  }
  
  public int workingTime(LocalDate date) {
    for (Contract contract : this.periodChainContracts) {
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
  
  public List<Absence> requestIntervalAbsences() {
    if (this.requestOldAbsences == null) {
      if (this.isRequestEngine()) {
        this.requestOldAbsences = this.absenceComponentDao.orderedAbsences(this.requestPerson, 
            this.requestFrom, this.requestTo, Lists.newArrayList());
      }
      if (this.isScanEngine()) {
        //TODO: prelevarle dal scanEnhancedAbsences
        this.requestOldAbsences = this.absenceComponentDao.orderedAbsences(this.requestPerson, 
            this.scanFrom, null, Lists.newArrayList());
      }
    }
    return this.requestOldAbsences;
  }
  
  public boolean isConfiguredForNextScan() {
    return this.scanCurrentGroup != null;
  }
  
  public void resetPeriodChainSupportStructures() {
    this.periodChainSuccess = false;
    this.periodChainFrom = this.periodChain.from;
    this.periodChainTo = this.periodChain.to;
    AbsencePeriod currentAbsencePeriod = this.periodChain;
    while (currentAbsencePeriod.nextAbsencePeriod != null) {
      if (currentAbsencePeriod.nextAbsencePeriod.from.isBefore(this.periodChainFrom)) {
        this.periodChainFrom = currentAbsencePeriod.nextAbsencePeriod.from;
      }
      if (currentAbsencePeriod.nextAbsencePeriod.to.isAfter(this.periodChainTo)) {
        this.periodChainTo = currentAbsencePeriod.nextAbsencePeriod.to;
      }
      currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
    }
    this.periodChainContracts = Lists.newArrayList();
    for (Contract contract : requestPerson.contracts) {
      if (DateUtility.intervalIntersection(
          contract.periodInterval(), new DateInterval(periodChainFrom, periodChainTo)) != null) {
        this.periodChainContracts.add(contract);
      }
    }
    this.periodChainAbsencesAsc = null;
  }


  
  /**
   * Le assenze coinvolte nella catena.
   * @return
   */
  public List<EnhancedAbsence> periodChainAbsencesAsc() {
    
    if (isRequestEngine()) {
      setRequestPeriodChainAbsencesAsc();
    } 
    if (isScanEngine()) {
      setScanPeriodChainAbsencesAsc();
    }
    return this.periodChainAbsencesAsc;
  }

  /**
   * Le assenze della catena in caso di scan. Vengono processate una sola volta, 
   * la dimensione è statica ma si devono usare quelle già caricate per potervi
   * memorizzare l'effettuato scan, ed eventualmente inserire quelleappartenenti al period 
   * precedente a scanFrom.
   * @return
   */
  private void setScanPeriodChainAbsencesAsc() {
    
    this.periodChainAbsencesAsc = Lists.newArrayList();

    Set<AbsenceType> absenceTypes = periodChainInvolvedCodes();
    if (absenceTypes.isEmpty()) {
      return;
    }

    //le assenze del periodo precedenti allo scan le scarico 
    List<Absence> previousAbsences = Lists.newArrayList();
    if (this.periodChainFrom == null || this.periodChainFrom.isBefore(this.scanFrom)) {
      previousAbsences = this.absenceComponentDao.orderedAbsences(this.requestPerson, 
          this.periodChainFrom, this.scanFrom.minusDays(1), Lists.newArrayList(absenceTypes));
      for (Absence absence : previousAbsences) {
        this.periodChainAbsencesAsc.add(EnhancedAbsence.builder().absence(absence).build());
      }
    }

    //le assenze del periodo appartenenti allo scan le recupero
    for (EnhancedAbsence enhancedAbsence: this.scanEnhancedAbsences) {
      if (absenceTypes.contains(enhancedAbsence.getAbsence().getAbsenceType())) {
        this.periodChainAbsencesAsc.add(enhancedAbsence);
      }
    }

  }

  
  
  /**
   * Le assenze della catena in caso di richiesta inserimento (sono dinamiche
   * in quanto contengono anche le assenza inserite fino all'iterata 
   * precedente).
   * @return
   */
  private void setRequestPeriodChainAbsencesAsc() {
    
    //TODO: quando avremo il mantenimento dei period riattivare 
    //la funzionalità lazy
    //    if (this.periodChainAbsencesAsc != null) {
    //      return this.periodChainAbsencesAsc;
    //    }

    //I tipi coinvolti...
    Set<AbsenceType> absenceTypes = periodChainInvolvedCodes();
    if (absenceTypes.isEmpty()) {
      this.periodChainAbsencesAsc = Lists.newArrayList();
      return;
    }

    //Le assenze precedenti e quelle precedentemente inserite
    List<Absence> periodAbsences = this.absenceComponentDao.orderedAbsences(this.requestPerson, 
        this.periodChainFrom, this.periodChainTo, Lists.newArrayList(absenceTypes));
    periodAbsences.addAll(this.requestInserts);
    
    //Le ordino tutte per data...
    SortedMap<LocalDate, List<EnhancedAbsence>> sortedEnhancedMap = Maps.newTreeMap();
    for (Absence absence : periodAbsences) {
      Verify.verifyNotNull(absence.justifiedType == null );     //rimuovere..
      List<EnhancedAbsence> enhancedAbsences = sortedEnhancedMap.get(absence.getAbsenceDate());
      if (enhancedAbsences == null) {
        enhancedAbsences = Lists.newArrayList();
        sortedEnhancedMap.put(absence.getAbsenceDate(), enhancedAbsences);
      }
      enhancedAbsences.add(EnhancedAbsence.builder().absence(absence).build());
    }
    
    //Popolo la lista definitiva
    this.periodChainAbsencesAsc = Lists.newArrayList();
    for (List<EnhancedAbsence> enhancedAbsences : sortedEnhancedMap.values()) {
      this.periodChainAbsencesAsc.addAll(enhancedAbsences);
    }
    
    return;

  }
  
  /**
   * I codici coinvolti nella periodChain
   * @return
   */
  private Set<AbsenceType> periodChainInvolvedCodes() {

    Set<AbsenceType> absenceTypes = Sets.newHashSet();
    AbsencePeriod currentAbsencePeriod = this.periodChain;
    while (currentAbsencePeriod != null) {
      if (currentAbsencePeriod.takableComponent.isPresent()) {
        absenceTypes.addAll(currentAbsencePeriod.takableComponent.get().takenCodes);
        //absenceTypes.addAll(currentAbsencePeriod.takableComponent.get().takableCodes);
      }
      if (currentAbsencePeriod.complationComponent.isPresent()) {
        absenceTypes.addAll(currentAbsencePeriod.complationComponent.get()
            .replacingCodesDesc.values());
        absenceTypes.addAll(currentAbsencePeriod.complationComponent.get().complationCodes);
      }
      currentAbsencePeriod = currentAbsencePeriod.nextAbsencePeriod;
    }

    return absenceTypes;

  }
  

    
}