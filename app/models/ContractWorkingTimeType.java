package models;

import com.google.common.base.Optional;

import models.base.IPeriodModel;
import models.base.PeriodModel;

import org.joda.time.LocalDate;

import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;


/**
 * Un periodo contrattuale.
 *
 * @author alessandro
 */
@Entity
@Table(name = "contracts_working_time_types")
public class ContractWorkingTimeType extends PeriodModel implements
        Comparable<ContractWorkingTimeType>, IPeriodModel {

  private static final long serialVersionUID = 3730183716240278997L;

  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contract_id")
  public Contract contract;

  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "working_time_type_id")
  public WorkingTimeType workingTimeType;

  @Required
  @Column(name = "begin_date")
  public LocalDate beginDate;

  @Column(name = "end_date")
  public LocalDate endDate;

  @Transient
  public LocalDate recomputeFrom = null;


  /**
   * Comparator ContractWorkingTimeType.
   */
  @Override
  public int compareTo(ContractWorkingTimeType compareCwtt) {
    if (beginDate.isBefore(compareCwtt.beginDate)) {
      return -1;
    } else if (beginDate.isAfter(compareCwtt.beginDate)) {
      return 1;
    } else {
      return 0;
    }
  }


  @Override
  public Contract getContract() {
    return this.contract;
  }


  @Override
  public LocalDate getBegin() {
    return this.beginDate;
  }


  @Override
  public Optional<LocalDate> getEnd() {
    return Optional.fromNullable(this.endDate);
  }
}
