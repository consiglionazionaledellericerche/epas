package manager.recaps.charts;

import lombok.EqualsAndHashCode;

import org.joda.time.LocalDate;

/**
 * @author daniele
 * @since 05/07/16.
 */
@EqualsAndHashCode
public class ResultFromFile {

  public String codice;
  public LocalDate dataAssenza;

  public ResultFromFile(String codice, LocalDate dataAssenza) {
    this.codice = codice;
    this.dataAssenza = dataAssenza;
  }

}
