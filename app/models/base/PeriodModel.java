package models.base;

import it.cnr.iit.epas.DateInterval;

import java.util.Comparator;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import org.joda.time.LocalDate;

@MappedSuperclass
public abstract class PeriodModel extends BaseModel 
    implements IPeriodModel, Comparable<PeriodModel> {

  private static final long serialVersionUID = 701063571599514955L;

  @Getter
  @Setter
  @NotNull
  @Column(name = "begin_date")
  public LocalDate beginDate;

  @Getter
  @Setter
  @Column(name = "end_date")
  public LocalDate endDate;

  private Comparator<PeriodModel> comparator() {
    return Comparator.comparing(
        PeriodModel::getBeginDate, Comparator.nullsFirst(LocalDate::compareTo))
        .thenComparing(PeriodModel::getId, Comparator.nullsFirst(Long::compareTo));
  }
  
  @Override
  public int compareTo(PeriodModel other) {
    return comparator().compare(this, other);
  }

  @Override
  public LocalDate calculatedEnd() {
    return endDate;
  }
  
  @Override
  public DateInterval periodInterval() {
    return new DateInterval(this.getBeginDate(), this.calculatedEnd()); 
  }

}
