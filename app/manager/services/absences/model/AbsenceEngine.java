package manager.services.absences.model;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.AbsencesReport;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Person;
import models.PersonChildren;
import models.absences.Absence;
import models.absences.AmountType;
import models.absences.GroupAbsenceType;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

public class AbsenceEngine {
  
  //Dependencies Injected
  public final AbsenceComponentDao absenceComponentDao;
  public final PersonChildrenDao personChildrenDao;

  public Person person;
  public List<PersonChildren> childrenAsc = null; //all children   
  public DateInterval childInterval;

  // Richiesta inserimento  
  public AbsenceEngineRequest request;

  // Richiesta scan
  public AbsenceEngineScan scan;
  
  // AbsencePeriod Chain
  public PeriodChain periodChain;

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
      AbsenceEngineUtility absenceEngineUtility,
      Person person, GroupAbsenceType groupAbsenceType, 
      LocalDate from, LocalDate to) {
    this.absenceComponentDao = absenceComponentDao;
    this.personChildrenDao = personChildrenDao;
    this.person = person;
    
    AbsenceEngineRequest request = new AbsenceEngineRequest();
    request.group = groupAbsenceType;
    request.from = from;
    request.to = to;
    this.request = request;
    
    this.report = new AbsencesReport();
  }
  
  /**
   * Costruttore per richiesta di scan.
   * @param absenceComponentDao
   * @param personChildrenDao
   * @param absenceEngineUtility
   * @param person
   * @param scanFrom
   * @param absencesToScan
   */
  public AbsenceEngine(AbsenceComponentDao absenceComponentDao, PersonChildrenDao personChildrenDao,
      AbsenceEngineUtility absenceEngineUtility,
      Person person, LocalDate scanFrom, List<Absence> absencesToScan) {
    this.absenceComponentDao = absenceComponentDao;
    this.personChildrenDao = personChildrenDao;
    this.person = person;
    
    this.scan = new AbsenceEngineScan();
    this.scan.absenceEngineUtility = absenceEngineUtility;
    this.scan.scanFrom = scanFrom;
    this.scan.scanAbsences = absencesToScan;
    for (Absence absence : this.scan.scanAbsences) {
      Set<GroupAbsenceType> groupsToScan = absenceEngineUtility.involvedGroup(absence.absenceType); 
      this.scan.absencesGroupsToScan.put(absence, groupsToScan);
    }
    
    this.report = new AbsencesReport();
  }
  
  public boolean isRequestEngine() {
    return request != null;
  }
  
  public boolean isScanEngine() {
    return request == null;
  }
  
  public GroupAbsenceType engineGroup() {
    if (isRequestEngine()) {
      return this.request.group;
    } 
    if (isScanEngine()) {
      return this.scan.currentGroup;
    }
    return null;
  }
  
  public List<PersonChildren> orderedChildren() {
    if (this.childrenAsc == null) {
      this.childrenAsc = personChildrenDao.getAllPersonChildren(this.person);
    }
    return this.childrenAsc;
  }
  
  public int workingTime(LocalDate date) {
    for (Contract contract : this.periodChain.contracts) {
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

}