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
@Table(name="person_months")
@Entity
public class PersonMonth extends Model {
	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;	
	
	@Column
	public Integer year;
	@Column
	public Integer month;	
	
	@Column
	public Integer remainingHours;
	
	@Column
	public Integer compensatoryRest;
	
	@Transient
	private LocalDate date;
	
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
	public int getResidualFromPastMonth(){
		int residual = 0;
		if(month < 4){
			List<PersonMonth> pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month < ? " +
					"and pm.year = ?", person, month, year).fetch();			
			
			for(PersonMonth personMonth : pm){
				residual = residual+personMonth.remainingHours;
			}
			PersonYear py = PersonYear.find("Select py from PersonYear py where py.person = ? and py.year = ?", person, year-1).first();
			residual = residual + py.remainingHours;
		}
		else{
			List<PersonMonth> pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month >=  ? and pm.month < ?" +
					" and pm.year = ?", person, 4, month, year).fetch();
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
	public int getMonthResidual(){
		int residual = 0;
		LocalDate date = new LocalDate();
		
		if(month == date.getMonthOfYear() && year == date.getYear()){
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date < ? and pd.progressive != ? " +
					"order by pd.date desc", person, date, 0).first();
			if(pd == null){
				pd = new PersonDay(person, date.minusDays(1));
			}
			residual = pd.progressive;
		}
		else{
			LocalDate hotDate = new LocalDate(year,month,1).dayOfMonth().withMaximumValue();
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, hotDate).first();
			residual = pd.progressive;
		}
		return residual;
	}
	
	/**
	 * 
	 * @param month
	 * @param year
	 * @return il numero di giorni di riposo compensativo utilizzati in quel mese 
	 */
	public int getCompensatoryRest(){
		if (compensatoryRest != null) {
			return compensatoryRest;
		}
		
		compensatoryRest = 0;
		LocalDate beginMonth = new LocalDate(year, month, 1);

		return ((Long) Absence.find("Select count(abs) from Absence abs where abs.person = ? and abs.date between ? and ? and abs.absenceType.code = ?", 
				person, beginMonth, beginMonth.dayOfMonth().withMaximumValue(), "91").first()).intValue();
		
	}
	
	/**
	 * 
	 * @return il numero di minuti residui dell'anno precedente per quella persona
	 */
	public int getResidualPastYear(){
		
		int residual = 0;
		PersonYear py = PersonYear.findById(person);
		residual = py.remainingHours;
		return residual;
	}
	
	/**
	 * 
	 * @param month
	 * @param year
	 * @return il totale derivante dalla differenza tra le ore residue e le eventuali ore di riposo compensativo
	 */
	public int getTotalOfMonth(){
		int total = 0;
		int compensatoryRest = getCompensatoryRest();
		int monthResidual = getMonthResidual();
		int residualFromPastMonth = getResidualFromPastMonth();
		total = residualFromPastMonth+monthResidual-(compensatoryRest*432); //numero di giorni di riposo compensativo moltiplicati 
		//per il numero di minuti presenti in 7 ore e 12 minuti, ovvero il tempo di lavoro.
		
		return total;
	}
	
	
	

}
