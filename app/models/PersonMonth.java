package models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.envers.Audited;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.validation.Required;
import play.db.jpa.Model;

import lombok.Data;

@Audited
@Table(name="person_month")
@Entity
public class PersonMonth extends Model {
	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;	
	
	@Column
	public int year;
	@Column
	public int month;	
	
	@Column
	public int remainingHours;
	
	@Transient
	private int workingDays;
	@Transient
	private LocalDate date;
	
	//private Person person;
	@Transient
	private int daysAtWorkOnHoliday;
	@Transient
	private int daysAtWorkOnWorkingDays;
	@Transient
	private Integer workingHours;
	@Transient
	private int differenceHoursAtEndOfMonth;
	@Transient
	private int justifiedAbsence = 0;
	@Transient
	private int notJustifiedAbsence = 0;	
	@Transient
	private int mealTicketToRender;	
	@Transient
	public List<PersonMonth> persons = null;
	
	public PersonMonth(Person person, int year, int month){
		this.person = person;	
		this.year = year;
		this.month = month;
	}
	
	public PersonMonth(Person person, LocalDate date){
		this.person = person;	
		this.date = date;
	}
	
	public List<PersonMonth> getPersons(){
		if(persons != null){
			return persons;
		}
		persons = new ArrayList<PersonMonth>();
		List <Person> persone = Person.findAll();
		for(Person p : persone){
			persons.add(new PersonMonth(p, new LocalDate(date)));
		}
		
		return persons;
	}
	
	
	

}
