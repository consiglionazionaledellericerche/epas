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
  
  //*********
  
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
  
}
