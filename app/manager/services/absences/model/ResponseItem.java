package manager.services.absences.model;

import lombok.Builder;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;

import org.joda.time.LocalDate;
import org.testng.collections.Lists;

import java.util.List;

/**
 * Record di esito inserimento assenza.
 * 
 * @author alessandro
 *
 */
public class ResponseItem {

  public LocalDate date;
  public Absence absence;
  public AbsenceType absenceType;
  public AbsenceOperation operation;
  public List<ConsumedResidualAmount> consumedResidualAmount = Lists.newArrayList();
  public AbsenceProblem absenceProblem;

  public ResponseItem(AbsenceType absenceType, AbsenceOperation operation, LocalDate date) {
    this.absenceType = absenceType;
    this.operation = operation;
    this.date = date;
  }
  
  
  public enum AbsenceOperation {
    insert, insertComplation, remainingBefore, remainingAfter, cancel;
  }

  public enum AbsenceProblem {
    limitExceeded, wrongComplationPosition, notAtTheWeekEnd;
  }

  @Builder
  public static class ConsumedResidualAmount {

    public AmountType amountType;                     // | units | minutes | units |
    public int amount;                               // | 02:00 | 01:00   |   1   |
    public int workingTime;                           // | 07:12 |

    public String printAmount(int amount) {
      if (amountType.equals(AmountType.units)) {
        int units = amount / 100;
        int percent = amount % 100;
        return units + " + " + percent + "%";
      }
      int hours = amount / 60; //since both are ints, you get an int
      int minutes = amount % 60;
      return String.format("%d:%02d", hours, minutes);
    }

    public String residualName;       // | res. anno passato | residuo anno corrente | lim. anno |
    public LocalDate expireResidual;  // |     31/03/2016    |      31/03/2017       | 31/12/2016| 
    public int totalResidual;         // |     20:00         |      07:00            |    28     |
    public int usedResidualBefore;    // |     00:00         |      00:00            |    6      |

    public int residualBefore() {
      return this.totalResidual - this.usedResidualBefore;
    }

    public int residualAfter() {
      return this.residualBefore() - this.amount;
    }

    public boolean canTake() {
      return residualAfter() >= 0;
    }
  }
}
