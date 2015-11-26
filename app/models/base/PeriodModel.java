package models.base;

import com.google.common.base.Optional;

import models.Contract;

import org.joda.time.LocalDate;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class PeriodModel extends BaseModel {

  /**
   * Il contratto dell'IPeriodModel.
   */
  public abstract Contract getContract();

  /**
   * L'inizio del periodo.
   */
  public abstract LocalDate getBegin();

  /**
   * La fine del periodo.
   */
  public abstract Optional<LocalDate> getEnd();

}
