package models;

import models.base.BaseModel;
import org.joda.time.LocalDate;
import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * @author cristian
 *
 */
//@Audited
@Table(name="person_months_recap")
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

	@Column(name="training_hours")
	public Integer trainingHours;

	@Column(name="hours_approved")
	public Boolean hoursApproved;

	/**
	 * aggiunta la date per test di getMaximumCoupleOfStampings ---da eliminarefromDate
	 * @param person
	 * @param year
	 * @param month
	 */
	public PersonMonthRecap(Person person, int year, int month){
		this.person = person;	
		this.year = year;
		this.month = month;

	}

}
