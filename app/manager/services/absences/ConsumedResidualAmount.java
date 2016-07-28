package manager.services.absences;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import models.absences.AmountType;

import org.joda.time.LocalDate;

@Builder @Getter @Setter(AccessLevel.PACKAGE)
public class ConsumedResidualAmount {

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

  public boolean canTake() {
    return residualAfter() >= 0;
  }
  
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
}
