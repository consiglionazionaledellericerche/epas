package models.exports;

import lombok.AllArgsConstructor;
import models.Person;
import models.PersonReperibilityType;
import org.joda.time.LocalDate;

/**
 * Classe di supporto per l'esportazione delle informazioni relative alla reperibilità delle
 * persone.
 *
 * @author cristian
 */
@AllArgsConstructor
public class ReperibilityPeriod {

  public final Person person;
  public final LocalDate start;
  public LocalDate end;
  public PersonReperibilityType reperibilityType;

  /**
   * Costruttore.
   * @param person la persona
   * @param start la data di inizio della reperibilità
   * @param end la data di fine della reperibilità
   */
  public ReperibilityPeriod(Person person, LocalDate start, LocalDate end) {
    this.person = person;
    this.start = start;
    this.end = end;
  }

}
