package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

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
	
	@Column(name = "remaining_vacation_days")
	public Integer remainingVacationDays;
	
	@Column(name = "remaining_minutes")
	public Integer remainingMinutes;
	

	public PersonYear(Person person, int year){
		this.person = person;
		this.year = year;
	}
				
}
