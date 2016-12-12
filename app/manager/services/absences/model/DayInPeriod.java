package manager.services.absences.model;


import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.Setter;

import manager.services.absences.errors.AbsenceError;

import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;

import org.joda.time.LocalDate;

import java.util.List;

@Getter
@Setter
public class DayInPeriod {


  private LocalDate date;
  private AbsencePeriod absencePeriod;

  //Lo stato limiti (anche quelli in eccesso)
  private List<TakenAbsence> takenAbsences = Lists.newArrayList();
  private List<Absence> existentComplations = Lists.newArrayList();
  private ComplationAbsence complationAbsence; //quando è unico

  //Il rimpiazzamento
  private List<Absence> existentReplacings = Lists.newArrayList();
  private AbsenceType correctReplacing;

  public DayInPeriod(LocalDate date, AbsencePeriod absencePeriod) {
    this.date = date;
    this.absencePeriod = absencePeriod;
  }

  /**
   * Se l'assenza è inserita come taken nel giorno.
   *
   * @param absence assenza
   * @return esito
   */
  public boolean containTakenAbsence(Absence absence) {
    for (TakenAbsence takenAbsence : takenAbsences) {
      if (takenAbsence.absence.equals(absence)) {
        return true;
      }
    }
    return false;
  }

  /**
   * I rimpiazzamenti scorretti nel giorno.
   *
   * @return list
   */
  public List<Absence> existentWrongReplacing() {
    if (correctReplacing == null) {
      return existentReplacings;
    }
    List<Absence> wrong = Lists.newArrayList();
    for (Absence existentReplacing : existentReplacings) {
      if (!existentReplacing.absenceType.equals(correctReplacing)) {
        wrong.add(existentReplacing);
      }
    }
    return wrong;
  }

