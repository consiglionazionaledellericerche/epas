package models.absences;

import lombok.Getter;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Audited
@Entity
@Table(name = "absence_types_justified_behaviours")
public class AbsenceTypeJustifiedBehaviour extends BaseModel {

  private static final long serialVersionUID = -3532986170397408935L;

  public enum JustifiedTypeBehaviour {
   no_overtime,
   reduce_overtime;
  }

  @Required
  @ManyToOne
  @JoinColumn(name = "absence_type_id")
  public AbsenceType absenceType;
  
  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "justified_behaviour_id")
  public JustifiedBehaviour justifiedBehaviour;
  
  @Getter
  @Column
  public Integer data;

}
