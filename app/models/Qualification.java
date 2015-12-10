package models;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import play.data.validation.Required;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;


/**
 * @author dario
 */
@Entity
@Audited
@Table(name = "qualifications")
public class Qualification extends BaseModel {

  private static final long serialVersionUID = 7147378609067987191L;

  @OneToMany(mappedBy = "qualification")
  public List<Person> person;

  @ManyToMany(mappedBy = "qualifications")
  public List<AbsenceType> absenceTypes;

  @Required
  public int qualification;

  @Required
  public String description;

  @Override
  public String getLabel() {
    return this.description;
  }

  @Override
  public String toString() {
    return getLabel();
  }

}
