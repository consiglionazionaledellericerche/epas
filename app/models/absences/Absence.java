package models.absences;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.Getter;

import models.Person;
import models.PersonDay;
import models.absences.JustifiedType.JustifiedTypeName;
import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.db.jpa.Blob;

import java.util.List;
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

  @Getter
  @Column(name = "justified_minutes", nullable = true)
  public Integer justifiedMinutes;

  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "justified_type_id")
  public JustifiedType justifiedType;
  
  @NotAudited
  @OneToMany(mappedBy = "absence", cascade = {CascadeType.REMOVE})
  public Set<AbsenceTrouble> troubles = Sets.newHashSet();
  
  @Override
  public String toString() {
    if (personDay == null) {
      return this.getAbsenceDate() + " - " + this.getAbsenceType().code;
    }
    return this.getPersonDay().person.fullName() 
        + " - " + this.getAbsenceDate() 
        + " - " + this.getAbsenceType().code;
  }
  
  // TODO: spostare la relazione dal person day alla person e persistere il campo date.

  //Data da valorizzare in caso di assenza non persistit per simulazione
  @Getter
  @Transient
  public LocalDate date;
  
  /**
   * Getter per la data assenza.
   * @return data
   */
  @Transient
  public LocalDate getAbsenceDate() {
    if (this.getPersonDay() != null && this.getPersonDay().getDate() != null) { 
      return this.getPersonDay().getDate();
    } 
    if (this.getDate() != null) {
      return this.getDate();
    }
    throw new IllegalStateException();
  }
  
  /**
   * Il tempo giustificato dall'assenza.
   * @return minuti
   */
  @Transient
  public int justifiedTime() {
    if (this.justifiedType == null) {
      throw new IllegalStateException();
    }
    if (this.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
      return this.absenceType.justifiedTime;
    }
    if (this.justifiedType.name.equals(JustifiedTypeName.specified_minutes) 
        || this.justifiedType.name.equals(JustifiedTypeName.specified_minutes_limit)) {
      if (this.justifiedMinutes == null) {
        throw new IllegalStateException();
      }
      return this.justifiedMinutes;
    }
    return 0;
  }
  
  /**
   * Se l'assenza non giustifica niente.
   * @return esito
   */
  @Transient
  public boolean nothingJustified() {
    if (this.justifiedType == null) {
      throw new IllegalStateException();
    }
    if (this.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes) 
        && this.absenceType.justifiedTime == 0) {
      return true;
    }
    if (this.justifiedType.name.equals(JustifiedTypeName.nothing)) {
      return true;
    }
    return false;
  }

  /**
   * Se l'assenza giustifica tutto il giorno.
   * @return esito
   */
  @Transient 
  public boolean justifiedAllDay() {
    if (this.justifiedType == null) {
      throw new IllegalStateException();
    }
    if (this.justifiedType.name.equals(JustifiedTypeName.all_day)) {
      return true;
    }
    return false;
  }
  
  /**
   * Se l'assenza giustifica niente ma scala il giorno dal limite.
   * @return esito
   */
  @Transient 
  public boolean justifiedAllDayLimit() {
    if (this.justifiedType == null) {
      throw new IllegalStateException();
    }
    if (this.justifiedType.name.equals(JustifiedTypeName.all_day_limit)) {
      return true;
    }
    return false;
  }
  
  /**
   * Le altre assenze con ruolo di rimpiazzamento nel giorno per quel gruppo.
   * @param groupAbsenceType gruppo
   * @return lista di assenze.
   */
  @Transient
  public List<Absence> replacingAbsences(GroupAbsenceType groupAbsenceType) {
    if (this.personDay == null || !this.personDay.isPersistent()) {
      return Lists.newArrayList();
    }
    List<Absence> replacings = Lists.newArrayList();
    for (Absence absence : this.personDay.absences) {
      if (absence.equals(this)) {
        continue;
      }
      if (groupAbsenceType.complationAbsenceBehaviour != null 
          && groupAbsenceType.complationAbsenceBehaviour.replacingCodes
          .contains(absence.absenceType)) {
        replacings.add(absence);
      }
    }
    return replacings;
  }
  
  /**
   * Se l'assenza ha un codice di rimpiazzamento nel giorno a lei associabile.
   * @return esito
   */
  @Transient
  public boolean hasReplacing() {
    for (ComplationAbsenceBehaviour complation : this.absenceType.complationGroup) {
      for (Absence absence : this.personDay.absences) {
        if (absence.equals(this)) {
          continue;
        }
        if (complation.replacingCodes.contains(absence.absenceType)) {
          return true;
        }
      }
    }
    return false;
  }
  
  /**
   * Se l'assenza ha un ruolo di rimpiazzamento ma nel giorno non esiste il completamento
   * che l'ha generata.
   * @param involvedGroups i gruppi da controllare 
   * @return esito
   */
  @Transient
  public boolean isOrphanReplacing(Set<GroupAbsenceType> involvedGroups) {
    for (GroupAbsenceType groupAbsenceType : involvedGroups) {
      if (groupAbsenceType.complationAbsenceBehaviour == null) {
        continue;
      }
      if (groupAbsenceType.complationAbsenceBehaviour.replacingCodes.contains(this.absenceType)) {
        for (Absence absence : this.personDay.absences) {
          if (absence.replacingAbsences(groupAbsenceType).contains(this)) {
            return false;
          }
        }
      }
    }
    return true;
  }
  
  /**
   * Se l'assenza ha il ruolo di rimpiazzamento per quei gruppi.
   * @param involvedGroups gruppi
   * @return esito
   */
  @Transient
  public boolean isReplacing(Set<GroupAbsenceType> involvedGroups) {
    for (GroupAbsenceType groupAbsenceType : involvedGroups) {
      if (groupAbsenceType.complationAbsenceBehaviour == null) {
        continue;
      }
      if (groupAbsenceType.complationAbsenceBehaviour.replacingCodes.contains(this.absenceType)) {
        return true;
      }
    }
    return true;
  }

  /**
   * Fondamentale per far funzionare alcune drools
   * @return Restituisce il proprietario della timbratura.
   */
  public Person getOwner() {
    return personDay.person;
  }

  /**
   * Utile per effettuare i controlli temporali sulle drools
   * @return il mese relativo alla data della timbratura.
   */
  public YearMonth getYearMonth() {
    return new YearMonth(personDay.date.getYear(),personDay.date.getMonthOfYear());
  }

  /**
   * Al momento viene usato solo nella drools EmployeeCanEditAbsence per fare le verifiche
   * sugli inserimenti delle assenze dei dipendenti.
   * Da rimuovere appena si crea il nuovo metodo che fa dei controlli utilizzando la nuova
   * modellazione dei gruppi dei codici di assenza
   *
   * @return la stringa del codice di assenza.
   */
  public String getCode() {
    return absenceType.code;
  }
}
