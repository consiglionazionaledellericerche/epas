package models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.joda.time.LocalDate;

import play.Logger;
import play.db.jpa.Model;

import lombok.Data;



@Data
public class PersonMonth extends Model{
	
	private int workingDays;
	
	private final LocalDate date;
	
	private Person person;

	private int daysAtWorkOnHoliday;
	
	private int daysAtWorkOnWorkingDays;
	
	private Integer workingHours;
	
	private int differenceHoursAtEndOfMonth;
	
	private Absence justifiedAbsence = null;
	
	private Absence notJustifiedAbsence = null;
	
	private int mealTicketToRender;
	
	public PersonMonth(LocalDate data){
		this.date= data;
	}
	
	/**
	 * calcola il numero di giorni lavorativi per il mese in questione.
	 * @return workingDays
	 */
	public int monthWorkingDays(){
		
		int month = date.getMonthOfYear();
		int year = date.getYear();
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		firstDayOfMonth.set(year, month, 1);
		for (int day = 1; day <= firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH); day++) {
			LocalDate newDate = new LocalDate(year,month,day);
			
			if(newDate.getDayOfWeek()!=6 || newDate.getDayOfWeek()!=7 
					|| (newDate.getMonthOfYear() != 12) && (newDate.getDayOfMonth() != 25)
					|| (newDate.getMonthOfYear() != 12) && (newDate.getDayOfMonth() != 26) 
					|| (newDate.getMonthOfYear() != 12) && (newDate.getDayOfMonth() != 8)
					|| (newDate.getMonthOfYear() != 6) && (newDate.getDayOfMonth() != 2) 
					|| (newDate.getMonthOfYear() != 4) && (newDate.getDayOfMonth() != 25)
					|| (newDate.getMonthOfYear() != 5) && (newDate.getDayOfMonth() != 1) 
					|| (newDate.getMonthOfYear() != 8) && (newDate.getDayOfMonth() != 15)
					|| (newDate.getMonthOfYear() != 1) && (newDate.getDayOfMonth() != 1) 
					|| (newDate.getMonthOfYear() != 1) && (newDate.getDayOfMonth() != 6)
					||(newDate.getMonthOfYear() != 11) && (newDate.getDayOfMonth() != 1))
				workingDays++;				
			
		}	
		
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
	 * calcola quanti giorni una persona ha lavorato in giorni festivi
	 * @return daysAtWorkOnHoliday
	 */
	public int workingDaysInHoliday(){
		
		List<PersonDay> pd = new ArrayList<PersonDay>();
		pd = PersonDay.find("Select pd from PersonDay pd where person = ? and date >= ? and date < ?", person, date, date.plusMonths(1)).fetch();
		for(PersonDay p : pd){
			List<Stamping> timbrature = p.getStampings();
			if(timbrature!=null){
				boolean festa = p.isHoliday();
				if (festa = true)
					daysAtWorkOnHoliday++;
			}
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
	
	/**
	 * calcola il numero di buoni pasto da restituire a partire dai giorni di presenza effettuati nel mese precedente con più di
	 * 6 ore e 30 minuti di presenza a lavoro (390 è il conteggio in minuti delle 6 ore e 30 necessarie a poter ottenere un buono mensa)
	 * @return mealTicketToRender
	 */
	public int mealTicketToRender(){
		List<PersonDay> pd = new ArrayList<PersonDay>();
		pd = PersonDay.find("Select pd from PersonDay pd where person = ? and date >= ? and date < ?", person, date, date.plusMonths(1)).fetch();
		for(PersonDay p : pd){
			if(p.timeAtWork() < 390)
				mealTicketToRender++;
		}
		
		return mealTicketToRender;
	}
	
	/**
	 * calcola il numero di assenze giustificate (con codice di assenza) fatte dalla persona nel periodo considerato
	 * @return numberOfJustifiedAbsence
	 */
	public int getJustifiedAbsence(){
		int numberOfJustifiedAbsence = 0;
		List<PersonDay> pd = new ArrayList<PersonDay>();
		pd = PersonDay.find("Select pd from PersonDay pd where person = ? and date >= ? and date < ?", person, date, date.plusMonths(1)).fetch();
		for(PersonDay p : pd){
			List <AbsenceType> listaAssenze = p.absenceList();
			numberOfJustifiedAbsence = listaAssenze.size();
		}
		return numberOfJustifiedAbsence;
	}
}
