/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package models.absences;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.Person;
import models.PersonDay;
import models.TimeVariation;
import models.absences.JustifiedBehaviour.JustifiedBehaviourName;
import models.absences.JustifiedType.JustifiedTypeName;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.data.validation.Unique;
import play.db.jpa.Blob;

/**
 * Modello per le assenze.
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "absences")
public class Absence extends BaseModel {

  private static final long serialVersionUID = -1963061850354314327L;

  // Vecchia Modellazione (da rimuovere)

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private PersonDay personDay;

  // Nuova Modellazione

  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  private AbsenceType absenceType;

  @Column(name = "absence_file", nullable = true)
  private Blob absenceFile;

  @Getter
  private Integer justifiedMinutes;

  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  private JustifiedType justifiedType;

  @NotAudited
  @OneToMany(mappedBy = "absence", cascade = {CascadeType.REMOVE})
  private Set<AbsenceTrouble> troubles = Sets.newHashSet();

  //Nuovo campo per la gestione delle missioni in caso di modifica delle date
  @Unique(value = "externalIdentifier,personDay")
  private Long externalIdentifier;

  //Nuovi campi per la possibilità di inserire le decurtazioni di tempo per i 91s
  private LocalDate expireRecoverDate;

  private int timeToRecover;

  @Audited
  @OneToMany(mappedBy = "absence", cascade = {CascadeType.ALL})
  private Set<TimeVariation> timeVariations = Sets.newHashSet();

  private String note;

  @NotAudited
  private LocalDateTime updatedAt;

  @PreUpdate
  @PrePersist
  private void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  @Override
  public String toString() {
    if (personDay == null) {
      return this.getAbsenceDate() + " - " + this.getAbsenceType().getCode();
    }
    return this.getPersonDay().getPerson().fullName()
        + " - " + this.getAbsenceDate()
        + " - " + this.getAbsenceType().getCode();
  }

  // TODO: spostare la relazione dal person day alla person e persistere il campo date.

  //Data da valorizzare in caso di assenza non persistit per simulazione
  @Getter
  @Transient
  public LocalDate date;

  /**
   * Getter per la data assenza.
   *
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
   *
   * @return minuti
   */
  @Transient
  public int justifiedTime() {
    if (this.justifiedType == null) {
      throw new IllegalStateException();
    }
    if (this.justifiedType.getName().equals(JustifiedTypeName.absence_type_minutes)) {
      return this.absenceType.getJustifiedTime();
    }
    if (this.justifiedType.getName().equals(JustifiedTypeName.specified_minutes)
        || this.justifiedType.getName().equals(JustifiedTypeName.specified_minutes_limit)) {
      if (this.justifiedMinutes == null) {
        throw new IllegalStateException();
      }
      return this.justifiedMinutes;
    }
    return 0;
  }

  /**
   * Se l'assenza non giustifica niente.
   *
   * @return esito
   */
  @Transient
  public boolean nothingJustified() {
    if (this.justifiedType == null) {
      throw new IllegalStateException();
    }
    if (this.justifiedType.getName().equals(JustifiedTypeName.absence_type_minutes)
        && this.absenceType.getJustifiedTime() == 0) {
      return true;
    }
    if (this.justifiedType.getName().equals(JustifiedTypeName.nothing)) {
      return true;
    }
    return false;
  }

  /**
   * Le altre assenze con ruolo di rimpiazzamento nel giorno per quel gruppo.
   *
   * @param groupAbsenceType gruppo
   * @return lista di assenze.
   */
  @Transient
  public List<Absence> replacingAbsences(GroupAbsenceType groupAbsenceType) {
    if (this.personDay == null || !this.personDay.isPersistent()) {
      return Lists.newArrayList();
    }
    List<Absence> replacings = Lists.newArrayList();
    for (Absence absence : this.personDay.getAbsences()) {
      if (absence.equals(this)) {
        continue;
      }
      if (groupAbsenceType.getComplationAbsenceBehaviour() != null
          && groupAbsenceType.getComplationAbsenceBehaviour().getReplacingCodes()
          .contains(absence.absenceType)) {
        replacings.add(absence);
      }
    }
    return replacings;
  }

  /**
   * Se l'assenza ha un codice di rimpiazzamento nel giorno a lei associabile.
   *
   * @return esito
   */
  @Transient
  public boolean hasReplacing() {
    for (ComplationAbsenceBehaviour complation : this.absenceType.getComplationGroup()) {
      for (Absence absence : this.personDay.getAbsences()) {
        if (absence.equals(this)) {
          continue;
        }
        if (complation.getReplacingCodes().contains(absence.absenceType)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Se l'assenza ha un ruolo di rimpiazzamento ma nel giorno non esiste il completamento che l'ha
   * generata.
   *
   * @param involvedGroups i gruppi da controllare
   * @return esito
   */
  @Transient
  public boolean isOrphanReplacing(Set<GroupAbsenceType> involvedGroups) {
    for (GroupAbsenceType groupAbsenceType : involvedGroups) {
      if (groupAbsenceType.getComplationAbsenceBehaviour() == null) {
        continue;
      }
      if (groupAbsenceType.getComplationAbsenceBehaviour().getReplacingCodes()
          .contains(this.absenceType)) {
        for (Absence absence : this.personDay.getAbsences()) {
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
   *
   * @param involvedGroups gruppi
   * @return esito
   */
  @Transient
  public boolean isReplacing(Set<GroupAbsenceType> involvedGroups) {
    for (GroupAbsenceType groupAbsenceType : involvedGroups) {
      if (groupAbsenceType.getComplationAbsenceBehaviour() == null) {
        continue;
      }
      if (groupAbsenceType.getComplationAbsenceBehaviour().getReplacingCodes()
          .contains(this.absenceType)) {
        return true;
      }
    }
    return true;
  }

  /**
   * Fondamentale per far funzionare alcune drools.
   *
   * @return Restituisce il proprietario della timbratura.
   */
  public Person getOwner() {
    return personDay.getPerson();
  }

  /**
   * Utile per effettuare i controlli temporali sulle drools.
   *
   * @return il mese relativo alla data della timbratura.
   */
  public YearMonth getYearMonth() {
    return new YearMonth(personDay.getDate().getYear(), personDay.getDate().getMonthOfYear());
  }

  /**
   * Al momento viene usato solo nella drools EmployeeCanEditAbsence per fare le verifiche sugli
   * inserimenti delle assenze dei dipendenti. Da rimuovere appena si crea il nuovo metodo che fa
   * dei controlli utilizzando la nuova modellazione dei gruppi dei codici di assenza
   *
   * @return la stringa del codice di assenza.
   */
  public String getCode() {
    return absenceType.getCode();
  }

  /**
   * Controlla se viene violata la quantità minima giustificabile.
   *
   * @return true se viene violata la quantità minima giustificabile, false altrimenti.
   */
  public boolean violateMinimumTime() {
    Optional<AbsenceTypeJustifiedBehaviour> behaviour =
        this.absenceType.getBehaviour(JustifiedBehaviourName.minimumTime);
    if (behaviour.isPresent()) {
      return behaviour.get().getData() > this.justifiedMinutes;
    }
    return false;
  }

  /**
   * Controlla se viene violata la quantità massima giustificabile.
   *
   * @return true se viene violata la quantità massima giustificabile, false altrimenti.
   */
  public boolean violateMaximumTime() {
    Optional<AbsenceTypeJustifiedBehaviour> behaviour =
        this.absenceType.getBehaviour(JustifiedBehaviourName.maximumTime);
    if (behaviour.isPresent()) {
      return behaviour.get().getData() < this.justifiedMinutes;
    }
    return false;
  }
}
