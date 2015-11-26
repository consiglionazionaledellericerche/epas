package models.base;


import com.google.common.base.Optional;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * Il modello è un periodo del contratto con un valore.
 *
 * @author alessandro
 */
public interface IPeriodModel<T extends PeriodModel> {
  
  /**
   * Il target del periodo.
   * @return
   */
  IPeriodTarget getTarget();
  
  /**
   * Imposta il target del periodo.
   * @param target
   */
  void setTarget(IPeriodTarget target);
  
  /**
   * L'inizio del periodo.
   * 
   * @return
   */
  LocalDate getBegin();
  
  /**
   * Imposta la fine del periodo.
   * @param end
   */
  void setBegin(LocalDate begin);
  
  /**
   * La fine del periodo.
   * 
   * @return
   */
  Optional<LocalDate> getEnd();
  
  /**
   * Imposta la fine del periodo.
   * @param end
   */
  void setEnd(Optional<LocalDate> end);
  

  /**
   * Il valore del periodo. 
   * @return
   */
  IPeriodValue getValue();
  
  /**
   * Imposta il valore del periodo. 
   * @return
   */
  void setValue(IPeriodValue value);
  

  /**
   * La lista dei periodi.
   * @return
   */
  List<IPeriodModel> orderedPeriods();
  
  /**
   * Una nuova istanza del tipo PeriodModel.
   * @return
   */
  PeriodModel newInstance();
  
  
  

}
