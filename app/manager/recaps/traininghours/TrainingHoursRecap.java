package manager.recaps.traininghours;

import models.Person;
import org.joda.time.LocalDate;

/**
 * Riepilogo delle ore di formazione.
 */
public class TrainingHoursRecap {

  public Person person;
  public Integer trainingHours;
  public LocalDate begin;
  public LocalDate end;
  public boolean sentToAttestati;

  /**
   * Costruttore.
   *
   * @param person la persona
   * @param trainingHours le ore di formazione
   * @param begin la data di inizio
   * @param end la data di fine
   * @param sentToAttestati se sono state inviate ad Attestati
   */
  public TrainingHoursRecap(Person person, Integer trainingHours, LocalDate begin, LocalDate end,
      boolean sentToAttestati) {
    this.person = person;
    this.trainingHours = trainingHours;
    this.begin = begin;
    this.end = end;
    this.sentToAttestati = sentToAttestati;
  }
}
