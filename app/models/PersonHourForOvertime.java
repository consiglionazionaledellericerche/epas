package models;

import models.base.BaseModel;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;



@Table(name="person_hour_for_overtime")
@Entity
public class PersonHourForOvertime extends BaseModel{

	private static final long serialVersionUID = -298105801035472529L;

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
