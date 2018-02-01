package models.exports;

import models.Person;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

/**
 * Esportazione delle informazioni relative alla missione.
 * @author dario
 *
 */
public class MissionFromClient {

  public String tipoMissione;
  public int codiceSede;
  public Long id;
  public Person person;
  public int matricola;
  public LocalDateTime dataInizio;
  public LocalDateTime dataFine;
  public Long idOrdine;
}
