package models.exports;

import models.Person;
import models.PersonReperibilityType;

import org.joda.time.LocalDate;

/**
 * Classe di supporto per l'importazione delle informazioni relative ai giorni di assenza delle
 * persone reperibili.
 *
 * @author arianna
 */
public class AbsenceReperibilityPeriod {

  public final Person person;
  public final LocalDate start;
  public final PersonReperibilityType reperibilityType;
  public LocalDate end;

  public AbsenceReperibilityPeriod(Person person, LocalDate start, PersonReperibilityType type) {
    this.person = person;
    this.start = start;
    this.reperibilityType = type;
  }

  public AbsenceReperibilityPeriod(Person person, LocalDate start, LocalDate end, PersonReperibilityType type) {
    this.person = person;
    this.start = start;
    this.end = end;
    this.reperibilityType = type;
  }
}
