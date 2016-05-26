package models;

import lombok.Getter;
import lombok.Setter;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

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


/**
 * Classe che rappresenta un giorno, sia esso lavorativo o festivo di una persona.
 *
 * @author cristian
 * @author dario
 */
@Entity
@Audited
@Table(name = "person_days",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"person_id", "date"})})
@Getter
@Setter
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
   * Tempo giustificato da assenze che non contribuiscono al tempo per buono pasto.
   */
  @Column(name = "justified_time_no_meal")
  public Integer justifiedTimeNoMeal = 0;

  /**
   * Tempo giustificato da assenze che contribuiscono al tempo per buono pasto.
   */
  @Column(name = "justified_time_meal")
  public Integer justifiedTimeMeal = 0;

  public Integer difference = 0;

  public Integer progressive = 0;

  /**
   * Minuti tolti per pausa pranzo breve.
   */
  public Integer decurted = 0;

  @Column(name = "is_ticket_available")
  public boolean isTicketAvailable = true;

  @Column(name = "is_ticket_forced_by_admin")
  public boolean isTicketForcedByAdmin = false;

  @Column(name = "accepted_holiday_working_time")
  public boolean acceptedHolidayWorkingTime = false;

  @Column(name = "is_working_in_another_place")
  public boolean isWorkingInAnotherPlace = false;

  @Column(name = "is_holiday")
  public boolean isHoliday = false;

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
  public MealTicket mealTicketAssigned = null;

  @Transient
  public boolean isConsideredExitingNow = false;
  
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
   * 
   * @return
   */
  public boolean isPast() {
    return this.date.isBefore(LocalDate.now());
  }
  
  /**
   * 
   * @return
   */
  public boolean isFuture() {
    return this.date.isAfter(LocalDate.now());
  }

  /**
   * Nel caso di orario effettuato fuori dalla finestra dell'orario sede.
   *
   * TODO: vedere se aggiungere una colonna invece di calcolare questo invariante.
   *
   * timeAtWork = StampingTime - decurtedWork - decurtedMeal + justifiedTimeMeal +
   * justifiedTimeNoMeal
   *
   * -->
   *
   * decurtedWork = StampingTime - timeAtWork - decurtedMeal + justifiedTimeMeal +
   * justifiedTimeNoMeal
   */
  @Transient
  public int getDecurtedWork() {

    if (stampingsTime == null || this.timeAtWork == null) {
      return 0;
    }

    int decurtedWork = stampingsTime - this.timeAtWork;

    if (decurted != null) {
      decurtedWork -= this.decurted;
    }
    if (justifiedTimeMeal != null) {
      decurtedWork += this.justifiedTimeMeal;
    }
    if (justifiedTimeNoMeal != null) {
      decurtedWork += this.justifiedTimeNoMeal;
    }

    return decurtedWork;
  }
  
  /**
   * Il tempo assegnabile è quello a lavoro meno i giustificativi.
   * assignableTime = timeAtWork - justifiedTimeMeal - justifiedTimeNoMeal
   * @return
   */
  @Transient
  public int getAssignableTime() {
    return this.timeAtWork - this.justifiedTimeMeal - this.justifiedTimeNoMeal;
  }
  
  @Override
  public String toString() {
    return String.format(
        "PersonDay[%d] - person.id = %d, date = %s, difference = %s, isTicketAvailable = %s, "
        + "isTicketForcedByAdmin = %s, modificationType = %s, progressive = %s, timeAtWork = %s",
        id, person.id, date, difference, isTicketAvailable, isTicketForcedByAdmin, 
        stampModificationType, progressive, timeAtWork);
  }

}
