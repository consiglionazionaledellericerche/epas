/**
 * 
 */
package models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
		EntityManager em = JPA.em();
		WorkingTimeTypeDay wttd = em.createQuery("SELECT wttd FROM Person p JOIN p.workingTimeType wtt JOIN wtt.workingTimeTypeDay wttd " + 
				"WHERE p.id = :personId AND wttd.dayOfWeek = :day", WorkingTimeTypeDay.class)
				.setParameter("personId", person.id)
				.setParameter("day", date.getDayOfWeek())
				.getSingleResult();
		return !wttd.holiday;
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
			//si calcola prendendola dal db
		}
		return absence;
	}
	
	public List<Stamping> getStampings() {
		if (stampings == null) {
			EntityManager em = JPA.em();

			stampings = 
				em.createQuery("SELECT s FROM Stamping WHERE s.person = :person and date >= :startDate and date < endDate ORDER BY date", 
					Stamping.class)
				.setParameter("person", person)
				.setParameter("startDate", date.toDateMidnight())
				.setParameter("endDate", date.plusDays(1).toDateMidnight())
				.getResultList();
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
	 * @param date
	 * @return numero di minuti in cui una persona è stata a lavoro in quella data
	 */
	public int timeAtWork(Person person, LocalDate date){
		
		List<Stamping> listStamp = Stamping.find("select s from Stamping s " +
			    "where s.person = ? and s.date = ? order by date", person, date).fetch();
		
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
		if(date != null){
		
			if((date.getDayOfMonth()==1) && (date.getDayOfWeek()==6 || date.getDayOfWeek()==7))
				return 0;			
			if((date.getDayOfMonth()==2) && (date.getDayOfWeek()==7))
				return 0;
			else{
				progressive = progressive+difference;
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
	public List<String> absenceList(Person person, Date date) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		
		List<String> listaAssenze = new ArrayList<String>();
		EntityManager em = JPA.em();
		listaAssenze = em.createQuery("Select code from Absence, AbsenceType" +
				"where Absence.absenceType = absenceType and Absence.person = :person and Absence.date = :date")
				.setParameter("person", person)
				.setParameter("date", date)
				.getResultList();
		//Connection mypostgresCon = getMyPostgresConnection();
		return listaAssenze;
		
	}
	
	/**
	 * 
	 * @param date
	 * @param timeAtWork
	 * @param person
	 * @return se la persona può usufruire del buono pasto per quella data
	 */
	public boolean mealTicket(LocalDate date, int timeAtWork, Person person){
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
