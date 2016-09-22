package manager.services.absences;

import com.google.common.collect.Lists;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;

import org.assertj.core.util.Sets;
import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

@Builder @Getter @Setter
public class DayStatus {
  
  private LocalDate date;
  
  public List<TakenAbsence> takenAbsences;
  // L'errore
  public Absence overtakenLimitAbsence = null;
  
  //Errori gravi
  public Set<Absence> complationSameDay;        //due o più completamenti  
  public Set<Absence> replacingSameDay;         //due o più rimpiazzamenti
  public Absence compromisedComplation;         //sequenza compromessa
  public Absence compromisedReplacing;          //sequenza compromessa
  
  private Absence complationAbsence;
  private AmountType amountTypeComplation;
  private int residualBeforeComplation = 0;
  private int consumedComplation = 0;
  private int residualAfterComplation = 0;
  
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

