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
	
	/**
	 * @param actualMonth, actualYear
	 * @return la somma dei residui mensili passati fino a questo momento; se siamo in un mese prima di aprile i residui da calcolare 
	 * sono su quello relativo all'anno precedente + i residui mensili fino a quel mese; se siamo in un mese dopo aprile, invece,
	 * i residui da considerare sono solo quelli da aprile fino a quel momento
	 */
	public int getResidualFromPastMonth(int actualMonth, int actualYear){
		int residual = 0;
		if(actualMonth <= 4){
			List<PersonMonth> pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month < ? and pm.year = ?" +
					" or pm.year = ? and pm.month = ? ", person, actualMonth, actualYear, actualYear-1, 12).fetch();
			for(PersonMonth personMonth : pm){
				residual = residual+personMonth.remainingHours;
			}
		}
		else{
			List<PersonMonth> pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month >=  ? and pm.month < ?" +
					" and pm.year = ?", person, 4, actualMonth, actualYear).fetch();
			for(PersonMonth personMonth : pm){
				residual = residual+personMonth.remainingHours;
			}
		}
		return residual;
	}
	
	/**
	 * 
	 * @param month, year
	 * @return il residuo di ore all'ultimo giorno del mese se visualizzo un mese passato, al giorno attuale se visualizzo il mese
	 * attuale
	 */
	public int getMonthResidual(int month, int year){
		int residual = 0;
		LocalDate date = new LocalDate();
		
		if(month == date.getMonthOfYear() && year == date.getYear()){
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date.minusDays(1)).first();
			if(pd == null){
				pd = new PersonDay(person, date.minusDays(1));
			}
			residual = pd.progressive;
		}
		else{
			int day = 0;
			if(month==1 || month==3 || month==5 || month==7 || month==8 ||	month==10 || month==12)
				day = 31;
			if(month==4 || month==6 || month==9 || month==11)
				day = 30;
			if(month==2){
				if(year==2008 || year==2012 || year==2016 || year== 2020)
					day = 29;
				else 
					day = 28;
			}
			LocalDate hotDate = new LocalDate(year,month,day);
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, hotDate).first();
			residual = pd.progressive;
		}
		return residual;
	}
	
	
	

}
