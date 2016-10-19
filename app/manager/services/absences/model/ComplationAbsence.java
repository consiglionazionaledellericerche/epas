package manager.services.absences.model;

import lombok.Builder;

import models.absences.Absence;
import models.absences.AmountType;

@Builder 
public class ComplationAbsence {
  public Absence absence;

  public AmountType amountType;
  public int residualComplationBefore = 0;
  public int consumedComplation = 0;
  public int residualComplationAfter = 0;

}
