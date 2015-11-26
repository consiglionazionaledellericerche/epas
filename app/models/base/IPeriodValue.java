package models.base;

import com.google.common.base.Optional;

import models.Contract;

import org.joda.time.LocalDate;

/**
 * Il modello è un valore di un periodo.
 * 
 * @author alessandro
 *
 * @param <T>
 */
public interface IPeriodValue<T extends BaseModel> {

  T getValue();
  
}
