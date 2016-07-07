package models;

import models.absences.AbsenceType;
import models.base.BaseModel;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;

import org.hibernate.envers.Audited;

import play.data.validation.Required;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * @author dario
 */
@Audited
@Entity
@Table(name = "absence_type_groups")
public class AbsenceTypeGroup extends BaseModel {

  private static final long serialVersionUID = -8664634519147481684L;

  @OneToMany(mappedBy = "absenceTypeGroup")
  public List<AbsenceType> absenceTypes;

  public String description;
  
  @Required
  public String label;

  @Column(name = "minutes_excess")
  public Boolean minutesExcess;

  @Column(name = "limit_in_minute")
  public Integer limitInMinute;

  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "accumulation_type")
  public AccumulationType accumulationType;

  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "accumulationBehaviour")
  public AccumulationBehaviour accumulationBehaviour;

  @OneToOne
  @JoinColumn(name = "replacing_absence_type_id")
  public AbsenceType replacingAbsenceType;
  
  public String toString() {
    return label;
  }
}
