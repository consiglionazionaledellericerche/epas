package manager.services.absences;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import models.absences.Absence;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import java.util.List;

/**
 * Record di esito inserimento assenza.
 * 
 * @author alessandro
 *
 */
@Builder @Getter @Setter(AccessLevel.PACKAGE)
public class ResponseItem {

  private LocalDate date;
  private Absence absence;
  private AbsenceType absenceType;
  private AbsenceOperation operation;
  private List<ConsumedResidualAmount> consumedResidualAmount = Lists.newArrayList();
  private AbsenceProblem absenceProblem;
  
  public enum AbsenceOperation {
    insert, insertReplacing, remainingBefore, remainingAfter, cancel;
  }

  public enum AbsenceProblem {
    notOnHoliday,
    twoComplationSameDay,
    limitExceeded;
  }
}
