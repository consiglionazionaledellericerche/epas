/**
 * 
 */
package models;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Data;

import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.db.jpa.Model;

/**
 * Classe che rappresenta un giorno, sia esso lavorativo o festivo di una persona.
 *  
 * @author cristian
 *
 */
public class PersonDay extends Model {

	private final Person person;
	
	public final LocalDate date;
	
	public final LocalDateTime startOfDay;
	public final LocalDateTime endOfDay;
	
	private List<Stamping> stampings = null;
	
	private Absence absence = null;
	
	
	/**
	 * Totale del tempo lavorato nel giorno in minuti 
	 */


	private Integer timeAtWork;

	private static int progressive = 0;

	public int difference;
	
	public PersonDay(Person person, LocalDate date) {
		this.person = person;
		this.date = date;
		this.startOfDay = new LocalDateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 0, 0);
		this.endOfDay = new LocalDateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 23, 59);
	}
	
	/**	 
	 * Calcola se un giorno è lavorativo o meno. L'informazione viene calcolata a partire
	 * dal giorno e dal WorkingTimeType corrente della persona
	 * 
	 * @return true se il giorno corrente è un giorno lavorativo, false altrimenti.
	 */
	public boolean isWorkingDay() {
		boolean isWorkingDay = false;
	
		WorkingTimeTypeDay wttd2 = WorkingTimeTypeDay.find("Select wttd from WorkingTimeType wtt, WorkingTimeTypeDay wttd, Person p " +
				"where p.workingTimeType = wtt and wttd.workingTimeType = wtt and p = ? and wttd.dayOfWeek = ? ", person, date.getDayOfWeek()).first();
		Logger.warn("In isWorkingDay il giorno chiamato è: " +date.getDayOfWeek() +" mentre la persona è: " +person.id);
		isWorkingDay = wttd2.holiday;
		return isWorkingDay;
	}
	
	public boolean isAbsent() {
		if (getAbsence() != null) {
			if (!stampings.isEmpty()) {
				Logger.warn("Attenzione il giorno %s è presente un'assenza (%s) (Person = %s), però ci sono anche %d timbrature.", 
					absence, absence.person, stampings.size());
			}
		}
		return absence != null;
	}
	
	public Absence getAbsence() {
		if (absence == null) {
			
			absence = Absence.find("Select abs from Absence abs where abs.person = ? " +
					" and abs.date = ? ", person, date).first();
			
		}
		return absence;
	}
	
	/**
	 * 
	 * @return la lista di timbrature giornaliere
	 */
	public List<Stamping> getStampings() {
		if (stampings == null) {
			
			stampings = Stamping.find("SELECT s FROM Stamping s " +
					"WHERE s.person = ? and date between ? and ? " +
					"ORDER BY date", person, startOfDay, endOfDay).fetch();		
			
			
		}
		return stampings;
	}
	
	
	/**
	 * 
	 * @param data
	 * @return true se il giorno in questione è un giorno di festa. False altrimenti
	 */
	public boolean isHoliday(){
		if (date!=null){

			Logger.warn("Nel metodo isHoliday la data è: " +date);
			
			if((date.getDayOfWeek() == 7)||(date.getDayOfWeek() == 6))
				return true;		
			if((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 25))
				return true;
			if((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 26))
				return true;
			if((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 8))
				return true;
			if((date.getMonthOfYear() == 6) && (date.getDayOfMonth() == 2))
				return true;
			if((date.getMonthOfYear() == 4) && (date.getDayOfMonth() == 25))
				return true;
			if((date.getMonthOfYear() == 5) && (date.getDayOfMonth() == 1))
				return true;
			if((date.getMonthOfYear() == 8) && (date.getDayOfMonth() == 15))
				return true;
			if((date.getMonthOfYear() == 1) && (date.getDayOfMonth() == 1))
				return true;
			if((date.getMonthOfYear() == 1) && (date.getDayOfMonth() == 6))
				return true;
			if((date.getMonthOfYear() == 11) && (date.getDayOfMonth() == 1))
				return true;			
		}
		return false;
	}
	
	/**
	 * 
	 * @param date
	 * @return numero di minuti in cui una persona è stata a lavoro in quella data
	 */
	public int timeAtWork(){
		
		List<Stamping> listStamp = Stamping.find("select s from Stamping s " +
 		    "where s.person = ? and s.date between ? and ? order by date", person, startOfDay, endOfDay).fetch();
		for(Stamping s : listStamp){
			System.out.println("gli elementi della lista di timbrature: " +s.date);
		}
		int size = listStamp.size();
		timeAtWork = 0;
		
		if(((size / 2 == 1) && (size % 2 == 1)) || ((size / 2 == 0) && (size % 2 == 1))){
			LocalDateTime ora = new LocalDateTime();
			//ora.now();
			int nowToMinute = toMinute(ora);
			int orelavoro=0;
			for(Stamping s : listStamp){
				if(s.way == Stamping.WayType.in)
					orelavoro -= toMinute(s.date);				
				if(s.way == Stamping.WayType.out)
					orelavoro += toMinute(s.date);
				if(orelavoro < 0)
					timeAtWork = nowToMinute + orelavoro;
				else 
					timeAtWork = nowToMinute - orelavoro;
				
			}
			return timeAtWork;	
		}		
		
		else{
			int orealavoro=0;
			for(Stamping s : listStamp){
				if(s.way == Stamping.WayType.in){
					orealavoro -= toMinute(s.date);
					//timeAtWork -= toMinute(s.date);		
					System.out.println("Timbratura di ingresso: "+orealavoro);	
				}
				if(s.way == Stamping.WayType.out){
					orealavoro += toMinute(s.date);
					//timeAtWork += toMinute(s.date);
					System.out.println("Timbratura di uscita: "+orealavoro);
				}
				timeAtWork = orealavoro;
			}
			return timeAtWork;
		}
		
	}
	
	/**
	 * 
	 * @param date
	 * @return il progressivo delle ore in più o in meno rispetto al normale orario previsto per quella data
	 */
	public int getProgressive(){
		if(progressive == 0){
			if(date != null){
			
				if((date.getDayOfMonth()==1) && (date.getDayOfWeek()==6 || date.getDayOfWeek()==7))
					return 0;			
				if((date.getDayOfMonth()==2) && (date.getDayOfWeek()==7))
					return 0;
				else{
					progressive = progressive+difference;
				}
			}
		}
		
		return progressive;
	}
	
	
	/**
	 * 
	 * @param date
	 * @return calcola il numero di minuti di cui è composta la data passata come parametro (di cui considera solo
	 * ora e minuti
	 */
	private static int toMinute(LocalDateTime date){
		int dateToMinute = 0;
		
		if (date!=null){
			int hour = date.get(DateTimeFieldType.hourOfDay());
			int minute = date.get(DateTimeFieldType.minuteOfHour());
			
			dateToMinute = (60*hour)+minute;
		}
		return dateToMinute;
	}
	
	/**
	 * 
	 * @param person
	 * @param date
	 * @return la lista di codici di assenza fatti da quella persona in quella data
	 */
	public List<Absence> absenceList() {
		
		List<Absence> listaAssenze = new ArrayList<Absence>();
		listaAssenze = AbsenceType.find("SELECT abs FROM Absence abs " +
				" WHERE abs.person = ? AND abs.date = ?", person, date).fetch();

		if(listaAssenze != null){
			for (Absence abs : listaAssenze) {
				Logger.warn("Codice: " +abs.absenceType.code);
			}
		}
		else
			Logger.warn("Non ci sono assenze" );
	
		return listaAssenze;
		
	}
	
	/**
	 * 
	 * @param date
	 * @param timeAtWork
	 * @param person
	 * @return se la persona può usufruire del buono pasto per quella data
	 */
	public boolean mealTicket(){
		boolean isMealTicketAvailable = false;
		if (timeAtWork == null) {
			timeAtWork = timeAtWork();
			
		}
		if(timeAtWork == 0){
			isMealTicketAvailable = false;
		}
		else{
			if(person.workingTimeType.description.equals("normale-mod") && timeAtWork>=432)
				isMealTicketAvailable=true;
			
		}
		return isMealTicketAvailable;

	}
	
	/**
	 * 
	 * @return la differenza tra l'orario di lavoro giornaliero e l'orario standard in minuti
	 */
	public int getDifference(){
		if(date != null){
			if((date.getDayOfMonth()==1) && (date.getDayOfWeek()==6 || date.getDayOfWeek()==7))
				return 0;			
			if((date.getDayOfMonth()==2) && (date.getDayOfWeek()==7))
				return 0;
			if((date.getDayOfWeek()==7) || date.getDayOfWeek()==6)
				return 0;
			List<Absence> listaAssenze = absenceList();
			List<Stamping> listaTimbrature = getStampings();
			if(listaAssenze.size()!=0){
				difference = 0;
			}
			if(listaTimbrature.size()==0){
				difference = 0;
			}
			else{
				int minTimeWorking = 432;
				timeAtWork = timeAtWork();
				difference = timeAtWork-minTimeWorking;
				//return difference;
			}
				
		}
		
		return difference;
	}
	
}
