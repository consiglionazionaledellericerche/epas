package manager.attestati.dto.show;

import lombok.ToString;

/**
 * Codice assenza esportato da attestati.
 *
 * @author alessandro
 *
 */
@ToString
public class CodiceAssenza {

  public int id;

  public String codice;
  public String descrizione;
  public String qtFrequenza;
  public String tipoFrequenza;
  public String tipologia;
}
