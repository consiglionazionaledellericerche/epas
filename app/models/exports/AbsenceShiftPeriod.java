/**
 *
 */
package models.exports;

import org.joda.time.LocalDate;

import models.Person;
import models.ShiftType;

/**
 * Classe di supporto per l'importazione delle informazioni relative ai giorni di assenza delle
 * persone in turno.
 *
 * @author arianna
 */
public class AbsenceShiftPeriod {

  public final Person person;
  public final LocalDate start;
  public final ShiftType shiftType;
  public LocalDate end;

  public AbsenceShiftPeriod(Person person, LocalDate start, ShiftType type) {
    this.person = person;
    this.start = start;
    this.shiftType = type;
  }

  public AbsenceShiftPeriod(Person person, LocalDate start, LocalDate end, ShiftType type) {
    this.person = person;
    this.start = start;
    this.end = end;
    this.shiftType = type;
  }
}
