package manager.services.absences.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
  
  //Errori gravi
  public Set<Absence> complationSameDay;        //due o più completamenti  
  public Set<Absence> replacingSameDay;         //due o più rimpiazzamenti
  public Absence compromisedComplation;         //sequenza compromessa
  public Absence compromisedReplacing;          //sequenza compromessa

  //Lo stato limiti
  public List<TakenAbsence> takenAbsences;
  public Absence overtakenLimitAbsence = null;
  
  //Lo stato di completamento
  private Absence complationAbsence;
  private AmountType amountTypeComplation;
  private int residualBeforeComplation = 0;
  private int consumedComplation = 0;
  private int residualAfterComplation = 0;
  
  //Il rimpiazzamento
  private Absence existentReplacing;
  private AbsenceType correctReplacing;
  
  public boolean complationCompromised() {
    if (this.absencePeriod.compromisedReplacingDate != null 
        && !this.absencePeriod.compromisedReplacingDate.isAfter(this.date)) {
      return true;
    }
    return false;
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
  
  public List<Absence> absencesNotPersisted() {
    List<Absence> absences = Lists.newArrayList();
    for (TakenAbsence takenAbsence : this.takenAbsences) {
      if (!takenAbsence.absence.isPersistent()) {
        absences.add(takenAbsence.absence);
      }
    }
    if (this.existentReplacing != null && !this.existentReplacing.isPersistent()) {
      absences.add(this.existentReplacing);
    }
    return absences;
    
  }

  public List<RowRecap> buildDayRows() {
    
    if (this.complationSameDay == null) {
      this.complationSameDay = Sets.newHashSet();
    }
    if (this.replacingSameDay == null) {
      this.replacingSameDay = Sets.newHashSet();
    }
    if (this.takenAbsences == null) {
      this.takenAbsences = Lists.newArrayList();
    }
    
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
    Absence replacing = null;                                       
    AbsenceType missingReplacing = null;
    if (this.replacingSameDay.isEmpty()) {
      replacing = this.existentReplacing;
      if (this.existentReplacing == null && this.missingReplacing()) {
        missingReplacing = this.correctReplacing;
      }
    }
    
    //1) Le assenze takable non completamento
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
    //2) La assenza takable completamento (quando è solo una)
    if (takenComplation != null) {
      RowRecap rowRecap = new RowRecap();
      if (!datePrinted) {
        rowRecap.date = this.date;
        datePrinted = true;
      }
      rowRecap.absence = takenComplation.absence;
      rowRecap.usableLimit = printAmount(takenComplation.residualBeforeTakable, takenComplation.amountTypeTakable);
      rowRecap.usableTaken = printAmount(takenComplation.consumedTakable, takenComplation.amountTypeTakable);
      if (rowRecap.usableLimit.equals("-1.0")) {
        rowRecap.usableLimit = "";
        rowRecap.usableTaken = "";
      }
      rowRecap.consumedComplationAbsence = printAmount(this.consumedComplation, this.amountTypeComplation);
      if (!complationCompromised() && !complationCompromisedInThisDay()) {
        rowRecap.consumedComplationBefore = printAmount(this.residualBeforeComplation, this.amountTypeComplation);
      }
      if (missingReplacing != null) {
        rowRecap.missingReplacing = missingReplacing;
      }
      rowsRecap.add(rowRecap);
    }
    
    //3) L'assenza di rimpiazzamento (quando è solo una)
    if (replacing != null) {
      RowRecap rowRecap = new RowRecap();
      if (!datePrinted) {
        rowRecap.date = this.date;
        datePrinted = true;
      }
      rowRecap.absence = replacing;      
      if (!complationCompromised()) {
        rowRecap.consumedComplationNext = 
            printAmount(this.residualAfterComplation, this.amountTypeComplation);
      }
      rowsRecap.add(rowRecap);
    }
    
    //4) Le assenze takable completamento (quando sono più di una)
    for (Absence absence : complationSameDay) {
      RowRecap rowRecap = new RowRecap();
      if (!datePrinted) {
        rowRecap.date = this.date;
        datePrinted = true;
      }
      rowRecap.absence = absence;
      rowsRecap.add(rowRecap);
    }
    
    //4) Le assenze di rimpiazzamento (quando sono più di una)
    for (Absence absence : replacingSameDay) {
      RowRecap rowRecap = new RowRecap();
      if (!datePrinted) {
        rowRecap.date = this.date;
        datePrinted = true;
      }
      rowRecap.absence = absence;
      rowsRecap.add(rowRecap);
    }
    
    return rowsRecap;
  }
  
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
    if (amountType == null) {
      return "";
    }
    String format = "";
    if (amountType.equals(AmountType.units)) {
      if (amount == 0) {
        return "0";// giorno lavorativo";
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
      return ((double)amount / 100 )+ "";
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

