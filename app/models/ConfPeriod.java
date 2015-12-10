package models;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Audited
@Entity
@Table(name = "conf_period")
public class ConfPeriod extends BaseModel {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id")
  public Office office;

  @Required
  @NotNull
  @Column(name = "date_from")
  public LocalDate dateFrom;

  @Column(name = "date_to")
  public LocalDate dateTo;

  @Column(name = "field")
  public String field;

  @Column(name = "field_value")
  public String fieldValue;

}
