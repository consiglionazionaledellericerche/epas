package models.exports;

import lombok.ToString;
import models.Person;
import org.joda.time.LocalDateTime;

/**
 * Esportazione delle informazioni relative alla missione.
 *
 * @author dario
 */
@ToString
public class MissionFromClient {

  public String tipoMissione;
  public String codiceSede;
  public Long id;
  public Person person;
  public String matricola;
  public LocalDateTime dataInizio;
  public LocalDateTime dataFine;
  public Long idOrdine;

}