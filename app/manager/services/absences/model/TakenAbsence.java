package manager.services.absences.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AmountType;

import org.joda.time.LocalDate;

@Builder @Getter @Setter(AccessLevel.PACKAGE)
public class TakenAbsence {

  public Absence absence;
  
  public AmountType amountTypeTakable;
  public int consumedTakable;
  public int residualBeforeTakable;
  
  
  public int workingTime;
  public AbsenceProblem absenceProblem;
  
}
