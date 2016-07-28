package manager.services.absences;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import manager.services.absences.model.AbsenceEngine;
import manager.services.absences.model.AbsenceEngineFactory;
import manager.services.absences.model.AbsencePeriod.ProblemType;
import manager.services.absences.web.AbsenceRequestForm;
import manager.services.absences.web.AbsenceRequestFormFactory;

import models.Person;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;

import org.joda.time.LocalDate;

/**
 * Interfaccia epas per il componente assenze.
 * le richieste via form.
 * @author alessandro
 *
 */
public class AbsenceService {

  private final AbsenceRequestFormFactory absenceRequestFormFactory;
  private final AbsenceEngineFactory absenceEngineFactory;
  private final AbsenceEngineCore absenceEngineCore;
  private final AbsenceEngineUtility absenceEngineUtility;
  
  @Inject
  public AbsenceService(AbsenceRequestFormFactory absenceRequestFormFactory, 
      AbsenceEngineFactory absenceEngineFactory, AbsenceEngineCore absenceEngineCore,
      AbsenceEngineUtility absenceEngineUtility) {
    this.absenceRequestFormFactory = absenceRequestFormFactory;
    this.absenceEngineFactory = absenceEngineFactory;
    this.absenceEngineCore = absenceEngineCore;
    this.absenceEngineUtility = absenceEngineUtility;
  }
  
  /**
   * Genera la form di inserimento assenza.
   * @param person
   * @param from
   * @param to
   * @param groupAbsenceType
   * @return
   */
  public AbsenceRequestForm buildInsertForm(Person person, LocalDate from, LocalDate to, 
      GroupAbsenceType groupAbsenceType) {
    
     //TODO: Preconditions se groupAbsenceType presente verificare che permesso per la persona
    
    return absenceRequestFormFactory.buildAbsenceRequestForm(person, from, to, 
        groupAbsenceType, null, null, null);
  }
  
  /**
   * Riconfigura la form di inserimento assenza con i nuovi parametri forniti.
   * @param person
   * @param from
   * @param to
   * @param groupAbsenceType
   * @param absenceType
   * @param justifiedType
   * @param specifiedMinutes
   * @return
   */
  public AbsenceRequestForm configureInsertForm(Person person, LocalDate from, LocalDate to, 
      GroupAbsenceType groupAbsenceType, AbsenceType absenceType, 
      JustifiedType justifiedType, Integer hours, Integer minutes) {
    
    return absenceRequestFormFactory.buildAbsenceRequestForm(person, from, to, 
        groupAbsenceType, absenceType, justifiedType, 
        absenceEngineUtility.getMinutes(hours, minutes));
  }


  /**
   * Esegue la richiesta
   * @param person
   * @param groupAbsenceType
   * @param from
   * @param to
   * @param insert
   * @param absenceTypeRequest
   * @param justifiedType
   * @param specifiedMinutes
   * @return
   */
  public AbsenceEngine doRequest(Person person, GroupAbsenceType groupAbsenceType, LocalDate from,
      LocalDate to, AbsenceRequestType absenceTypeRequest, AbsenceType absenceType, 
      JustifiedType justifiedType, Integer hours, Integer minutes) {

    AbsenceEngine absenceEngine = absenceEngineFactory
        .buildAbsenceEngineInstance(person, groupAbsenceType, from);
    
    if (absenceEngine.absenceEngineProblem.isPresent()) {
      return absenceEngine;
    }
    
    if (!absenceTypeRequest.equals(absenceTypeRequest.insert)) {
      absenceEngine.setProblem(ProblemType.unsupportedOperation);
      return absenceEngine;
    }
    
    absenceEngineCore.doRequest(absenceEngine, AbsenceRequestType.insert, absenceType, 
        justifiedType, Optional.fromNullable(absenceEngineUtility.getMinutes(hours, minutes)));
    
    return absenceEngine;
  }
  
  public enum AbsenceRequestType {
    insert, cancel; // insertSimulated, cancelSimulated;
  }
  
  
}
