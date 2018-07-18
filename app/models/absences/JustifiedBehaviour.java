package models.absences;

import com.google.common.collect.Sets;

import lombok.Getter;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Audited
@Entity
@Table(name = "justified_behaviours")
public class JustifiedBehaviour extends BaseModel {

  private static final long serialVersionUID = -3532986170397408935L;

  public enum JustifiedBehaviourName {
    minimumTime,
    maximumTime,
    percentageTime,
    no_overtime,
    reduce_overtime;
  }

  @Getter
  @Column
  @Enumerated(EnumType.STRING)
  public JustifiedBehaviourName name;
  
  @OneToMany(mappedBy = "justifiedBehaviour")
  public Set<AbsenceTypeJustifiedBehaviour> absenceTypesJustifiedBehaviours = Sets.newHashSet();

  @Override
  public String toString() {
    return this.name.name();
  }

}
