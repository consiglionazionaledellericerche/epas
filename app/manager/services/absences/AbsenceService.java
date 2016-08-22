package manager.services.absences;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import manager.services.absences.model.AbsenceEngine;
import manager.services.absences.model.AbsencePeriod.EnhancedAbsence;
import manager.services.absences.web.AbsenceRequestForm;
import manager.services.absences.web.AbsenceRequestFormFactory;

import models.Person;
import models.absences.Absence;
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
  private final AbsenceEngineCore absenceEngineCore;
  private final AbsenceEngineUtility absenceEngineUtility;
  
  @Inject
  public AbsenceService(AbsenceRequestFormFactory absenceRequestFormFactory, 
      AbsenceEngineCore absenceEngineCore,
      AbsenceEngineUtility absenceEngineUtility) {
    this.absenceRequestFormFactory = absenceRequestFormFactory;
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


  public enum AbsenceRequestType {
    insert, cancel; // insertSimulated, cancelSimulated;
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
  public AbsenceEngine insert(Person person, GroupAbsenceType groupAbsenceType, LocalDate from,
      LocalDate to, AbsenceRequestType absenceTypeRequest, AbsenceType absenceType, 
      JustifiedType justifiedType, Integer hours, Integer minutes) {

    Preconditions.checkArgument(absenceTypeRequest.equals(absenceTypeRequest.insert));
    
    AbsenceEngine absenceEngine = absenceEngineCore
        .buildInsertAbsenceEngine(person, groupAbsenceType, from, to);
    if (absenceEngine.report.containsProblems()) {
      return absenceEngine;
    }
  
    Integer specifiedMinutes = absenceEngineUtility.getMinutes(hours, minutes);
    
    while (absenceEngine.currentDate() != null) {

      //Preparare l'assenza da inserire
      Absence absence = new Absence();
      absence.date = absenceEngine.currentDate();
      absence.absenceType = absenceType;
      absence.justifiedType = justifiedType;
      if (specifiedMinutes != null) {
        absence.justifiedMinutes = specifiedMinutes;
      }
  
      absenceEngineCore.absenceInsertRequest(absenceEngine, AbsenceRequestType.insert, 
          EnhancedAbsence.builder().absence(absence)
          .requestedJustifiedType(justifiedType)
          .absenceTypeToInfer(absenceType == null)
          .build());
      
      if (absenceEngine.report.containsProblems()) {
        return absenceEngine;
      }
      
      absenceEngineCore.configureNextInsertDate(absenceEngine);
    }
    
    return absenceEngine;
  }
  

  /**
   * Esegue lo scanning delle assenze della persona a partire dalla data
   * passata come parametro per verificarne la correttezza.
   * Gli errori riscontrati vengono persistiti all'assenza.
   * 
   * Microservices
   * Questo metodo dovrebbe avere una person dove sono fetchate tutte le 
   * informazioni per i calcoli non mantenute del componente assenze:
   * 
   * I Contratti / Tempi a lavoro / Piani ferie
   * I Figli
   * Le Altre Tutele
   * 
   * @param person
   * @param from
   */
  public AbsenceEngine scanner(Person person, LocalDate from) {
    
    AbsenceEngine absenceEngine = absenceEngineCore.scannerAbsenceEngine(person, from);
    
    return absenceEngine;
    
  }
  
  public AbsenceEngine residual(Person person, GroupAbsenceType groupAbsenceType, LocalDate date) {
    
    if (date == null) {
      date = LocalDate.now();
    }
    if (groupAbsenceType == null) {
      //TODO: scegliere il primo GroupAbsenceType che ha senso inizializzare
      groupAbsenceType = (GroupAbsenceType)GroupAbsenceType.findAll().get(0);
    }
    
    AbsenceEngine absenceEngine = absenceEngineCore.residualAbsenceEngine(person, groupAbsenceType, date);
    return absenceEngine;
  }
  
  
}
