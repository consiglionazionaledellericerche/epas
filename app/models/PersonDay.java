/**
 * 
 */
package models;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;

import lombok.Data;

import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.db.jpa.JPA;

/**
 * Classe che rappresenta un giorno, sia esso lavorativo o festivo di una persona.
 *  
 * @author cristian
 *
 */
@Data
public class PersonDay {

	public final LocalDate date;
	
	private final Person person;
	
	private List<Stamping> stampings = null;
	
	private Absence absence = null;
	
	/**
	 * Totale del tempo lavorato nel giorno in minuti 
	 */
	private Integer dailyTime;

	private int timeAtWork;

	private int progressive;

	private int difference;
	
	public PersonDay(Person person, LocalDate date) {
		this.person = person;
		this.date = date;
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
	
	public List<Stamping> getStampings(Person person, LocalDateTime date2) {
		if (stampings == null) {
			
			stampings = Stamping.find("SELECT s FROM Stamping s " +
					"WHERE s.person = ? and date between ? and ? " +
					"ORDER BY date", 
					person, date2, date2.plusDays(1)).fetch();
							
		}
		return stampings;
	}
	
	/**
	 * 
	 * @param data
	 * @return true se il giorno in questione è un giorno di festa. False altrimenti
	 */
	public boolean isHoliday(LocalDate date){
		if (date!=null){

			Logger.warn("Nel metodo isHoliday la data è: " +date);
			
			if(date.getDayOfWeek() == 7)
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
	 * @param date2
	 * @return numero di minuti in cui una persona è stata a lavoro in quella data
	 */
	public int timeAtWork(Person person, LocalDateTime date2){
		
		List<Stamping> listStamp = Stamping.find("select s from Stamping s " +
			    "where s.person = ? and s.date between ? and ? order by date", person, date2, date2.plusDays(1)).fetch();
		
		int size = listStamp.size();
		timeAtWork = 0;
		if(size%2 != 0){
			/**
			 * vuol dire che ci sono più timbrature di ingresso rispetto alle uscite o viceversa,
			 * come devo gestire questa cosa???
			 */
		}
		else{
			
			Iterator<Stamping> iter = listStamp.iterator();
			while(iter.hasNext()){
				Stamping s = iter.next();
				if(s.way == Stamping.WayType.in){
					timeAtWork -= toMinute(s.date);		
					System.out.println("Timbratura di ingresso: "+timeAtWork);
				}
				if(s.way == Stamping.WayType.out){
					timeAtWork += toMinute(s.date);
					System.out.println("Timbratura di uscita: "+timeAtWork);
				}
			}
		}
		System.out.println("Totale: "+timeAtWork);
		return timeAtWork;
		
	}
	
	/**
	 * 
	 * @param date
	 * @return il progressivo delle ore in più o in meno rispetto al normale orario previsto per quella data
	 */
	public int progressive(LocalDate date){
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
	public int toMinute(LocalDateTime date){
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
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public List<AbsenceType> absenceList() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		
		List<AbsenceType> listaAssenze = new ArrayList<AbsenceType>();
		
		listaAssenze = AbsenceType.find("SELECT abt FROM AbsenceType abt, Absence abs, Person p " +
				"WHERE abs.person = p AND abs.absenceType = abt AND p = ? AND abs.date = ?", person, date).fetch();
		
		if(listaAssenze != null){
			Iterator iter = listaAssenze.iterator();
			while(iter.hasNext()){
				AbsenceType abt = (AbsenceType) iter.next(); 
				Logger.warn("Codice: " +abt.code);
			    //System.out.print("Codice: " +abt.code );
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
	public boolean mealTicket(LocalDateTime date, int timeAtWork, Person person){
		boolean isMealTicketAvailable;
		if(timeAtWork == 0){
			isMealTicketAvailable = false;
		}
			
		else{
			timeAtWork = timeAtWork(person, date);
			isMealTicketAvailable = true;
		}
		return isMealTicketAvailable;
	}
	
	public int difference(){
		if(difference == 0){
			
		}
		return difference;
	}
	
	public String workingTimeType(Person person){
		
		String description = new String();
		WorkingTimeType wtt = WorkingTimeType.find("Select wtt from Person p, WorkingTimeType wtt " +
				"where p.workingTimeType = wtt and p = ?", person).first();
				
		description = wtt.description;
		return description;
	}
	
	
	public int mealTicketToUse(){
		return 0;
		
	}
	
	public int mealTicketToReturn(){
		return 0;
	}
	
	public int officeWorkingDay(){
		return 0;
	}
	
	
}
