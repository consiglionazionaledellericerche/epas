package models.base;

import com.google.common.base.Optional;

import it.cnr.iit.epas.DateUtility;

import lombok.Getter;
import lombok.Setter;

import org.joda.time.LocalDate;

import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public abstract class PeriodModel extends BaseModel implements IPeriodModel, Comparable<PeriodModel> {

  @Getter
  @Setter
  @Required
  @Column(name = "begin_date")
  public LocalDate beginDate;

  @Getter
  @Setter
  @Column(name = "end_date")
  public LocalDate endDate;

  @Override
  public int compareTo(PeriodModel other) {
    if (other == null || beginDate == null) {
      return 0;
    }
    return beginDate.compareTo(other.beginDate);
  }
  
  @Override
  public LocalDate calculatedEnd() {
    return endDate;
  }

}