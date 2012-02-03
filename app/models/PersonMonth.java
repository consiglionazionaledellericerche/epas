package models;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import play.db.jpa.Model;

import lombok.Data;



@Data
public class PersonMonth extends Model{
	
	public int workingDays;
	
	public final LocalDate date;
	
	private List<Person> person;

	private int daysAtWorkOnHoliday;
	
	private int daysAtWorkOnWorkingDays;
	
	private Integer workingHours;
	
	private int differenceHoursAtEndOfMonth;
	
	private Absence justifiedAbsence = null;
	
	private Absence notJustifiedAbsence = null;
	
	private int mealTicketToRender;
	
	public PersonMonth(LocalDate data){
		this.date = data;
	}
	
	/**
	 * calcola il numero di giorni lavorativi per il mese in questione.
	 * @return workingDays
	 */
	public int monthWorkingDays(){
		
		
		return workingDays;
	}
	/**
	 * calcola il numero di giorni lavorativi che la persona ha avuto nel mese in questione a partire dalle timbrature giornaliere.
	 * @return workingDays
	 */
	public int getWorkingDays(){
		
		int giorniLavoro = 0;
		List<PersonDay> pd = new ArrayList<PersonDay>();
		pd = PersonDay.find("Select pd from PersonDay pd where person = ? and date >= ? and date < ?", person, date, date.plusMonths(1)).fetch();
		for(PersonDay p : pd){
			List<Stamping> timbrature = p.getStampings();
			if(timbrature != null){
				giorniLavoro++;
			}
			daysAtWorkOnWorkingDays = giorniLavoro;
		}		
		return daysAtWorkOnWorkingDays;
	}
	/**
	 * query sull'oggetto Person per ritornare tutti i dipendenti 
	 * @return List<Person>
	 */
	public List<Person> getPersons(){
		if (person == null){
			
			person = Person.findAll();
		}
		
		return person;
		
	}
	/**
	 * calcola quanti giorni una persona ha lavorato in giorni festivi
	 * @return daysAtWorkOnHoliday
	 */
	public int workingDaysInHoliday(){
		
		List<PersonDay> pd = new ArrayList<PersonDay>();
		pd = PersonDay.find("Select pd from PersonDay pd where person = ? and date >= ? and date < ?", person, date, date.plusMonths(1)).fetch();
		for(PersonDay p : pd){
			//p.
		}
				
				
		return daysAtWorkOnHoliday;
	}
	
	/**
	 * calcola, a partire dalla lista di PersonDay per il mese in questione, le ore effettivamente lavorate in quel mese.
	 * @return workingHours
	 */
	public int monthHoursRecap(){
		
		if(workingHours == null){
			List<PersonDay> pd = new ArrayList<PersonDay>();
			pd = PersonDay.find("Select pd from PersonDay pd where person = ? and date >= ? and date < ?", person, date, date.plusMonths(1)).fetch();
			for(PersonDay p : pd){
				workingHours += p.timeAtWork();
			}
		}
		return workingHours;
	}
	
	//public Absence 
}
