package models.absences;

import lombok.Getter;

import models.PersonDay;
import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.db.jpa.Blob;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "justified_type_id")
  private JustifiedType justifiedType;
  
  // TODO: spostare la relazione dal person day alla person e persistere il campo date.

  /** Data da valorizzare in caso di assenza non persistita per simulazione */
  @Getter
  @Transient
  public LocalDate date;

  @Override
  public String toString() {
    return String.format("Absence[%d] - personDay.id = %d, absenceType.id = %s",
            id, personDay.id, absenceType.id);
  }
}
