package manager.response;

import com.google.common.base.Function;

import com.fasterxml.jackson.annotation.JsonFilter;

import helpers.rest.JacksonModule;

import models.Absence;

import org.joda.time.LocalDate;

@JsonFilter(JacksonModule.FILTER)
public class AbsencesResponse {

  public static final String CODICE_FERIE_GIA_PRESENTE = "Il codice di assenza é già presente in almeno uno dei giorni in cui lo si voleva inserire";
  public static final String CODICE_GIORNALIERO_GIA_PRESENTE = "Esiste già un codice di assenza giornaliero nel periodo indicato. Operazione annullata";
  public static final String NESSUN_CODICE_FERIE_DISPONIBILE_PER_IL_PERIODO_RICHIESTO = "Nessun codice ferie disponibile per il periodo richiesto";
  public static final String RIPOSI_COMPENSATIVI_ESAURITI = "Numero di giorni di riposo compensativo esauriti per l'anno corrente";
  public static final String MONTE_ORE_INSUFFICIENTE = "Monte ore insufficiente per l'assegnamento del riposo compensativo";
  public static final String NON_UTILIZZABILE_NEI_FESTIVI = "Codice non utilizzabile in un giorno festivo";
  public static final String CODICI_MALATTIA_FIGLI_NON_DISPONIBILE = "Impossibile usufruire dei codici ferie per malattia dei figli";
  public static final String NESSUN_CODICE_FERIE_ANNO_PRECEDENTE_37 = "Nessun codice ferie dell'anno precedente 37 utilizzabile";
  public static final String ERRORE_GENERICO = "Impossibile inserire il codice d'assenza";
  public static final String PERSONDAY_PRECEDENTE_NON_PRESENTE = "Nessun personday per il giorno precedente a quando si intende inserire il codice con allegato. Verificare";

  private LocalDate date;
  private String absenceCode;
  private String warning;
  private boolean insertSucceeded = false;
  private boolean isHoliday = false;
  private boolean isDayInReperibilityOrShift = false;
  private Absence absenceAdded;

  public AbsencesResponse(LocalDate date, String absenceCode) {
    this.date = date;
    this.absenceCode = absenceCode;
  }

  public AbsencesResponse(LocalDate date, String absenceCode, String warning) {
    super();
    this.date = date;
    this.absenceCode = absenceCode;
    this.warning = warning;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public String getAbsenceCode() {
    return absenceCode;
  }

  public void setAbsenceCode(String absenceCode) {
    this.absenceCode = absenceCode;
  }

  public String getWarning() {
    return warning;
  }

  public void setWarning(String warning) {
    this.warning = warning;
  }

  public boolean isInsertSucceeded() {
    return insertSucceeded;
  }

  public void setInsertSucceeded(boolean insertSucceeded) {
    this.insertSucceeded = insertSucceeded;
  }

  public boolean isHoliday() {
    return isHoliday;
  }

  public void setHoliday(boolean isHoliday) {
    this.isHoliday = isHoliday;
  }

  public boolean isDayInReperibilityOrShift() {
    return isDayInReperibilityOrShift;
  }

  public void setDayInReperibilityOrShift(boolean isDayInReperibilityOrShift) {
    this.isDayInReperibilityOrShift = isDayInReperibilityOrShift;
  }

  public Absence getAbsenceAdded() {
    return absenceAdded;
  }

  public void setAbsenceAdded(Absence absenceAdded) {
    this.absenceAdded = absenceAdded;
  }

  public enum toDate implements Function<AbsencesResponse, LocalDate> {
    INSTANCE;

    @Override
    public LocalDate apply(AbsencesResponse air) {
      return air.date;
    }
  }

}
