package models;

import lombok.Getter;

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
  public Integer timeAtWork;

  public Integer difference;

  public Integer progressive;

  /**
   * Minuti tolti per pausa pranzo breve.
   */
  public Integer decurted;

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
  public PersonDay previousPersonDayInMonth = null;

  @Transient
  public Contract personDayContract = null;

  @Transient
  public MealTicket mealTicketAssigned = null;


  public PersonDay(
      Person person, LocalDate date, int timeAtWork, int difference, int progressive) {
    this.person = person;
    this.date = date;
    this.timeAtWork = timeAtWork;
    this.difference = difference;
    this.progressive = progressive;
  }

  public PersonDay(Person person, LocalDate date) {
    this(person, date, 0, 0, 0);
  }

  /**
   * Controlla che il personDay cada nel giorno attuale.
   */
  public boolean isToday() {
    return this.date.isEqual(new LocalDate());
  }

  @Override
  public String toString() {
    return String.format(
        "PersonDay[%d] - person.id = %d, date = %s, difference = %s, isTicketAvailable = %s, "
        + "modificationType = %s, progressive = %s, timeAtWork = %s",
        id, person.id, date, difference, isTicketAvailable, stampModificationType,
        progressive, timeAtWork);
  }

}
