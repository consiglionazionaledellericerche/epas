package models.base;

import com.google.common.base.Optional;

import models.Contract;
import models.ContractWorkingTimeType;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

@MappedSuperclass
public abstract class PeriodModel extends BaseModel implements IPeriodModel,  Comparable<PeriodModel> {
  
  /**
   * Contiene l'informazione se all'interno del periodo vi è la prima data da ricalcolare.
   */
  public LocalDate recomputeFrom; 
  
  @Override
  public int compareTo(PeriodModel o) {
    if (getBegin().isBefore(o.getBegin())) {
      return -1;
    } else if (getBegin().isAfter(o.getBegin())) {
      return 1;
    } else {
      return 0;
    }
  }
  
}