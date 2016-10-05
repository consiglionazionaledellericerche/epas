package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.PersonChildrenDao;
import dao.absences.AbsenceComponentDao;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.errors.AbsenceTypeError;
import manager.services.absences.errors.CriticalError;

import models.Contract;
import models.ContractWorkingTimeType;
import models.Person;
import models.PersonChildren;
import models.absences.Absence;
import models.absences.AbsenceTrouble;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceTrouble.AbsenceTypeProblem;
import models.absences.AbsenceTrouble.RequestProblem;
import models.absences.AbsenceType;
import models.absences.ComplationAbsenceBehaviour;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.PeriodType;
import models.absences.TakableAbsenceBehaviour;
import models.absences.TakableAbsenceBehaviour.TakeCountBehaviour;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

public class AbsenceEngine {
  
  //Dependencies Injected
  public AbsenceComponentDao absenceComponentDao;
  public PersonChildrenDao personChildrenDao;
  public AbsenceEngineUtility absenceEngineUtility;

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
  

  
  public AbsenceEngine(Person person, AbsenceComponentDao absenceComponentDao,
      AbsenceEngineUtility absenceEngineUtility, PersonChildrenDao personChildrenDao) {
    this.person = person;
    this.absenceComponentDao = absenceComponentDao;
    this.absenceEngineUtility = absenceEngineUtility;
    this.personChildrenDao = personChildrenDao;
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
      return this.scan.nextGroupToScan;
    }
    return null;
  }
  
  public List<PersonChildren> orderedChildren() {
    if (this.childrenAsc == null) {
      this.childrenAsc = personChildrenDao.getAllPersonChildren(this.person);
    }
    return this.childrenAsc;
  }
  
 
  

  
  

}