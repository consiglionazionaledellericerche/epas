package models.absences;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Audited
@Entity
@Table(name = "complation_absence_behaviours")
public class ComplationAbsenceBehaviour extends BaseModel {

  @Column(name = "name")
  public String name;
  
  @Column(name = "amount_type")
  @Enumerated(EnumType.STRING)
  public AmountType amountType;

  @ManyToMany
  @JoinTable(name = "complation_codes_group", 
  joinColumns = { @JoinColumn(name = "complation_behaviour_id") }, 
  inverseJoinColumns = { @JoinColumn(name = "absence_types_id") })
  public Set<AbsenceType> complationCodes;

  @ManyToMany
  @JoinTable(name = "replacing_codes_group", 
  joinColumns = { @JoinColumn(name = "complation_behaviour_id") }, 
  inverseJoinColumns = { @JoinColumn(name = "absence_types_id") })

  public Set<AbsenceType> replacingCodes;
  


}
