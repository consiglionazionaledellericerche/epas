package models.base;

import com.google.common.base.Optional;

import models.Contract;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

@MappedSuperclass
public abstract class PeriodModel extends BaseModel {

  /**
   * Il contratto dell'IPeriodModel.
   * @return
   */
  public abstract Contract getContract();
  
  /**
   * L'inizio del periodo.
   * 
   * @return
   */
  public abstract LocalDate getBegin();
  
  /**
   * La fine del periodo.
   * 
   * @return
   */
  public abstract Optional<LocalDate> getEnd();
  
}