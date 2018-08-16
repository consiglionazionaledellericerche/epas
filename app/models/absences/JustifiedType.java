package models.absences;

import com.google.common.collect.Lists;

import lombok.Getter;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Audited
@Entity
@Table(name = "justified_types")
public class JustifiedType extends BaseModel {

  private static final long serialVersionUID = -3532986170397408935L;

  public enum JustifiedTypeName {

    nothing,

    // il tempo giustificato è definito dal tipo di assenza
    absence_type_minutes,
    half_day,
    all_day,
    all_day_percentage, // giustifica un giorno, scala una percentuale (ex. 661G)

    // il tempo giustificato è specificato nell'assenza.
    specified_minutes,
    missing_time,
    
    // assegna il tempo a lavoro come timbrature (ex telelavoro)
    assign_all_day,

    // scala una giornata dal limite (ex. congedi altro coniuge)
    all_day_limit,
        
    // i minuti specificati scalano dal limite e non dal tempo a lavoro (ex. permessi brevi)
    specified_minutes_limit,
    
    
    // altri (documentare)
    complete_day_and_add_overtime,
    recover_time;

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
