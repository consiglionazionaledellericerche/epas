package models.absences;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

@Audited
@Entity
@Table(name = "justified_types")
public class JustifiedType extends BaseModel {

  private static final long serialVersionUID = -3532986170397408935L;

  public enum JustifiedTypeName {

    nothing,

    absence_type_minutes,
    specified_minutes,
    missing_time,
    half_day,
    all_day,

    assign_all_day,

    all_day_limit,
    absence_type_limit,
    specified_minutes_limit;

  }

  @Getter
  @Column
  @Enumerated(EnumType.STRING)
  public JustifiedTypeName name;

  @ManyToMany(mappedBy = "justifiedTypesPermitted")
  public Set<AbsenceType> absenceTypes;

  @OneToMany(mappedBy = "justifiedType")
  public List<Absence> absences = Lists.newArrayList();

  @Override
  public String toString() {
    return this.name.name();
  }

}
