package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import models.base.BaseModel;



@Table(name="person_hour_for_overtime")
@Entity
public class PersonHourForOvertime extends BaseModel{

	/**
	 * numero di ore assegnato (viene modificato mese per mese) di straordinari per quella persona che è responsabile di gruppo
	 */
	public Integer numberOfHourForOvertime;
	
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "person_id")
	public Person person;
	
	
	public PersonHourForOvertime(Person person, Integer numberOfHourForOvertime) {
		this.person = person;
		this.numberOfHourForOvertime = numberOfHourForOvertime;
	}


	public Integer getNumberOfHourForOvertime() {
		return numberOfHourForOvertime;
	}


	public void setNumberOfHourForOvertime(Integer numberOfHourForOvertime) {
		this.numberOfHourForOvertime = numberOfHourForOvertime;
	}
}
