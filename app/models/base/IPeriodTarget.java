package models.base;

import com.google.common.base.Optional;

import models.Contract;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * Il modello è il target del periodo. EX. Contract
 * 
 * @author alessandro
 *
 * @param <T>
 */
public interface IPeriodTarget<T extends BaseModel> {

  T getValue();
}
