package models.base;


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
   * Imposta la fine del periodo.
   * @param end la data di fine del periodo
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


}
