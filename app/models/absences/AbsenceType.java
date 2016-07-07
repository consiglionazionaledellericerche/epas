package models.absences;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.Getter;

import models.AbsenceTypeGroup;
import models.Qualification;
import models.base.BaseModel;
import models.enumerate.JustifiedTimeAtWork;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * @author dario
 */
@Entity
@Table(name = "absence_types")
@Audited
public class AbsenceType extends BaseModel {

  private static final long serialVersionUID = 7157167508454574329L;

  // Vecchia Modellazione (Da rimuovere)
  
  @ManyToOne
  @JoinColumn(name = "absence_type_group_id")
  public AbsenceTypeGroup absenceTypeGroup;

  @Required
  @Enumerated(EnumType.STRING)
  @Column(name = "justified_time_at_work")
  public JustifiedTimeAtWork justifiedTimeAtWork;
  
  // Nuova Modellazione
  
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

  @Column(name = "considered_week_end")
  public boolean consideredWeekEnd = false;
  
  @Column(name = "time_for_mealticket")
  public boolean timeForMealTicket = false;
  
  @Column(name = "justified_time")
  public Integer justifiedTime;
  
  @Column(name = "justified_type_permitted")
  @Enumerated(EnumType.STRING)
  public JustifiedTypePermitted justifiedTypePermitted;
  
  
  @OneToMany(mappedBy = "absenceType")
  @LazyCollection(LazyCollectionOption.EXTRA)
  public Set<Absence> absences = Sets.newHashSet();

  @ManyToMany(mappedBy = "takenCodes")
  public Set<TakableAbsenceBehaviour> takenGroup;

  @ManyToMany(mappedBy = "takableCodes")
  public Set<TakableAbsenceBehaviour> takableGroup;
  
  @ManyToMany(mappedBy = "complationCodes")
  public Set<ComplationAbsenceBehaviour> complationGroup;
  
  @ManyToMany(mappedBy = "replacingCodes")
  public Set<ComplationAbsenceBehaviour> replacingGroup;
  
  
  
  // Metodi
  
  @Transient
  public String getShortDescription() {
    if (description != null && description.length() > 60) {
      return description.substring(0, 60) + "...";
    }
    return description;
  }
  
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

  
  public enum JustifiedTypePermitted {
    absence_type, minutes, quelloCheManca;
  }
}
