package models;

import models.base.BaseModel;

import org.joda.time.LocalDate;

import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * @author dario
 * @author alessandro
 */
@Entity
@Table(name = "persons_working_time_types")
public class PersonWorkingTimeType extends BaseModel {

  private static final long serialVersionUID = 4762746376542370546L;

  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id")
  public Person person;

  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "working_time_type_id")
  public WorkingTimeType workingTimeType;


  @Column(name = "begin_date")
  public LocalDate beginDate;


  @Column(name = "end_date")
  public LocalDate endDate;
}
