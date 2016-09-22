package manager.services.absences;

import com.google.common.collect.Lists;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import manager.services.absences.model.AbsencePeriod;

import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.AmountType;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

@Builder @Getter @Setter
public class DayStatus {
  
  
  private LocalDate date;
  private AbsencePeriod absencePeriod;
  
  public List<TakenAbsence> takenAbsences;
  // L'errore
  public Absence overtakenLimitAbsence = null;
  
  //Errori gravi
  public Set<Absence> complationSameDay;        //due o più completamenti  
  public Set<Absence> replacingSameDay;         //due o più rimpiazzamenti
  public Absence compromisedComplation;         //sequenza compromessa
  public Absence compromisedReplacing;          //sequenza compromessa
  public boolean firstComplationErrorThisDay;
  
  private Absence complationAbsence;
  private AmountType amountTypeComplation;
  private int residualBeforeComplation = 0;
  private int consumedComplation = 0;
  private int residualAfterComplation = 0;
  
  private Absence existentReplacing;
  private AbsenceType correctReplacing;
  
//  public boolean correct() {
//    return !wrongTypeOfReplacing() && !missingReplacing() && !tooEarlyReplacing();
//  }

  public boolean complationCompromised() {
    return compromisedComplation != null || compromisedReplacing != null;
  }
  
  public boolean complationCompromisedInThisDay() {
    if (this.absencePeriod.compromisedReplacingDate == null) {
      return false;
    }
    return this.absencePeriod.compromisedReplacingDate.equals(this.date);
  }
  
  public boolean wrongTypeOfReplacing() {
    return correctReplacing != null && existentReplacing != null 
        && !existentReplacing.getAbsenceType().equals(correctReplacing);
  }
  
  public boolean missingReplacing() {
    return correctReplacing != null && existentReplacing == null;
  }

  public boolean tooEarlyReplacing() {
    return correctReplacing == null && existentReplacing != null;
  }
  
  
  

  public List<RowRecap> buildDayRows() {
    
    List<RowRecap> rowsRecap = Lists.newArrayList();
    boolean datePrinted = false;
    
    List<TakenAbsence> takenNotComplations = Lists.newArrayList();
    TakenAbsence takenComplation = null;
    for (TakenAbsence takenAbsence : takenAbsences) {
      if (!this.complationSameDay.contains(takenAbsence.absence)) {
        if (this.complationAbsence != null && this.complationAbsence.equals(takenAbsence.absence)) {
          takenComplation = takenAbsence;  
        } else {
          takenNotComplations.add(takenAbsence);
        }
      } 
    }
    
    //1) 
    for (TakenAbsence takenNotComplation : takenNotComplations) {
      RowRecap rowRecap = new RowRecap();
      if (!datePrinted) {
        rowRecap.date = this.date;
        datePrinted = true;
      }
      rowRecap.absence = takenNotComplation.absence;
      rowRecap.usableLimit = printAmount(takenNotComplation.residualBeforeTakable, takenNotComplation.amountTypeTakable);
      rowRecap.usableTaken = printAmount(takenNotComplation.consumedTakable, takenNotComplation.amountTypeTakable);
      rowsRecap.add(rowRecap);
    }
    //2)
    if (takenComplation != null) {
      RowRecap rowRecap = new RowRecap();
      if (!datePrinted) {
        rowRecap.date = this.date;
        datePrinted = true;
      }
      rowRecap.absence = takenComplation.absence;
      rowRecap.usableLimit = printAmount(takenComplation.residualBeforeTakable, takenComplation.amountTypeTakable);
      rowRecap.usableTaken = printAmount(takenComplation.consumedTakable, takenComplation.amountTypeTakable);
      rowRecap.consumedComplationAbsence = printAmount(this.consumedComplation, this.amountTypeComplation);
      if (!complationCompromised() && !complationCompromisedInThisDay()) {
        rowRecap.consumedComplationBefore = printAmount(this.residualBeforeComplation, this.amountTypeComplation);
      }
      rowsRecap.add(rowRecap);
    }
    
    
    Absence replacing = null;
    AbsenceType missingReplacing = null;
    if (this.replacingSameDay.isEmpty()) {
      replacing = this.existentReplacing;
      if (this.existentReplacing == null && this.missingReplacing()) {
        missingReplacing = this.correctReplacing;
      }
    }
    
    if (replacing != null) {
      
    }
    

    
    
    return null;
  }
  
//if (isComplationAbsence) {
//rowRecap.consumedComplationBefore = printAmount(this.residualBeforeComplation, 
//    this.amountTypeComplation);
//}

  
  public static class RowRecap {
    
    public LocalDate date;
    
    public Absence absence;
    public AbsenceType missingReplacing;
    
    public String usableLimit = "";
    public String usableTaken = "";
    
    public String consumedComplationBefore = "";
    public String consumedComplationAbsence = "";
    
    public String consumedComplationNext = "";

  }
  
  
  //FIXME metodo provvisorio per fare le prove.
  private String printAmount(int amount, AmountType amountType) {
    String format = "";
    if (amountType.equals(AmountType.units)) {
      if (amount == 0) {
        return "0%";// giorno lavorativo";
      }
//      int units = amount / 100;
//      int percent = amount % 100;
//      String label = " giorni lavorativi";
//      if (units == 1) {
//        label = " giorno lavorativo";
//      }
//      if (units > 0 && percent > 0) {
//        return units + label + " + " + percent + "% di un giorno lavorativo";  
//      } else if (units > 0) {
//        return units + label;
//      } else if (percent > 0) {
//        return percent + "% di un giorno lavorativo";
//      }
      return amount + "%";
    }
    if (amountType.equals(AmountType.minutes)) {
      if (amount == 0) {
        return "0 minuti";
      }
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

