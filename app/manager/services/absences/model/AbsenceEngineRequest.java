package manager.services.absences.model;

import com.google.common.collect.Lists;

import manager.services.absences.web.AbsenceRequestForm;

import models.absences.Absence;
import models.absences.GroupAbsenceType;

import org.joda.time.LocalDate;

import java.util.List;

public class AbsenceEngineRequest {

  public AbsenceRequestForm absenceRequestForm;
  public LocalDate from;
  public LocalDate to;
  public LocalDate currentDate;
  public GroupAbsenceType group;
  public List<Absence> requestInserts = Lists.newArrayList();

  public AbsenceEngine nextDate(AbsenceEngine absenceEngine) {

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
    return absenceEngine;
  }


}
