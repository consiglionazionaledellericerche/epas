package models;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import play.data.validation.Required;
import play.data.validation.Unique;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;


/**
 * Tipo di reperibilità.
 * 
 * @author cristian
 */
@Audited
@Entity
@Table(name = "person_reperibility_types")
public class PersonReperibilityType extends BaseModel {

  private static final long serialVersionUID = 3234688199593333012L;

  @Required
  @Unique
  public String description;

  @OneToMany(mappedBy = "personReperibilityType")
  public List<PersonReperibility> personReperibilities;

  /* responsabile della reperibilità */
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "supervisor")
  @Required
  public Person supervisor;
  
  public boolean disabled;
  
  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "office_id")
  @NotNull
  public Office office; 
 

  @Override
  public String toString() {
    return String.format("PersonReperibilityType[%d] - description = %s", id, description);
  }
}
