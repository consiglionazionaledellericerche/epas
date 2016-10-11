package models.exports;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import models.Person;
import models.ShiftType;

import org.joda.time.LocalDate;

/**
 * Classe di supporto per l'importazione delle informazioni relative ai giorni di assenza delle
 * persone in turno.
 *
 * @author arianna
 */
@RequiredArgsConstructor
@AllArgsConstructor
public class AbsenceShiftPeriod {

  public final Person person;
  public final LocalDate start;
  public LocalDate end;
  public final ShiftType shiftType;

}
