package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.joda.time.DateTimeConstants;

import play.db.jpa.Model;

/**
 * 
 * @author dario
 * questa classe rappresenta il residuo annuale relativo a una certa persona per quel che riguarda le ore fatte in più (o in meno)
 * 
 */
@Audited
@Entity
@Table(name="person_years")
public class PersonYear extends Model{
	
	@ManyToOne
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	@Column
	public int year;
	@Column
	public int remainingDays;
	@Column
	public int remainingHours;
	
	public PersonYear(Person person, int year){
		this.person = person;
		this.year = year;
	}
	
	public void update(){
		int yearProgressive = 0;
//		List<PersonMonth> personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and " +
//				"pm.year = ? and pm.month between ? and ?", person, date.getYear()-1, DateTimeConstants.APRIL, DateTimeConstants.DECEMBER).fetch();
//		for(PersonMonth permon : personMonth){
//			yearProgressive = yearProgressive+permon.remainingHours;
//			
//		}
//		this.remainingHours = yearProgressive;
//		this.save();
	}
	
	
}
