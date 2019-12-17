package models.exports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;
import models.Person;
import org.joda.time.LocalDateTime;

/**
 * Esportazione delle informazioni relative alla missione.
 *
 * @author dario
 */
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MissionFromClient {

  public String tipoMissione;
  public String destinazioneMissione;
  public String codiceSede;
  public Long id;
  public Person person;
  public String matricola;
  public LocalDateTime dataInizio;
  public LocalDateTime dataFine;
  public Long idOrdine;
  public int anno;
  public Long numero;

}