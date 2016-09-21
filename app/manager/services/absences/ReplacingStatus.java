package manager.services.absences;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;

import org.joda.time.LocalDate;

@Builder @Getter @Setter
public class ReplacingStatus {
  
  private LocalDate date;

  private Absence complationAbsence;
  private AmountType amountTypeComplation;
  private int residualBeforeComplation;
  private int consumedComplation;
  private int residualAfterComplation;
  
  private Absence existentReplacing;
  private AbsenceType correctReplacing;
  
  public boolean correct() {
    return !wrongType() && !onlyCorrect() && !onlyExisting();
  }
  
  public boolean wrongType() {
    return correctReplacing != null && existentReplacing != null 
        && !existentReplacing.getAbsenceType().equals(correctReplacing);
  }
  
  public boolean onlyCorrect() {
    return correctReplacing != null && existentReplacing == null;
  }

  public boolean onlyExisting() {
    return correctReplacing == null && existentReplacing != null;
  }

}

