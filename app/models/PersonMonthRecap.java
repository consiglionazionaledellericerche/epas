package models;

import com.google.common.base.MoreObjects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.joda.time.LocalDate;

import play.data.validation.Required;


/**
 * Entità ore di formazione.
 * 
 * @author cristian
 */
//@Audited
@Table(name = "person_months_recap")
@Entity
public class PersonMonthRecap extends BaseModel {

  private static final long serialVersionUID = -8423858325056981355L;

  @Required
  @ManyToOne(optional = false)
  @JoinColumn(name = "person_id", nullable = false, updatable = false)
  public Person person;


  public Integer year;

  public Integer month;


  public LocalDate fromDate;


  public LocalDate toDate;

  @Column(name = "training_hours")
  public Integer trainingHours;

  @Column(name = "hours_approved")
  public Boolean hoursApproved = false;
  
  /**
   * Costruisce un nuono oggetto di ore formazione.
   * @param person person
   * @param year anno
   * @param month mese
   */
  public PersonMonthRecap(Person person, int year, int month) {
    this.person = person;
    this.year = year;
    this.month = month;
  }
  
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(PersonMonthRecap.class)
        .add("person", person.fullName())
        .add("matricola", person.number)
        .add("year", year)
        .add("month", month)        
        .toString();
  }
  
  /**
   * Ritorna true se le ore si riferiscono al mese attuale od al mese precedente 
   * e non sono ancora state approvate.
   * 
   * @return se possono essere modificate.
   */
  public boolean isEditable() {
    
    if (hoursApproved) {
      return false;
    }
    
    LocalDate date = LocalDate.now();
    //mese attuale
    if (month == date.getMonthOfYear() && year == date.getYear()) { 
      return true;
    }
    //mese precedente
    if (month == date.minusMonths(1).getMonthOfYear() && year == date.minusMonths(1).getYear()) {
      return true;
    }
    
    return false;
  }

}
