package manager.services.absences;

import com.google.common.base.MoreObjects;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import manager.services.absences.AbsencesReport.ReportAbsenceProblem;

import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import java.util.List;

@Builder @Getter @Setter(AccessLevel.PACKAGE)
public class InsertResultItem {

  private LocalDate date;
  private Absence absence;
  private AbsenceType absenceType;
  private Operation operation;
  private AbsenceProblem absenceProblem;
  
  private List<ConsumedResidualAmount> consumedResidualAmount = Lists.newArrayList();
  
  public enum Operation {
    check, insert, insertReplacing, remainingBefore, remainingAfter, cancel;
  }
  
  public String toString() {
    return MoreObjects.toStringHelper(ReportAbsenceProblem.class)
        .add("date", date)
        .add("code", absence.absenceType.code)
        .add("code2", absenceType.code)
        .add("operation", operation)
        .add("problem", absenceProblem)
        .toString();
  }

}
