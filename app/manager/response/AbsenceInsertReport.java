package manager.response;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import java.util.List;
import org.joda.time.LocalDate;

public class AbsenceInsertReport {

  private List<AbsencesResponse> absences = Lists.newArrayList();
  private List<String> warnings = Lists.newArrayList();
  private List<LocalDate> datesInTrouble = Lists.newArrayList();
  private int totalAbsenceInsert = 0;
  private int absenceInReperibilityOrShift = 0;

  /**
   * Aggiunge il riepilogo dell'inserimento assenza e incrementa i contatori corretti.
   * 
   * @param response Il riepilogo dell'inserimento assenza.
   */
  public void add(AbsencesResponse response) {
    absences.add(response);
    if (response.isInsertSucceeded()) {
      totalAbsenceInsert++;
    }
    if (response.isDayInReperibilityOrShift()) {
      absenceInReperibilityOrShift++;
    }
  }

  public boolean hasWarningOrDaysInTrouble() {
    return !(warnings.isEmpty() && datesInTrouble.isEmpty());
  }

  public List<AbsencesResponse> getAbsences() {
    return absences;
  }

  public void setAbsences(List<AbsencesResponse> absences) {
    this.absences = absences;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }

  public List<LocalDate> getDatesInTrouble() {
    return datesInTrouble;
  }

  public void setDatesInTrouble(List<LocalDate> datesInTrouble) {
    this.datesInTrouble = datesInTrouble;
  }

  public int getTotalAbsenceInsert() {
    return totalAbsenceInsert;
  }

  public void setTotalAbsenceInsert(int totalAbsenceInsert) {
    this.totalAbsenceInsert = totalAbsenceInsert;
  }

  public int getAbsenceInReperibilityOrShift() {
    return absenceInReperibilityOrShift;
  }

  /**
   * Setter del campo absenceInReperibilityOrShift.
   * 
   * @param absenceInReperibilityOrShift numero di assenze in giorni di reperibilità o turno.
   */
  public void setAbsenceInReperibilityOrShift(int absenceInReperibilityOrShift) {
    this.absenceInReperibilityOrShift = absenceInReperibilityOrShift;
  }

  /**
   * Metodo che ritorna la lista dei giorni di reperibilità o turno.
   * @return la lista dei giorni in cui si è in reperibilità o turno.
   */
  public List<LocalDate> datesInReperibilityOrShift() {

    return FluentIterable.from(absences).filter(
        new Predicate<AbsencesResponse>() {
          @Override
          public boolean apply(AbsencesResponse air) {
            return air.isDayInReperibilityOrShift();
          }
        }).transform(AbsencesResponse.ToDate.INSTANCE).toList();
  }

}
