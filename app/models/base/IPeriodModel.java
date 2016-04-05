package models.base;


import it.cnr.iit.epas.DateInterval;

import org.joda.time.LocalDate;

import play.db.Model;

/**
 * Il modello è un periodo del contratto con un valore.
 *
 * @author alessandro
 */
public interface IPeriodModel extends Model {

  /**
   * L'inizio del periodo.
   *
   * @return l'inizio del periodo
   */
  LocalDate getBeginDate();

  /**
   * Imposta l'inizio del periodo.
   * @param begin l'inizio del periodo
   */
  void setBeginDate(LocalDate begin);

  /**
   * La fine del periodo.
   *
   * @return la fine del periodo
   */
  LocalDate getEndDate();

  /**
   * Imposta la fine del periodo.
   * @param end la fine del periodo
   */
  void setEndDate(LocalDate end);

  /**
   * L'effettiva data fine nel caso di periodi complessi.
   */
  LocalDate calculatedEnd();
  
  
  /**
   * L'intervallo calcolato del periodo. Considera beginDate e calculatedEnd.
   * @return l'intervallo.
   */
  DateInterval periodInterval();

}
