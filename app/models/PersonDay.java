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

package models;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import models.absences.Absence;
import models.base.BaseModel;
import models.enumerate.MealTicketBehaviour;
import models.enumerate.Troubles;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import play.data.validation.Required;


/**
 * Classe che rappresenta un giorno, sia esso lavorativo o festivo di una persona.
 *
 * @author Cristian Lucchesi
 * @author Dario Tagliaferri
 */
@Entity
@Audited
@Table(name = "person_days",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"person_id", "date"})})
@Getter
@Setter
@Slf4j
public class PersonDay extends BaseModel {

  private static final long serialVersionUID = -5013385113251848310L;

  @Required
  @ManyToOne(optional = false)
  @JoinColumn(name = "person_id", nullable = false)
  public Person person;

  @Getter
  @Required
  public LocalDate date;

  @Column(name = "time_at_work")
  public Integer timeAtWork = 0;

  /**
   * Tempo all'interno di timbrature valide.
   */
  @Column(name = "stamping_time")
  public Integer stampingsTime = 0;
  
  /**
   * Tempo lavorato al di fuori della fascia apertura/chiusura.
   */
  @Column(name = "out_opening")
  public Integer outOpening = 0;
  
  /**
   * Tempo lavorato al di fuori della fascia apertura/chiusura ed approvato.
   */
  @Column(name = "approved_out_opening")
  public Integer approvedOutOpening = 0;

  /**
   * Tempo giustificato da assenze che non contribuiscono al tempo per buono pasto.
   */
  @Column(name = "justified_time_no_meal")
  public Integer justifiedTimeNoMeal = 0;

  /**
   * Tempo giustificato da assenze che contribuiscono al tempo per buono pasto.
   */
  @Column(name = "justified_time_meal")
  public Integer justifiedTimeMeal = 0;
  
  /**
   * Tempo giustificato per uscita/ingresso da zone diverse opportunamente definite.
   */
  @Column(name = "justified_time_between_zones")
  public Integer justifiedTimeBetweenZones = 0;
  
  /**
   * Tempo di lavoro in missione. Si può aggiungere in fase di modifica del codice missione 
   * dal tabellone timbrature.
   */
  @Column(name = "working_time_in_mission")
  public Integer workingTimeInMission = 0;

  public Integer difference = 0;

  public Integer progressive = 0;

  /**
   * Minuti tolti per pausa pranzo breve.
   */
  @Column(name = "decurted_meal")
  public Integer decurtedMeal = 0;

  @Column(name = "is_ticket_available")
  public boolean isTicketAvailable;

  @Column(name = "is_ticket_forced_by_admin")
  public boolean isTicketForcedByAdmin;

  @Column(name = "is_working_in_another_place")
  public boolean isWorkingInAnotherPlace;

  @Column(name = "is_holiday")
  public boolean isHoliday;
  
  /**
   * Tempo lavorato in un giorno di festa.
   */
  @Column(name = "on_holiday")
  public Integer onHoliday = 0;
  
  /**
   * Tempo lavorato in un giorni di festa ed approvato.
   */
  @Column(name = "approved_on_holiday")
  public Integer approvedOnHoliday = 0;

  @OneToMany(mappedBy = "personDay", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
  @OrderBy("date ASC")
  public List<Stamping> stampings = new ArrayList<Stamping>();

  @OneToMany(mappedBy = "personDay", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
  public List<Absence> absences = new ArrayList<Absence>();

  @NotAudited
  @OneToMany(mappedBy = "personDay", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
  public List<PersonDayInTrouble> troubles = new ArrayList<PersonDayInTrouble>();
  
  @ManyToOne
  @JoinColumn(name = "stamp_modification_type_id")
  public StampModificationType stampModificationType;

  @Transient
  public MealTicket mealTicketAssigned;

  @Transient
  public boolean isConsideredExitingNow;

  /**
   * Costruttore.
   *
   * @param person      person
   * @param date        date
   * @param timeAtWork  timeAtWork
   * @param difference  difference
   * @param progressive progressive
   */
  public PersonDay(Person person, LocalDate date,
      int timeAtWork, int difference, int progressive) {
    this.person = person;
    this.date = date;
    this.timeAtWork = timeAtWork;
    this.difference = difference;
    this.progressive = progressive;
  }

  /**
   * Costruttore.
   *
   * @param person person
   * @param date   date
   */
  public PersonDay(Person person, LocalDate date) {
    this(person, date, 0, 0, 0);
  }

  /**
   * Controlla che il personDay cada nel giorno attuale.
   */
  public boolean isToday() {
    return this.date.isEqual(LocalDate.now());
  }

  /**
   * Controlla se la data del personDay è passata rispetto a LocalDate.now().
   *
   * @return true se la data del personDay è passata, false altrimenti.
   */
  public boolean isPast() {
    return this.date.isBefore(LocalDate.now());
  }

  /**
   * Controlla se la data del personDay è futura rispetto a LocalDate.now().
   *
   * @return true se la data del personDay è futura, false altrimenti.
   */
  public boolean isFuture() {
    return this.date.isAfter(LocalDate.now());
  }

  /**
   * Orario decurtato perchè effettuato fuori dalla fascia di apertura/chiusura.
   */
  @Transient
  public int getDecurtedWork() {

    return this.outOpening - this.approvedOutOpening;
  }
  
  /**
   * Orario decurtato perchè effettuato in un giorno di festa.
   */
  @Transient
  public int getDecurtedWorkOnHoliday() {

    return this.onHoliday - this.approvedOnHoliday;
  }

  /**
   * Il tempo assegnabile è quello a lavoro meno i giustificativi.
   * assignableTime = timeAtWork - justifiedTimeMeal - justifiedTimeNoMeal
   */
  @Transient
  public int getAssignableTime() {
    return this.timeAtWork - this.justifiedTimeMeal - this.justifiedTimeNoMeal;
  }
  
  /**
   * 
   * @param mealTicketBehaviour
   */
  @Transient
  public void setTicketAvailable(MealTicketBehaviour mealTicketBehaviour) {
    switch (mealTicketBehaviour) {
      case allowMealTicket:
        this.isTicketAvailable = true;
        break;
      case notAllowMealTicket:
        this.isTicketAvailable = false;
        break;
      case preventMealTicket:
        this.isTicketAvailable = false;
        break;
        default:
          break;        
    }
  }


  /**
   * metodo che resetta un personday azzerando i valori in esso contenuti.
   */
  @Transient
  public void reset() {
    long id = this.id;
    try {
      BeanUtils.copyProperties(this, new PersonDay(this.person, this.date));
      this.id = id;
      this.save();
    } catch (IllegalAccessException iae) {
      log.error("Impossibile accedere all'istanza dell'oggetto {}", this.getClass());
    } catch (InvocationTargetException ite) {
      log.error("Errore sulla chiamata del metodo");
    }
  }
  
  @Transient
  public boolean hasError(Troubles trouble) {
    return this.troubles.stream().anyMatch(error -> error.cause == trouble);
  }
  
  @Override
  public String toString() {
    return String.format(
        "PersonDay[%d] - person.id = %d, date = %s, difference = %s, isTicketAvailable = %s, "
            + "isTicketForcedByAdmin = %s, modificationType = %s, "
            + "progressive = %s, timeAtWork = %s",
        id, person.id, date, difference, isTicketAvailable, isTicketForcedByAdmin,
        stampModificationType, progressive, timeAtWork);
  }

}
