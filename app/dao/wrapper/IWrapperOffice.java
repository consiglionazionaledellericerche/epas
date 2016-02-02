package dao.wrapper;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import models.Office;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import java.util.List;

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
  
  /**
   * Il primo mese per cui è possibile effettuare l'upload degli attestati. 
   * @return yearMonth se esiste.
   */
  public Optional<YearMonth> getFirstMonthUploadable();
  
  /**
   * La lista degli anni di cui è possibile effettuare l'invio degli attestati per la sede.
   * @return lista degli anni
   */
  public List<Integer> getYearUploadable();
  
  /**
   * La lista dei mesi di cui è possibile effettuare l'invio degli attestati.
   * @return lista dei mesi
   */
  public List<Integer> getMonthUploadable();

  /**
   * Il mese di cui presumubilmente occorre fare l'invio attestati. (Il precedente rispetto a 
   * quello attuale se non è precedente al primo mese per attestati).
   * @return prossimo mese da inviare
   */
  public Optional<YearMonth> nextYearMonthToUpload();
  
  /**
   * Se il mese passato come parametro è inviabile.
   * @param yearMonth mese da verificare
   * @return esito
   */
  public boolean isYearMonthUploadable(YearMonth yearMonth);

}
