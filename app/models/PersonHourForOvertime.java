package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.db.jpa.Model;

@Table(name="person_hour_for_overtime")
@Entity
public class PersonHourForOvertime extends Model{

	/**
	 * numero di ore assegnato (viene modificato mese per mese) di straordinari per quella persona che è responsabile di gruppo
	 */
	public Integer numberOfHourForOvertime;
	
	@OneToOne
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
