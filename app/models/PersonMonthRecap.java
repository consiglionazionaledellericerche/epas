package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.joda.time.LocalDate;

import play.data.validation.Required;

import com.google.common.base.Optional;

import dao.PersonMonthRecapDao;


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


	
	public static PersonMonthRecap build(Person person, int year, int month){
		PersonMonthRecap pmr = null;
		Optional<PersonMonthRecap> pm = PersonMonthRecapDao.getPersonMonthRecapByPersonYearAndMonth(person, year, month);
//		PersonMonthRecap pm = PersonMonthRecap.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
//				person, month, year).first();
		if(!pm.isPresent()){
			pmr = new PersonMonthRecap(person, year, month);
			pmr.create();			
		}
		else
			pmr = pm.get();
		//pm.aggiornaRiepiloghi();
		return pmr;

	}

	
	public static PersonMonthRecap getInstance(Person person, int year, int month) {
		PersonMonthRecap pmr = null;
		Optional<PersonMonthRecap> personMonth = PersonMonthRecapDao.getPersonMonthRecapByPersonYearAndMonth(person, year, month);
		//PersonMonthRecap personMonth = PersonMonthRecap.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", person, year, month).first();
		if (!personMonth.isPresent()) {
			pmr = new PersonMonthRecap(person, year, month);
		}
		else
			pmr = personMonth.get();
		return pmr;
	}



		

	
}
