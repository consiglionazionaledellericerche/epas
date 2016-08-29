package models.absences;

import com.google.common.collect.Sets;

import lombok.Getter;

import models.PersonDay;
import models.absences.JustifiedType.JustifiedTypeName;
import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.db.jpa.Blob;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;


/**
 * @author dario
 */
@Audited
@Entity
@Table(name = "absences")
public class Absence extends BaseModel {

  private static final long serialVersionUID = -1963061850354314327L;

  // Vecchia Modellazione (da rimuovere)
  
  @Getter
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "personDay_id", nullable = false)
  public PersonDay personDay;

  // Nuova Modellazione
  
  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "absence_type_id")
  public AbsenceType absenceType;
  
  @Column(name = "absence_file", nullable = true)
  public Blob absenceFile;

  @Column(name = "justified_minutes", nullable = true)
  public Integer justifiedMinutes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "justified_type_id")
  public JustifiedType justifiedType;
  
  @NotAudited
  @OneToMany(mappedBy = "absence", cascade = {CascadeType.REMOVE})
  public Set<AbsenceTrouble> troubles = Sets.newHashSet();
  
  // TODO: spostare la relazione dal person day alla person e persistere il campo date.

  /** Data da valorizzare in caso di assenza non persistita per simulazione */
  @Getter
  @Transient
  public LocalDate date;
  
  @Transient
  public LocalDate getAbsenceDate() {
    if (this.personDay != null && this.personDay.date != null) { 
      return this.personDay.date;
    } 
    if (this.date != null) {
      return this.date;
    }
    throw new IllegalStateException();
  }
  
  @Transient
  public int justifiedTime() {
    if (this.justifiedType != null) {
      if (this.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
        return this.absenceType.justifiedTime;
      }
      if (this.justifiedType.name.equals(JustifiedTypeName.specified_minutes)) {
        if (this.justifiedMinutes == null) {
          this.justifiedMinutes = -1;
          this.save();
        }
        return this.justifiedMinutes;
      }
    } else {
      //TODO: la vecchia implementazione..
    }
    return 0;
  }
  
  @Transient
  public boolean nothingJustified() {
    if (this.justifiedType != null) {
      if (this.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes) 
          && this.absenceType.justifiedTime == 0) {
        return true;
      }
      if (this.justifiedType.name.equals(JustifiedTypeName.nothing)) {
        return true;
      }
    } else {
      //TODO: la vecchia implementazione..
    }
    return false;
  }

  @Transient 
  public boolean justifiedAllDay() {
    if (this.justifiedType != null) {
      if (this.justifiedType.name.equals(JustifiedTypeName.all_day)) {
        return true;
      }
    }else {
      //TODO: la vecchia implementazione..
    }
    return false;

  }

  @Override
  public String toString() {
    return String.format("Absence[%d] - personDay.id = %d, absenceType.id = %s",
            id, personDay.id, absenceType.id);
  }
}
