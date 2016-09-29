package manager.services.absences.model;

import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateUtility;

import manager.services.absences.AbsenceEngineUtility;
import manager.services.absences.AbsenceService.AbsenceRequestType;
import manager.services.absences.model.AbsencesReport.ReportRequestProblem;
import manager.services.absences.web.AbsenceRequestForm;

import models.absences.Absence;
import models.absences.AbsenceTrouble.RequestProblem;
import models.absences.GroupAbsenceType;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;

import org.joda.time.LocalDate;

import java.util.List;

public class AbsenceEngineRequest {

  public AbsenceEngineUtility absenceEngineUtility;
  public AbsenceEngine absenceEngine;

  
  public AbsenceRequestForm absenceRequestForm;
  public LocalDate from;
  public LocalDate to;
  public LocalDate currentDate;
  public GroupAbsenceType group;
  
  public List<Absence> requestInserts = Lists.newArrayList(); //TODO: spostare nel report
  
  /**
   * Configura il prossimo inserimento da effettuare.
   * @param absenceEngine
   * @return
   */
  public AbsenceEngine configureNextInsert() {
    
    AbsenceEngineRequest request = absenceEngine.request;
    //prima iterata
    if (request.currentDate == null) {
      request.currentDate = request.from;
    } else {
      //iterata successiva
      request.currentDate = request.currentDate.plusDays(1);
      if (request.to == null 
          || request.currentDate.isAfter(request.to)) {
        request.currentDate = null;
        return absenceEngine;
      }
    }
    if (absenceEngine.request.currentDate == null) {
      return absenceEngine;
    }
    
    absenceEngine.buildPeriodChain(absenceEngine.request.group, 
        absenceEngine.request.currentDate);
    return absenceEngine;
  }
  
  
  /**  
   * 
   * @param absenceEngine
   * @param absenceRequestType
   * @param absenceType
   * @param justifiedType
   * @param specifiedMinutes
   * @return
   */
  public AbsenceEngine doInsertRequest(AbsenceEngine absenceEngine, 
      AbsenceRequestType absenceRequestType, Absence absence, JustifiedType requestedJustifiedType,
      boolean absenceTypeToInfer) {
    
    // Provo a inserire l'assenza in ogni periodo della catena...
    for (AbsencePeriod currentAbsencePeriod : absenceEngine.periodChain.periods) {
     
      //Se la data non appartiene al period vado al successivo
      if (!DateUtility.isDateIntoInterval(absenceEngine.request.currentDate, 
          currentAbsencePeriod.periodInterval())) {
        continue;
      }
      
      //Inferire il tipo se necessario
      if (absenceTypeToInfer) {
        absence = absenceEngineUtility.inferAbsenceType(
            currentAbsencePeriod, absence, requestedJustifiedType);
      }

      if (absenceTypeToInfer && absence.absenceType == null) {
        continue;
      }

      //Vincoli
      if (absenceEngineUtility.requestConstraints(absenceEngine, currentAbsencePeriod, absence)
          .report.containsProblems()) {
        return absenceEngine;          
      }
      if (absenceEngineUtility.genericConstraints(absenceEngine, absence, absenceEngine.periodChain.allCodeAbsencesAsc)
          .report.containsProblems()) {
        return absenceEngine;          
      }
      if (absenceEngineUtility.groupConstraints(absenceEngine, currentAbsencePeriod, absence)
          .report.containsProblems()) {
        return absenceEngine;     
      }

      //Inserimento e riepilogo inserimento
      if (performInsert(absenceEngine, currentAbsencePeriod, absenceRequestType, 
          absence).periodChain.success) {
        return absenceEngine;
      }
      
      //Al periodo successivo se dovevevo inferire il tipo resetto
      if (absenceTypeToInfer) {
        absence.absenceType = null;
      }
    }

    //Esco e non sono mai riuscito a inferire il tipo.
    if (absenceTypeToInfer && absence.absenceType == null) {
      absenceEngine.report.addRequestProblem(ReportRequestProblem.builder()
          .requestProblem(RequestProblem.CantInferAbsenceCode)
          .date(absenceEngine.request.currentDate)
          .build());
    }
    
    return absenceEngine;
  }
  
  /**
   * 
   * @param absencePeriod
   * @param absenceType
   * @param date
   * @return
   */
  private AbsenceEngine performInsert(AbsenceEngine absenceEngine, 
      AbsencePeriod absencePeriod, AbsenceRequestType absenceRequestType,
      Absence absence) {
    
    //simple grouping
    if (absencePeriod.groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.simpleGrouping)) {
      DayStatus insertDayStatus = DayStatus.builder()
          .date(absence.getAbsenceDate()).absencePeriod(absencePeriod).build();
      insertDayStatus.takenAbsences = Lists.newArrayList(TakenAbsence.builder().absence(absence).build());
      absenceEngine.report.addInsertDayStatus(insertDayStatus);
      absenceEngine.request.requestInserts.add(absence);
      absenceEngine.periodChain.success = true;
      return absenceEngine;
    }
    
    DayStatus insertDayStatus = DayStatus.builder()
        .date(absence.getAbsenceDate()).absencePeriod(absencePeriod).build();
    absenceEngine.report.addInsertDayStatus(insertDayStatus);
    
    //Takable component
    if (absencePeriod.isTakable()) {
      
      int takenAmount = absenceEngineUtility.absenceJustifiedAmount(absenceEngine, 
          absence, absencePeriod.takeAmountType);

      insertDayStatus.takenAbsences = Lists.newArrayList(TakenAbsence.builder()
          .absence(absence)
          .consumedTakable(takenAmount)
          .amountTypeTakable(absencePeriod.takeAmountType)
          .residualBeforeTakable(absencePeriod.getRemainingAmount())
          .workingTime(absenceEngine.workingTime(absenceEngine.request.currentDate))
          .build());


      //Aggiungo l'assenza alle strutture dati per l'eventuale iterata successiva.
      absencePeriod.addAbsenceTaken(absence, takenAmount);
      absenceEngine.request.requestInserts.add(absence);
    }

    //Complation replacing
    if (absencePeriod.isComplation()) {
      if (absencePeriod.complationCodes.contains(absence.absenceType)) {

        //aggiungere l'assenza ai completamenti     
        absencePeriod.complationAbsencesByDay.put(absence.getAbsenceDate(), absence);

        //aggiungere il rimpiazzamento se c'è
        absencePeriod.computeReplacingStatusInPeriod();
        DayStatus dayStatus = absencePeriod.getDayStatus(absence.getAbsenceDate());

        insertDayStatus.setComplationAbsence(absence);
        insertDayStatus.setConsumedComplation(dayStatus.getConsumedComplation());
        insertDayStatus.setResidualBeforeComplation(dayStatus.getResidualBeforeComplation());
        insertDayStatus.setResidualAfterComplation(dayStatus.getResidualAfterComplation());
        insertDayStatus.setAmountTypeComplation(absencePeriod.complationAmountType);
        
        if (dayStatus.missingReplacing()) {
          Absence replacingAbsence = new Absence();
          replacingAbsence.absenceType = dayStatus.getCorrectReplacing();
          replacingAbsence.date = dayStatus.getDate();
          replacingAbsence.justifiedType = absenceEngine.absenceComponentDao
              .getOrBuildJustifiedType(JustifiedTypeName.nothing);

          //todo cercarlo fra quelli permit e se non c'è nothing errore
          insertDayStatus.setExistentReplacing(replacingAbsence);
          absenceEngine.request.requestInserts.add(absence);
        }
      }

    }

    //success
    absenceEngine.periodChain.success = true;

    return absenceEngine; 
  }


}
