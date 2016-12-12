package models.absences;

import com.google.common.base.Joiner;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.Getter;

import models.Qualification;
import models.absences.GroupAbsenceType.GroupAbsenceTypePattern;
import models.base.BaseModel;
import models.enumerate.QualificationMapping;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "absence_types")
@Audited
public class AbsenceType extends BaseModel {

  private static final long serialVersionUID = 7157167508454574329L;

  @ManyToMany
  public List<Qualification> qualifications = Lists.newArrayList();

  @Getter
  @Required
  public String code;

  @Column(name = "certification_code")
  public String certificateCode;

  public String description;

  @Column(name = "valid_from")
  public LocalDate validFrom;

  @Column(name = "valid_to")
  public LocalDate validTo;

  @Column(name = "internal_use")
  public boolean internalUse = false;
  
  @Getter
  @Column(name = "considered_week_end")
  public boolean consideredWeekEnd = false;
  
  @Getter
  @Column(name = "time_for_mealticket")
  public boolean timeForMealTicket = false;
  
  @Getter
  @Column(name = "justified_time")
  public Integer justifiedTime;
  
  @Getter
  @ManyToMany
  @JoinTable(name = "absence_types_justified_types", 
      joinColumns = { @JoinColumn(name = "absence_types_id") }, 
      inverseJoinColumns = { @JoinColumn(name = "justified_types_id") })
  public Set<JustifiedType> justifiedTypesPermitted = Sets.newHashSet();
  
  @Getter
  @Column(name = "replacing_time")
  public Integer replacingTime;
  
  @Getter
  @ManyToOne
  @JoinColumn(name = "replacing_type_id")
  public JustifiedType replacingType;
  
  @OneToMany(mappedBy = "absenceType")
  @LazyCollection(LazyCollectionOption.EXTRA)
  public Set<Absence> absences = Sets.newHashSet();

  @ManyToMany(mappedBy = "takenCodes")
  public Set<TakableAbsenceBehaviour> takenGroup = Sets.newHashSet();

  @ManyToMany(mappedBy = "takableCodes")
  public Set<TakableAbsenceBehaviour> takableGroup = Sets.newHashSet();
  
  @ManyToMany(mappedBy = "complationCodes")
  public Set<ComplationAbsenceBehaviour> complationGroup = Sets.newHashSet();
  
  @ManyToMany(mappedBy = "replacingCodes")
  public Set<ComplationAbsenceBehaviour> replacingGroup = Sets.newHashSet();
  
  // Metodi
  
  /**
   * Descrizione limitata a 60 caratteri.
   * @return short description
   */
  @Transient
  public String getShortDescription() {
    if (description != null && description.length() > 60) {
      return description.substring(0, 60) + "...";
    }
    return description;
  }
  
  /**
   * Se il codice è scaduto.
   * @return esito
   */
  @Transient
  public boolean isExpired() {
    if (validTo == null) {
      return false;
    }
    return LocalDate.now().isAfter(validTo);
  }

  @Override
  public String toString() {
    return Joiner.on(" - ").skipNulls().join(code, description);
  }
  
  /**
   * Se fra i tipi giustificativi c'è quello all day.
   * @return esito
   */
  @Transient
  public boolean isAllDayPermitted() {
    for (JustifiedType justifiedType: this.justifiedTypesPermitted) {
      if (justifiedType.name.equals(JustifiedType.JustifiedTypeName.all_day)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Se fra i tipi giustificativi c'è quello absence type minutes.
   * @return esito
   */  
  @Transient
  public boolean isAbsenceTypeMinutesPermitted() {
    for (JustifiedType justifiedType: this.justifiedTypesPermitted) {
      if (justifiedType.name.equals(JustifiedType.JustifiedTypeName.absence_type_minutes)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Se fra i tipi giustificativi c'è quello specified minutes.
   * @return esito
   */
  @Transient
  public boolean isSpecifiedMinutesPermitted() {
    for (JustifiedType justifiedType: this.justifiedTypesPermitted) {
      if (justifiedType.name.equals(JustifiedType.JustifiedTypeName.specified_minutes)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Se fra i tipi giustificativi c'è quello nothing.
   * @return esito
   */
  @Transient
  public boolean isNothingPermitted() {
    for (JustifiedType justifiedType: this.justifiedTypesPermitted) {
      if (justifiedType.name.equals(JustifiedType.JustifiedTypeName.nothing)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Se il codice di assenza è utilizzabile per tutte le qualifiche del mapping.
   * @param mapping mapping
   * @return esito
   */
  @Transient
  public boolean isQualificationMapping(QualificationMapping mapping) {
    Set<Integer> set = ContiguousSet.create(mapping.getRange(), 
        DiscreteDomain.integers());
    Set<Integer> actuals = Sets.newHashSet();
    for (Qualification qualification : qualifications) {
      actuals.add(qualification.qualification);
    }
    for (Integer item : set) {
      if (!actuals.contains(item)) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * I gruppi coinvolti dal tipo assenza.
   * 
   * @param onlyProgrammed non filtrare i soli programmati
   * @return entity set
   */
  public Set<GroupAbsenceType> involvedGroupAbsenceType(boolean onlyProgrammed) {

    //TODO: da fare la fetch perchè è usato in tabellone timbrature per ogni codice assenza.
    
    Set<GroupAbsenceType> groups = Sets.newHashSet();
    for (TakableAbsenceBehaviour behaviour : this.takableGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    for (TakableAbsenceBehaviour behaviour : this.takenGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    for (ComplationAbsenceBehaviour behaviour : this.complationGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    for (ComplationAbsenceBehaviour behaviour : this.replacingGroup) {
      groups.addAll(behaviour.groupAbsenceTypes);
    }
    if (!onlyProgrammed) {
      return groups;
    }
    Set<GroupAbsenceType> filteredGroup = Sets.newHashSet();
    for (GroupAbsenceType groupAbsenceType : groups) {
      if (groupAbsenceType.pattern.equals(GroupAbsenceTypePattern.programmed)) {
        filteredGroup.add(groupAbsenceType);
      }
    }
    return filteredGroup;
  }
  
  /**
   * Se il codice è coinvolto solo in gruppi semplici.
   * @return esito
   */
  public boolean onlySimpleGroupInvolved() {
    for (GroupAbsenceType group : involvedGroupAbsenceType(false)) {
      if (group.pattern == GroupAbsenceTypePattern.simpleGrouping) {
        continue;
      }
      return false;
    }
    return true;
  }
  
}
