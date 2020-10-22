package models.exports;

import models.Person;
import models.ShiftType;
import models.enumerate.ShiftSlot;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

/**
 * Classe di supporto per l'esportazione delle informazioni relative ai turni delle persone.
 *
 * @author dario, arianna
 */
public class ShiftPeriod {

  public final LocalDate start;
  public final ShiftType shiftType;
  public final boolean cancelled;
  public Person person;
  public LocalDate end;
  public ShiftSlot shiftSlot;
  public LocalTime startSlot;
  public LocalTime endSlot;

  /**
   * Costruttore.
   * @param person la persona
   * @param start la data di inizio
   * @param end la data di fine
   * @param shiftType l'attività di turno
   * @param cancelled se il turno è cancellato
   * @param shiftSlot lo slot di turno
   * @param startSlot quando inizia lo slot di turno
   * @param endSlot quando finisce lo slot di turno
   */
  public ShiftPeriod(
      Person person, LocalDate start, LocalDate end, ShiftType shiftType,
      boolean cancelled, ShiftSlot shiftSlot, LocalTime startSlot, LocalTime endSlot) {
    this.person = person;
    this.start = start;
    this.end = end;
    this.cancelled = cancelled;
    this.shiftType = shiftType;
    this.shiftSlot = shiftSlot;
    this.startSlot = startSlot;
    this.endSlot = endSlot;
  }

  /**
   * Costruttore.
   * @param person la persona
   * @param start la data di inizio
   * @param end la data di fine
   * @param shiftType l'attività di turno
   * @param cancelled se il turno è stato cancellato
   * @param shiftSlot lo slot di turno
   */
  public ShiftPeriod(Person person, LocalDate start, LocalDate end, ShiftType shiftType,
      boolean cancelled, ShiftSlot shiftSlot) {
    this.person = person;
    this.start = start;
    this.end = end;
    this.cancelled = cancelled;
    this.shiftType = shiftType;
    this.shiftSlot = shiftSlot;
  }

  // for periods of 1 day where end date is null
  /**
   * Costruttore.
   * @param person la persona
   * @param start la data del turno (per 1 solo giorno, cioè quando endDate is null)
   * @param shiftType l'attività di turno
   * @param cancelled se il turno è stato cancellato
   * @param shiftSlot lo slot di turno
   * @param startSlot l'orario di inizio dello slot
   * @param endSlot l'orario di fine dello slot
   */
  public ShiftPeriod(Person person, LocalDate start, ShiftType shiftType,
      boolean cancelled, ShiftSlot shiftSlot, LocalTime startSlot, LocalTime endSlot) {
    this.person = person;
    this.start = start;
    this.shiftType = shiftType;
    this.cancelled = cancelled;
    this.shiftSlot = shiftSlot;
    this.startSlot = startSlot;
    this.endSlot = endSlot;
  }

  // for cancelled shift
  /**
   * Costruttore.
   * @param start la data di inizio
   * @param end la data di fine
   * @param shiftType l'attività di turno
   * @param cancelled se il turno è cancellato
   * @param startSlot l'orario di inizio dello slot
   * @param endSlot l'orario di fine dello slot
   */
  public ShiftPeriod(LocalDate start, LocalDate end, ShiftType shiftType,
      boolean cancelled, LocalTime startSlot, LocalTime endSlot) {
    this.start = start;
    this.end = end;
    this.shiftType = shiftType;
    this.cancelled = cancelled;
    this.startSlot = startSlot;
    this.endSlot = endSlot;
  }
  
  // for cancelled shift
  /**
   * Costruttore.
   * @param start la data di inizio
   * @param end la data di fine
   * @param shiftType l'attività di turno
   * @param cancelled se il turno è cancellato
   */
  public ShiftPeriod(LocalDate start, LocalDate end, ShiftType shiftType,
      boolean cancelled) {
    this.start = start;
    this.end = end;
    this.shiftType = shiftType;
    this.cancelled = cancelled;

  }
}
