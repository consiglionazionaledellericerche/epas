package models.absences;

import models.base.BaseModel;

import org.assertj.core.util.Lists;
import org.hibernate.envers.Audited;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Audited
@Entity
@Table(name = "justified_types")
public class JustifiedType extends BaseModel {

  public enum JustifiedTypeName {
    nothing, 
    absence_type_minutes, 
    specified_minutes, 
    missing_time, 
    half_day, 
    all_day, 
    assign_all_day,
    
  }
  
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
