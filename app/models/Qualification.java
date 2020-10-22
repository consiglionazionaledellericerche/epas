package models;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.absences.AbsenceType;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import play.data.validation.Required;


/**
 * Qualifiche CNR (livello 1, 2, 3, 4, ...).
 * 
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

  @Getter
  @Setter
  @Required
  public int qualification;

  @Required
  public String description;

  /**
   * I livelli I-III.
   * 
   * @return
   */
  @Transient
  public boolean isTopQualification() {
    return qualification <= 3;
  }
  
  @Override
  public String getLabel() {
    return this.description;
  }

  @Override
  public String toString() {
    return getLabel();
  }

  
}