  /**
   * Se nel giorno c'è un rimpiazzamento mancante.
   *
   * @return esito
   */
  public boolean isReplacingMissing() {
    if (correctReplacing == null) {
      return false;
    }
    for (Absence existentReplacing : existentReplacings) {
      if (existentReplacing.absenceType.equals(correctReplacing)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Le assenze taken che non sono di completamento nel giorno.
   *
   * @return list
   */
  public List<TakenAbsence> takenNotComplation() {
    List<TakenAbsence> notComplation = Lists.newArrayList();
    for (TakenAbsence takenAbsence : takenAbsences) {
      if (complationAbsence == null
          || !complationAbsence.absence.equals(takenAbsence.absence)) {
        notComplation.add(takenAbsence);
      }
    }
    return notComplation;
  }

  /**
   * L'assenza taken di completamento nel giorno (è unica altrimenti sono collocate in
   * existentComplations.
   *
   * @return takenAbsence
   */
  public TakenAbsence takenComplation() {
    for (TakenAbsence takenAbsence : takenAbsences) {
      if (complationAbsence != null
          && complationAbsence.absence.equals(takenAbsence.absence)) {
        return takenAbsence;
      }
    }
    return null;
  }


  public static class TemplateRow {

    public LocalDate date;

    public Absence absence;
    public GroupAbsenceType groupAbsenceType;
    public boolean beforeInitialization = false;
    public List<AbsenceError> absenceErrors = Lists.newArrayList();
    public List<AbsenceError> absenceWarnings = Lists.newArrayList();

    public boolean usableColumn = false;
    public String usableLimit;
    public String usableTaken;

    public boolean complationColumn = false;
    public String consumedComplationBefore;
    public String consumedComplationAbsence;
    public String consumedComplationNext;

    public boolean isReplacingRow = false;

    public boolean onlyNotOnHoliday() {
      return absenceErrors.size() == 1
          && absenceErrors.iterator().next().absenceProblem.equals(AbsenceProblem.NotOnHoliday);
    }

  }

  /**
   * Le righe della tabella web per il periodo.
   *
   * @param nothing il tempo giustificato nothing da associare al rimpiazzamento (FIXME)
   * @return list
   */
  public List<TemplateRow> allTemplateRows() {
    
    //FIXME: questo justifiedType serve per i replacing. Injettarlo
    JustifiedType nothing = new JustifiedType();
    nothing.name = JustifiedTypeName.nothing;   

    List<TemplateRow> templateRows = Lists.newArrayList();

    for (TakenAbsence takenAbsence : takenNotComplation()) {
      TemplateRow takenRow = buildTakenNotComplation(takenAbsence,
          takenAbsence.beforeInitialization);
      templateRows.add(takenRow);
    }

    TakenAbsence takenComplation = takenComplation();
    if (takenComplation != null) {
      TemplateRow complationRow = buildTakenComplation(takenComplation,
          takenComplation.beforeInitialization);
      templateRows.add(complationRow);
      if (correctReplacing != null) {
        TemplateRow replacingRow = buildReplacing(takenComplation, nothing);
        templateRows.add(replacingRow);
      }
    }

    return templateRows;
  }

  /**
   * Le righe della tabella web del periodo legate ad un inserimento.
   *
   * @param nothing il tempo giustificato nothing da associare al rimpiazzamento (FIXME)
   * @return list
   */
  public List<TemplateRow> templateRowsForInsert(final JustifiedType nothing) {

    List<TemplateRow> templateRows = Lists.newArrayList();

    if (absencePeriod.attemptedInsertAbsence == null
        || !absencePeriod.attemptedInsertAbsence.getAbsenceDate().equals(date)) {
      return templateRows;
    }

    TakenAbsence insertTakenAbsence = null;
    for (TakenAbsence takenAbsence : takenAbsences) {
      if (takenAbsence.absence.equals(absencePeriod.attemptedInsertAbsence)) {
        insertTakenAbsence = takenAbsence;
      }
    }

    if (takenNotComplation().contains(insertTakenAbsence)) {
      TemplateRow takenRow = buildTakenNotComplation(insertTakenAbsence,
          insertTakenAbsence.beforeInitialization);
      templateRows.add(takenRow);
    }
    if (takenComplation() != null && takenComplation().equals(insertTakenAbsence)) {
      TemplateRow complationRow = buildTakenComplation(insertTakenAbsence,
          insertTakenAbsence.beforeInitialization);
      templateRows.add(complationRow);

      //se non ci sono errori inserisco il replacing
      if (!absencePeriod.errorsBox.containAbsenceErrors(insertTakenAbsence.absence)) {
        if (correctReplacing != null) {
          TemplateRow replacingRow = buildReplacing(insertTakenAbsence, nothing);
          templateRows.add(replacingRow);
        }
      }
    }

    return templateRows;
  }

  private TemplateRow buildTakenNotComplation(TakenAbsence takenAbsence,
      boolean beforeInitialization) {
    TemplateRow takenRow = new TemplateRow();
    takenRow.date = takenAbsence.absence.getAbsenceDate();
    takenRow.beforeInitialization = beforeInitialization;
    takenRow.absence = takenAbsence.absence;
    takenRow.groupAbsenceType = absencePeriod.groupAbsenceType;
    takenRow.absenceErrors = absencePeriod.errorsBox.absenceErrors(takenRow.absence);
    takenRow.absenceWarnings = absencePeriod.errorsBox.absenceWarnings(takenRow.absence);
    if (absencePeriod.isTakableWithLimit()) {
      takenRow.usableColumn = true;
      if (!takenRow.onlyNotOnHoliday() && !beforeInitialization) {
        takenRow.usableLimit = formatAmount(takenAbsence.periodResidualBefore(),
            takenAbsence.amountType);
        takenRow.usableTaken = formatAmount(takenAbsence.takenAmount, takenAbsence.amountType);
      }
    }

    return takenRow;
  }

  private TemplateRow buildTakenComplation(TakenAbsence takenComplation,
      boolean beforeInitialization) {
    TemplateRow complationRow = new TemplateRow();
    complationRow.beforeInitialization = beforeInitialization;
    complationRow.date = complationAbsence.absence.getAbsenceDate();
    complationRow.absence = complationAbsence.absence;
    complationRow.groupAbsenceType = absencePeriod.groupAbsenceType;
    complationRow.absenceErrors = absencePeriod.errorsBox.absenceErrors(complationRow.absence);
    complationRow.absenceWarnings = absencePeriod.errorsBox.absenceWarnings(complationRow.absence);
    if (absencePeriod.isTakableWithLimit()) {
      complationRow.usableColumn = true;
    }
    complationRow.complationColumn = true;
    if (!complationRow.onlyNotOnHoliday() && !beforeInitialization) {
      complationRow.usableLimit = formatAmount(takenComplation.periodResidualBefore(),
          takenComplation.amountType);
      complationRow.usableTaken = formatAmount(takenComplation.takenAmount,
          takenComplation.amountType);
    }
    complationRow.consumedComplationAbsence = formatAmount(complationAbsence.consumedComplation,
        complationAbsence.amountType);
    complationRow.consumedComplationBefore = formatAmount(complationAbsence
        .residualComplationBefore, complationAbsence.amountType);

    return complationRow;
  }

  private TemplateRow buildReplacing(TakenAbsence takenComplation, JustifiedType nothing) {
    TemplateRow replacingRow = new TemplateRow();
    replacingRow.date = date;
    replacingRow.isReplacingRow = true;
    replacingRow.usableColumn = false;
    replacingRow.complationColumn = true;
    Absence absence = new Absence();
    absence.date = this.date;
    absence.absenceType = this.correctReplacing;
    absence.justifiedType = nothing;
    replacingRow.absence = absence;
    replacingRow.groupAbsenceType = absencePeriod.groupAbsenceType;
    replacingRow.consumedComplationNext = formatAmount(complationAbsence.residualComplationAfter,
        complationAbsence.amountType);
    return replacingRow;
  }

  //FIXME metodo provvisorio per fare le prove.
  private static String formatAmount(int amount, AmountType amountType) {
    if (amountType == null) {
      return "";
    }
    String format = "";
    if (amountType.equals(AmountType.units)) {
      if (amount == 0) {
        return "0 giorni";// giorno lavorativo";
      }
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
      return ((double) amount / 100) + " giorni";
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

