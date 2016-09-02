package manager.services.absences;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.services.absences.InsertResultItem.Operation;
import manager.services.absences.model.AbsenceEngine;
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
@Slf4j
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
  public AbsencesReport insert(Person person, GroupAbsenceType groupAbsenceType, LocalDate from,
      LocalDate to, AbsenceRequestType absenceTypeRequest, AbsenceType absenceType, 
      JustifiedType justifiedType, Integer hours, Integer minutes) {

    Preconditions.checkArgument(absenceTypeRequest.equals(absenceTypeRequest.insert));
    
    AbsenceEngine absenceEngine = absenceEngineCore
        .buildInsertAbsenceEngine(person, groupAbsenceType, from, to);
    if (absenceEngine.report.containsProblems()) {
      return absenceEngine.report;
    }
  
    Integer specifiedMinutes = absenceEngineUtility.getMinutes(hours, minutes);
    
    while (absenceEngine.request.currentDate != null) {

      //Preparare l'assenza da inserire
      Absence absence = new Absence();
      absence.date = absenceEngine.request.currentDate;
      absence.absenceType = absenceType;
      absence.justifiedType = justifiedType;
      if (specifiedMinutes != null) {
        absence.justifiedMinutes = specifiedMinutes;
      }
      boolean absenceTypeToInfer = absenceType == null;

      //Esecuzione
      absenceEngineCore.absenceInsertRequest(absenceEngine, AbsenceRequestType.insert, absence, 
          justifiedType, absenceTypeToInfer);  
      
      //Errori
      if (absenceEngine.report.containsProblems()) {
        return absenceEngine.report;
      }
     
      //Configura la prossima data
      absenceEngineCore.configureNext(absenceEngine);
    }
    
    return absenceEngine.report;
  }
  
  public AbsencesReport forceInsert(Person person, GroupAbsenceType groupAbsenceType, LocalDate from,
      LocalDate to, AbsenceRequestType absenceTypeRequest, AbsenceType absenceType, 
      JustifiedType justifiedType, Integer hours, Integer minutes) {
    
    Preconditions.checkArgument(absenceTypeRequest.equals(absenceTypeRequest.insert));
    Preconditions.checkArgument(absenceType != null);   //il tipo quando forzo non si inferisce
    Preconditions.checkArgument(absenceType.justifiedTypesPermitted.contains(justifiedType));
    
    Integer specifiedMinutes = absenceEngineUtility.getMinutes(hours, minutes);
    LocalDate currentDate = from;
    if (to == null) {
      to = from;
    }
    
    AbsencesReport report = new AbsencesReport();
    
    while (!currentDate.isAfter(to)) {
      
      //Preparare l'assenza da inserire
      Absence absence = new Absence();
      absence.date = currentDate;
      absence.absenceType = absenceType;
      absence.justifiedType = justifiedType;
      if (specifiedMinutes != null) {
        absence.justifiedMinutes = specifiedMinutes;
      }
      
      InsertResultItem absenceResultItem = InsertResultItem.builder()
          .absence(absence)
          .absenceType(absence.getAbsenceType())
          .operation(Operation.insert)
          .consumedResidualAmount(Lists.newArrayList())
          .date(currentDate).build();
      report.addInsertResultItem(absenceResultItem);
      
      currentDate = currentDate.plusDays(1);
    }
    
    return report;
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
    
    log.debug("");
    log.debug("");
    log.debug("");
    log.debug("");
    log.debug("Lanciata procedura scan assenze person={}, from={}", person.fullName(), from);
    
    AbsenceEngine absenceEngine = absenceEngineCore.scannerAbsenceEngine(person, from);
    
    log.debug("");
    log.debug("");
    log.debug("");
    log.debug("");
    
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
