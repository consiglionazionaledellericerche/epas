package models;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;


/**
 * Rappresenta un giorno di reperibilità di una persona reperibile
 *
 * @author cristian
 */
@Audited
@Entity
@Table(name = "person_reperibility_days", uniqueConstraints = {@UniqueConstraint(columnNames = {"person_reperibility_id", "date"})})
public class PersonReperibilityDay extends BaseModel {

  private static final long serialVersionUID = 6170327692153445002L;

  @Required
  @ManyToOne
  @JoinColumn(name = "person_reperibility_id", nullable = false)
  public PersonReperibility personReperibility;

  @Required

  public LocalDate date;

  @Column(name = "holiday_day")
  public Boolean holidayDay;

  @ManyToOne
  @JoinColumn(name = "reperibility_type")
  public PersonReperibilityType reperibilityType;
}
