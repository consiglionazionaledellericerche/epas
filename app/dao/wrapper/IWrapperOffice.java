package dao.wrapper;

import org.joda.time.LocalDate;

import models.Office;

/**
 * Office potenziato.
 *
 * @author alessandro
 */
public interface IWrapperOffice extends IWrapperModel<Office> {

  /**
   * @return la data di installazione della sede.
   */
  LocalDate initDate();

}
