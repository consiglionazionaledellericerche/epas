package manager.services.absences;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import manager.services.absences.AbsencesReport.ReportAbsenceProblem;

import models.absences.Absence;
import models.absences.AmountType;

import org.joda.time.LocalDate;

@Builder @Getter @Setter(AccessLevel.PACKAGE)
public class AbsenceStatus {

  public enum StatusType {
    takable, complation, simple;
  }
  
  public Absence absence;
  public StatusType type;
  
  public AmountType amountTypeTakable;
  public int consumedTakable;
  public int residualBeforeTakable;
  //public int residualAfterTakable;
  
  public AmountType amountTypeComplation;
  public int consumedComplation;
  public int residualBeforeComplation;
  //public int residualBeforeComplation;
  
  public ReportAbsenceProblem reportProblem;
  
  
  
  private AmountType amountType;                    // | units | minutes | units |
  private int amount;                               // | 02:00 | 01:00   |   1   |
  private int workingTime;                          // | 07:12 |

  private String residualName;       // | res. anno passato | residuo anno corrente | lim. anno |
  private LocalDate expireResidual;  // |     31/03/2016    |      31/03/2017       | 31/12/2016| 
  private int totalResidual;         // |     20:00         |      07:00            |    28     |
  private int usedResidualBefore;    // |     00:00         |      00:00            |    6      |

  public int residualBefore() {
    return this.totalResidual - this.usedResidualBefore;
  }

  public int residualAfter() {
    return this.residualBefore() - this.amount;
  }

  /**
   * FIXME Versione provvisoria per fare alcune prove...
   * @param amount
   * @return
   */
  public String printAmount(int amount) {

    String format = "";
    if (amountType.equals(AmountType.units)) {
      int units = amount / 100;
      int percent = amount % 100;
      String label = " giorni lavorativi";
      if (units == 1) {
        label = " giorno lavorativo";
      }
      if (units > 0 && percent > 0) {
        return units + label + " + " + percent + "% di un giorno lavorativo";  
      } else if (units > 0) {
        return units + label;
      } else if (percent > 0) {
        return percent + "% di un giorno lavorativo";
      }
    }
    if (amountType.equals(AmountType.minutes)) {
      int hours = amount / 60; //since both are ints, you get an int
      int minutes = amount % 60;

      if (hours > 0 && minutes > 0) {
        format = hours + " ore " + minutes + " minuti";
      } else if (hours > 0) {
        format = hours + " ore";
      } else if (minutes > 0) {
        format = minutes + " minuti";
      }
    }
    return format;
  }
}
