package manager.attestati.dto.internal.clean;

import lombok.Builder;

import org.joda.time.LocalDate;

/**
 * Modella i dati contrattuali in attestati. Questo dto contiene le informazioni consumate da
 * ePAS e viene generato a ripulendo i dati forniti dagli endPoint interni di attestati.
 * @author alessandro
 *
 */
@Builder
public class ContrattoAttestati {

  public int matricola;
  public LocalDate beginContract;
  public LocalDate endContract;

  //Tipologia?
  //Parttime?
  

}
